from __future__ import annotations

import os
import sqlite3
from pathlib import Path
from typing import Any

from PySide6.QtCore import QObject, Property, Signal, Slot


class TaskStore(QObject):
    tasksChanged = Signal()
    selectedTaskChanged = Signal()

    def __init__(self) -> None:
        super().__init__()
        base = Path(os.environ.get("LOCALAPPDATA", Path.home())) / "NYRA"
        base.mkdir(parents=True, exist_ok=True)
        self._db_path = base / "nyra.sqlite3"
        self._tasks: list[dict[str, Any]] = []
        self._selected_id: int | None = None
        self._initialize_db()
        self._seed_if_empty()
        self.refresh()

    def _connect(self) -> sqlite3.Connection:
        con = sqlite3.connect(self._db_path)
        con.row_factory = sqlite3.Row
        return con

    def _initialize_db(self) -> None:
        with self._connect() as con:
            con.execute(
                """
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL DEFAULT '',
                    priority TEXT NOT NULL DEFAULT 'NORMAL',
                    due_text TEXT NOT NULL DEFAULT '',
                    status TEXT NOT NULL DEFAULT 'ATIVA',
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
            )

    def _seed_if_empty(self) -> None:
        with self._connect() as con:
            count = con.execute("SELECT COUNT(*) FROM tasks").fetchone()[0]
            if count:
                return
            con.executemany(
                "INSERT INTO tasks(title, description, priority, due_text, status) VALUES (?, ?, ?, ?, ?)",
                [
                    (
                        "Cotação Hospital",
                        "Analisar se os materiais solicitados são compatíveis antes de decidir participação.",
                        "ALTA",
                        "Hoje",
                        "ATIVA",
                    ),
                    (
                        "3DRN Store",
                        "Revisar o staging e preparar as mudanças para publicação.",
                        "NORMAL",
                        "Amanhã",
                        "ATIVA",
                    ),
                    (
                        "PsicoGestão",
                        "Organizar pendências do portal do paciente e fluxo de pagamentos.",
                        "NORMAL",
                        "Sem prazo",
                        "AGUARDANDO",
                    ),
                ],
            )

    def _row_to_dict(self, row: sqlite3.Row) -> dict[str, Any]:
        return {
            "id": row["id"],
            "title": row["title"],
            "description": row["description"],
            "priority": row["priority"],
            "dueText": row["due_text"],
            "status": row["status"],
            "createdAt": row["created_at"],
        }

    @Property("QVariantList", notify=tasksChanged)
    def tasks(self) -> list[dict[str, Any]]:
        return self._tasks

    @Property("QVariantMap", notify=selectedTaskChanged)
    def selectedTask(self) -> dict[str, Any]:
        for task in self._tasks:
            if task["id"] == self._selected_id:
                return task
        return self._tasks[0] if self._tasks else {}

    @Slot()
    def refresh(self) -> None:
        with self._connect() as con:
            rows = con.execute(
                "SELECT id, title, description, priority, due_text, status, created_at FROM tasks ORDER BY CASE status WHEN 'ATIVA' THEN 0 WHEN 'AGUARDANDO' THEN 1 ELSE 2 END, id DESC"
            ).fetchall()
        self._tasks = [self._row_to_dict(row) for row in rows]
        if self._selected_id is None and self._tasks:
            self._selected_id = self._tasks[0]["id"]
        self.tasksChanged.emit()
        self.selectedTaskChanged.emit()

    @Slot(int)
    def selectTask(self, task_id: int) -> None:
        if self._selected_id == task_id:
            return
        self._selected_id = task_id
        self.selectedTaskChanged.emit()

    @Slot(str, str, str, str)
    def addTask(self, title: str, description: str = "", priority: str = "NORMAL", due_text: str = "") -> None:
        title = title.strip()
        if not title:
            return
        priority = priority.strip().upper() or "NORMAL"
        with self._connect() as con:
            cur = con.execute(
                "INSERT INTO tasks(title, description, priority, due_text, status) VALUES (?, ?, ?, ?, 'ATIVA')",
                (title, description.strip(), priority, due_text.strip()),
            )
            self._selected_id = int(cur.lastrowid)
        self.refresh()

    @Slot(int)
    def toggleDone(self, task_id: int) -> None:
        with self._connect() as con:
            row = con.execute("SELECT status FROM tasks WHERE id = ?", (task_id,)).fetchone()
            if row is None:
                return
            new_status = "ATIVA" if row["status"] == "CONCLUÍDA" else "CONCLUÍDA"
            con.execute("UPDATE tasks SET status = ? WHERE id = ?", (new_status, task_id))
        self.refresh()

    @Slot(int)
    def deleteTask(self, task_id: int) -> None:
        with self._connect() as con:
            con.execute("DELETE FROM tasks WHERE id = ?", (task_id,))
        if self._selected_id == task_id:
            self._selected_id = None
        self.refresh()
