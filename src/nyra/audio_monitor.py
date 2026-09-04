from __future__ import annotations

import math
from array import array

from PySide6.QtCore import QObject, Property, Signal, Slot, QTimer
from PySide6.QtMultimedia import QAudioFormat, QAudioSource, QMediaDevices


class AudioMonitor(QObject):
    levelChanged = Signal()
    listeningChanged = Signal()
    errorChanged = Signal()

    def __init__(self) -> None:
        super().__init__()
        self._level = 0.0
        self._listening = False
        self._error = ""
        self._source: QAudioSource | None = None
        self._device = None
        self._format = QAudioFormat()
        self._silence = QTimer(self)
        self._silence.setSingleShot(True)
        self._silence.setInterval(250)
        self._silence.timeout.connect(self._reset_level)

    def _reset_level(self):
        self._level = 0.0
        self.levelChanged.emit()

    @Property(float, notify=levelChanged)
    def level(self) -> float:
        return self._level

    @Property(bool, notify=listeningChanged)
    def listening(self) -> bool:
        return self._listening

    @Property(str, notify=errorChanged)
    def error(self) -> str:
        return self._error

    def _set_error(self, message: str) -> None:
        if self._error == message:
            return
        self._error = message
        self.errorChanged.emit()

    @Slot(bool)
    def setListening(self, enabled: bool) -> None:
        if enabled == self._listening:
            return
        if not enabled:
            self._stop()
            return

        try:
            input_device = QMediaDevices.defaultAudioInput()
            if input_device.isNull():
                self._set_error("Nenhum microfone disponível")
                return
            audio_format = QAudioFormat()
            audio_format.setSampleRate(16000)
            audio_format.setChannelCount(1)
            audio_format.setSampleFormat(QAudioFormat.Int16)
            if not input_device.isFormatSupported(audio_format):
                audio_format = input_device.preferredFormat()
            self._source = QAudioSource(input_device, audio_format, self)
            self._format = audio_format
            self._device = self._source.start()
            if self._device is None:
                self._set_error("Não foi possível iniciar o microfone")
                self._source = None
                return
            self._device.readyRead.connect(self._read_audio)
            self._listening = True
            self._set_error("")
            self.listeningChanged.emit()
        except Exception as exc:  # hardware/driver failures must not close Nyra
            self._set_error(str(exc))
            self._stop()

    def _stop(self) -> None:
        self._silence.stop()
        if self._source is not None:
            self._source.stop()
            self._source.deleteLater()
        self._source = None
        self._device = None
        was_listening = self._listening
        self._listening = False
        self._level = 0.0
        self.levelChanged.emit()
        if was_listening:
            self.listeningChanged.emit()

    def _set_level(self, value: float) -> None:
        value = max(0.0, min(1.0, value))
        value = 0.0 if value < 0.025 else value
        smoothed = max(value, self._level * 0.65)
        if smoothed < 0.008:
            smoothed = 0.0
        if smoothed == self._level:
            return
        self._level = smoothed
        self.levelChanged.emit()

    @Slot()
    def _read_audio(self) -> None:
        if self._device is None:
            return
        raw = bytes(self._device.readAll())
        self._silence.start()
        sample_format = self._format.sampleFormat()
        types = {QAudioFormat.UInt8: ('B', 128.0), QAudioFormat.Int16: ('h', 32768.0),
                 QAudioFormat.Int32: ('i', 2147483648.0), QAudioFormat.Float: ('f', 1.0)}
        if sample_format not in types:
            return
        code, divisor = types[sample_format]
        samples = array(code)
        samples.frombytes(raw[:len(raw) - len(raw) % samples.itemsize])
        if not samples:
            return
        offset = 128 if sample_format == QAudioFormat.UInt8 else 0
        rms = math.sqrt(sum(((sample - offset) / divisor) ** 2 for sample in samples) / len(samples))
        self._set_level(min(1.0, rms * 3.6))
