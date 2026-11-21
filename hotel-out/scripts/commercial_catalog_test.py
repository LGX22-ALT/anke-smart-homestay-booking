"""Integration checks against the running Vue application and real local API."""
from pathlib import Path
from urllib.parse import parse_qs, urlparse
from playwright.sync_api import sync_playwright, expect

BASE='http://127.0.0.1:2222'
OUT=Path(__file__).resolve().parents[1]/'test-artifacts'
OUT.mkdir(exist_ok=True)
with sync_playwright() as p:
    browser=p.chromium.launch()
    page=browser.new_page(viewport={'width':1440,'height':1000})
    page.emulate_media(reduced_motion='reduce')
    errors=[]
    page.on('pageerror',lambda error:errors.append(str(error)))
    page.goto(BASE,wait_until='networkidle')
    for city in ['北京','上海','广州','深圳','杭州','成都','厦门','大理']:
        page.locator('.city-tabs button').filter(has_text=city).click()
        expect(page.locator('.stay-card')).to_have_count(8 if city=='杭州' else 6)
        for detail in page.locator('.stay-card .card-detail').all_text_contents():
            assert city in detail,(city,detail)
        page.locator('#destinations .section-action').click()
        expect(page.locator('.attraction-card')).to_have_count(8 if city=='广州' else 6)
        assert all(city in s for s in page.locator('.attraction-card .city-badge').all_text_contents())
    page.locator('.attraction-card').first.click()
    page.get_by_role('button',name='查看大理住宿').click()
    page.wait_for_url('**/search?**')
    assert parse_qs(urlparse(page.url).query)['city']==['大理']
    page.goto(BASE+'/attractions',wait_until='networkidle')
    page.get_by_label('景点城市',exact=True).select_option('上海')
    expect(page.locator('.explore-card')).to_have_count(6)
    page.get_by_label('搜索景点',exact=True).fill('外滩')
    expect(page.locator('.explore-card')).to_have_count(1)
    page.locator('.explore-card').click()
    expect(page.get_by_role('link',name='查找上海住宿')).to_have_attribute('href','/search?city=%E4%B8%8A%E6%B5%B7')
    page.get_by_role('link',name='查找上海住宿').click()
    page.wait_for_url('**/search?**')
    page.goto(BASE+'/hotel/2000',wait_until='networkidle')
    expect(page.locator('body')).to_contain_text('舒适大床房')
    expect(page.locator('.description')).to_contain_text('不作为实际入住依据')
    page.goto(BASE,wait_until='networkidle')
    expect(page.locator('.booking-hero')).to_have_count(0)
    expect(page.locator('.home-main > .ai-stay')).to_be_visible()
    assert page.locator('.home-main > *').first.get_attribute('class') == 'ai-stay'
    page.locator('.suggestion-list button').first.click()
    expect(page.get_by_role('textbox',name='住宿需求',exact=True)).not_to_have_value('')
    page.screenshot(path=str(OUT/'commercial-home-desktop.png'),full_page=True)
    for width in [1440,1024,768,390,360]:
        page.set_viewport_size({'width':width,'height':844})
        assert page.evaluate('document.documentElement.scrollWidth <= innerWidth + 1'),width
    page.screenshot(path=str(OUT/'commercial-home-mobile.png'),full_page=True)
    assert not errors,errors
    browser.close()
print('PASS: 8 cities, hotel and attraction counts, city links, room detail, AI first-screen entry and suggestions, responsive widths; no runtime errors.')
