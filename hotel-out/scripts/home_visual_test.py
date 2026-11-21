import json
import os
from pathlib import Path

from playwright.sync_api import expect, sync_playwright


BASE_URL = os.environ.get("HOTEL_BASE_URL", "http://127.0.0.1:5173")
OUTPUT = Path(__file__).resolve().parent / "artifacts"
OUTPUT.mkdir(exist_ok=True)


def assert_no_overflow(page):
    dimensions = page.evaluate(
        "({ viewport: innerWidth, content: document.documentElement.scrollWidth })"
    )
    assert dimensions["content"] <= dimensions["viewport"] + 1, dimensions


with sync_playwright() as playwright:
    browser = playwright.chromium.launch(headless=True)
    errors = []
    desktop = browser.new_page(viewport={"width": 1440, "height": 1000})
    desktop.on("pageerror", lambda error: errors.append(str(error)))
    desktop.goto(BASE_URL, wait_until="domcontentloaded")
    expect(desktop.locator("#featured")).to_have_attribute("aria-busy", "false", timeout=20000)
    cards = desktop.locator(".stay-card")
    assert 0 < cards.count() <= 8
    assert desktop.locator("#featured").bounding_box()["y"] < desktop.locator("#destinations").bounding_box()["y"]
    assert len(desktop.locator("#featured .recommend-grid").evaluate(
        "el => getComputedStyle(el).gridTemplateColumns.split(' ')"
    )) == 4
    first_url = cards.first.get_attribute("href")
    cards.first.click()
    expect(desktop).to_have_url(BASE_URL + first_url)
    desktop.goto(BASE_URL, wait_until="domcontentloaded")
    expect(desktop.locator(".stay-card").first).to_be_visible(timeout=20000)

    expect(desktop.locator(".attraction-card")).to_have_count(4)
    desktop.locator("#destinations .section-action").click()
    expect(desktop.locator(".attraction-card")).to_have_count(5)
    desktop.locator(".attraction-card").first.click()
    expect(desktop.get_by_role("dialog")).to_be_visible()
    expect(desktop.locator(".dialog-description")).not_to_be_empty()
    desktop.locator(".el-dialog__headerbtn").click()
    desktop.locator("#destinations .section-action").click()
    desktop.locator("#destinations").scroll_into_view_if_needed()
    desktop.wait_for_function(
        "() => [...document.querySelectorAll('main img')].every(img => img.complete)",
        timeout=20000,
    )
    statuses = desktop.locator("main img").evaluate_all(
        "images => images.map(img => ({alt: img.alt, loaded: img.naturalWidth > 0, fallback: img.hidden}))"
    )
    assert all(item["loaded"] or item["fallback"] for item in statuses), statuses
    assert_no_overflow(desktop)
    desktop.evaluate("scrollTo(0, 0)")
    desktop.screenshot(path=str(OUTPUT / "home-recommendations-desktop.png"), full_page=True)

    mobile = browser.new_page(viewport={"width": 390, "height": 844})
    mobile.on("pageerror", lambda error: errors.append(str(error)))
    mobile.goto(BASE_URL, wait_until="domcontentloaded")
    expect(mobile.locator(".stay-card").first).to_be_visible(timeout=20000)
    assert len(mobile.locator("#featured .recommend-grid").evaluate(
        "el => getComputedStyle(el).gridTemplateColumns.split(' ')"
    )) == 2
    assert_no_overflow(mobile)
    mobile.locator("#destinations").scroll_into_view_if_needed()
    mobile.wait_for_function(
        "() => [...document.querySelectorAll('main img')].every(img => img.complete)",
        timeout=20000,
    )
    mobile.evaluate("scrollTo(0, 0)")
    mobile.screenshot(path=str(OUTPUT / "home-recommendations-mobile.png"), full_page=True)

    # A failed hotel request must leave attractions usable and allow retry.
    failure = browser.new_page(viewport={"width": 1440, "height": 1000})
    failure.route("**/api/hotels?*", lambda route: route.fulfill(
        status=500, content_type="application/json",
        body=json.dumps({"code": 500, "message": "Unavailable", "data": None}),
    ))
    failure.goto(BASE_URL, wait_until="domcontentloaded")
    expect(failure.locator(".section-empty button")).to_be_visible(timeout=20000)
    expect(failure.locator(".attraction-card")).to_have_count(4)
    failure.unroute("**/api/hotels?*")
    failure.locator(".section-empty button").click()
    expect(failure.locator(".stay-card").first).to_be_visible(timeout=20000)

    browser.close()
    assert not errors, errors
    print(json.dumps({"result": "passed", "images": statuses}, ensure_ascii=True))
