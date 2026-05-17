def test_delete_account_wrong_password(client, auth_headers):
    resp = client.post(
        "/auth/delete-account",
        json={"password": "wrong-password-xyz"},
        headers=auth_headers,
    )
    assert resp.status_code == 400


def test_delete_account_then_login_fails(client):
    import uuid

    from app.services.registration_email import LAST_SENT_CODES

    email = f"del_{uuid.uuid4().hex[:8]}@example.com"
    password = "testpassword123"
    reg = client.post(
        "/auth/register/start",
        json={"email": email, "password": password},
    )
    assert reg.status_code == 200, reg.text
    code = LAST_SENT_CODES[email.lower()]
    reg2 = client.post(
        "/auth/register/complete",
        json={"email": email, "password": password, "code": code},
    )
    assert reg2.status_code == 200, reg2.text
    token = reg2.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    del_resp = client.post(
        "/auth/delete-account",
        json={"password": password},
        headers=headers,
    )
    assert del_resp.status_code == 200, del_resp.text

    login = client.post(
        "/auth/login",
        data={"username": email, "password": password},
    )
    assert login.status_code == 401
