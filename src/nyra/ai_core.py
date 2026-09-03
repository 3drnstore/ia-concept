from __future__ import annotations

import json
import os
from urllib.error import HTTPError, URLError
from urllib.parse import urlparse
from urllib.request import Request, urlopen

from PySide6.QtCore import QObject, Property, QThread, Signal, Slot


SYSTEM_PROMPT = """Você é Nyra (N.Y.R.A. — Neural Yielding Reasoning Assistant), uma assistente pessoal local.
Responda em português do Brasil por padrão, de forma natural, objetiva e humana.
Você roda localmente no computador do usuário. Não afirme ter acessado internet, arquivos, câmera,
microfone, dispositivos IoT ou outras ferramentas a menos que uma ferramenta real tenha fornecido esse dado.
Quando uma ação externa for necessária, explique brevemente o motivo e peça autorização de forma natural.
"""


class _CompletionWorker(QThread):
    completed = Signal(str)
    failed = Signal(str)

    def __init__(self, endpoint: str, model: str, message: str, parent: QObject | None = None) -> None:
        super().__init__(parent)
        self._endpoint = endpoint
        self._model = model
        self._message = message

    def run(self) -> None:
        try:
            payload = {
                "model": self._model,
                "messages": [
                    {"role": "system", "content": SYSTEM_PROMPT},
                    {"role": "user", "content": self._message},
                ],
                "temperature": 0.65,
                "stream": False,
            }
            request = Request(
                f"{self._endpoint.rstrip('/')}/v1/chat/completions",
                data=json.dumps(payload).encode("utf-8"),
                headers={"Content-Type": "application/json"},
                method="POST",
            )
            with urlopen(request, timeout=180) as response:
                body = json.loads(response.read().decode("utf-8"))
            text = body["choices"][0]["message"]["content"].strip()
            if not text:
                raise ValueError("O modelo retornou uma resposta vazia.")
            self.completed.emit(text)
        except HTTPError as exc:
            self.failed.emit(f"llama.cpp respondeu com HTTP {exc.code}.")
        except URLError:
            self.failed.emit("O núcleo local de IA não está disponível. Inicie o llama.cpp server.")
        except (KeyError, IndexError, TypeError, ValueError, json.JSONDecodeError) as exc:
            self.failed.emit(f"Resposta inválida do núcleo local: {exc}")
        except Exception as exc:  # pragma: no cover - defensive boundary around local runtime
            self.failed.emit(f"Falha no núcleo local de IA: {exc}")


class AiCore(QObject):
    statusChanged = Signal()
    busyChanged = Signal()
    responseReady = Signal(str)
    errorOccurred = Signal(str)

    def __init__(self) -> None:
        super().__init__()
        self._endpoint = os.environ.get("NYRA_LLAMA_URL", "http://127.0.0.1:8080")
        self._model = os.environ.get("NYRA_MODEL", "Qwen3-8B")
        self._status = "OFFLINE"
        self._busy = False
        self._worker: _CompletionWorker | None = None
        self._validate_local_endpoint()
        self.detectRuntime()

    def _validate_local_endpoint(self) -> None:
        parsed = urlparse(self._endpoint)
        if parsed.scheme not in {"http", "https"}:
            raise ValueError("NYRA_LLAMA_URL deve usar http ou https.")
        if parsed.hostname not in {"127.0.0.1", "localhost", "::1"}:
            raise ValueError("O AI Core aceita apenas um endpoint local por padrão.")

    @Property(str, notify=statusChanged)
    def status(self) -> str:
        return self._status

    @Property(str, constant=True)
    def modelName(self) -> str:
        return self._model

    @Property(str, constant=True)
    def endpoint(self) -> str:
        return self._endpoint

    @Property(bool, notify=busyChanged)
    def busy(self) -> bool:
        return self._busy

    def _set_status(self, value: str) -> None:
        if self._status != value:
            self._status = value
            self.statusChanged.emit()

    def _set_busy(self, value: bool) -> None:
        if self._busy != value:
            self._busy = value
            self.busyChanged.emit()

    @Slot()
    def detectRuntime(self) -> None:
        try:
            request = Request(f"{self._endpoint.rstrip('/')}/health", method="GET")
            with urlopen(request, timeout=0.8) as response:
                if 200 <= response.status < 500:
                    self._set_status("READY")
                    return
        except Exception:
            pass
        self._set_status("OFFLINE")

    @Slot(str)
    def sendMessage(self, message: str) -> None:
        message = message.strip()
        if not message:
            return
        if self._busy:
            self.errorOccurred.emit("Nyra ainda está processando a mensagem anterior.")
            return

        self._set_busy(True)
        self._set_status("THINKING")
        worker = _CompletionWorker(self._endpoint, self._model, message, self)
        self._worker = worker
        worker.completed.connect(self._on_completed)
        worker.failed.connect(self._on_failed)
        worker.finished.connect(worker.deleteLater)
        worker.start()

    @Slot(str)
    def _on_completed(self, text: str) -> None:
        self._set_busy(False)
        self._set_status("READY")
        self.responseReady.emit(text)
        self._worker = None

    @Slot(str)
    def _on_failed(self, message: str) -> None:
        self._set_busy(False)
        self._set_status("OFFLINE")
        self.errorOccurred.emit(message)
        self._worker = None
