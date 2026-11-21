import os
from pathlib import Path
from urllib.parse import urlparse, parse_qs
from playwright.sync_api import sync_playwright, expect

BASE = os.environ.get("HOTEL_BASE_URL", "http://127.0.0.1:2222")
OUT = Path(__file__).parent / "artifacts"
OUT.mkdir(exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width":1440,"height":1000})
    errors = []
    requests = []
    page.on("pageerror", lambda e: errors.append(str(e)))
    page.on("request", lambda r: requests.append(r.url) if urlparse(r.url).path=="/api/hotels" else None)
    page.goto(BASE, wait_until="networkidle")
    page.get_by_role("button", name="切换城市", exact=True).click()
    page.get_by_role("button", name="杭州", exact=True).last.click()
    expect(page.get_by_role("button", name="切换城市", exact=True)).to_have_text("杭州")
    expect(page.locator(".suggestion-list")).to_contain_text("杭州")
    expect(page.locator(".city-tabs button.active")).to_have_text("杭州")
    page.wait_for_timeout(300)
    assert any(parse_qs(urlparse(url).query).get("city")==["杭州"] for url in requests)
    page.reload(wait_until="networkidle")
    expect(page.get_by_role("button", name="切换城市", exact=True)).to_have_text("杭州")
    page.get_by_role("button", name="切换城市", exact=True).click()
    page.get_by_label("搜索城市", exact=True).fill("广州")
    expect(page.locator(".city-options button")).to_have_count(1)
    page.locator(".city-options button").click()
    page.locator(".site-nav-item").filter(has_text="酒店民宿").click()
    page.wait_for_url("**/search?**")
    assert parse_qs(urlparse(page.url).query)["city"]==["广州"]
    page.goto(BASE + "/search?city=杭州&district=西湖区&guests=2&sort=priceAsc", wait_until="networkidle")
    page.get_by_role("button", name="切换城市", exact=True).click()
    page.get_by_role("button", name="上海", exact=True).last.click()
    page.wait_for_function("location.search.includes(encodeURIComponent('上海'))")
    query = parse_qs(urlparse(page.url).query)
    assert query["city"]==["上海"] and query["guests"]==["2"] and query["sort"]==["priceAsc"]
    assert "district" not in query
    expect(page.get_by_label("目的地")).to_have_value("上海")
    page.locator(".user-menu-trigger").hover()
    expect(page.get_by_role("navigation",name="账户功能").get_by_role("link",name="民宿订单")).to_be_visible()
    page.get_by_role("navigation",name="账户功能").get_by_role("link",name="民宿订单").click()
    page.wait_for_url("**/login?**")
    page.goto(BASE,wait_until="networkidle")
    page.get_by_role("button",name="切换城市",exact=True).click()
    page.get_by_role("button",name="全国",exact=True).click()
    expect(page.locator(".city-picker")).not_to_be_visible()
    expect(page.locator(".ai-stay")).to_be_visible()
    page.screenshot(path=str(OUT/"navigation-desktop.png"),full_page=True)
    for width in [1440,1200,1024,980,768,390,360]:
        page.set_viewport_size({"width":width,"height":844})
        assert page.evaluate("document.documentElement.scrollWidth <= innerWidth + 1"),width
        expect(page.get_by_role("button",name="切换城市",exact=True)).to_be_visible()
    page.get_by_role("button",name="切换城市",exact=True).click()
    expect(page.locator(".city-picker")).to_be_visible()
    assert page.locator(".city-picker").bounding_box()["x"] >= 0
    page.get_by_role("button",name="北京",exact=True).last.click()
    expect(page.locator(".city-picker")).not_to_be_visible()
    page.screenshot(path=str(OUT/"navigation-mobile.png"),full_page=True)
    assert not errors,errors
    browser.close()
    print("Navigation, city persistence, filters, login guard and responsive checks passed.")
