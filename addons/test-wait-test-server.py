"""The private test launcher must not open a client after its server fails."""
import importlib.util
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import MagicMock, patch

spec=importlib.util.spec_from_file_location('wait_test_server',Path(__file__).with_name('wait-test-server.py'))
server=importlib.util.module_from_spec(spec)
spec.loader.exec_module(server)


class ReadinessTest(unittest.TestCase):
    def test_startup_failure(self):
        with tempfile.TemporaryDirectory() as directory:
            log=Path(directory)/'latest.log'
            log.write_text('Failed to start the minecraft server\n',encoding='utf-8')
            with patch.object(server,'minecraft_ready') as connection:
                self.assertEqual(server.wait(log),1)
                connection.assert_not_called()

    def test_ready_needs_a_socket(self):
        with tempfile.TemporaryDirectory() as directory:
            log=Path(directory)/'latest.log'
            log.write_text('Done (12.5s)! For help, type "help"\n',encoding='utf-8')
            with patch.object(server,'minecraft_ready',return_value=True) as connection:
                self.assertEqual(server.wait(log),0)
                connection.assert_called_once_with()

    def test_previous_log_is_not_ready(self):
        self.assertEqual(server.status('Starting minecraft server version 26.3'),'waiting')
        with tempfile.TemporaryDirectory() as directory:
            log=Path(directory)/'latest.log'
            log.write_text('Done (12.5s)! For help, type "help"\n',encoding='utf-8')
            os.utime(log,(server.time.time()-60,server.time.time()-60))
            with patch.object(server.time,'monotonic',side_effect=[0,0,181]),patch.object(server.time,'sleep'):
                self.assertEqual(server.wait(log),1)

    def test_existing_listener_blocks_second_launch(self):
        with patch.object(server.socket,'create_connection',return_value=MagicMock()):
            self.assertEqual(server.check_free(),1)
        with patch.object(server.socket,'create_connection',side_effect=ConnectionRefusedError):
            self.assertEqual(server.check_free(),0)

    def test_client_only_requires_ready_server(self):
        with tempfile.TemporaryDirectory() as directory:
            log=Path(directory)/'latest.log'
            log.write_text('Done (12.5s)! For help, type "help"\n',encoding='utf-8')
            with patch.object(server,'minecraft_ready',return_value=True):
                self.assertEqual(server.check_running(log),0)
            with patch.object(server,'minecraft_ready',return_value=False):
                self.assertEqual(server.check_running(log),1)

    def test_rotated_log_does_not_block_running_server(self):
        with tempfile.TemporaryDirectory() as directory:
            log=Path(directory)/'latest.log'
            log.write_text('Server empty for 60 seconds, pausing\n',encoding='utf-8')
            with patch.object(server,'minecraft_ready',return_value=True):
                self.assertEqual(server.check_running(log),0)
            log.unlink()
            with patch.object(server,'minecraft_ready',return_value=True):
                self.assertEqual(server.check_running(log),0)

    def test_status_reply_handles_fragmented_packets(self):
        payload=server.json.dumps({'players':{'online':0},'version':{'name':'26.3'}}).encode()
        body=b'\x00'+server.varint(len(payload))+payload
        data=bytearray(server.varint(len(body))+body)
        stream=MagicMock()
        def receive(length):
            chunk=bytes(data[:min(length,3)])
            del data[:len(chunk)]
            return chunk
        stream.recv.side_effect=receive
        connection=MagicMock()
        connection.__enter__.return_value=stream
        with patch.object(server.socket,'create_connection',return_value=connection):
            self.assertTrue(server.minecraft_ready())
        stream.sendall.assert_called_once()

    def test_socket_without_minecraft_reply_is_not_ready(self):
        for response in [b'',b'HTTP/1.1 200 OK',b'\xff'*5]:
            stream=MagicMock()
            data=bytearray(response)
            def receive(length):
                chunk=bytes(data[:length]);del data[:length];return chunk
            stream.recv.side_effect=receive
            connection=MagicMock()
            connection.__enter__.return_value=stream
            with patch.object(server.socket,'create_connection',return_value=connection):
                self.assertFalse(server.minecraft_ready())
        with patch.object(server.socket,'create_connection',side_effect=ConnectionRefusedError):
            self.assertFalse(server.minecraft_ready())


if __name__=='__main__':
    unittest.main()
