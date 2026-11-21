"""Login entrance regression; only existing demo accounts, no DB writes."""
import os
import re
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

BASE = os.environ.get('HOTEL_BASE_URL', 'http://127.0.0.1:2222')
ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'hotel-out/test-artifacts/login-entrance'
OUT.mkdir(parents=True, exist_ok=True)
sql = (ROOT / 'hotel-back/sql/hotel_booking.sql').read_text(encoding='utf-8')
credential = re.search(r'Demo password for all accounts:\s*(\S+)', sql).group(1)
passed = []

with sync_playwright() as p:
    browser = p.chromium.launch()
    context = browser.new_context(viewport={'width': 1440, 'height': 960})
    page = context.new_page()
    errors = []
    page.on('pageerror', lambda e: errors.append(str(e)))
    page.goto(BASE + '/login', wait_until='networkidle')
    expect(page.locator('.mode-tabs button')).to_have_count(2)
    expect(page.locator('.demo-accounts')).to_have_count(0)
    assert page.get_by_role('button', name='商家登录', exact=True).bounding_box()['y'] > page.locator('.login-button').bounding_box()['y']
    page.screenshot(path=str(OUT / 'desktop.png'), full_page=True)
    page.get_by_role('button', name='注册', exact=True).click()
    for label in ['用户名', '手机号', '邮箱', '密码']:
        expect(page.get_by_label(label, exact=True)).to_be_visible()
    page.get_by_role('button', name='商家登录', exact=True).click()
    expect(page.locator('h2')).to_have_text('商家登录')
    expect(page.locator('.mode-tabs')).to_have_count(0)
    page.get_by_role('button', name='管理员登录', exact=True).click()
    expect(page.locator('h2')).to_have_text('管理员登录')
    page.get_by_role('button', name='返回用户登录', exact=True).click()
    page.get_by_label('密码', exact=True).fill('Temporary-UI-Value')
    page.get_by_role('button', name='显示密码', exact=True).click()
    expect(page.get_by_label('密码', exact=True)).to_have_attribute('type', 'text')
    page.get_by_role('button', name='商家登录', exact=True).click()
    expect(page.get_by_label('密码', exact=True)).to_have_value('')
    expect(page.get_by_label('密码', exact=True)).to_have_attribute('type', 'password')
    passed.append('entrance placement, registration fields, switching and password visibility')
    for width in [320, 390, 768]:
        page.set_viewport_size({'width': width, 'height': 844})
        page.reload(wait_until='networkidle')
        assert page.evaluate('document.documentElement.scrollWidth <= innerWidth + 1')
        page.get_by_role('button', name='管理员登录', exact=True).click()
        expect(page.locator('h2')).to_have_text('管理员登录')
        if width == 390:
            page.screenshot(path=str(OUT / 'mobile-admin.png'), full_page=True)
    passed.append('320/390/768px layouts and compact entrance operation')
    assert not errors, errors
    context.close()

    context = browser.new_context()
    page = context.new_page()
    page.goto(BASE + '/login', wait_until='networkidle')
    page.locator('.login-button').click()
    expect(page.locator('.form-error')).to_contain_text('手机号')
    held = []
    page.route('**/api/user/login', lambda route: held.append(route))
    page.get_by_label('手机号', exact=True).fill('13800000003')
    page.get_by_label('密码', exact=True).fill('Controlled-Test-Value')
    page.locator('.login-button').click()
    expect(page.locator('.login-button')).to_be_disabled()
    expect(page.locator('.login-button')).to_have_text('正在提交…')
    page.locator('form').evaluate("el => el.dispatchEvent(new Event('submit', {bubbles:true,cancelable:true}))")
    assert len(held) == 1
    held[0].fulfill(json={'code': 401, 'message': '账号或密码错误'})
    expect(page.locator('.login-button')).to_be_enabled()
    expect(page.get_by_label('手机号', exact=True)).to_have_value('13800000003')
    assert '/login' in page.url
    passed.append('validation, pending state, duplicate submit prevention and failed login recovery (controlled response)')
    context.close()

    for role, phone, destination in [('user', '13800000003', '/'), ('merchant', '13800000002', '/merchant/dashboard'), ('admin', '13800000001', '/admin/dashboard')]:
        context = browser.new_context()
        page = context.new_page()
        page.goto(BASE + '/login', wait_until='networkidle')
        if role != 'user':
            page.get_by_role('button', name='商家登录' if role == 'merchant' else '管理员登录', exact=True).click()
        page.get_by_label('手机号', exact=True).fill(phone)
        page.get_by_label('密码', exact=True).fill(credential)
        with page.expect_response(lambda r: '/api/user/login' in r.url and r.request.method == 'POST') as response:
            page.locator('.login-button').click()
        assert response.value.json().get('code') == 200, 'Demo login rejected: ' + role
        page.wait_for_url(BASE + destination, timeout=15000)
        assert page.evaluate("localStorage.getItem('hotel_user_role')") == role
        if role == 'user':
            page.goto(BASE + '/admin/dashboard', wait_until='networkidle')
            assert '/403' in page.url
        context.close()
        passed.append('real API login and default destination: ' + role)

    context = browser.new_context()
    page = context.new_page()
    page.goto(BASE + '/user/orders', wait_until='networkidle')
    assert '/login?redirect=' in page.url
    page.get_by_label('手机号', exact=True).fill('13800000003')
    page.get_by_label('密码', exact=True).fill(credential)
    page.locator('.login-button').click()
    page.wait_for_url(BASE + '/user/orders', timeout=15000)
    passed.append('protected page return after real login')
    context.close()
    browser.close()

(OUT / 'results.txt').write_text('\n'.join('PASS ' + x for x in passed), encoding='utf-8')
print('\n'.join('PASS ' + x for x in passed))
