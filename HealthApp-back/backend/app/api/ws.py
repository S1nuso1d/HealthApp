from fastapi import APIRouter, Query, WebSocket, WebSocketDisconnect

from app.core.security import ACCESS_TOKEN_TYPE, TokenError, user_id_from_token
from app.services.realtime_manager import realtime_manager

router = APIRouter(tags=["WebSocket"])


def decode_user_id_from_token(token: str) -> int:
    """Принимает только access-токен: refresh живёт 30 дней и в query-строке опаснее."""
    try:
        return user_id_from_token(token, ACCESS_TOKEN_TYPE)
    except TokenError as exc:
        raise ValueError("Invalid token") from exc


@router.websocket("/ws")
async def websocket_endpoint(
    websocket: WebSocket,
    token: str = Query(...),
):
    try:
        user_id = decode_user_id_from_token(token)
    except ValueError:
        await websocket.close(code=1008)
        return

    await realtime_manager.connect(user_id, websocket)

    try:
        await realtime_manager.send_to_user(
            user_id,
            {
                "type": "connected",
                "message": "WebSocket подключен",
            },
        )

        while True:
            # держим соединение живым
            await websocket.receive_text()
    except WebSocketDisconnect:
        realtime_manager.disconnect(user_id, websocket)
    except Exception:
        realtime_manager.disconnect(user_id, websocket)
        try:
            await websocket.close()
        except Exception:
            pass