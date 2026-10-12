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
            with patch.object(server.socket,'create_connection') as connection:
                self.assertEqual(server.wait(log),1)
                connection.assert_not_called()

    def test_ready_needs_a_socket(self):
        with tempfile.TemporaryDirectory() as directory:
            log=Path(directory)/'latest.log'
            log.write_text('Done (12.5s)! For help, type "help"\n',encoding='utf-8')
            with patch.object(server.socket,'create_connection',return_value=MagicMock()) as connection:
                self.assertEqual(server.wait(log),0)
                connection.assert_called_once_with(('127.0.0.1',25565),timeout=1)

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
            with patch.object(server.socket,'create_connection',return_value=MagicMock()):
                self.assertEqual(server.check_running(log),0)
            with patch.object(server.socket,'create_connection',side_effect=ConnectionRefusedError):
                self.assertEqual(server.check_running(log),1)


if __name__=='__main__':
    unittest.main()
