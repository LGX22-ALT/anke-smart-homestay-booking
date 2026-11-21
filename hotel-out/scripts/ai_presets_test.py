"""Browser regression with controlled AI responses; never send keys to a provider."""
import json
import re
from pathlib import Path
from playwright.sync_api import sync_playwright, expect

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'hotel-out/test-artifacts/ai-presets-20261119'
OUT.mkdir(parents=True, exist_ok=True)
BASE = 'http://127.0.0.1:2222'
state = {'enabled': False, 'endpoint': '', 'model': '', 'hasApiKey': False}
calls = []
held = []
control = {'hold': False, 'error': False}

def reply(route, data, code=200):
    route.fulfill(json={'code': code, 'message': '操作成功' if code == 200 else '模型账户余额不足，请前往服务商控制台充值', 'data': data})

def ai_api(route):
    request = route.request
    if request.method == 'GET':
        return reply(route, state)
    body = request.post_data_json
    calls.append(body)
    if request.url.endswith('/test'):
        if control['hold']:
            held.append(route)
            return
        return reply(route, {'message': '连接成功，模型可以正常返回 JSON'}, 1001 if control['error'] else 200)
    state.update({k: body[k] for k in ('enabled', 'endpoint', 'model')})
    state['hasApiKey'] = bool(body['apiKey']) or (state['hasApiKey'] and not body['clearApiKey'])
    return reply(route, state)

with sync_playwright() as pw:
    browser = pw.chromium.launch()
    context = browser.new_context(viewport={'width': 1440, 'height': 1050})
    page = context.new_page()
    errors = []
    page.on('pageerror', lambda error: errors.append(str(error)))
    context.route('**/api/admin/ai/**', ai_api)
    seed = (ROOT / 'hotel-back/sql/hotel_booking.sql').read_text(encoding='utf8')
    credential = re.search(r'Demo password for all accounts:\s*(\S+)', seed).group(1)
    page.goto(BASE + '/login', wait_until='networkidle')
    page.get_by_role('button', name='管理员登录', exact=True).click()
    page.get_by_label('手机号', exact=True).fill('13800000001')
    page.get_by_label('密码', exact=True).fill(credential)
    page.locator('.login-button').click()
    page.wait_for_url('**/admin/dashboard')
    page.goto(BASE + '/admin/ai', wait_until='networkidle')
    panel = page.locator('.ai-settings')
    expect(panel.locator('.provider-choice')).to_have_count(7)
    assert panel.get_by_label('使用模型').input_value() == 'deepseek-flash'
    results = []
    presets = [
        ('DeepSeek', 'https://api.deepseek.com/chat/completions', 'deepseek-flash'),
        ('通义千问', 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions', 'qwen-plus'),
        ('Kimi', 'https://api.moonshot.cn/v1/chat/completions', 'kimi-k3.1-pro'),
        ('智谱 GLM', 'https://open.bigmodel.cn/api/paas/v4/chat/completions', 'glm-4.7-flash'),
        ('硅基流动', 'https://api.siliconflow.cn/v1/chat/completions', 'Qwen/Qwen3.6-27B'),
        ('OpenAI', 'https://api.openai.com/v1/chat/completions', 'gpt-4.1-mini'),
    ]
    for name, endpoint, model in presets:
        panel.get_by_role('button', name=re.compile(re.escape(name))).click()
        expect(panel.locator('input[type=url]')).to_have_count(0)
        expect(panel.get_by_label('API 密钥')).to_have_value('')
        count = len(calls)
        panel.get_by_role('button', name='测试连接', exact=True).click()
        expect(panel.locator('.error')).to_contain_text('API 密钥')
        assert len(calls) == count
        panel.get_by_label('API 密钥').fill('qa-only-placeholder')
        panel.get_by_role('button', name='测试连接', exact=True).click()
        expect(panel.locator('.success')).to_be_visible()
        assert calls[-1]['endpoint'] == endpoint and calls[-1]['model'] == model
        assert calls[-1]['apiKey'] == 'qa-only-placeholder'
        results.append(name + ': automatic endpoint/model and test payload passed (controlled response)')
    # Save a preset and reload; preserve its saved credential without exposing it.
    panel.locator('.enable-control').click()
    expect(panel.locator('.enable-control input')).to_be_checked()
    panel.get_by_role('button', name='保存并应用', exact=True).click()
    expect(panel.get_by_label('API 密钥')).to_have_value('')
    page.reload(wait_until='networkidle')
    expect(panel.get_by_role('button', name=re.compile('OpenAI'))).to_have_attribute('aria-pressed', 'true')
    panel.get_by_role('button', name='测试连接', exact=True).click()
    expect(panel.locator('.success')).to_be_visible()
    assert calls[-1]['apiKey'] == '' and not calls[-1]['clearApiKey']
    # A different provider cannot reuse the saved key.
    panel.get_by_role('button', name=re.compile('DeepSeek')).click()
    expect(panel.locator('.success')).to_have_count(0)
    panel.get_by_role('button', name='测试连接', exact=True).click()
    expect(panel.locator('.error')).to_contain_text('API 密钥')
    # Custom values survive saving and reload, and changing an endpoint clears typed keys.
    panel.get_by_role('button', name=re.compile('自定义')).click()
    panel.locator('input[type=url]').fill('https://gateway.example/v1/chat/completions')
    panel.get_by_label('模型 ID', exact=False).fill('private-model')
    panel.get_by_label('API 密钥').fill('qa-only-placeholder')
    control['hold'] = True
    panel.get_by_role('button', name='测试连接', exact=True).click()
    expect(panel.get_by_label('API 密钥')).to_be_disabled()
    expect(panel.locator('.provider-choice').first).to_be_disabled()
    expect(panel.get_by_role('button', name='保存并应用', exact=True)).to_be_disabled()
    assert held
    reply(held.pop(), {'message': '连接成功，模型可以正常返回 JSON'})
    control['hold'] = False
    expect(panel.locator('.success')).to_be_visible()
    panel.get_by_role('button', name='保存并应用', exact=True).click()
    expect(panel.get_by_label('API 密钥')).to_have_value('')
    page.reload(wait_until='networkidle')
    expect(panel.get_by_role('button', name=re.compile('自定义'))).to_have_attribute('aria-pressed', 'true')
    expect(panel.locator('input[type=url]')).to_have_value('https://gateway.example/v1/chat/completions')
    control['error'] = True
    panel.get_by_role('button', name='测试连接', exact=True).click()
    expect(panel.locator('.error')).to_contain_text('余额不足')
    results.append('save/reload, custom configuration, credential isolation, busy state and error display passed (controlled responses)')
    # Clean screenshot, no credentials filled or network recordings.
    panel.get_by_role('button', name=re.compile('DeepSeek')).click()
    expect(panel.get_by_role('button', name=re.compile('DeepSeek'))).to_have_attribute('aria-pressed', 'true')
    expect(page.locator('.el-message')).to_have_count(0, timeout=6000)
    page.screenshot(path=str(OUT / 'desktop.png'), full_page=True)
    for width in [768, 390, 360, 320]:
        page.set_viewport_size({'width': width, 'height': 900})
        assert not page.evaluate('document.documentElement.scrollWidth > innerWidth + 1')
    page.screenshot(path=str(OUT / 'mobile.png'), full_page=True)
    assert not errors, errors
    results.append('1440/768/390/360/320 layout passed; no page runtime exceptions')
    (OUT / 'results.json').write_text(json.dumps({'results': results, 'pageErrors': errors, 'upstreamCalls': 0}, ensure_ascii=False, indent=2), encoding='utf8')
    print(json.dumps({'results': results, 'upstreamCalls': 0}, ensure_ascii=False))
    browser.close()
