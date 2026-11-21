"""Default avatar and name persistence in the existing offline demo mode."""
from pathlib import Path
import os, subprocess, time, urllib.request
from playwright.sync_api import sync_playwright, expect

ROOT=Path(__file__).resolve().parents[1]
env={**os.environ,'VITE_USE_MOCK':'true'}
log=(ROOT/'test-artifacts/profile-mock-vite.log').open('w',encoding='utf-8')
server=subprocess.Popen(['node',str(ROOT/'node_modules/vite/bin/vite.js'),'--port','2232','--strictPort'],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT)
try:
    for attempt in range(80):
        try: urllib.request.urlopen('http://127.0.0.1:2232',timeout=1).close();break
        except Exception:
            if server.poll() is not None: raise RuntimeError('Vite failed to start')
            time.sleep(.25)
    else: raise RuntimeError('Vite startup timeout')
    with sync_playwright() as p:
        browser=p.chromium.launch()
        for role,path in [('user','/user/profile'),('merchant','/merchant/profile')]:
            context=browser.new_context()
            page=context.new_page()
            page.goto('http://127.0.0.1:2232',wait_until='networkidle')
            page.evaluate('''role=>{
                localStorage.setItem('hotel_token','demo-profile-test');
                localStorage.setItem('hotel_user_role',role);
                localStorage.setItem('hotel_user_info',JSON.stringify({id:11,name:'初始名称',role,phone:'13800000003',avatar:'/avatars/forest.svg'}));
            }''',role)
            page.goto('http://127.0.0.1:2232'+path,wait_until='networkidle')
            page.get_by_label('个人名称',exact=True).fill('新的个人名称')
            page.get_by_role('button',name='使用晴空头像',exact=True).click()
            page.get_by_role('button',name='保存修改',exact=True).click()
            expect(page.locator('.identity-success')).to_have_text('个人资料已保存')
            page.reload(wait_until='networkidle')
            expect(page.get_by_label('个人名称',exact=True)).to_have_value('新的个人名称')
            expect(page.get_by_role('button',name='使用晴空头像',exact=True)).to_have_attribute('aria-pressed','true')
            header=page.locator('.site-header' if role=='user' else '.office-topbar')
            expect(header).to_contain_text('新的个人名称')
            expect(header.locator('.account-avatar img')).to_have_attribute('src','/avatars/sky.svg')
            context.close()
        browser.close()
    print('PASS: offline demo user and merchant names/default avatars persist and sync to headers.')
finally:
    server.terminate()
    try: server.wait(timeout=5)
    except subprocess.TimeoutExpired: server.kill()
    log.close()
