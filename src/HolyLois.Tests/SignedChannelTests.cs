using HolyLois.Core;
using System.Net;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

internal static class SignedChannelTests
{
    public static async Task RunAsync()
    {
        using var rsa = RSA.Create(2048);
        var file = new PackFile("mods/test.jar", "https://github.com/pjampjam/HLMC-Reborn/releases/download/pack-v2.0.1/test.jar", 1, new string('a',64));
        var defaults = file with { Path = "defaults.zip", Policy = "seed" };
        var note = new ReleaseNote("2.0.0", "2026-10-12", "Last announcement", [], [], []);
        var quiet = new PackManifest(1, "2.0.1", "26.3", "0.19.5", 25, ServerAddress.Public, [file], [], defaults, [note]);
        var normal = quiet with { Version = "2.0.0" };
        Check(quiet.SilentPatch && !normal.SilentPatch, "Signed history did not distinguish a quiet patch.");
        var json = JsonSerializer.SerializeToUtf8Bytes(quiet, JsonSettings.Options);
        Check(!Encoding.UTF8.GetString(json).Contains("silentPatch"), "Quiet policy added an incompatible wire field.");
        Check(JsonSerializer.Deserialize<PackManifest>(json,JsonSettings.Options)!.SilentPatch, "Legacy-compatible signed quiet policy did not round trip.");
        var signature = rsa.SignData(json, HashAlgorithmName.SHA256, RSASignaturePadding.Pss);
        using var web = new ChannelWeb(PackFeed.StableUrl, json, signature);
        using var http = new HttpClient(web);
        var feed = new PackFeed(http,rsa.ExportSubjectPublicKeyInfoPem());
        var first = await feed.FetchAsync(CancellationToken.None);
        Check(feed.Accept(first, normal).SilentPatch, "Verified quiet update was rejected.");
        _ = await feed.FetchAsync(CancellationToken.None);
        FreshPairs(web.Requests);
        signature[0] ^= 1;
        await Reject(() => { feed.Accept(new(json,signature),normal); return Task.CompletedTask; });
        Check(normal.Version == "2.0.0", "Invalid metadata changed the active release.");

        var catalog = Encoding.ASCII.GetBytes("holylois-app-v1\n1.4.4\nhttps://github.com/" + AppReleasePolicy.Repository +
            "/releases/download/v1.4.4/HolyLoisReborn.exe\n"+new string('b',64)+"\n2048\n");
        var appSignature = rsa.SignData(catalog,HashAlgorithmName.SHA256,RSASignaturePadding.Pss);
        using var appWeb = new ChannelWeb(AppReleasePolicy.FeedUrl,catalog,appSignature);
        using var appHttp = new HttpClient(appWeb);
        var appFeed = new AppReleaseFeed(appHttp,rsa.ExportSubjectPublicKeyInfoPem());
        _ = await appFeed.FetchAsync(CancellationToken.None); _ = await appFeed.FetchAsync(CancellationToken.None);
        FreshPairs(appWeb.Requests);
        appWeb.ContentBytes = new byte[65537];
        await Reject(() => appFeed.FetchAsync(CancellationToken.None));
        web.ContentBytes = new byte[2*1024*1024+1];
        await Reject(() => feed.FetchAsync(CancellationToken.None));
        Console.WriteLine("PASS Fresh paired pack/app requests, unchanged signatures/limits and compatible quiet policy");
    }
    private static void FreshPairs(List<(Uri Uri,bool NoCache,bool NoStore)> requests)
    {
        Check(requests.Count==4 && requests.All(r=>r.NoCache && r.NoStore && r.Uri.Query.StartsWith("?hl=",StringComparison.Ordinal)), "Channel requests did not bypass cached metadata.");
        Check(requests[0].Uri.Query==requests[1].Uri.Query && requests[2].Uri.Query==requests[3].Uri.Query
            && requests[0].Uri.Query!=requests[2].Uri.Query, "A signature pair must share one fresh check identity.");
        Check(requests[1].Uri.AbsolutePath==requests[0].Uri.AbsolutePath+".sig", "Signature URL changed outside the channel.");
    }
    private static void Check(bool value,string message) { if(!value)throw new Exception(message); }
    private static async Task Reject(Func<Task> work) { try { await work(); } catch(InvalidDataException) { return; } throw new Exception("Unsafe metadata accepted."); }
    private sealed class ChannelWeb(string url, byte[] content, byte[] signature) : HttpMessageHandler
    {
        public byte[] ContentBytes = content;
        public readonly List<(Uri Uri,bool NoCache,bool NoStore)> Requests = [];
        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request,CancellationToken token)
        {
            var uri=request.RequestUri!;
            Check(uri.GetLeftPart(UriPartial.Path)==url || uri.GetLeftPart(UriPartial.Path)==url+".sig", "Unexpected metadata destination.");
            Requests.Add((uri,request.Headers.CacheControl?.NoCache==true,request.Headers.CacheControl?.NoStore==true));
            return Task.FromResult(new HttpResponseMessage(HttpStatusCode.OK) { RequestMessage=request,
                Content=new ByteArrayContent(uri.AbsolutePath.EndsWith(".sig",StringComparison.Ordinal)?signature:ContentBytes) });
        }
    }
}
