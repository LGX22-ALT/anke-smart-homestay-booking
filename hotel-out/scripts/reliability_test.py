"""Regression checks against the real-mode frontend with deterministic API failures."""
import json
import os
from pathlib import Path
from urllib.parse import urlparse
from playwright.sync_api import sync_playwright

BASE = os.environ.get('TEST_BASE_URL', 'http://127.0.0.1:2222')
ARTIFACTS = Path(__file__).resolve().parents[1] / 'test-artifacts' / 'reliability'
ARTIFACTS.mkdir(parents=True, exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    for role, path in [('user', '/user/orders'), ('merchant', '/merchant/orders'),
                       ('admin', '/admin/orders'), ('merchant', '/merchant/hotels'),
                       ('admin', '/admin/users'), ('merchant', '/merchant/calendar')]:
        context = browser.new_context(viewport={'width': 1280, 'height': 900})
        context.add_init_script('''localStorage.setItem('hotel_token','test');
            localStorage.setItem('hotel_user_info',JSON.stringify({id:3,name:'测试用户'}));
            localStorage.setItem('hotel_user_role',%s);''' % json.dumps(role))
        page = context.new_page()
        errors = []
        mock_requests = []
        page.on('pageerror', lambda e: errors.append(str(e)))
        page.on('console', lambda m: errors.append(m.text) if m.type == 'error' and '503' not in m.text else None)
        page.on('request', lambda r: mock_requests.append(r.url) if '/src/mocks/' in r.url else None)
        page.route('**/*', lambda r: r.fulfill(status=503, json={'code':503,'message':'测试服务不可用'})
                   if urlparse(r.request.url).path.startswith('/api/') else r.continue_())
        page.goto(BASE + path, wait_until='networkidle')
        page.wait_for_selector('main, .bo-page', timeout=15000)
        assert page.get_by_text('加载失败', exact=False).count(), f'{path}: missing persistent error state: {page.url}, {errors}, {page.locator("body").inner_text()[:800]}'
        assert page.get_by_role('button', name='重试', exact=True).count(), f'{path}: missing retry'
        assert page.locator('tbody tr, .order-list article').count() == 0, f'{path}: stale/mock rows'
        assert not mock_requests, f'{path}: mock module loaded in real mode'
        assert not errors, f'{path}: {errors}'
        page.screenshot(path=str(ARTIFACTS / (role + path.replace('/', '-') + '-error.png')), full_page=True)
        page.unroute('**/*')
        requests = []
        def empty(route):
            if not urlparse(route.request.url).path.startswith('/api/'):
                return route.continue_()
            requests.append(route.request.url)
            return route.fulfill(json={'code':200, 'data': {'total':0,'pages':0,'list':[]}
                                 if path not in ['/merchant/calendar'] else {'total':0,'pages':0,'list':[]}})
        page.route('**/*', empty)
        page.get_by_role('button', name='重试', exact=True).click()
        page.wait_for_function("!document.querySelector('[role=alert]')")
        assert page.locator('tbody tr, .order-list article').count() == 0
        assert requests, f'{path}: retry did not fetch'
        assert not errors, errors
        if path.endswith('/orders'):
            page.unroute('**/*')
            def orders(route):
                url = urlparse(route.request.url)
                if not url.path.startswith('/api/'):
                    return route.continue_()
                from urllib.parse import parse_qs
                params = parse_qs(url.query)
                number = int(params.get('pageNum', ['1'])[0])
                requests.append(route.request.url)
                row = {'id':100 + number,'orderNo':f'REAL-PAGE-{number}','hotelId':1,
                       'hotel':'真实接口测试房源','guestName':'测试住客','guestPhone':'13800000003',
                       'checkIn':'2026-12-01','checkOut':'2026-12-02','totalAmount':100,
                       'orderStatus':'PAID','paymentStatus':'PAID'}
                return route.fulfill(json={'code':200,'data':{'total':11,'pages':2,'list':[row]}})
            page.route('**/*', orders)
            page.reload(wait_until='networkidle')
            page.get_by_role('button',name='下一页',exact=True).click()
            page.wait_for_function("document.body.innerText.includes('2 / 2')")
            assert any('pageNum=2' in url for url in requests), requests
            assert page.get_by_text('真实接口测试房源',exact=True).count() if role != 'merchant' else page.get_by_text('测试住客',exact=True).count()
            page.set_viewport_size({'width':390,'height':844})
            page.screenshot(path=str(ARTIFACTS / (role + '-orders-mobile.png')),full_page=True)
            if role == 'user':
                assert page.evaluate('document.documentElement.scrollWidth <= innerWidth'), 'mobile overflow'
            else:
                page.get_by_placeholder('搜索订单号',exact=True).fill('REAL')
                page.get_by_placeholder('搜索订单号',exact=True).press('Enter')
                page.wait_for_function("document.body.innerText.includes('1 / 2')")
                assert any('keyword=REAL' in url and 'pageNum=1' in url for url in requests)
            assert not errors, errors
        context.close()
        print('PASS failure isolation, retry, empty state' + (', server pagination' if path.endswith('/orders') else '') + ':', path)
    browser.close()
