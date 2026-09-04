import json
import math
import threading
import time
import unittest
from array import array
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from unittest.mock import patch

from PySide6.QtCore import QCoreApplication
from PySide6.QtMultimedia import QAudioFormat
from nyra.audio_monitor import AudioMonitor
from nyra.ai_core import AiCore

app = QCoreApplication.instance() or QCoreApplication([])


class AudioTests(unittest.TestCase):
    def test_pcm_formats_silence_and_stop(self):
        class Device:
            def readAll(self): return self.data
        for fmt, code, peak, offset in [
            (QAudioFormat.UInt8, 'B', 127, 128),
            (QAudioFormat.Int16, 'h', 32767, 0),
            (QAudioFormat.Int32, 'i', 2147483647, 0),
            (QAudioFormat.Float, 'f', 1.0, 0),
        ]:
            monitor = AudioMonitor()
            monitor._format.setSampleFormat(fmt)
            device = Device()
            values = [offset + peak * .2 * math.sin(i * .2) for i in range(1024)]
            device.data = array(code, values if code == 'f' else map(int, values)).tobytes()
            monitor._device = device
            monitor._read_audio()
            self.assertGreater(monitor.level, .45)
            self.assertLess(monitor.level, .6)
            device.data = array(code, [offset] * 1024).tobytes()
            for _ in range(30): monitor._read_audio()
            self.assertEqual(monitor.level, 0)
            monitor._set_level(.8)
            monitor._stop()
            self.assertEqual(monitor.level, 0)


class AiTests(unittest.TestCase):
    def test_local_request_response_and_history(self):
        requests = []
        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *_): pass
            def do_GET(self):
                self.send_response(200); self.end_headers(); self.wfile.write(b'{}')
            def do_POST(self):
                requests.append(json.loads(self.rfile.read(int(self.headers['Content-Length']))))
                self.send_response(200); self.end_headers()
                self.wfile.write(json.dumps({'choices':[{'message':{'content':'Resposta local'}}]}).encode())
        server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        threading.Thread(target=server.serve_forever, daemon=True).start()
        try:
            with patch.dict('os.environ', {'NYRA_LLAMA_URL':f'http://127.0.0.1:{server.server_port}'}):
                core = AiCore()
            responses=[]
            core.responseReady.connect(responses.append)
            for text in ['Olá', 'Continue']:
                core.sendMessage(text)
                deadline=time.monotonic()+5
                while core.busy and time.monotonic()<deadline:
                    app.processEvents(); time.sleep(.01)
                self.assertFalse(core.busy)
            self.assertEqual(responses, ['Resposta local']*2)
            self.assertEqual([m['role'] for m in requests[1]['messages']], ['system','user','assistant','user'])
            self.assertEqual(core.status, 'READY')
        finally:
            server.shutdown(); server.server_close()

    def test_remote_endpoint_rejected(self):
        with patch.dict('os.environ', {'NYRA_LLAMA_URL':'https://example.com'}):
            with self.assertRaises(ValueError): AiCore()


if __name__ == '__main__': unittest.main()
