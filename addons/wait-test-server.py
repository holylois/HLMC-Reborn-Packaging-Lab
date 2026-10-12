"""Wait for the private server's ready line and loopback socket; fail before opening a disconnected client."""
import socket
import sys
import time
from pathlib import Path


def status(text):
    if 'Failed to start the minecraft server' in text or 'Failed to bind to port' in text:
        return 'failed'
    if 'Done (' in text and 'For help, type' in text:
        return 'ready'
    return 'waiting'


def wait(log, timeout=180):
    started=time.time()
    deadline=time.monotonic()+timeout
    while time.monotonic()<deadline:
        # Ignore the previous run's ready line until this launch has written a fresh log.
        if log.exists() and log.stat().st_mtime>=started-2:
            state=status(log.read_text(encoding='utf-8',errors='replace'))
            if state=='failed':
                print(f'Test server failed to start. See {log}. Minecraft was not opened.')
                return 1
            if state=='ready':
                try:
                    with socket.create_connection(('127.0.0.1',25565),timeout=1):
                        print('Private test server ready on localhost:25565.')
                        return 0
                except OSError:
                    pass
        time.sleep(1)
    print(f'Test server did not become ready within {timeout} seconds. See {log}. Minecraft was not opened.')
    return 1


def check_free():
    try:
        with socket.create_connection(('127.0.0.1',25565),timeout=1):
            print('Local port 25565 is already in use. Stop the old private test server before starting another one.')
            return 1
    except OSError:
        return 0


def check_running(log):
    if log.exists() and status(log.read_text(encoding='utf-8',errors='replace'))=='ready':
        try:
            with socket.create_connection(('127.0.0.1',25565),timeout=1):
                return 0
        except OSError:
            pass
    print('The private test server is not ready. Use Test-Holy-Lois.cmd to start both server and client.')
    return 1


if __name__=='__main__':
    log=Path(__file__).resolve().parent/'run/server/logs/latest.log'
    raise SystemExit(check_free() if '--check-free' in sys.argv else check_running(log) if '--check-running' in sys.argv else wait(log))
