def test_meal_put_delete(client, auth_headers):
    create = client.post(
        "/meal/",
        json={
            "meal_type": "breakfast",
            "name": "Тестовый завтрак",
            "calories": 400.0,
            "meal_time": "2026-05-14T08:30:00",
            "source": "manual",
        },
        headers=auth_headers,
    )
    assert create.status_code == 200, create.text
    meal_id = create.json()["id"]

    upd = client.put(
        f"/meal/{meal_id}",
        json={
            "meal_type": "breakfast",
            "name": "Обновлённо",
            "calories": 350.0,
            "meal_time": "2026-05-14T08:30:00",
            "source": "manual",
        },
        headers=auth_headers,
    )
    assert upd.status_code == 200, upd.text
    assert upd.json()["name"] == "Обновлённо"

    delete = client.delete(f"/meal/{meal_id}", headers=auth_headers)
    assert delete.status_code == 200, delete.text

    hist = client.get("/meal/history", headers=auth_headers)
    assert hist.status_code == 200
    ids = [m["id"] for m in hist.json()]
    assert meal_id not in ids
