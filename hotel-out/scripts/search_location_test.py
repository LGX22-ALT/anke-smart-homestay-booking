from pathlib import Path

from playwright.sync_api import sync_playwright


BASE_URL = "http://127.0.0.1:5173"
OUTPUT = Path(__file__).resolve().parent / "artifacts"
OUTPUT.mkdir(exist_ok=True)


with sync_playwright() as playwright:
    browser = playwright.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1000, "height": 900})
    errors = []
    page.on("pageerror", lambda error: errors.append(str(error)))
    page.route(
        "**/api/hotels**",
        lambda route: route.fulfill(
            status=200,
            content_type="application/json",
            body='{"code":200,"message":"操作成功","data":{"total":0,"pages":0,"list":[]}}',
        ),
    )

    page.goto(f"{BASE_URL}/search", wait_until="networkidle")
    selects = page.locator(".filter-band select")
    city = selects.nth(0)
    district = selects.nth(1)

    assert city.locator("option").all_text_contents() == ["全部", "北京", "上海", "广州", "深圳"]
    assert district.is_disabled()
    assert district.locator("option").all_text_contents() == ["请先选择城市"]

    city.select_option("广州")
    page.wait_for_load_state("networkidle")
    assert not district.is_disabled()
    assert district.locator("option").all_text_contents() == [
        "全部区域",
        "越秀区",
        "海珠区",
        "荔湾区",
        "天河区",
        "白云区",
        "黄埔区",
        "番禺区",
        "花都区",
        "南沙区",
        "从化区",
        "增城区",
    ]
    assert "city=%E5%B9%BF%E5%B7%9E" in page.url

    district.select_option("天河区")
    page.wait_for_load_state("networkidle")
    assert "district=%E5%A4%A9%E6%B2%B3%E5%8C%BA" in page.url
    page.screenshot(path=str(OUTPUT / "search-city-district.png"), full_page=True)

    browser.close()
    assert not errors, errors
    print("Search location checks passed: four cities, linked districts, URL state")
