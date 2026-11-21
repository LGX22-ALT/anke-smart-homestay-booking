"""Real isolated H2-backed browser checks; no auth traces or credentials in evidence."""
from pathlib import Path
import io, json, os, subprocess, time, urllib.request
from PIL import Image
from playwright.sync_api import sync_playwright, expect

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'test-artifacts/merchant-chat'
OUT.mkdir(parents=True,exist_ok=True)
BASE='http://127.0.0.1:2236'
env={**os.environ,'VITE_BACKEND_URL':os.environ['CHAT_BACKEND_URL'],'VITE_USE_MOCK':'false'}
log=(OUT/'vite.log').open('w',encoding='utf-8')
server=subprocess.Popen(['node',str(ROOT/'node_modules/vite/bin/vite.js'),'--port','2236','--strictPort'],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT)
checks=[]
try:
    for _ in range(100):
        try: urllib.request.urlopen(BASE,timeout=1).close(); break
        except Exception:
            if server.poll() is not None: raise RuntimeError('Vite startup failed')
            time.sleep(.2)
    else: raise RuntimeError('Vite startup timeout')
    with sync_playwright() as p:
        browser=p.chromium.launch()
        contexts=[]
        errors=[]
        def context(role,id):
            c=browser.new_context(viewport={'width':1440,'height':1000},reduced_motion='reduce')
            # Token stays in memory; never write storage state or traces.
            session={'hotel_token':os.environ['CHAT_'+role.upper()+'_TOKEN'],'hotel_user_info':json.dumps({'id':id,'name':'Name '+str(id),'avatar':'/avatars/forest.svg','role':role}),'hotel_user_role':role}
            c.add_init_script('(()=>{const v='+json.dumps(session)+';if(!localStorage.getItem("chat_test_seeded")){for(const [k,x] of Object.entries(v))localStorage.setItem(k,x);localStorage.setItem("chat_test_seeded","1")}})()')
            contexts.append(c)
            page=c.new_page();page.on('pageerror',lambda e:errors.append(str(e)))
            return page
        merchant=context('merchant',3)
        merchant.goto(BASE+'/merchant/profile')
        expect(merchant.locator('#profile-name')).to_have_value('Name 3')
        image=io.BytesIO();Image.new('RGB',(300,240),'#6246a0').save(image,format='PNG')
        merchant.locator('input[type=file]').set_input_files({'name':'host.png','mimeType':'image/png','buffer':image.getvalue()})
        merchant.locator('#profile-name').fill('广州民宿房东')
        merchant.get_by_role('button',name='保存修改',exact=True).click()
        expect(merchant.locator('.identity-success')).to_be_visible(timeout=10000)
        saved_profile=merchant.evaluate('JSON.parse(localStorage.getItem("hotel_user_info"))')
        avatar=saved_profile['avatar']
        assert avatar.startswith('/uploads/avatars/')
        checks.append('merchant uploads real image and saves name')

        user=context('user',1)
        user.goto(BASE+'/')
        host=user.locator('.stay-host').filter(has_text='广州民宿房东')
        expect(host).to_be_visible();expect(host.locator('img')).to_have_attribute('src',avatar)
        assert host.locator('img').evaluate('(img)=>img.complete && img.naturalWidth>0')
        host.screenshot(path=str(OUT/'home-host-avatar.png'))
        user.goto(BASE+'/hotel/1')
        expect(user.locator('.merchant-card img')).to_have_attribute('src',avatar)
        user.locator('.merchant-card').screenshot(path=str(OUT/'detail-host-avatar.png'))
        user.get_by_role('button',name='私信商家',exact=True).click()
        expect(user).to_have_url(BASE+'/messages/1')
        expect(user.get_by_text('从一句问候开始',exact=True)).to_be_visible()
        expect(user.locator('.chat-thread-heading img')).to_have_attribute('src',avatar)
        checks.append('home/detail show uploaded merchant avatar; enquiry opens dedicated route')

        user.locator('#chat-draft').fill('有停车位吗？')
        user.locator('#chat-draft').dispatch_event('keydown',{'key':'Enter','ctrlKey':True,'isComposing':True})
        expect(user.locator('#chat-draft')).to_have_value('有停车位吗？')
        expect(user.locator('.chat-message')).to_have_count(0)
        user.get_by_role('button',name='发送消息',exact=True).click()
        expect(user.locator('.chat-message p')).to_have_text(['有停车位吗？'])
        user.reload();expect(user.locator('.chat-message p')).to_have_text(['有停车位吗？'])
        checks.append('send persists after refresh; IME composition does not submit')

        merchant.goto(BASE+'/merchant/messages')
        expect(merchant.locator('.chat-message p')).to_have_text(['有停车位吗？'])
        merchant.locator('#chat-draft').fill('可以免费停车，入住前联系我即可。')
        merchant.get_by_role('button',name='发送消息',exact=True).click()
        expect(merchant.locator('#chat-draft')).to_have_value('')
        merchant.screenshot(path=str(OUT/'merchant-chat.png'))
        expect(user.locator('.chat-message p')).to_have_text(['有停车位吗？','可以免费停车，入住前联系我即可。'],timeout=12000)
        checks.append('merchant receives enquiry and user receives real reply through polling')

        user.route('**/api/messages/hotels/1',lambda route:route.fulfill(status=200,json={'code':500,'message':'测试发送失败'}) if route.request.method=='POST' else route.continue_())
        user.locator('#chat-draft').fill('谢谢，请问几点入住？')
        user.get_by_role('button',name='发送消息',exact=True).click()
        expect(user.locator('.chat-send-error')).to_be_visible();expect(user.locator('#chat-draft')).to_have_value('谢谢，请问几点入住？')
        user.unroute('**/api/messages/hotels/1')
        user.get_by_role('button',name='发送消息',exact=True).click()
        expect(user.locator('.chat-message p')).to_have_count(3)
        expect(user.locator('#chat-draft')).to_have_value('')
        checks.append('failed send retains draft and retry sends once')
        expect(user.locator('.el-message')).to_have_count(0,timeout=8000)
        for width,height in [(1440,1000),(1024,900),(768,1024),(390,844),(360,800)]:
            user.set_viewport_size({'width':width,'height':height})
            assert user.evaluate('document.documentElement.scrollWidth<=innerWidth+1'),f'overflow {width}'
            expect(user.locator('#chat-draft')).to_be_visible()
            if width<=768:
                user.get_by_role('button',name='返回会话列表').click()
                expect(user.locator('.chat-conversations')).to_be_visible()
                user.locator('.chat-conversation').first.click()
                expect(user.locator('#chat-draft')).to_be_visible()
            expect(user.locator('.chat-timeline')).to_have_attribute('aria-busy','false')
            user.screenshot(path=str(OUT/f'chat-{width}.png'))
        checks.append('five viewport sizes, mobile back/reopen, no document overflow')
        user.set_viewport_size({'width':390,'height':844})
        user.get_by_role('button',name='返回会话列表').click()
        merchant.locator('#chat-draft').fill('下午两点可以入住。')
        merchant.get_by_role('button',name='发送消息',exact=True).click()
        expect(user.locator('.chat-unread')).to_have_text('1',timeout=12000)
        user.locator('.chat-conversation').first.click()
        expect(user.locator('.chat-message p')).to_have_count(4)
        expect(user.locator('.chat-unread')).to_have_count(0)
        checks.append('mobile list keeps new reply unread until conversation is opened')
        user.set_viewport_size({'width':1440,'height':1000})
        user.goto(BASE+'/messages')
        expect(user.locator('.chat-conversation')).to_have_count(1)
        user.locator('.chat-conversation').click()
        expect(user).to_have_url(BASE+'/messages/1')
        expect(user.locator('.chat-message p')).to_have_count(4)
        checks.append('message centre reopens saved conversation')

        user.route('**/api/messages',lambda route:route.fulfill(status=200,json={'code':500,'message':'测试加载失败'}))
        user.set_viewport_size({'width':390,'height':844})
        user.goto(BASE+'/messages')
        expect(user.locator('.chat-conversations .chat-error')).to_be_visible()
        user.unroute('**/api/messages')
        user.get_by_role('button',name='重新加载',exact=True).click()
        expect(user.locator('.chat-conversation')).to_have_count(1)
        checks.append('mobile initial loading error exposes retry and recovers')

        result=user.evaluate('''async()=>{const r=await fetch('/api/messages/hotels/2',{method:'POST',headers:{'Content-Type':'application/json',Authorization:'Bearer '+localStorage.getItem('hotel_token')},body:JSON.stringify({content:'第二家民宿咨询'})});return (await r.json()).code}''')
        assert result==200
        user.goto(BASE+'/messages/1')
        expect(user.locator('.chat-conversation')).to_have_count(2)
        user.locator('#chat-draft').fill('第一家尚未发送的草稿')
        user.get_by_role('button',name='返回会话列表').click()
        user.locator('.chat-conversation').filter(has_text='Stay 2').click()
        expect(user.locator('.chat-message p')).to_have_text(['第二家民宿咨询'])
        expect(user.locator('#chat-draft')).to_have_value('')
        user.get_by_role('button',name='返回会话列表').click()
        user.locator('.chat-conversation').filter(has_text='Stay 1').click()
        expect(user.locator('#chat-draft')).to_have_value('第一家尚未发送的草稿')
        checks.append('switching conversations isolates messages and preserves each draft')

        user.get_by_role('button',name='返回会话列表').click()
        user.route('**/api/messages',lambda route:route.fulfill(status=200,json={'code':500,'message':'切换会话失败'}))
        user.locator('.chat-conversation').filter(has_text='Stay 2').click()
        expect(user.locator('.chat-thread .chat-error')).to_be_visible()
        user.unroute('**/api/messages')
        user.locator('.chat-thread').get_by_role('button',name='重新加载',exact=True).click()
        expect(user.locator('.chat-message p')).to_have_text(['第二家民宿咨询'])
        checks.append('mobile route-switch loading failure stays visible and retries target thread')

        guest=browser.new_context();g=guest.new_page();g.goto(BASE+'/hotel/1')
        g.get_by_role('button',name='登录后私信',exact=True).click()
        expect(g).to_have_url(BASE+'/login?redirect=/messages/1')
        checks.append('guest enquiry preserves target chat in login redirect')
        assert not errors,errors
        (OUT/'results.json').write_text(json.dumps({'checks':checks,'runtime_errors':errors,'backend':'isolated H2; real HTTP','mocked_cases':['one failed send','one failed conversation-list load']},ensure_ascii=False,indent=2),encoding='utf-8')
        print('PASS: '+str(len(checks))+' browser scenarios; real upload and bidirectional chat; no page errors.')
        browser.close()
finally:
    server.terminate()
    try: server.wait(timeout=8)
    except subprocess.TimeoutExpired: server.kill();server.wait(timeout=5)
    log.close()
