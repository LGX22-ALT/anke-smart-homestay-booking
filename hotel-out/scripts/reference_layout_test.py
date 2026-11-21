from pathlib import Path
from playwright.sync_api import sync_playwright, expect

OUT=Path(__file__).resolve().parents[1]/'test-artifacts/reference-layout'
OUT.mkdir(parents=True,exist_ok=True)
with sync_playwright() as p:
    browser=p.chromium.launch()
    page=browser.new_page(viewport={'width':1440,'height':1000},reduced_motion='reduce')
    errors=[]
    page.on('pageerror',lambda e:errors.append(str(e)))
    page.goto('http://127.0.0.1:2222/hotel/1',wait_until='networkidle')
    nav=page.get_by_role('navigation',name='房源详情导航')
    for label in ['房型','评价','介绍','商家','设施','周边']:
        button=nav.get_by_role('button',name=label,exact=True)
        button.click()
        expect(button).to_have_attribute('aria-current','location')
        expect(nav.locator('[aria-current="location"]')).to_have_count(1)
        indicator=nav.locator('.section-indicator')
        expect(indicator).to_be_visible()
        box=button.bounding_box(); line=indicator.bounding_box()
        assert abs((box['x']+box['width']/2)-(line['x']+line['width']/2))<2,label
        nav.screenshot(path=str(OUT/f'detail-nav-{button.get_attribute("data-section")}.png'))
    page.locator('#rooms').scroll_into_view_if_needed()
    page.evaluate("window.scrollTo(0,document.querySelector('#rooms').getBoundingClientRect().top+scrollY-90)")
    expect(nav.get_by_role('button',name='房型',exact=True)).to_have_attribute('aria-current','location')
    page.screenshot(path=str(OUT/'detail-desktop.png'),full_page=True)
    for width in [390,360]:
        page.set_viewport_size({'width':width,'height':844})
        nav.get_by_role('button',name='周边',exact=True).click()
        expect(nav.get_by_role('button',name='周边',exact=True)).to_have_attribute('aria-current','location')
        assert page.evaluate('document.documentElement.scrollWidth <= innerWidth+1'),width
    page.set_viewport_size({'width':1440,'height':1000})
    page.emulate_media(reduced_motion='no-preference')
    for section in ['overview','facilities','reviews','rooms','surroundings']:
        button=nav.locator(f'[data-section="{section}"]')
        button.click()
        page.wait_for_timeout(1100)
        expect(button).to_have_attribute('aria-current','location')
        box=button.bounding_box(); line=nav.locator('.section-indicator').bounding_box()
        assert abs(box['x']-line['x'])<2,section
    page.mouse.wheel(0,-1200)
    page.wait_for_timeout(500)
    assert nav.locator('[aria-current="location"]').get_attribute('data-section')!='surroundings'
    page.emulate_media(reduced_motion='reduce')
    page.goto('http://127.0.0.1:2222/',wait_until='networkidle')
    expect(page.locator('.home-main > .ai-stay')).to_be_visible()
    expect(page.locator('.booking-hero')).to_have_count(0)
    assert page.locator('.assistant-composer').evaluate('(e)=>getComputedStyle(e).borderRadius')=='24px'
    assert 'linear-gradient' in page.locator('.home-page').evaluate('(e)=>getComputedStyle(e).backgroundImage')
    page.locator('.suggestion-list button').first.click()
    expect(page.get_by_role('textbox',name='住宿需求',exact=True)).not_to_have_value('')
    for width in [1770,1440,1024,768,390,360]:
        page.set_viewport_size({'width':width,'height':1000})
        assert page.evaluate('document.documentElement.scrollWidth <= innerWidth+1'),width
        main=page.locator('.home-main').bounding_box()
        assert abs(main['x']-(width-main['width'])/2)<2,width
        if width in [1440,390]:page.screenshot(path=str(OUT/f'home-{width}.png'),full_page=True)
    assert not errors,errors
    browser.close()
print('PASS: six section links, moving underline, scroll sync, narrow nav; centered homepage at six widths, AI suggestions and no runtime errors.')
