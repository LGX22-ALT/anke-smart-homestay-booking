import concurrent.futures
import json
import urllib.request
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
titles = [
    ['Forbidden City','Temple of Heaven','Summer Palace','Badaling','Shichahai','Olympic Forest Park'],
    ['The Bund','Yu Garden','Oriental Pearl Tower','Wukang Road','Shanghai Disney Resort','Zhujiajiao'],
    ['Canton Tower','Beijing Road','Chimelong Safari Park','Shamian','Chen Clan Ancestral Hall','Baiyun Mountain (Guangzhou)','Yuexiu Park','Yongqing Fang'],
    ['Window of the World','Shenzhen Bay Park','Lianhuashan Park','Dameisha Beach','Dapeng Fortress','OCT Loft'],
    ['West Lake','Lingyin Temple','Xixi National Wetland Park','Qiandao Lake','Hefang Street','Liangzhu Museum'],
    ['Chengdu Research Base of Giant Panda Breeding','Kuanzhai Xiangzi','Wuhou Shrine','Jinli','People\'s Park (Chengdu)','Du Fu Thatched Cottage'],
    ['Gulangyu','Nanputuo Temple','Huandao Road','Zengcuoan','Jimei School Village','Xiamen Garden Expo Park'],
    ['Dali City','Erhai','Cangshan','Three Pagodas','Xizhou','Shuanglang'],
]
p = ROOT/'src/constants/destinations.js'
items = json.loads(subprocess.check_output(['node', '--input-type=module', '-e', "import { additionalAttractions } from './src/constants/destinations.js'; process.stdout.write(JSON.stringify(additionalAttractions))"],cwd=ROOT).decode('utf8'))
def fetch(item):
    if item.get('image'): return item['name']+' cached'
    ci,i = map(int,item['id'].split('-')[1:])
    try:
        title=titles[ci][i]
        url='https://en.wikipedia.org/api/rest_v1/page/summary/'+urllib.request.quote(title.replace(' ','_'))+'?redirect=true&catalog=20261115'
        req=urllib.request.Request(url,headers={'User-Agent':'TravelCatalogDemo/1.0'})
        info=json.load(urllib.request.urlopen(req,timeout=15))
        thumb=info.get('thumbnail',{}).get('source')
        if not thumb: return item['name']+' no thumbnail'
        dest=ROOT/'public/attractions'/f"{item['id']}.jpg"
        dest.write_bytes(urllib.request.urlopen(urllib.request.Request(thumb,headers={'User-Agent':'TravelCatalogDemo/1.0'}),timeout=20).read())
        original=info.get('originalimage',{}).get('source','')
        filename=urllib.request.unquote(original.split('/')[-1].split('?')[0])
        if filename and '/thumb/' in original: filename=urllib.request.unquote(original.split('/')[-2])
        credit='https://commons.wikimedia.org/wiki/File:'+urllib.request.quote(filename)
        item.update(image=f"/attractions/{item['id']}.jpg",source=credit,author='Wikimedia Commons · 作者见来源页',license='图片授权信息见来源页',licenseUrl=credit)
        return item['name']+' OK'
    except Exception as e: return item['name']+' '+str(e)[:90]
with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    for result in pool.map(fetch,items): print(result)
p.write_text('export const additionalAttractions = '+json.dumps(items,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
