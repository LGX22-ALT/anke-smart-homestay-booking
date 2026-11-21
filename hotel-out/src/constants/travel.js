import { additionalAttractions } from './destinations'
export const attractions = [
  {
    name: '广州塔',
    district: '海珠区 · 珠江新城对岸',
    description:
      '沿珠江抬头看见广州的城市天际线。傍晚登塔看落日，入夜后再沿江散步，是第一次到广州不能错过的行程。',
    tag: '城市地标',
    image: '/attractions/sight-canton-tower.jpg',
    source: 'https://commons.wikimedia.org/wiki/File:Canton_Tower_20241027.jpg',
    author: 'Tim Wu',
    license: 'CC BY-SA 4.0',
    licenseUrl: 'https://creativecommons.org/licenses/by-sa/4.0/',
  },
  {
    name: '北京路步行街',
    district: '越秀区 · 北京路文化旅游区',
    description:
      '千年商脉与现代街区在这里交汇。可以看古道遗址、逛骑楼商铺，也能一路寻找糖水、肠粉和地道粤味。',
    tag: '逛吃首选',
    image: '/attractions/beijing-road-street-2025.jpg',
    source: 'https://commons.wikimedia.org/wiki/File:20250222_Street_scene_of_Beijing_Road_03.jpg',
    author: 'Windmemories',
    license: 'CC BY-SA 4.0',
    licenseUrl: 'https://creativecommons.org/licenses/by-sa/4.0/',
  },
  {
    name: '广州长隆',
    district: '番禺区 · 长隆旅游度假区',
    description:
      '从野生动物世界到欢乐世界，一站安排亲子互动与主题游乐。适合留出完整一天，慢慢体验园区里的精彩项目。',
    tag: '亲子度假',
    image: '/attractions/sight-chimelong.jpg',
    source:
      'https://commons.wikimedia.org/wiki/File:Asian_Elephant_at_Chimelong_Safari_Park,_Guangzhou.jpg',
    author: 'Haiqal08',
    license: 'CC BY-SA 4.0',
    licenseUrl: 'https://creativecommons.org/licenses/by-sa/4.0/',
  },
  {
    name: '沙面岛',
    district: '荔湾区 · 珠江白鹅潭畔',
    description:
      '古树、花园与欧陆建筑围成安静街区。午后适合沿林荫道散步，在老建筑与街角咖啡馆之间感受广州慢生活。',
    tag: '城市漫步',
    image: '/attractions/shamian-riverside-promenade.jpg',
    source: 'https://commons.wikimedia.org/wiki/File:Shamian_island_(87606).jpg',
    author: 'Rc1959',
    license: 'CC BY-SA 4.0',
    licenseUrl: 'https://creativecommons.org/licenses/by-sa/4.0/',
  },
  {
    name: '陈家祠',
    district: '荔湾区 · 中山七路',
    description:
      '木雕、石雕、砖雕与陶塑浓缩了岭南建筑工艺。穿过庭院与厅堂，能在丰富细节里读懂广府文化。',
    tag: '岭南文化',
    image: '/attractions/chen-clan-roof-ridge-2019.jpg',
    source: 'https://commons.wikimedia.org/wiki/File:%E9%A1%B6%E9%83%A8%E6%B5%AE%E9%9B%95.jpg',
    author: 'veronicakkk',
    license: 'CC BY-SA 3.0',
    licenseUrl: 'https://creativecommons.org/licenses/by-sa/3.0/',
  },
]
  .map((item, index) => ({
    ...item,
    id: ['canton-tower', 'beijing-road', 'chimelong', 'shamian', 'chen-clan'][index],
    city: '广州',
  }))
  .concat(
    additionalAttractions.filter(
      (item) =>
        item.city !== '广州' ||
        !['广州塔', '北京路步行街', '广州长隆', '沙面岛', '陈家祠'].includes(item.name),
    ),
  )

export const travelCities = ['北京', '上海', '广州', '深圳', '杭州', '成都', '厦门', '大理']
export const guides = [
  {
    id: 'guangzhou-weekend',
    title: '广州两天一夜，住进城市烟火里',
    city: '广州',
    theme: '城市漫步',
    image: attractions[0].image,
    summary: '把老街、美食与珠江两岸的风景串成一个轻松周末。',
    days: [
      ['北京路步行街：走走老街，安排午餐', '陈家祠：留时间看岭南建筑细节', '沙面岛：傍晚沿江散步'],
      ['广州塔周边：看珠江两岸风景', '留半天自由时间，按返程时间安排用餐'],
    ],
    tips: '住宿可按交通便利程度选择；出发前请通过景区官方渠道确认开放安排。',
  },
  {
    id: 'family-holiday',
    title: '长隆亲子出游，留足慢慢玩的时间',
    city: '广州',
    theme: '亲子度假',
    image: attractions[2].image,
    summary: '少换酒店、少赶路，把一天留给园区，一天留给休息。',
    days: [
      ['到达广州，办理入住', '和商家确认交通方式，准备第二天出游物品'],
      ['按兴趣选择长隆园区，预留完整一天', '游玩后回住处休息，不安排紧凑转场'],
    ],
    tips: '不同园区需分别确认票种、入园规则与儿童适用条件；本平台暂不销售门票。',
  },
  {
    id: 'old-guangzhou',
    title: '老广州半日漫步：从骑楼到江畔',
    city: '广州',
    theme: '岭南文化',
    image: attractions[3].image,
    summary: '不赶景点，在树荫和街巷之间认识这座城市。',
    days: [['北京路：街巷散步与当地小吃', '陈家祠：体验岭南传统建筑', '沙面：沿江步道慢走']],
    tips: '这是一份路线建议，交通与停留时间请按个人情况调整。',
  },
]
