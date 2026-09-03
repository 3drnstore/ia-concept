from __future__ import annotations

import os
import platform
import subprocess
from collections import deque
from pathlib import Path

import psutil
from PySide6.QtCore import QObject, Property, QTimer, Signal, Slot


class SystemMonitor(QObject):
    """Small, non-blocking bridge exposing real host telemetry to QML.

    CPU/RAM come from psutil. On Windows, GPU load is the sum of 3D/Compute
    engine utilization returned by the native GPU Engine performance counters.
    Some driver/Windows combinations do not publish those counters; that state
    is explicitly exposed instead of inventing a value.
    """

    metricsChanged = Signal()
    speakingChanged = Signal()

    def __init__(self) -> None:
        super().__init__()
        self._cpu = 0.0
        self._ram = 0.0
        self._gpu: float | None = None
        self._gpu_name = self._detect_gpu_name()
        self._gpu_status = "Aguardando contador"
        self._voice = 0.0
        self._speaking = False
        self._db_size = 0
        self._cpu_history = deque([0.0] * 36, maxlen=36)
        self._gpu_history = deque([0.0] * 36, maxlen=36)
        self._ram_history = deque([0.0] * 36, maxlen=36)
        self._voice_history = deque([0.0] * 36, maxlen=36)
        self._gpu_tick = 0
        psutil.cpu_percent(None)
        self._timer = QTimer(self)
        self._timer.timeout.connect(self._sample)
        self._timer.start(1000)
        self._sample()

    def _detect_gpu_name(self) -> str:
        if platform.system() != "Windows":
            return "GPU não disponível neste sistema"
        try:
            command = [
                "powershell", "-NoProfile", "-NonInteractive", "-Command",
                "(Get-CimInstance Win32_VideoController | Select-Object -ExpandProperty Name) -join ' / '",
            ]
            result = subprocess.run(command, capture_output=True, text=True, timeout=4,
                                    creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
            return result.stdout.strip() or "Adaptador gráfico do Windows"
        except (OSError, subprocess.SubprocessError):
            return "Adaptador gráfico do Windows"

    def _read_windows_gpu(self) -> None:
        if platform.system() != "Windows":
            self._gpu = None
            self._gpu_status = "Contadores GPU disponíveis apenas no Windows"
            return
        script = (
            "$s=(Get-Counter '\\GPU Engine(*)\\Utilization Percentage' -ErrorAction Stop).CounterSamples;"
            "$v=($s|? {$_.InstanceName -match 'engtype_(3D|Compute)'}|measure CookedValue -Sum).Sum;"
            "[math]::Round([math]::Min(100,[double]$v),1)"
        )
        try:
            result = subprocess.run(
                ["powershell", "-NoProfile", "-NonInteractive", "-Command", script],
                capture_output=True, text=True, timeout=5,
                creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0),
            )
            if result.returncode == 0 and result.stdout.strip():
                self._gpu = max(0.0, min(100.0, float(result.stdout.strip().replace(",", "."))))
                self._gpu_status = "Performance Counter · 3D/Compute"
            else:
                raise ValueError(result.stderr.strip())
        except (OSError, ValueError, subprocess.SubprocessError):
            self._gpu = None
            self._gpu_status = "Utilização indisponível pelo driver/Windows"

    def _sample(self) -> None:
        self._cpu = psutil.cpu_percent(None)
        self._ram = psutil.virtual_memory().percent
        if self._gpu_tick % 3 == 0:
            self._read_windows_gpu()
        self._gpu_tick += 1
        db = Path(os.environ.get("LOCALAPPDATA", Path.home())) / "NYRA" / "nyra.sqlite3"
        self._db_size = db.stat().st_size if db.exists() else 0
        self._cpu_history.append(self._cpu)
        self._gpu_history.append(self._gpu or 0.0)
        self._ram_history.append(self._ram)
        self._voice = 100.0 if self._speaking else 0.0
        self._voice_history.append(self._voice)
        self.metricsChanged.emit()

    @Property(float, notify=metricsChanged)
    def cpuPercent(self) -> float: return self._cpu

    @Property(float, notify=metricsChanged)
    def ramPercent(self) -> float: return self._ram

    @Property(float, notify=metricsChanged)
    def gpuPercent(self) -> float: return self._gpu if self._gpu is not None else -1.0

    @Property(str, constant=True)
    def gpuName(self) -> str: return self._gpu_name

    @Property(str, notify=metricsChanged)
    def gpuStatus(self) -> str: return self._gpu_status

    @Property(float, notify=metricsChanged)
    def voiceLevel(self) -> float: return self._voice

    @Property(bool, notify=speakingChanged)
    def speaking(self) -> bool: return self._speaking

    @Property(str, notify=metricsChanged)
    def dbSize(self) -> str:
        return f"{self._db_size / 1024:.0f} KB" if self._db_size < 1024 ** 2 else f"{self._db_size / 1024 ** 2:.1f} MB"

    @Property("QVariantList", notify=metricsChanged)
    def cpuHistory(self) -> list[float]: return list(self._cpu_history)

    @Property("QVariantList", notify=metricsChanged)
    def gpuHistory(self) -> list[float]: return list(self._gpu_history)

    @Property("QVariantList", notify=metricsChanged)
    def ramHistory(self) -> list[float]: return list(self._ram_history)

    @Property("QVariantList", notify=metricsChanged)
    def voiceHistory(self) -> list[float]: return list(self._voice_history)

    @Slot(bool)
    def setSpeaking(self, value: bool) -> None:
        value = bool(value)
        if self._speaking != value:
            self._speaking = value
            self.speakingChanged.emit()
