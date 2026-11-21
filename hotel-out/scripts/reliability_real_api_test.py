"""Local integration regression; creates one marked test order, then cancels it."""
import os
from datetime import date, timedelta
from pathlib import Path
from urllib.parse import urlparse, parse_qs
from playwright.sync_api import sync_playwright

BASE=os.environ.get('TEST_BASE_URL','http://127.0.0.1:2223')
ARTIFACTS=Path(__file__).resolve().parents[1]/'test-artifacts/reliability'
ARTIFACTS.mkdir(parents=True,exist_ok=True)
with sync_playwright() as p:
    browser=p.chromium.launch(headless=True)
    context=browser.new_context(viewport={'width':1440,'height':1000})
    page=context.new_page()
    errors=[]
    page.on('pageerror',lambda e:errors.append(str(e)))
    page.route('**/*',lambda r:r.abort() if r.request.resource_type=='image' and not r.request.url.startswith(BASE) else r.continue_())
    page.goto(BASE+'/login',wait_until='networkidle')
    page.get_by_role('button',name='住客账号',exact=False).click()
    page.wait_for_url(BASE+'/')
    token=page.evaluate("localStorage.getItem('hotel_token')")
    headers={'Authorization':'Bearer '+token}
    start=(date.today()+timedelta(days=5)).isoformat()
    end=(date.today()+timedelta(days=6)).isoformat()
    def availability():
        response=context.request.get(BASE+'/api/hotels/2000/availability',params={'checkIn':start,'checkOut':end})
        payload=response.json();assert payload['code']==200
        return {row['roomTypeId']:row['availableStock'] for row in payload['data']}
    before=availability()
    page.goto(BASE+f'/hotel/2000?checkIn={start}&checkOut={end}',wait_until='networkidle')
    favorite_response=context.request.get(BASE+'/api/user/favorites',headers=headers).json()
    assert favorite_response['code']==200
    was_favorite=2000 in favorite_response['data']
    page.get_by_role('button',name='已收藏' if was_favorite else '收藏',exact=True).click()
    page.reload(wait_until='networkidle')
    assert page.get_by_role('button',name='收藏' if was_favorite else '已收藏',exact=True).count()
    # Restore original favorite state with an explicit, idempotent command.
    method=context.request.put if was_favorite else context.request.delete
    assert method(BASE+'/api/user/favorites/2000',headers=headers).json()['code']==200
    page.get_by_role('button',name='预订此房型',exact=True).first.click()
    page.wait_for_url(BASE+'/order/confirm')
    page.get_by_label('备注',exact=True).fill('本地可靠性自动回归：验证后取消')
    page.get_by_role('button',name='提交订单并支付',exact=True).click()
    page.wait_for_url('**/pay/result?orderId=*')
    page.get_by_role('heading',name='预订成功',exact=True).wait_for()
    order_id=parse_qs(urlparse(page.url).query)['orderId'][0]
    order=context.request.get(BASE+'/api/orders/'+order_id,headers=headers).json()['data']
    assert order['orderStatus']=='PAID' and order['hotel']=='故宫博物院 · 栖悦酒店'
    assert availability()[order['roomTypeId']]==before[order['roomTypeId']]-1
    page.get_by_role('link',name='查看我的订单',exact=True).click()
    row=page.locator('.order-list article').filter(has_text=order['orderNo'])
    row.get_by_role('button',name='取消订单',exact=True).click()
    row.get_by_text('已取消',exact=True).wait_for()
    assert availability()==before, 'inventory not restored'
    page.screenshot(path=str(ARTIFACTS/'real-user-order-cancelled.png'),full_page=True)
    page.goto(BASE+'/pay/result?orderId='+order_id+'&status=success',wait_until='networkidle')
    page.get_by_role('heading',name='已取消',exact=True).wait_for()
    assert not page.get_by_role('heading',name='预订成功',exact=True).count(), 'query parameter forged payment success'
    page.goto(BASE+'/pay/result?status=success',wait_until='networkidle')
    page.get_by_text('加载失败',exact=True).wait_for()
    assert not errors,errors
    for role,button,paths in [
        ('merchant','商家账号',['/merchant/orders','/merchant/calendar','/merchant/dashboard','/merchant/statistics']),
        ('admin','管理员账号',['/admin/orders','/admin/users','/admin/dashboard'])]:
        page.evaluate('localStorage.clear()')
        page.goto(BASE+'/login',wait_until='networkidle')
        page.get_by_role('button',name=button,exact=False).click()
        page.wait_for_url(BASE+'/'+role+'/dashboard')
        for path in paths:
            page.goto(BASE+path,wait_until='networkidle')
            assert not page.get_by_text('加载失败',exact=True).count(),path
            assert not errors,errors
            if path.endswith('/calendar'):
                assert page.locator('.day-cell button').count()>20
                assert page.locator('.selector-row select').nth(1).locator('option').count()>0
                page.screenshot(path=str(ARTIFACTS/'real-merchant-calendar.png'),full_page=True)
            if path=='/admin/dashboard':
                assert '28,460' not in page.locator('body').inner_text()
    context.close();browser.close()
    print('PASS real API: login, favorite persistence/restoration, booking, simulated payment, cancellation/inventory restoration, payment-state verification, merchant/admin lists and real calendar; no page errors')
