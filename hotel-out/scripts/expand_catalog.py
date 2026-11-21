"""Generate repeatable local demonstration inventory and destination content."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = {
    '北京': [('故宫博物院','东城区','历史文化'),('天坛公园','东城区','历史文化'),('颐和园','海淀区','园林漫步'),('八达岭长城','延庆区','山野风景'),('什刹海','西城区','城市漫步'),('奥林匹克森林公园','朝阳区','自然风光')],
    '上海': [('外滩','黄浦区','城市地标'),('豫园','黄浦区','园林漫步'),('东方明珠','浦东新区','城市地标'),('武康路','徐汇区','城市漫步'),('上海迪士尼度假区','浦东新区','亲子度假'),('朱家角古镇','青浦区','历史文化')],
    '广州': [('广州塔','海珠区','城市地标'),('北京路步行街','越秀区','城市漫步'),('广州长隆','番禺区','亲子度假'),('沙面岛','荔湾区','城市漫步'),('陈家祠','荔湾区','历史文化'),('白云山','白云区','山野风景'),('越秀公园','越秀区','园林漫步'),('永庆坊','荔湾区','岭南文化')],
    '深圳': [('世界之窗','南山区','亲子度假'),('深圳湾公园','南山区','海滨休闲'),('莲花山公园','福田区','园林漫步'),('大梅沙海滨公园','盐田区','海滨休闲'),('大鹏所城','大鹏新区','历史文化'),('华侨城创意文化园','南山区','城市漫步')],
    '杭州': [('西湖','西湖区','自然风光'),('灵隐寺','西湖区','历史文化'),('西溪湿地','西湖区','自然风光'),('千岛湖','淳安县','自然风光'),('河坊街','上城区','城市漫步'),('良渚博物院','余杭区','历史文化')],
    '成都': [('成都大熊猫繁育研究基地','成华区','亲子度假'),('宽窄巷子','青羊区','城市漫步'),('武侯祠','武侯区','历史文化'),('锦里','武侯区','城市漫步'),('人民公园','青羊区','园林漫步'),('杜甫草堂','青羊区','历史文化')],
    '厦门': [('鼓浪屿','思明区','海滨休闲'),('南普陀寺','思明区','历史文化'),('环岛路','思明区','海滨休闲'),('曾厝垵','思明区','城市漫步'),('集美学村','集美区','历史文化'),('厦门园林博览苑','集美区','园林漫步')],
    '大理': [('大理古城','大理市','历史文化'),('洱海','大理市','自然风光'),('苍山','大理市','山野风景'),('崇圣寺三塔','大理市','历史文化'),('喜洲古镇','大理市','城市漫步'),('双廊古镇','大理市','海滨休闲')],
}
PHOTOS = ['photo-1566073771259-6a8506099945','photo-1611892440504-42a792e24d32','photo-1564013799919-ab600027ffc6','photo-1542314831-068cd1dbfeeb','photo-1520250497591-112f2f40a3f4','photo-1445019980597-93fa8acb246c']
def photo(key): return f'https://images.unsplash.com/{key}?auto=format&fit=crop&w=960&q=80'
hotels, sights = [], []
for ci, (city, places) in enumerate(DATA.items()):
    for i, (name, district, tag) in enumerate(places):
        sights.append(dict(id=f'city-{ci}-{i}',city=city,name=name,district=district,tag=tag,description=f'将{name}加入{city}行程，感受{tag}的城市体验。可结合所在区域安排出游与住宿，开放及预约安排请以景区官方信息为准。',image='',source='https://www.mct.gov.cn/',author='目的地资料',license='文字导览',licenseUrl='https://www.mct.gov.cn/'))
    for i in range(6):
        name, district, _ = places[i]
        hid = 2000 + ci * 10 + i
        price = [328,458,568,688,398,828][i]
        typ = ['精品酒店','民宿','度假屋'][i%3]
        hotels.append(dict(id=hid,name=f'{name} · '+['栖悦酒店','拾光民宿','云庭度假屋','和居酒店','泊舍民宿','隐山度假屋'][i],city=city,district=district,address=f'{city}{district}（演示地址）',type=typ,score=0,price=price,status='已上架',tags=['演示房源','免费 WiFi','行李寄存'],facilities=['免费 WiFi','免费停车','行李寄存','空调'],image=photo(PHOTOS[(ci+i)%6]),gallery=[photo(PHOTOS[(ci+i)%6]),photo(PHOTOS[(ci+i+1)%6])],description=f'位于{city}{district}的{typ}，适合城市探索与短途休闲。此房源为本地演示数据，名称、图片、地址、价格和库存均用于演示，不代表真实商家。',rooms=[dict(id=hid*10+j,name=rn,bed=bed,guests=g,stock=8,price=price+extra) for j,(rn,bed,g,extra) in enumerate([('舒适大床房','1.8 米大床',2,0),('景观双床房','两张 1.2 米床',2,80),('家庭套房','大床 + 单人床',3,200)],1)]))
(ROOT/'src/constants/demoHotels.js').write_text('export const demoHotels = '+json.dumps(hotels,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
if not (ROOT/'src/constants/destinations.js').exists():
    (ROOT/'src/constants/destinations.js').write_text('export const additionalAttractions = '+json.dumps(sights,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def sql(v):
    if isinstance(v,(list,dict)): v=json.dumps(v,ensure_ascii=False)
    return "'"+str(v).replace('\\','\\\\').replace("'","''")+"'"
lines=['-- Local demonstration catalog. Idempotent; existing rows are preserved.','SET NAMES utf8mb4;','START TRANSACTION;']
for h in hotels:
    cols=['id','merchant_id','name','city','district','address','type','cover_image','gallery','description','facilities','nearby_info','status','min_price','score','review_count']
    vals=[h['id'],1,h['name'],h['city'],h['district'],h['address'],h['type'],h['image'],h['gallery'],h['description'],h['facilities'],[], 'ONLINE',h['price'],0,0]
    lines.append(f"INSERT INTO tb_hotel ({','.join(cols)}) SELECT {','.join(map(sql,vals))} WHERE NOT EXISTS (SELECT 1 FROM tb_hotel WHERE id={h['id']});")
    for r in h['rooms']:
        vals=[r['id'],h['id'],r['name'],h['image'],35,r['bed'],r['guests'],r['price'],r['stock'],h['facilities']]
        lines.append(f"INSERT INTO tb_room_type (id,hotel_id,name,image,area,bed,max_guests,base_price,stock,facilities) SELECT {','.join(map(sql,vals))} WHERE NOT EXISTS (SELECT 1 FROM tb_room_type WHERE id={r['id']});")
        lines.append(f"INSERT INTO tb_room_calendar (room_type_id,calendar_date,price,available_stock,is_available) SELECT {r['id']},DATE_ADD(CURDATE(), INTERVAL n DAY),{r['price']},8,1 FROM (SELECT a.n + b.n * 10 + c.n * 100 AS n FROM (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) a CROSS JOIN (SELECT 0 n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) b CROSS JOIN (SELECT 0 n UNION SELECT 1) c) days WHERE n < 180 AND NOT EXISTS (SELECT 1 FROM tb_room_calendar WHERE room_type_id={r['id']} AND calendar_date=DATE_ADD(CURDATE(), INTERVAL n DAY));")
lines.append('COMMIT;')
(ROOT.parent/'hotel-back/sql/migrations/20261115_demo_city_catalog.sql').write_text('\n'.join(lines),encoding='utf8')
print(f'Generated {len(hotels)} hotels, {len(sights)} destination entries; SQL ready.')
