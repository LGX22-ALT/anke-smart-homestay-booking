from pathlib import Path
from urllib.parse import urlparse

from playwright.sync_api import sync_playwright


BASE_URL = "http://127.0.0.1:5173"
OUTPUT = Path(__file__).resolve().parent / "artifacts"
OUTPUT.mkdir(exist_ok=True)


def wait(page):
    page.wait_for_load_state("networkidle")


with sync_playwright() as playwright:
    browser = playwright.chromium.launch(headless=True)
    errors = []

    context = browser.new_context(viewport={"width": 1440, "height": 900})
    page = context.new_page()
    page.on("console", lambda message: errors.append(message.text) if message.type == "error" else None)
    page.on("pageerror", lambda error: errors.append(str(error)))
    page.goto(BASE_URL)
    wait(page)
    page.screenshot(path=str(OUTPUT / "home-desktop.png"), full_page=True)
    assert page.locator("body").count() == 1

    page.goto(f"{BASE_URL}/merchant/dashboard")
    wait(page)
    assert "/login" in page.url and "redirect=" in page.url
    context.close()

    context = browser.new_context(viewport={"width": 1280, "height": 800})
    page = context.new_page()
    page.goto(f"{BASE_URL}/login")
    wait(page)
    page.locator(".demo-accounts button").nth(0).click()
    wait(page)
    page.goto(f"{BASE_URL}/admin/dashboard")
    wait(page)
    assert urlparse(page.url).path == "/403", page.url
    context.close()

    for account_index, expected_path in ((1, "/merchant/dashboard"), (2, "/admin/dashboard")):
        context = browser.new_context(viewport={"width": 1280, "height": 800})
        page = context.new_page()
        page.goto(f"{BASE_URL}/login")
        wait(page)
        page.locator(".demo-accounts button").nth(account_index).click()
        page.wait_for_url(f"**{expected_path}*")
        wait(page)
        assert urlparse(page.url).path == expected_path, (account_index, page.url)
        context.close()

    context = browser.new_context(viewport={"width": 390, "height": 844})
    page = context.new_page()
    page.goto(BASE_URL)
    wait(page)
    dimensions = page.evaluate("({width: innerWidth, scroll: document.documentElement.scrollWidth})")
    assert dimensions["scroll"] <= dimensions["width"] + 1, dimensions
    page.screenshot(path=str(OUTPUT / "home-mobile.png"), full_page=True)
    context.close()

    browser.close()
    assert not errors, errors
    print("Playwright smoke checks passed: routes, roles, console, desktop, mobile")
