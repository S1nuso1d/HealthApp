"""Вызовы REST API FatSecret (OAuth 1.0).

Публичные методы вроде ``foods.search`` и ``food.find_id_for_barcode`` — это
*signed* (не delegated) запросы: подпись только Consumer Key + Consumer Secret,
без resource owner token. См. https://platform.fatsecret.com/docs/guides/authentication
"""

from __future__ import annotations

import logging

import requests
from requests_oauthlib import OAuth1

logger = logging.getLogger(__name__)

FATSECRET_REST_URL = "https://platform.fatsecret.com/rest/server.api"


def _oauth_consumer_only(consumer_key: str, consumer_secret: str) -> OAuth1:
    return OAuth1(consumer_key, client_secret=consumer_secret)


def foods_search(
    consumer_key: str,
    consumer_secret: str,
    expression: str,
    max_results: int = 20,
) -> dict:
    auth = _oauth_consumer_only(consumer_key, consumer_secret)
    data = {
        "method": "foods.search",
        "format": "json",
        "search_expression": expression.strip()[:200],
        "max_results": str(max_results),
    }
    resp = requests.post(
        FATSECRET_REST_URL,
        data=data,
        auth=auth,
        timeout=30,
    )
    if not resp.ok:
        logger.warning("FatSecret HTTP %s: %s", resp.status_code, resp.text[:800])
    resp.raise_for_status()
    return resp.json()


def food_get_by_id(
    consumer_key: str,
    consumer_secret: str,
    food_id: str,
) -> dict:
    auth = _oauth_consumer_only(consumer_key, consumer_secret)
    data = {
        "method": "food.get",
        "format": "json",
        "food_id": food_id.strip(),
    }
    resp = requests.post(
        FATSECRET_REST_URL,
        data=data,
        auth=auth,
        timeout=30,
    )
    if not resp.ok:
        logger.warning("FatSecret HTTP %s: %s", resp.status_code, resp.text[:800])
    resp.raise_for_status()
    return resp.json()


def food_get_by_barcode(
    consumer_key: str,
    consumer_secret: str,
    barcode: str,
) -> dict:
    auth = _oauth_consumer_only(consumer_key, consumer_secret)
    data = {
        "method": "food.find_id_for_barcode",
        "format": "json",
        "barcode": barcode.strip(),
    }
    resp = requests.post(
        FATSECRET_REST_URL,
        data=data,
        auth=auth,
        timeout=30,
    )
    if not resp.ok:
        logger.warning("FatSecret HTTP %s: %s", resp.status_code, resp.text[:800])
    resp.raise_for_status()
    return resp.json()
