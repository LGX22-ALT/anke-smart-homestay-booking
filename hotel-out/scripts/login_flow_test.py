from urllib.parse import urlparse

from playwright.sync_api import sync_playwright


BASE_URL = "http://127.0.0.1:5173"


with sync_playwright() as playwright:
    browser = playwright.chromium.launch(headless=True)
    context = browser.new_context(viewport={"width": 1280, "height": 800})
    page = context.new_page()
    errors = []
    page.on("pageerror", lambda error: errors.append(str(error)))
    page.route(
        "**/*",
        lambda route: route.abort()
        if route.request.resource_type == "image"
        else route.continue_(),
    )

    page.goto(f"{BASE_URL}/login", wait_until="domcontentloaded")
    page.locator(".demo-accounts button").nth(0).click()
    page.wait_for_url(f"{BASE_URL}/", timeout=15_000)

    session = page.evaluate(
        """() => ({
            token: localStorage.getItem('hotel_token'),
            role: localStorage.getItem('hotel_user_role'),
            user: localStorage.getItem('hotel_user_info')
        })"""
    )
    assert urlparse(page.url).path == "/", page.url
    assert session["token"], session
    assert session["role"] == "user", session
    assert "13800000003" in session["user"], session
    assert not errors, errors

    context.close()
    browser.close()
    print("Login flow passed: API, session persistence, user redirect")
