from __future__ import annotations

import os
import shutil
from pathlib import Path
from tempfile import TemporaryDirectory

from PySide6.QtCore import QObject, Property, QProcess, QSettings, QTimer, QUrl, Signal, Slot
from PySide6.QtMultimedia import QAudioOutput, QMediaPlayer, QMediaDevices


class LocalVoice(QObject):
    changed = Signal()
    speakingChanged = Signal()
    errorOccurred = Signal(str)

    def __init__(self):
        super().__init__()
        self.settings = QSettings()
        self._status = "Piper local — configure o executável e a voz"
        self._temp = TemporaryDirectory(prefix="nyra-voice-")
        self._wav = str(Path(self._temp.name) / "speech.wav")
        self._process = QProcess(self)
        self._process.finished.connect(self._finished)
        self._process.errorOccurred.connect(lambda _: self._fail("Não foi possível executar o Piper."))
        self._timer = QTimer(self)
        self._timer.setSingleShot(True)
        self._timer.setInterval(120000)
        self._timer.timeout.connect(self._timeout)
        self._output = QAudioOutput(self)
        self._player = QMediaPlayer(self)
        self._player.setAudioOutput(self._output)
        self._player.playbackStateChanged.connect(lambda _: self.speakingChanged.emit())
        self._player.errorOccurred.connect(lambda *_: self._fail(self._player.errorString()))
        self._player.mediaStatusChanged.connect(self._media_status)

    def _get(self, key, default=""):
        return self.settings.value("NyraDevices/" + key, default)

    def _set(self, key, value):
        self.settings.setValue("NyraDevices/" + key, value)
        self.changed.emit()

    @Property(str, notify=changed)
    def executable(self):
        default = Path(os.environ.get("LOCALAPPDATA", str(Path.home()))) / "NYRA/voice/runtime/Scripts/piper.exe"
        return str(self._get("piper", os.environ.get("NYRA_PIPER_EXE", shutil.which("piper") or str(default))))

    @Property(str, notify=changed)
    def model(self):
        default = Path(os.environ.get("LOCALAPPDATA", str(Path.home()))) / "NYRA/voice/models/pt_BR-faber-medium.onnx"
        return str(self._get("voice", os.environ.get("NYRA_PIPER_MODEL", str(default))))

    @Property(bool, notify=changed)
    def enabled(self):
        return str(self._get("enabled", "true")).lower() == "true"

    @Property(bool, notify=changed)
    def microphoneAllowed(self):
        return str(self._get("microphone", "true")).lower() == "true"

    @Property(bool, notify=changed)
    def cameraAllowed(self):
        return str(self._get("camera", "false")).lower() == "true"

    @Property(str, notify=changed)
    def cameraId(self):
        return str(self._get("cameraId"))

    @Property('QVariantList', notify=changed)
    def cameras(self):
        return [{"label": c.description(), "value": bytes(c.id()).hex()} for c in QMediaDevices.videoInputs()]

    @Property(str, notify=changed)
    def status(self):
        return self._status

    @Property(bool, notify=speakingChanged)
    def speaking(self):
        return self._player.playbackState() == QMediaPlayer.PlayingState

    @Slot(str, str)
    def configure(self, executable, model):
        self.stop()
        self._set("piper", executable.strip())
        self._set("voice", model.strip())
        self._status = "Configuração salva — use Testar voz"
        self.changed.emit()

    @Slot(str, bool)
    def setPermission(self, key, value):
        if key not in {"enabled", "microphone", "camera"}:
            return
        self._set(key, value)
        if key == "enabled" and not value:
            self.stop()

    @Slot(str)
    def selectCamera(self, value):
        self._set("cameraId", value)

    @Slot()
    def refreshDevices(self):
        self.changed.emit()

    @Slot(str)
    def speak(self, text):
        if not self.enabled or not text.strip():
            return
        self.stop()
        if not Path(self.executable).is_file() or not Path(self.model).is_file() or not Path(self.model + ".json").is_file():
            self._fail("Configure o executável Piper, a voz .onnx e seu arquivo .onnx.json em Configurações.")
            return
        Path(self._wav).unlink(missing_ok=True)
        self._status = "Preparando voz local…"
        self.changed.emit()
        self._process.setProgram(self.executable)
        self._process.setArguments(["-m", self.model, "-f", self._wav])
        self._process.start()
        self._process.write((text + "\n").encode("utf-8"))
        self._process.closeWriteChannel()
        self._timer.start()

    def _finished(self, code, status):
        self._timer.stop()
        if status != QProcess.NormalExit or code != 0:
            self._fail("Piper não conseguiu sintetizar a voz. Confira o modelo e a instalação.")
            return
        if not Path(self._wav).is_file():
            self._fail("Piper não gerou o áudio esperado.")
            return
        self._status = "Reproduzindo voz local"
        self.changed.emit()
        self._player.setSource(QUrl.fromLocalFile(self._wav))
        self._player.play()

    def _media_status(self, status):
        if status == QMediaPlayer.EndOfMedia:
            self._status = "Piper pronto"
            self.changed.emit()

    def _fail(self, message):
        self._timer.stop()
        self._status = message
        self.changed.emit()
        self.errorOccurred.emit(message)

    def _timeout(self):
        self.stop()
        self._fail("Piper excedeu o tempo de síntese. Tente uma resposta mais curta.")

    @Slot()
    def stop(self):
        self._timer.stop()
        self._player.stop()
        self._player.setSource(QUrl())
        if self._process.state() != QProcess.NotRunning:
            self._process.blockSignals(True)
            self._process.kill()
            self._process.waitForFinished(1500)
            self._process.blockSignals(False)

    @Slot()
    def shutdown(self):
        self.stop()
        self._temp.cleanup()
