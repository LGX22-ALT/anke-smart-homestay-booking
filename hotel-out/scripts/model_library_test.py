"""Controlled UI regression: no real model keys or upstream inference calls."""
import json
import re
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'hotel-out/test-artifacts/model-library'
OUT.mkdir(parents=True, exist_ok=True)
items = []
sent = []
def reply(route, data):
    route.fulfill(json={'code': 200, 'message': '成功', 'data': data})
def api(route):
    path = route.request.url.split('/api/admin/ai/connections')[1].split('?')[0]
    method = route.request.method
    body = route.request.post_data_json if method in ('PUT', 'POST') else None
    if method == 'GET': return reply(route, items)
    if path.endswith('/test'): return reply(route, {'message': '连接成功', 'latencyMs': 123})
    if path.endswith('/activate'):
        for item in items: item['active'] = item['id'] == path.split('/')[1]
        return reply(route, {})
    if method == 'DELETE':
        items[:] = [i for i in items if i['id'] != path[1:]]
        return reply(route, None)
    data = dict(body)
    data.pop('apiKey', None)
    data.update(id=path[1:] or str(len(items)+1), hasApiKey=True, active=not items)
    items[:] = [i for i in items if i['id'] != data['id']]
    items.append(data)
    return reply(route, data)

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={'width': 1440, 'height': 1000})
    errors = []
    page.on('pageerror', lambda e: errors.append(str(e)))
    page.route('**/api/admin/ai/connections**', api)
    page.goto('http://127.0.0.1:2222/login', wait_until='networkidle')
    page.get_by_role('button', name='管理员登录', exact=True).click()
    page.get_by_label('手机号', exact=True).fill('13800000001')
    seed = (ROOT / 'hotel-back/sql/hotel_booking.sql').read_text(encoding='utf8')
    page.get_by_label('密码', exact=True).fill(re.search(r'Demo password for all accounts:\s*(\S+)', seed).group(1))
    page.locator('.login-button').click()
    page.wait_for_url('**/admin/dashboard')
    page.goto('http://127.0.0.1:2222/admin/ai', wait_until='networkidle')
    page.get_by_role('button', name='接入模型', exact=True).first.click()
    dialog = page.get_by_role('dialog')
    expect(dialog).to_be_visible()
    expect(dialog.get_by_label('接口基础地址', exact=False)).to_have_value('https://api.deepseek.com')
    dialog.get_by_label('API Key', exact=True).fill('test-only-placeholder')
    dialog.get_by_role('button', name='测试连接', exact=True).click()
    expect(dialog.locator('.draft-feedback')).to_contain_text('123 ms')
    page.screenshot(path=str(OUT / 'dialog.png'), full_page=True)
    dialog.get_by_role('button', name='保存配置', exact=True).click()
    expect(dialog).not_to_be_visible()
    expect(page.locator('.connection-row')).to_have_count(1)
    page.get_by_role('button', name='接入模型', exact=True).click()
    dialog.get_by_role('button', name='通义千问', exact=True).click()
    dialog.get_by_label('API Key', exact=True).fill('test-only-placeholder')
    dialog.get_by_role('button', name='保存配置', exact=True).click()
    expect(page.locator('.connection-row')).to_have_count(2)
    second = page.locator('.connection-row').filter(has_text='通义千问')
    second.get_by_role('button', name='切换', exact=True).click()
    expect(second).to_have_class(re.compile('is-active'))
    second.get_by_role('button', name='编辑', exact=True).click()
    expect(dialog.get_by_label('API Key', exact=True)).to_have_value('')
    dialog.get_by_label('接口基础地址', exact=False).fill('https://gateway.example/v1')
    dialog.get_by_role('button', name='保存配置', exact=True).click()
    expect(dialog.locator('.draft-feedback')).to_contain_text('API 密钥')
    dialog.get_by_role('button', name='取消', exact=True).click()
    expect(page.locator('.el-message')).to_have_count(0, timeout=8000)
    page.screenshot(path=str(OUT / 'list.png'), full_page=True)
    for width in (768, 390, 320):
        page.set_viewport_size({'width': width, 'height': 900})
        assert not page.evaluate('document.documentElement.scrollWidth > innerWidth + 1')
    page.screenshot(path=str(OUT / 'mobile.png'), full_page=True)
    page.route('**/api/ai/status', lambda r: reply(r, {'enabled': True}))
    def search(route):
        sent.append(route.request.post_data_json['mode'])
        return reply(route, {'message': '回答结果', 'hotels': [], 'total': 0})
    page.route('**/api/ai/search', search)
    page.goto('http://127.0.0.1:2222/', wait_until='networkidle')
    for mode, label in [('deep', '深思考'), ('quick', '快回答'), ('auto', '自动模式')]:
        page.locator('.answer-mode-menu summary').click()
        page.locator('.answer-mode-popover').get_by_role('button', name=re.compile(label)).click()
        page.get_by_label('住宿需求', exact=True).fill('你好')
        page.locator('.composer-footer > button[type=submit]').click()
        expect(page.locator('.result-message')).to_contain_text('回答结果')
        page.wait_for_function('!document.querySelector(".composer-footer > button").disabled')
    assert sent == ['deep', 'quick', 'auto'], sent
    page.locator('.answer-mode-menu summary').click()
    page.screenshot(path=str(OUT / 'modes.png'), full_page=True)
    assert not errors, errors
    (OUT / 'results.json').write_text(json.dumps({'crud': 'passed', 'modePayloads': sent, 'widths': [1440,768,390,320], 'pageErrors': errors, 'externalCalls': 0}, ensure_ascii=False, indent=2), encoding='utf8')
    browser.close()
    print('Model library and three answer modes: PASS (controlled API responses)')
