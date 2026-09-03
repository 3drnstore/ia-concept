from __future__ import annotations

import sys
from pathlib import Path

from PySide6.QtCore import QUrl
from PySide6.QtGui import QGuiApplication
from PySide6.QtQml import QQmlApplicationEngine

from .task_store import TaskStore
from .system_monitor import SystemMonitor


def main() -> int:
    app = QGuiApplication(sys.argv)
    app.setApplicationName("Nyra")
    app.setOrganizationName("NYRA")

    engine = QQmlApplicationEngine()
    task_store = TaskStore()
    system_monitor = SystemMonitor()
    engine.rootContext().setContextProperty("taskStore", task_store)
    engine.rootContext().setContextProperty("systemMonitor", system_monitor)

    qml_path = Path(__file__).resolve().parent / "ui" / "Main.qml"
    engine.load(QUrl.fromLocalFile(str(qml_path)))

    if not engine.rootObjects():
        return 1
    return app.exec()


if __name__ == "__main__":
    raise SystemExit(main())
