import os
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

BASE = os.environ.get("HOTEL_BASE_URL", "http://127.0.0.1:2223")
OUT = Path(__file__).parent / "artifacts"
OUT.mkdir(exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 1440, "height": 1000})
    errors = []
    page.on("pageerror", lambda error: errors.append(str(error)))
    page.goto(BASE, wait_until="networkidle")
    expect(page.locator(".stay-card").first).to_be_visible()
    page.screenshot(path=str(OUT / "portal-home-desktop.png"), full_page=True)
    page.get_by_label("目的地城市").select_option(label="杭州")
    page.get_by_role("button", name="搜索住宿").click()
    expect(page).to_have_url(__import__("re").compile(".*search.*city=.*"))
    expect(page.locator(".result-card").first).to_be_visible()
    assert "checkIn=" in page.url and "checkOut=" in page.url
    page.get_by_role("button", name="加入对比").first.click()
    first_link = page.locator(".result-image").first
    assert "checkIn=" in first_link.get_attribute("href")
    first_link.click()
    expect(page.locator(".title-copy h1")).not_to_be_empty()
    page.goto(BASE + "/history", wait_until="networkidle")
    expect(page.locator(".explore-card")).to_have_count(1)
    page.reload(wait_until="networkidle")
    expect(page.locator(".explore-card")).to_have_count(1)
    page.goto(BASE + "/compare", wait_until="networkidle")
    expect(page.locator(".compare-grid article")).to_have_count(1)
    page.get_by_role("button", name="移出对比").click()
    expect(page.locator(".empty")).to_be_visible()
    page.goto(BASE + "/attractions", wait_until="networkidle")
    expect(page.locator(".explore-card")).to_have_count(5)
    page.get_by_label("搜索景点").fill("广州塔")
    expect(page.locator(".explore-card")).to_have_count(1)
    page.locator(".explore-card").click()
    expect(page.locator(".detail h1")).to_have_text("广州塔")
    page.reload(wait_until="networkidle")
    expect(page.locator(".detail h1")).to_have_text("广州塔")
    page.goto(BASE + "/guides", wait_until="networkidle")
    expect(page.locator(".explore-card")).to_have_count(3)
    page.locator(".explore-card").first.click()
    expect(page.locator(".itinerary")).to_have_count(2)
    page.goto(BASE + "/help", wait_until="networkidle")
    page.locator(".faq summary").first.click()
    expect(page.locator(".faq p").first).to_be_visible()
    page.goto(BASE + "/user/orders", wait_until="networkidle")
    assert "/login?redirect=" in page.url
    page.goto(BASE + "/search?maxPrice=500", wait_until="networkidle")
    expect(page.locator(".result-card")).to_have_count(0)
    page.goto(BASE + "/search?maxPrice=600", wait_until="networkidle")
    expect(page.locator(".result-card")).to_have_count(1)
    page.goto(BASE + "/search?sort=priceAsc", wait_until="networkidle")
    prices = page.locator(".result-foot p strong").all_text_contents()
    assert len(prices) == 2
    assert int(prices[0].replace("¥","")) <= int(prices[1].replace("¥",""))
    for path in ["/", "/attractions", "/guides/guangzhou-weekend", "/search", "/compare", "/history", "/help"]:
        page.set_viewport_size({"width":390, "height":844})
        page.goto(BASE + path, wait_until="networkidle")
        assert page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1"), path
    page.goto(BASE, wait_until="networkidle")
    page.screenshot(path=str(OUT / "portal-home-mobile.png"), full_page=True)
    assert not errors, errors
    browser.close()
    print("Portal navigation, persistence, search filters, auth and mobile checks passed.")
