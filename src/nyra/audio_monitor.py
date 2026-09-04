from __future__ import annotations

import math
from array import array

from PySide6.QtCore import QObject, Property, Signal, Slot
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
        if self._source is not None:
            self._source.stop()
            self._source.deleteLater()
        self._source = None
        self._device = None
        was_listening = self._listening
        self._listening = False
        self._set_level(0.0)
        if was_listening:
            self.listeningChanged.emit()

    def _set_level(self, value: float) -> None:
        value = max(0.0, min(1.0, value))
        smoothed = max(value, self._level * 0.72)
        if abs(smoothed - self._level) < 0.008:
            return
        self._level = smoothed
        self.levelChanged.emit()

    @Slot()
    def _read_audio(self) -> None:
        if self._device is None:
            return
        raw = bytes(self._device.readAll())
        if len(raw) < 2:
            return
        samples = array("h")
        samples.frombytes(raw[: len(raw) - (len(raw) % 2)])
        if not samples:
            return
        rms = math.sqrt(sum(sample * sample for sample in samples) / len(samples))
        self._set_level(min(1.0, rms / 9000.0))
