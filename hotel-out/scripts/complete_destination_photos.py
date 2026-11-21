"""Batch query Wikimedia page images; preserve existing licensed local assets."""
import json
import subprocess
import urllib.parse
import urllib.request
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
items=json.loads(subprocess.check_output(['node','--input-type=module','-e',"import {additionalAttractions as a} from './src/constants/destinations.js';process.stdout.write(JSON.stringify(a))"],cwd=ROOT).decode('utf8'))
def get(url):
    return urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'LocalTravelCatalog/1.0'}),timeout=20).read()
missing=[i for i in items if not i['image']]
params=urllib.parse.urlencode(dict(action='query',format='json',prop='pageimages',piprop='thumbnail',pithumbsize=640,redirects=1,converttitles=1,titles='|'.join(i['name'] for i in missing)))
result=json.loads(get('https://zh.wikipedia.org/w/api.php?'+params))['query']
aliases={i['from']:i['to'] for key in ['normalized','converted','redirects'] for i in result.get(key,[])}
pages={p['title']:p for p in result['pages'].values()}
for item in items:
    if item['image']:
        url=item['licenseUrl']
        if '/thumb/' in url:
            filename=urllib.parse.unquote(url.split('/')[-2])
            credit='https://commons.wikimedia.org/wiki/File:'+urllib.parse.quote(filename)
            item.update(source=credit,licenseUrl=credit,license='图片授权信息见来源页')
        continue
    name=item['name']
    for _ in range(4): name=aliases.get(name,name)
    thumb=pages.get(name,{}).get('thumbnail',{}).get('source')
    if not thumb: print(item['name'],'text guide');continue
    try:
        (ROOT/'public/attractions'/f"{item['id']}.jpg").write_bytes(get(thumb))
        filename=urllib.parse.unquote(thumb.split('/')[-2])
        credit='https://commons.wikimedia.org/wiki/File:'+urllib.parse.quote(filename)
        item.update(image=f"/attractions/{item['id']}.jpg",source=credit,author='Wikimedia Commons · 作者见来源页',license='图片授权信息见来源页',licenseUrl=credit)
        print(item['name'],'downloaded')
    except Exception as e: print(item['name'],str(e))
(ROOT/'src/constants/destinations.js').write_text('export const additionalAttractions = '+json.dumps(items,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
