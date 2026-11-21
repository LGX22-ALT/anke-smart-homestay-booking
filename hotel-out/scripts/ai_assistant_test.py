"""UI contract checks with intercepted responses; never calls a paid model API."""
import json
import os
from datetime import date, timedelta
from pathlib import Path
from urllib.parse import parse_qs, urlparse
from playwright.sync_api import sync_playwright, expect

BASE = os.environ.get('HOTEL_BASE_URL', 'http://127.0.0.1:2222')
OUT = Path(__file__).parent / 'artifacts'
OUT.mkdir(exist_ok=True)


def respond(route, data, code=200):
    route.fulfill(json={'code': code, 'message': '测试响应', 'data': data})


with sync_playwright() as p:
    browser = p.chromium.launch()
    context = browser.new_context(viewport={'width': 1440, 'height': 1000})
    page = context.new_page()
    errors = []
    page.on('pageerror', lambda error: errors.append(str(error)))
    page.route('**/api/ai/status', lambda route: respond(route, {'enabled': True}))
    received = []
    check_in = (date.today() + timedelta(days=7)).isoformat()
    check_out = (date.today() + timedelta(days=9)).isoformat()
    filters = {'city': '广州', 'guests': 2, 'maxPrice': 500, 'checkIn': check_in,
               'checkOut': check_out, 'pageNum': 1, 'pageSize': 3, 'sort': 'score'}
    fixture = {'message': '为你找到 1 家符合条件的住宿。', 'filters': filters, 'total': 1,
               'hotels': [{'id': 1, 'name': '测试住宿', 'city': '广州', 'district': '海珠区',
                           'minPrice': 399, 'coverImage': '/images/no-image.jpg'}]}

    def search(route):
        received.append(route.request.post_data_json)
        respond(route, fixture)

    page.route('**/api/ai/search', search)
    page.goto(BASE, wait_until='networkidle')
    expect(page.locator('.ai-stay')).to_be_visible()
    assert page.locator('.travel-search').count() == 0
    expect(page.get_by_role('button', name='帮我找住宿')).to_be_enabled()
    page.screenshot(path=str(OUT / 'ai-home-desktop.png'), full_page=True)
    page.locator('.suggestion-list button').first.click()
    expect(page.get_by_label('住宿需求')).not_to_have_value('')
    page.get_by_role('button', name='帮我找住宿').click()
    expect(page.locator('.ai-hotel')).to_have_count(1)
    expect(page.locator('.condition-list')).to_contain_text('广州')
    href = page.locator('.ai-hotel').get_attribute('href')
    query = parse_qs(urlparse(href).query)
    assert query['checkIn'] == [check_in] and query['guests'] == ['2']
    assert 'pageSize' not in query
    page.get_by_label('住宿需求').fill('预算改成每晚400以内')
    page.get_by_role('button', name='帮我找住宿').click()
    expect(page.get_by_label('住宿需求')).to_have_value('')
    assert len(received) == 2 and len(received[1]['history']) == 2
    page.screenshot(path=str(OUT / 'ai-home-results.png'), full_page=True)
    # A pending request disables repeated submissions, including the keyboard shortcut.
    pending = []
    page.route('**/api/ai/search', lambda route: pending.append(route))
    page.get_by_label('住宿需求').fill('继续帮我找')
    page.get_by_role('button', name='帮我找住宿').click()
    expect(page.get_by_role('button', name='正在找合适的住宿…')).to_be_disabled()
    page.get_by_label('住宿需求').press('Control+Enter')
    assert len(pending) == 1
    respond(pending[0], {'message': '暂时没有符合条件的房源，请调整预算。',
                         'filters': filters, 'total': 0, 'hotels': []})
    expect(page.locator('.ai-hotel')).to_have_count(0)
    expect(page.locator('.result-message')).to_contain_text('暂时没有')
    page.get_by_role('button', name='开始新需求').click()
    expect(page.locator('.assistant-results')).to_have_count(0)
    page.route('**/api/ai/search', lambda route: respond(route, None, 1001))
    page.get_by_label('住宿需求').fill('广州两人住宿')
    page.get_by_role('button', name='帮我找住宿').click()
    expect(page.locator('.assistant-error')).to_be_visible()
    expect(page.get_by_label('住宿需求')).to_have_value('广州两人住宿')
    expect(page.get_by_role('button', name='帮我找住宿')).to_be_enabled()
    page.route('**/api/ai/status', lambda route: respond(route, {'enabled': False}))
    page.reload(wait_until='networkidle')
    expect(page.get_by_role('button', name='帮我找住宿')).to_be_disabled()
    expect(page.get_by_role('link', name='先逛逛酒店')).to_be_visible()
    page.route('**/api/ai/status', lambda route: route.abort())
    page.reload(wait_until='networkidle')
    expect(page.get_by_role('button', name='重新连接')).to_be_visible()
    page.route('**/api/ai/status', lambda route: respond(route, {'enabled': True}))
    page.get_by_role('button', name='重新连接').click()
    expect(page.get_by_role('button', name='帮我找住宿')).to_be_enabled()
    for width in [1200, 980, 768, 390, 360]:
        page.set_viewport_size({'width': width, 'height': 844})
        assert page.evaluate('document.documentElement.scrollWidth <= innerWidth + 1'), width
    page.screenshot(path=str(OUT / 'ai-home-mobile.png'), full_page=True)

    # Backend administrator form. No credentials are written into browser storage.
    page.set_viewport_size({'width': 1440, 'height': 1000})
    page.evaluate("""() => {
        localStorage.setItem('hotel_token', 'ui-test-only');
        localStorage.setItem('hotel_user_role', 'admin');
        localStorage.setItem('hotel_user_info', JSON.stringify({name:'测试管理员'}));
    }""")
    config = {'enabled': False, 'endpoint': '', 'model': '', 'hasApiKey': False}
    saved = []

    def settings(route):
        if route.request.method == 'PUT':
            saved.append(route.request.post_data_json)
            config.update({'enabled': True, 'endpoint': saved[-1]['endpoint'],
                           'model': saved[-1]['model'], 'hasApiKey': True})
        respond(route, config)

    page.route('**/api/admin/banners', lambda route: respond(route, []))
    page.route('**/api/admin/ai/settings', settings)
    page.route('**/api/admin/ai/test', lambda route: respond(route, {'message': '连接成功'}))
    page.goto(BASE + '/admin/config', wait_until='networkidle')
    page.locator('.office-sidebar').get_by_role('link', name='大模型接入', exact=True).click()
    page.wait_for_url('**/admin/ai')
    expect(page.get_by_role('heading', name='大模型接入', exact=True)).to_be_visible()
    expect(page.locator('.office-sidebar a.router-link-active')).to_have_text('大模型接入')
    panel = page.locator('.ai-settings')
    panel.get_by_label('启用首页 AI 助手').check()
    panel.get_by_label('完整接口地址').fill('https://example.com/v1/chat/completions')
    panel.get_by_label('模型名称').fill('test-model')
    panel.get_by_label('API 密钥').fill('ui-test-secret')
    panel.get_by_role('button', name='测试连接').click()
    expect(panel.locator('.success')).to_be_visible()
    panel.get_by_role('button', name='保存配置').click()
    expect(panel.get_by_label('API 密钥')).to_have_value('')
    assert len(saved) == 1 and saved[0]['apiKey'] == 'ui-test-secret'
    assert not page.evaluate("JSON.stringify({...localStorage, ...sessionStorage}).includes('ui-test-secret')")
    page.screenshot(path=str(OUT / 'ai-admin-config.png'), full_page=True)
    page.reload(wait_until='networkidle')
    expect(panel.get_by_label('模型名称')).to_have_value('test-model')
    expect(panel.get_by_label('API 密钥')).to_have_value('')
    # Both entry points share the same backend configuration.
    page.goto(BASE + '/admin/config', wait_until='networkidle')
    page.get_by_role('button', name='AI 助手', exact=True).click()
    expect(panel.get_by_label('模型名称')).to_have_value('test-model')
    page.goto(BASE + '/admin/ai', wait_until='networkidle')
    page.set_viewport_size({'width': 390, 'height': 844})
    assert page.evaluate('document.documentElement.scrollWidth <= innerWidth + 1')
    expect(panel.get_by_role('button', name='保存配置')).to_be_visible()
    page.screenshot(path=str(OUT / 'ai-admin-mobile.png'), full_page=True)
    page.evaluate("localStorage.setItem('hotel_user_role', 'user')")
    page.goto(BASE + '/admin/ai', wait_until='networkidle')
    page.wait_for_url('**/403?**')
    page.evaluate('localStorage.clear()')
    page.goto(BASE + '/admin/ai', wait_until='networkidle')
    page.wait_for_url('**/login?**')
    assert parse_qs(urlparse(page.url).query)['redirect'] == ['/admin/ai']
    assert not errors, errors
    browser.close()
    print('AI homepage, conversation, errors, responsive layout and admin configuration passed.')
