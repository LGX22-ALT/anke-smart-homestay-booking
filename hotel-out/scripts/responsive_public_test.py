from pathlib import Path
from playwright.sync_api import sync_playwright,expect

OUT=Path(__file__).resolve().parents[1]/'test-artifacts/responsive-layout'
with sync_playwright() as p:
    browser=p.chromium.launch();page=browser.new_page(reduced_motion='reduce');errors=[]
    page.on('pageerror',lambda e:errors.append(str(e)))
    for path in ['/','/search?city=广州','/hotel/1','/attractions','/guides','/login']:
        page.goto('http://127.0.0.1:2222'+path,wait_until='networkidle')
        for width in [1440,390]:
            page.set_viewport_size({'width':width,'height':900})
            page.evaluate('new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)))')
            assert page.evaluate('document.documentElement.scrollWidth<=innerWidth+1'),(path,width)
            if path!='/login':
                nav=page.get_by_role('navigation',name='手机导航')
                if width==390:expect(nav).to_be_visible()
                else:expect(nav).not_to_be_visible()
        if path=='/':page.screenshot(path=str(OUT/'home-real-api-mobile.png'),full_page=True)
    response=page.request.get('http://127.0.0.1:2222/api/hotels?pageNum=1&pageSize=1')
    assert response.ok and response.json()['code']==200
    assert not errors,errors
    browser.close()
print('PASS: six public pages at desktop/mobile widths with real local API; hotel API business code 200, no runtime errors.')
