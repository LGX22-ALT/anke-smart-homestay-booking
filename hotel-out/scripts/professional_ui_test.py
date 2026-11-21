"""Public pages against local API; protected screen checks use an isolated mock server."""
import json
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

OUT=Path(__file__).resolve().parents[1]/'test-artifacts/professional-ui'
OUT.mkdir(parents=True,exist_ok=True)
results=[]
failures=[]
with sync_playwright() as p:
    browser=p.chromium.launch()
    def visit(page,base,path,slug):
        errors=[]
        handler=lambda e:errors.append(str(e))
        page.on('pageerror',handler)
        page.set_viewport_size({'width':1440,'height':1000})
        page.goto(base+path,wait_until='domcontentloaded')
        page.locator('main').first.wait_for()
        page.wait_for_timeout(400)
        assert page.locator('body').inner_text().strip(),path
        for width in [1440,768,390,360]:
            page.set_viewport_size({'width':width,'height':900})
            page.wait_for_timeout(60)
            overflow=page.evaluate('document.documentElement.scrollWidth > innerWidth + 1')
            if overflow:
                nodes=page.evaluate("[...document.querySelectorAll('main *')].filter(e=>e.getBoundingClientRect().right>innerWidth+2&&getComputedStyle(e).position!=='absolute').slice(0,6).map(e=>e.className)")
                failures.append({'path':path,'width':width,'overflow':nodes})
            if width in [1440,390]: page.screenshot(path=str(OUT/f'{slug}-{width}.png'),full_page=True)
        if errors: failures.append({'path':path,'errors':errors})
        results.append({'path':path,'environment':'local-api' if base.endswith('2222') else 'mock','widths':[1440,768,390,360]})
        page.remove_listener('pageerror',handler)
    public=browser.new_page()
    for path,slug in [('/','home'),('/login','login'),('/search?city=广州','search'),('/hotel/1','detail'),('/attractions','attractions'),('/guides','guides'),('/guides/guangzhou-weekend','guide-detail'),('/history','history'),('/compare','compare'),('/help','help'),('/403','forbidden')]:
        visit(public,'http://127.0.0.1:2222',path,slug)
    public.goto('http://127.0.0.1:2222/login')
    public.get_by_role('button',name='登录',exact=True).last.click()
    expect(public.get_by_role('alert')).to_contain_text('手机号')
    public.get_by_role('button',name='注册',exact=True).click()
    expect(public.get_by_label('用户名',exact=True)).to_be_visible()
    public.get_by_role('button',name='商家登录',exact=True).click()
    expect(public.get_by_role('heading',name='商家登录',exact=True)).to_be_visible()
    public.get_by_label('密码',exact=True).fill('temporary-ui-test')
    public.get_by_role('button',name='显示密码').click()
    expect(public.get_by_label('密码',exact=True)).to_have_attribute('type','text')
    public.keyboard.press('Tab')
    assert public.evaluate("getComputedStyle(document.activeElement).outlineStyle !== 'none'"),'keyboard focus'
    public.emulate_media(reduced_motion='reduce')
    assert public.evaluate("parseFloat(getComputedStyle(document.querySelector('button')).transitionDuration) < .01"),'reduced motion'
    # No auth snapshots or real tokens are saved. Protected pages use local demonstration data.
    for role,paths in [
        ('user',['/user/orders','/user/favorites','/user/reviews','/user/profile','/user/security','/user/recharge','/order/confirm','/pay/result']),
        ('merchant',['/merchant/dashboard','/merchant/hotels','/merchant/calendar','/merchant/orders','/merchant/comments','/merchant/messages','/merchant/statistics']),
        ('admin',['/admin/dashboard','/admin/users','/admin/audits','/admin/orders','/admin/comments','/admin/ai','/admin/config'])]:
        context=browser.new_context()
        context.add_init_script("localStorage.setItem('hotel_token','mock-ui');localStorage.setItem('hotel_user_role',"+json.dumps(role)+");localStorage.setItem('hotel_user_info',JSON.stringify({id:1,name:'界面验收',phone:'13800000003'}));")
        page=context.new_page()
        for path in paths: visit(page,'http://127.0.0.1:2228',path,path.strip('/').replace('/','-'))
        if role=='user':
            page.goto('http://127.0.0.1:2228/hotel/1',wait_until='domcontentloaded')
            book=page.get_by_role('button',name='预订此房型',exact=True).first
            expect(book).to_be_enabled()
            book.click()
            page.wait_for_url('**/order/confirm')
            expect(page.locator('.summary h2')).not_to_have_text('尚未选择房源')
            for width in [1440,390]:
                page.set_viewport_size({'width':width,'height':900})
                assert page.evaluate('document.documentElement.scrollWidth <= innerWidth + 1'),'populated checkout overflow'
                page.screenshot(path=str(OUT/f'checkout-filled-{width}.png'),full_page=True)
        if role in ['admin','merchant']:
            page.set_viewport_size({'width':390,'height':900})
            page.get_by_role('button',name='打开导航',exact=True).click()
            expect(page.locator('.office-sidebar.open')).to_be_visible()
            page.locator('.office-sidebar nav a').first.click()
            expect(page.locator('.office-sidebar.open')).to_have_count(0)
        context.close()
    browser.close()
(OUT/'results.json').write_text(json.dumps({'pages':results,'failures':failures},ensure_ascii=False,indent=2),encoding='utf8')
print(json.dumps({'pages':len(results),'checks':len(results)*4,'failures':failures},ensure_ascii=False))
assert not failures,failures
