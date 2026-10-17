namespace HolyLois.Core;

public interface IFileDownloader
{
    Task<string> GetAsync(PackFile file, IProgress<long>? progress, CancellationToken cancellationToken);
}

/// <summary>A download that kept failing after its retries (offline, timed out or damaged in transit).</summary>
public sealed class DownloadFailedException(string message, Exception? inner = null) : IOException(message, inner);

public sealed class DownloadCache(string root, HttpClient client) : IFileDownloader
{
    public static TimeSpan StallTimeout { get; set; } = TimeSpan.FromSeconds(30);

    public StorageCleanupResult RemoveInstalledCopies(IEnumerable<PackFile> approved, string instanceRoot)
    {
        var result = StorageCleanupResult.Empty;
        if (!Directory.Exists(root)) return result;
        foreach (var file in approved)
        {
            try
            {
                var cached = SafePaths.Resolve(root, file.Sha256.ToLowerInvariant() + ".verified");
                var installed = SafePaths.Resolve(instanceRoot, file.Path);
                if (!AtomicFiles.Matches(installed, file) || !AtomicFiles.Matches(cached, file)) continue;
                File.Delete(cached); result += new StorageCleanupResult(1, file.Size);
            }
            catch (Exception ex) when (ex is IOException or UnauthorizedAccessException) { }
        }
        return result;
    }
    public void Prune(IEnumerable<PackFile> approved)
    {
        if (!Directory.Exists(root)) return;
        SafePaths.RejectLinks(root);
        var keep = approved.Select(f => f.Sha256 + ".verified").ToHashSet(StringComparer.OrdinalIgnoreCase);
        foreach (var path in Directory.GetFiles(root, "*.verified"))
        {
            var name = Path.GetFileName(path);
            if (!System.Text.RegularExpressions.Regex.IsMatch(name, @"^[0-9a-fA-F]{64}\.verified$") || keep.Contains(name)) continue;
            SafePaths.RejectLinks(path); File.Delete(path);
        }
    }

    public async Task<string> GetAsync(PackFile file, IProgress<long>? progress, CancellationToken cancellationToken)
    {
        ManifestSecurity.ValidateDownloadUri(file.Url);
        Directory.CreateDirectory(root);
        var target = SafePaths.Resolve(root, file.Sha256.ToLowerInvariant() + ".verified");
        if (AtomicFiles.Matches(target, file)) { progress?.Report(file.Size); return target; }
        Exception? last = null;
        for (var attempt = 0; attempt < 3; attempt++)
        {
            var partial = target + "." + Guid.NewGuid().ToString("N") + ".part";
            // A stalled connection (open, but no bytes) is retried instead of waiting until the player cancels.
            using var stall = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
            try
            {
                stall.CancelAfter(StallTimeout);
                using var response = await client.GetAsync(file.Url, HttpCompletionOption.ResponseHeadersRead, stall.Token);
                response.EnsureSuccessStatusCode();
                ManifestSecurity.ValidateDownloadUri(response.RequestMessage?.RequestUri?.AbsoluteUri ?? file.Url);
                if (response.Content.Headers.ContentLength is long size && size != file.Size)
                    throw new InvalidDataException("Download size differs from the approved release.");
                await using var input = await response.Content.ReadAsStreamAsync(stall.Token);
                await using (var output = new FileStream(partial, FileMode.CreateNew, FileAccess.Write, FileShare.None, 81920, true))
                {
                    var buffer = new byte[81920]; long total = 0; int count;
                    stall.CancelAfter(StallTimeout);
                    while ((count = await input.ReadAsync(buffer, stall.Token)) > 0)
                    {
                        stall.CancelAfter(StallTimeout);
                        total += count;
                        if (total > file.Size) throw new InvalidDataException("Download exceeded the approved size.");
                        await output.WriteAsync(buffer.AsMemory(0, count), cancellationToken);
                        progress?.Report(total);
                    }
                    await output.FlushAsync(cancellationToken);
                }
                if (!AtomicFiles.Matches(partial, file)) throw new InvalidDataException("Download checksum failed. Your installed pack is unchanged.");
                File.Move(partial, target, true);
                return target;
            }
            // Timeouts surface as cancellations; only the player's own cancel stops the retries.
            catch (Exception ex) when (ex is HttpRequestException or IOException or InvalidDataException or OperationCanceledException && !cancellationToken.IsCancellationRequested)
            {
                last = ex;
                if (attempt < 2) await Task.Delay(TimeSpan.FromSeconds(attempt + 1), cancellationToken);
            }
            finally { if (File.Exists(partial)) File.Delete(partial); }
        }
        throw new DownloadFailedException("A pack file could not be downloaded safely. Retry when your connection is available.", last);
    }
}
