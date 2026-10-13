"""Check the private Minecraft server itself; rotated logs must not block reopening the client."""
import json
import socket
import struct
import sys
import time
from pathlib import Path


def status(text):
    if 'Failed to start the minecraft server' in text or 'Failed to bind to port' in text:
        return 'failed'
    if 'Done (' in text and 'For help, type' in text:
        return 'ready'
    return 'waiting'


def varint(value):
    result=bytearray()
    while True:
        byte=value & 127
        value >>= 7
        result.append(byte | (128 if value else 0))
        if not value:
            return bytes(result)


def read_exact(stream,length):
    result=bytearray()
    while len(result)<length:
        chunk=stream.recv(length-len(result))
        if not chunk:
            raise OSError('Incomplete Minecraft status reply')
        result.extend(chunk)
    return bytes(result)


def read_varint(stream):
    value=0
    for shift in range(0,35,7):
        byte=read_exact(stream,1)[0]
        value |= (byte & 127) << shift
        if not byte & 128:
            return value
    raise OSError('Invalid Minecraft status length')


def minecraft_ready():
    try:
        with socket.create_connection(('127.0.0.1',25565),timeout=5) as stream:
            host=b'localhost'
            hello=b'\x00'+varint(0)+varint(len(host))+host+struct.pack('>H',25565)+b'\x01'
            stream.sendall(varint(len(hello))+hello+b'\x01\x00')
            length=read_varint(stream)
            if not 2<length<1024**2 or read_varint(stream)!=0:
                return False
            count=read_varint(stream)
            if not 0<count<=length-2:
                return False
            reply=json.loads(read_exact(stream,count))
            # Do not log or retain the optional player-name sample.
            online=reply['players']['online']
            version=reply['version']['name']
            return type(online) is int and online>=0 and isinstance(version,str) and bool(version)
    except (OSError,ValueError,KeyError,TypeError):
        return False


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
            if state=='ready' and minecraft_ready():
                print('Private test server ready on localhost:25565.')
                return 0
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
    # A long-running server can rotate/truncate latest.log after its original Done line.
    if minecraft_ready():
        print('Private test server ready on localhost:25565.')
        return 0
    print('The private test server is not ready. Use Test-Holy-Lois.cmd to start both server and client.')
    return 1


if __name__=='__main__':
    log=Path(__file__).resolve().parent/'run/server/logs/latest.log'
    raise SystemExit(check_free() if '--check-free' in sys.argv else check_running(log) if '--check-running' in sys.argv else wait(log))
