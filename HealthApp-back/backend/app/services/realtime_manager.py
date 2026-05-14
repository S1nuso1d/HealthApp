from __future__ import annotations

import json
from collections import defaultdict
from typing import Dict, List

from fastapi import WebSocket


class RealtimeManager:
    def __init__(self) -> None:
        self.user_connections: Dict[int, List[WebSocket]] = defaultdict(list)

    async def connect(self, user_id: int, websocket: WebSocket) -> None:
        await websocket.accept()
        self.user_connections[user_id].append(websocket)

    def disconnect(self, user_id: int, websocket: WebSocket) -> None:
        if user_id not in self.user_connections:
            return

        self.user_connections[user_id] = [
            ws for ws in self.user_connections[user_id] if ws != websocket
        ]

        if not self.user_connections[user_id]:
            del self.user_connections[user_id]

    async def send_to_user(self, user_id: int, payload: dict) -> None:
        if user_id not in self.user_connections:
            return

        dead_connections: List[WebSocket] = []

        for websocket in self.user_connections[user_id]:
            try:
                await websocket.send_text(json.dumps(payload, ensure_ascii=False))
            except Exception:
                dead_connections.append(websocket)

        for websocket in dead_connections:
            self.disconnect(user_id, websocket)

    async def broadcast_user_update(
        self,
        user_id: int,
        source: str,
        message: str = "Данные пользователя обновлены",
    ) -> None:
        payload = {
            "type": "dashboard_update",
            "source": source,
            "message": message,
        }
        await self.send_to_user(user_id, payload)


realtime_manager = RealtimeManager()