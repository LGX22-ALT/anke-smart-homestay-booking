// Official Chat Completions presets, checked 2026-11-19. Keep credentials server-side.
export const aiProviders = [
  {
    id: 'deepseek',
    label: 'DeepSeek',
    mark: 'DS',
    note: '官方 API',
    summary: '适合中文检索、结构化提取与日常问答',
    endpoint: 'https://api.deepseek.com/chat/completions',
    models: ['deepseek-flash', 'deepseek-v4-pro'],
    keyUrl: 'https://platform.deepseek.com/api_keys',
    accent: '#4f6ef7',
    soft: '#eef2ff',
  },
  {
    id: 'qwen',
    label: '通义千问',
    mark: 'QW',
    note: '阿里云百炼',
    summary: '兼容 OpenAI 协议，适合中文业务场景',
    endpoint: 'https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions',
    models: ['qwen-plus'],
    keyUrl: 'https://bailian.console.aliyun.com/',
    accent: '#6e47d9',
    soft: '#f3efff',
  },
  {
    id: 'kimi',
    label: 'Kimi',
    mark: 'K',
    note: 'Moonshot AI',
    summary: '长文本理解与自然语言出行咨询',
    endpoint: 'https://api.moonshot.cn/v1/chat/completions',
    models: ['kimi-k3.1-pro', 'kimi-k3'],
    keyUrl: 'https://platform.kimi.com/',
    accent: '#16181d',
    soft: '#f1f2f4',
  },
  {
    id: 'glm',
    label: '智谱 GLM',
    mark: 'GL',
    note: '智谱开放平台',
    summary: '适合智能客服、信息整理和工具调用',
    endpoint: 'https://open.bigmodel.cn/api/paas/v4/chat/completions',
    models: ['glm-4.7-flash', 'glm-4.7'],
    keyUrl: 'https://bigmodel.cn/',
    accent: '#2967e8',
    soft: '#edf4ff',
  },
  {
    id: 'siliconflow',
    label: '硅基流动',
    mark: 'SF',
    note: '多模型聚合平台',
    summary: '一个密钥接入多个主流开源模型',
    endpoint: 'https://api.siliconflow.cn/v1/chat/completions',
    models: ['Qwen/Qwen3.6-27B'],
    keyUrl: 'https://cloud.siliconflow.cn/',
    accent: '#0a9a75',
    soft: '#eaf9f5',
  },
  {
    id: 'openai',
    label: 'OpenAI',
    mark: 'OA',
    note: '官方 API',
    summary: '成熟的通用模型与标准接口生态',
    endpoint: 'https://api.openai.com/v1/chat/completions',
    models: ['gpt-4.1-mini'],
    keyUrl: 'https://platform.openai.com/api-keys',
    accent: '#15866c',
    soft: '#eaf7f3',
  },
]

export function detectAiProvider(endpoint, model) {
  if (!endpoint && !model) return 'deepseek'
  return (
    aiProviders.find((item) => item.endpoint === endpoint && item.models.includes(model))?.id ||
    'custom'
  )
}

export function canReuseAiKey(hasKey, savedEndpoint, endpoint) {
  return Boolean(hasKey && savedEndpoint && savedEndpoint === endpoint.trim())
}
