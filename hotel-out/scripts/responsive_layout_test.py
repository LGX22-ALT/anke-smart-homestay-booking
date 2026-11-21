"""Same URLs with desktop, tablet, mobile and landscape layouts, offline fixtures only."""
from pathlib import Path
import json,os,subprocess,time,urllib.request
from playwright.sync_api import sync_playwright,expect

ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'test-artifacts/responsive-layout';OUT.mkdir(parents=True,exist_ok=True)
BASE='http://127.0.0.1:2233'
log=(OUT/'vite.log').open('w',encoding='utf-8')
server=subprocess.Popen(['node',str(ROOT/'node_modules/vite/bin/vite.js'),'--port','2233','--strictPort'],cwd=ROOT,env={**os.environ,'VITE_USE_MOCK':'true'},stdout=log,stderr=subprocess.STDOUT)
results=[]
try:
    for _ in range(80):
        try:urllib.request.urlopen(BASE,timeout=1).close();break
        except Exception:time.sleep(.25)
    else:raise RuntimeError('Vite startup failed')
    with sync_playwright() as p:
        browser=p.chromium.launch()
        for role,paths in [
            ('user',['/','/search','/search?city=广州','/hotel/1','/attractions','/guides','/history','/compare','/help','/user/orders','/user/profile','/user/favorites','/user/security','/user/recharge','/order/confirm','/pay/result']),
            ('merchant',['/merchant/dashboard','/merchant/hotels','/merchant/calendar','/merchant/orders','/merchant/messages','/merchant/comments','/merchant/statistics','/merchant/profile']),
            ('admin',['/admin/dashboard','/admin/users','/admin/audits','/admin/orders','/admin/comments','/admin/ai','/admin/config']),
            ('guest',['/login'])]:
            context=browser.new_context(reduced_motion='reduce')
            if role!='guest':context.add_init_script("localStorage.setItem('hotel_token','mock-responsive');localStorage.setItem('hotel_user_role',"+json.dumps(role)+");localStorage.setItem('hotel_user_info',JSON.stringify({id:1,name:'界面测试',phone:'13800000003'}));")
            page=context.new_page();errors=[];page.on('pageerror',lambda e:errors.append(str(e)))
            for path in paths:
                page.goto(BASE+path,wait_until='networkidle')
                for width,height in [(1440,1000),(1024,900),(820,900),(768,1024),(390,844),(360,800),(667,375)]:
                    page.set_viewport_size({'width':width,'height':height})
                    page.evaluate('new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)))')
                    assert page.evaluate('document.documentElement.scrollWidth<=innerWidth+1'),(path,width,'overflow')
                    if page.locator('.site-header').count():
                        if width<=768:
                            nav=page.get_by_role('navigation',name='手机导航')
                            expect(nav).to_be_visible();expect(nav.get_by_role('link')).to_have_count(4)
                            assert nav.bounding_box()['y']+nav.bounding_box()['height']<=height+1
                            assert page.locator('.app-shell').evaluate('(e)=>parseFloat(getComputedStyle(e).paddingBottom)')>=60
                            expect(page.locator('.site-nav')).not_to_be_visible()
                        else:
                            expect(page.locator('.site-nav')).to_be_visible()
                            expect(page.get_by_role('navigation',name='手机导航')).not_to_be_visible()
                    if width in [1440,390] and path in ['/','/search','/hotel/1','/login','/merchant/profile']:
                        page.screenshot(path=str(OUT/((path.strip('/').replace('/','-') or 'home')+f'-{width}.png')),full_page=True)
                    results.append({'path':path,'width':width,'height':height})
            if role in ['admin','merchant']:
                page.set_viewport_size({'width':390,'height':844})
                toggle=page.get_by_role('button',name='打开导航',exact=True);toggle.click()
                expect(page.get_by_role('button',name='收起手机导航')).to_be_focused()
                page.keyboard.press('Shift+Tab');page.keyboard.press('Tab')
                expect(page.get_by_role('button',name='收起手机导航')).to_be_focused()
                page.keyboard.press('Escape');expect(toggle).to_be_focused()
                toggle.click();page.locator('.office-backdrop').click(position={'x':350,'y':500})
                expect(toggle).to_have_attribute('aria-expanded','false')
                toggle.click();page.set_viewport_size({'width':1440,'height':1000})
                expect(page.locator('.office-main')).not_to_have_attribute('inert','')
            if role=='user':
                page.set_viewport_size({'width':390,'height':844});page.goto(BASE+'/',wait_until='networkidle')
                nav=page.get_by_role('navigation',name='手机导航');nav.get_by_role('link',name='酒店',exact=True).click();page.wait_for_url('**/search**')
                nav.get_by_role('link',name='我的',exact=True).click();page.wait_for_url('**/user/profile')
                page.get_by_role('button',name='打开账户菜单',exact=True).click()
                page.locator('.mobile-menu-signout').click()
                expect(page.get_by_role('button',name='登录或注册',exact=True)).to_be_visible()
                nav.get_by_role('link',name='我的',exact=True).click();page.wait_for_url('**/login?**')
            assert not errors,errors
            context.close()
        phone=browser.new_context(**p.devices['iPhone 13'])
        page=phone.new_page();page.goto(BASE+'/',wait_until='networkidle')
        nav=page.get_by_role('navigation',name='手机导航');expect(nav).to_be_visible()
        nav.get_by_role('link',name='酒店',exact=True).tap();page.wait_for_url('**/search**')
        page.set_viewport_size({'width':844,'height':390});expect(nav).to_be_visible()
        expect(page.locator('.site-nav')).not_to_be_visible()
        phone.close();browser.close()
    (OUT/'results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
    print(f'PASS: {len(results)} layout checks; desktop/mobile navigation, touch links, drawer Escape/backdrop/focus/resize; no runtime errors.')
finally:
    server.terminate()
    try:server.wait(timeout=5)
    except subprocess.TimeoutExpired:server.kill()
    log.close()
