"""AI conversation UI browser regression using a Vite frontend and mocked AI HTTP responses."""
from pathlib import Path
import json, os, subprocess, time, urllib.request
from playwright.sync_api import expect, sync_playwright

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'test-artifacts' / 'ai-chat'
OUT.mkdir(parents=True, exist_ok=True)
BASE = 'http://127.0.0.1:2237'
log = (OUT / 'vite.log').open('w', encoding='utf-8')
server = subprocess.Popen(
    ['node', str(ROOT / 'node_modules/vite/bin/vite.js'), '--port', '2237', '--strictPort'],
    cwd=ROOT, env={**os.environ, 'VITE_USE_MOCK': 'false'}, stdout=log, stderr=subprocess.STDOUT,
)
checks = []
try:
    for _ in range(100):
        try:
            urllib.request.urlopen(BASE, timeout=1).close()
            break
        except Exception:
            time.sleep(.2)
    else:
        raise RuntimeError('Vite startup timeout')
    with sync_playwright() as playwright:
        browser = playwright.chromium.launch()
        page = browser.new_page(viewport={'width': 1440, 'height': 1000}, reduced_motion='reduce')
        errors = []
        page.on('pageerror', lambda error: errors.append(str(error)))

        captured_cities = []
        captured_history_lengths = []

        def ai_route(route):
            if route.request.url.endswith('/api/ai/status'):
                return route.fulfill(status=200, json={'code': 200, 'data': {'enabled': True}})
            if route.request.url.endswith('/api/ai/search'):
                payload = json.loads(route.request.post_data or '{}')
                captured_cities.append(payload.get('city'))
                captured_history_lengths.append(len(payload.get('history') or []))
                return route.fulfill(status=200, json={'code': 200, 'data': {
                    'message': '根据你的条件，我找到两家适合的民宿。',
                    'filters': {'city': '广州'},
                    'hotels': [{'id': 1, 'name': '珠江边民宿', 'city': '广州',
                                'coverImage': '/avatars/ocean.svg', 'minPrice': 380}],
                }})
            return route.continue_()

        page.route('**/api/ai/**', ai_route)
        page.goto(BASE)
        expect(page.locator('#ai-stay-question')).to_be_visible()
        page.locator('#ai-stay-question').fill('广州周末适合带孩子的民宿')
        page.get_by_role('button', name='帮我找住宿').click()
        page.wait_for_url('**/ai-chat**')
        expect(page.locator('.ai-message.assistant').last).to_contain_text('根据你的条件', timeout=10000)
        expect(page.locator('.ai-result-card')).to_have_count(1)
        page.screenshot(path=str(OUT / 'desktop.png'))
        page.reload()
        expect(page.locator('.ai-message.assistant')).to_have_count(1, timeout=10000)
        page.locator('#ai-chat-input').fill('再找一间有停车位的')
        page.get_by_role('button', name='发送').click()
        expect(page.locator('.ai-message.assistant')).to_have_count(2, timeout=10000)
        for index in range(5):
            page.locator('#ai-chat-input').fill(f'follow-up-{index}')
            page.get_by_role('button', name='发送').click()
            expect(page.locator('.ai-message.assistant')).to_have_count(3 + index, timeout=10000)
        assert captured_history_lengths and max(captured_history_lengths) <= 6, captured_history_lengths
        checks.append('homepage redirect, session refresh and follow-up preserve conversation')

        page.set_viewport_size({'width': 390, 'height': 844})
        expect(page.locator('.mobile-tabbar')).to_be_visible()
        expect(page.locator('.mobile-tabbar a').first).to_contain_text('AI')
        page.screenshot(path=str(OUT / 'mobile.png'))
        checks.append('mobile five-item navigation and conversation layout render')

        page.goto(BASE + '/ai-chat')
        page.unroute('**/api/ai/**')
        page.route('**/api/ai/status', lambda route: route.fulfill(status=200, json={'code': 200, 'data': {'enabled': True}}))
        failed = [True]

        def fail_once(route):
            payload = json.loads(route.request.post_data or '{}')
            captured_history_lengths.append(len(payload.get('history') or []))
            if failed[0]:
                failed[0] = False
                return route.fulfill(status=200, json={'code': 500, 'message': '模型暂时不可用'})
            return route.fulfill(status=200, json={'code': 200, 'data': {'message': 'RETRY_OK', 'filters': {}, 'hotels': []}})

        page.route('**/api/ai/search', fail_once)
        page.locator('#ai-chat-input').fill('测试失败重试')
        page.get_by_role('button', name='发送').click()
        expect(page.locator('.ai-chat-error')).to_be_visible(timeout=10000)
        expect(page.locator('#ai-chat-input')).to_have_value('测试失败重试')
        failed_user_count = page.locator('.ai-message.user').count()
        page.get_by_role('button', name='重试').click()
        expect(page.locator('.ai-message.assistant').last).to_contain_text('RETRY_OK', timeout=10000)
        expect(page.locator('.ai-message.user')).to_have_count(failed_user_count)
        checks.append('failed request keeps draft and retry succeeds')
        page.evaluate('sessionStorage.clear()')
        page.unroute('**/api/ai/search')
        def deep_link_response(route):
            payload = json.loads(route.request.post_data or '{}')
            captured_history_lengths.append(len(payload.get('history') or []))
            return route.fulfill(status=200, json={'code': 200, 'data': {'message': 'DEEP_OK', 'filters': {}, 'hotels': []}})

        page.route('**/api/ai/search', deep_link_response)
        with page.expect_request('**/api/ai/search') as deep_request:
            page.goto(BASE + '/ai-chat?q=深圳停车民宿&city=深圳&mode=quick')
        expect(page.locator('.ai-message.assistant').last).to_contain_text('DEEP_OK', timeout=10000)
        assert json.loads(deep_request.value.post_data or '{}').get('city') == '深圳'
        assert max(captured_history_lengths) <= 6, captured_history_lengths
        checks.append('deep link city is applied before AI request')
        assert not errors, errors
        (OUT / 'results.json').write_text(json.dumps({'checks': checks, 'runtime_errors': errors, 'history_lengths': captured_history_lengths}, ensure_ascii=False, indent=2), encoding='utf-8')
        print(f'PASS: {len(checks)} AI conversation browser scenarios; no page errors.')
        browser.close()
finally:
    server.terminate()
    try:
        server.wait(timeout=8)
    except subprocess.TimeoutExpired:
        server.kill()
        server.wait(timeout=5)
    log.close()
