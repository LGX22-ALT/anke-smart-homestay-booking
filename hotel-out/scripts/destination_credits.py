import json
import re
import subprocess
import urllib.parse
import urllib.request
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
items=json.loads(subprocess.check_output(['node','--input-type=module','-e',"import {additionalAttractions as a} from './src/constants/destinations.js';process.stdout.write(JSON.stringify(a))"],cwd=ROOT).decode('utf8'))
titles=[urllib.parse.unquote(i['source'].split('/wiki/')[-1]) for i in items if i['image'] and '/wiki/File:' in i['source']]
params=urllib.parse.urlencode(dict(action='query',format='json',prop='imageinfo',iiprop='extmetadata',titles='|'.join(titles)))
url='https://commons.wikimedia.org/w/api.php?'+params
result=json.load(urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'LocalTravelCatalog/1.0'}),timeout=25))
meta={p['title']:p.get('imageinfo',[{}])[0].get('extmetadata',{}) for p in result['query']['pages'].values()}
for item in items:
    key=urllib.parse.unquote(item['source'].split('/wiki/')[-1])
    m=meta.get(key,{})
    if m:
        item['author']=re.sub('<[^>]+>','',m.get('Artist',{}).get('value','Wikimedia Commons'))[:300]
        item['license']=m.get('LicenseShortName',{}).get('value','图片授权信息见来源页')
        item['licenseUrl']=m.get('LicenseUrl',{}).get('value',item['source'])
(ROOT/'src/constants/destinations.js').write_text('export const additionalAttractions = '+json.dumps(items,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print('Updated image author and license metadata:',sum(bool(i['image']) for i in items))
