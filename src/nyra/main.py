from __future__ import annotations

import os
import sys
from pathlib import Path

from PySide6.QtCore import QUrl
from PySide6.QtGui import QGuiApplication
from PySide6.QtQml import QQmlApplicationEngine
from PySide6.QtQuickControls2 import QQuickStyle

from .ai_core import AiCore
from .task_store import TaskStore
from .system_monitor import SystemMonitor


def main() -> int:
    QQuickStyle.setStyle("Fusion")
    app = QGuiApplication(sys.argv)
    app.setApplicationName("Nyra")
    app.setOrganizationName("NYRA")

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

    # The Windows CI smoke test sets this variable. Creating the marker only
    # after a root object exists proves that Qt loaded the initial QML window;
    # merely observing a running process is not enough for GUI applications.
    ready_file = os.environ.get("NYRA_SMOKE_TEST_READY_FILE")
    if ready_file:
        Path(ready_file).write_text("qml-ready\n", encoding="utf-8")
    return app.exec()


if __name__ == "__main__":
    raise SystemExit(main())
