"""Runs against the isolated H2 backend started by ProfileAvatarTests; no auth traces."""
from pathlib import Path
import io, os, subprocess, time, urllib.request
from PIL import Image
from playwright.sync_api import sync_playwright, expect

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'test-artifacts/profile-avatar'
OUT.mkdir(parents=True,exist_ok=True)
BASE='http://127.0.0.1:2231'
env={**os.environ,'VITE_BACKEND_URL':os.environ['PROFILE_BACKEND_URL'],'VITE_USE_MOCK':'false'}
command=['node',str(ROOT/'node_modules/vite/bin/vite.js'),'--port','2231','--strictPort']
log=(OUT/'vite.log').open('w',encoding='utf-8')
server=subprocess.Popen(command,cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT)
try:
    for attempt in range(80):
        try:
            urllib.request.urlopen(BASE,timeout=1).close()
            break
        except Exception:
            if server.poll() is not None: raise RuntimeError('Vite failed to start')
            time.sleep(.25)
    else: raise RuntimeError('Vite startup timeout')
    with sync_playwright() as p:
        browser=p.chromium.launch()
        for role,account,path in [('user','13800000001','/user/profile'),('merchant','13800000002','/merchant/profile')]:
            context=browser.new_context(viewport={'width':1440,'height':1000},reduced_motion='reduce')
            page=context.new_page()
            errors=[]
            page.on('pageerror',lambda error:errors.append(str(error)))
            def login():
                page.goto(BASE+'/login?redirect='+path,wait_until='networkidle')
                page.get_by_label('手机号',exact=True).fill(account)
                page.get_by_label('密码',exact=True).fill('ProfileTest@123')
                page.locator('.login-button').click()
                page.wait_for_url('**'+path)
                expect(page.get_by_label('个人名称',exact=True)).to_be_visible()
            login()
            editor=page.locator('.identity-editor')
            expect(editor.locator('.default-avatar-list button')).to_have_count(6)
            assert editor.locator('.default-avatar-list img').evaluate_all('(imgs)=>imgs.every(i=>i.complete && i.naturalWidth>0)')
            name='林间旅人' if role=='user' else '山间小宿主理人'
            page.get_by_label('个人名称',exact=True).fill('   ')
            editor.get_by_role('button',name='保存修改',exact=True).click()
            expect(editor.get_by_role('alert')).to_contain_text('名称')
            page.get_by_label('个人名称',exact=True).fill('  '+name+'  ')
            editor.get_by_role('button',name='使用海风头像',exact=True).click()
            expect(editor.get_by_role('button',name='使用海风头像',exact=True)).to_have_attribute('aria-pressed','true')
            page.screenshot(path=str(OUT/f'{role}-default-avatar.png'),full_page=True)
            editor.get_by_role('button',name='保存修改',exact=True).click()
            expect(editor.get_by_role('status')).to_have_text('个人资料已保存')
            expect(page.get_by_label('个人名称',exact=True)).to_have_value(name)
            page.reload(wait_until='networkidle')
            expect(page.get_by_label('个人名称',exact=True)).to_have_value(name)
            expect(editor.get_by_role('button',name='使用海风头像',exact=True)).to_have_attribute('aria-pressed','true')
            image=io.BytesIO();Image.new('RGB',(120,80),'#477586').save(image,format='PNG')
            file=page.get_by_label('上传头像文件',exact=True)
            file.set_input_files({'name':'avatar.txt','mimeType':'text/plain','buffer':b'bad'})
            expect(editor.get_by_role('alert')).to_contain_text('JPG')
            file.set_input_files({'name':'large.png','mimeType':'image/png','buffer':bytes(5*1024*1024+1)})
            expect(editor.get_by_role('alert')).to_contain_text('5MB')
            file.set_input_files({'name':'profile.png','mimeType':'image/png','buffer':image.getvalue()})
            expect(editor.get_by_alt_text('头像预览')).to_have_attribute('src',__import__('re').compile('blob:'))
            # A failed save keeps the selected file and entered name for a retry.
            page.route('**/api/user/profile',lambda route:route.fulfill(status=200,json={'code':500,'message':'保存失败，请重试'}))
            editor.get_by_role('button',name='保存修改',exact=True).click()
            expect(editor.get_by_role('alert')).to_be_visible()
            expect(page.get_by_label('个人名称',exact=True)).to_have_value(name)
            page.unroute('**/api/user/profile')
            editor.get_by_role('button',name='保存修改',exact=True).click()
            expect(editor.get_by_role('status')).to_have_text('个人资料已保存')
            avatar=editor.get_by_alt_text('头像预览').get_attribute('src')
            assert avatar.startswith('/uploads/avatars/')
            assert page.request.get(BASE+avatar).ok
            header=page.locator('.site-header' if role=='user' else '.office-topbar')
            expect(header).to_contain_text(name)
            expect(header.locator('.account-avatar img')).to_have_attribute('src',avatar)
            page.reload(wait_until='networkidle')
            expect(editor.get_by_alt_text('头像预览')).to_have_attribute('src',avatar)
            page.screenshot(path=str(OUT/f'{role}-desktop.png'),full_page=True)
            for width in [1024,768,390,360]:
                page.set_viewport_size({'width':width,'height':900})
                page.evaluate('new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)))')
                assert page.evaluate('document.documentElement.scrollWidth<=innerWidth+1'),(role,width)
                if role=='user' and width==360:
                    boxes=page.locator('.center-nav nav').evaluate('''nav=>({
                        nav:nav.getBoundingClientRect().toJSON(),
                        links:[...nav.querySelectorAll('a')].map(a=>({text:a.textContent,rect:a.getBoundingClientRect().toJSON()}))
                    })''')
                    for item in boxes['links']:
                        box=item['rect']; nav=boxes['nav']
                        assert box['x']>=nav['x']-1 and box['right']<=nav['right']+1,boxes
            page.screenshot(path=str(OUT/f'{role}-mobile.png'),full_page=True)
            # Clear only the isolated browser session, then actually sign in again.
            page.evaluate('localStorage.clear()')
            page.set_viewport_size({'width':1440,'height':1000})
            login()
            expect(page.get_by_label('个人名称',exact=True)).to_have_value(name)
            expect(editor.get_by_alt_text('头像预览')).to_have_attribute('src',avatar)
            assert not errors,errors
            context.close()
        browser.close()
    print('PASS: user and merchant profile, six defaults, real upload, validation, failed-save retry, header sync, refresh, re-login, five widths; no runtime errors.')
finally:
    server.terminate()
    try: server.wait(timeout=5)
    except subprocess.TimeoutExpired: server.kill()
    log.close()
