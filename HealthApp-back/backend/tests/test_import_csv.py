def test_import_csv_hydration(client, auth_headers):
    body = {"text": "hydration;2026-05-14T11:00:00;333\n"}
    resp = client.post("/import/csv", json=body, headers=auth_headers)
    assert resp.status_code == 200, resp.text
    data = resp.json()
    assert data["hydration_created"] == 1
