from __future__ import annotations

import os
import sys
import ctypes
from pathlib import Path

from PySide6.QtCore import QUrl
from PySide6.QtGui import QGuiApplication, QIcon
from PySide6.QtQml import QQmlApplicationEngine
from PySide6.QtQuickControls2 import QQuickStyle

from .ai_core import AiCore
from .task_store import TaskStore
from .system_monitor import SystemMonitor


def main() -> int:
    # Give the frameless window its own Windows taskbar identity. Without an
    # explicit AppUserModelID Windows may group it under the Python/Qt host and
    # display the generic executable icon even though the EXE has an icon.
    if sys.platform == "win32":
        ctypes.windll.shell32.SetCurrentProcessExplicitAppUserModelID(
            "NYRA.NexusCommandCore.0.2"
        )

    QQuickStyle.setStyle("Fusion")
    app = QGuiApplication(sys.argv)
    app.setApplicationName("Nyra")
    app.setOrganizationName("NYRA")
    icon_path = Path(__file__).resolve().parent / "assets" / "nyra.ico"
    if icon_path.exists():
        app.setWindowIcon(QIcon(str(icon_path)))

    engine = QQmlApplicationEngine()
    task_store = TaskStore()
    system_monitor = SystemMonitor()
    ai_core = AiCore()
    engine.rootContext().setContextProperty("taskStore", task_store)
    engine.rootContext().setContextProperty("systemMonitor", system_monitor)
    engine.rootContext().setContextProperty("aiCore", ai_core)

    qml_path = Path(__file__).resolve().parent / "ui" / "ConceptShell.qml"
    engine.load(QUrl.fromLocalFile(str(qml_path)))

    if not engine.rootObjects():
        return 1

    if icon_path.exists():
        root_window = engine.rootObjects()[0]
        if hasattr(root_window, "setIcon"):
            root_window.setIcon(QIcon(str(icon_path)))

    # The Windows CI smoke test sets this variable. Creating the marker only
    # after a root object exists proves that Qt loaded the initial QML window;
    # merely observing a running process is not enough for GUI applications.
    ready_file = os.environ.get("NYRA_SMOKE_TEST_READY_FILE")
    if ready_file:
        Path(ready_file).write_text("qml-ready\n", encoding="utf-8")
    return app.exec()


if __name__ == "__main__":
    raise SystemExit(main())
