<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight, CircleCheck, Connection, Hide, Key, Lock, View } from '@element-plus/icons-vue'
import { getAiSettings, saveAiSettings, testAiConnection } from '@/api/modules/ai'
import { aiProviders, detectAiProvider, canReuseAiKey } from '@/constants/aiProviders'

const customProvider = {
  id: 'custom',
  label: '自定义服务',
  mark: '+',
  note: 'OpenAI 兼容接口',
  summary: '接入企业网关或其他兼容服务',
  accent: '#526070',
  soft: '#eef1f4',
}

const form = reactive({ enabled: false, endpoint: '', model: '', apiKey: '', clearApiKey: false })
const loading = ref(true)
const saving = ref(false)
const testing = ref(false)
const hasApiKey = ref(false)
const loadError = ref('')
const testMessage = ref('')
const error = ref('')
const providerId = ref('deepseek')
const savedEndpoint = ref('')
const showApiKey = ref(false)
const provider = computed(() => aiProviders.find((item) => item.id === providerId.value))
const selectedProvider = computed(() => provider.value || customProvider)
const reusableKey = computed(() =>
  canReuseAiKey(hasApiKey.value, savedEndpoint.value, form.endpoint),
)
const busy = computed(() => saving.value || testing.value)
const configurationReady = computed(() =>
  Boolean(form.endpoint.trim() && form.model.trim() && (form.apiKey.trim() || reusableKey.value)),
)
const connectionState = computed(() => {
  if (testing.value) return { label: '正在检测', state: 'checking' }
  if (testMessage.value) return { label: '连接正常', state: 'success' }
  if (error.value) return { label: '需要检查', state: 'error' }
  if (hasApiKey.value && form.enabled) return { label: '已启用', state: 'active' }
  if (configurationReady.value) return { label: '等待测试', state: 'ready' }
  return { label: '待配置', state: 'idle' }
})

function chooseProvider(id) {
  if (busy.value || id === providerId.value) return
  providerId.value = id
  form.apiKey = ''
  form.clearApiKey = false
  showApiKey.value = false
  if (provider.value) {
    form.endpoint = provider.value.endpoint
    form.model = provider.value.models[0]
  } else {
    form.endpoint = ''
    form.model = ''
  }
  error.value = ''
  testMessage.value = ''
}

watch(
  () => form.endpoint,
  (value, previous) => {
    if (value !== previous) form.apiKey = ''
  },
  { flush: 'sync' },
)
watch(
  () => [form.endpoint, form.model, form.apiKey, form.enabled, form.clearApiKey],
  () => {
    error.value = ''
    testMessage.value = ''
  },
  { flush: 'sync' },
)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getAiSettings()
    form.enabled = data.enabled
    form.endpoint = data.endpoint
    form.model = data.model
    hasApiKey.value = data.hasApiKey
    savedEndpoint.value = data.endpoint
    providerId.value = detectAiProvider(data.endpoint, data.model)
    if (!data.endpoint && !data.model) {
      form.endpoint = provider.value.endpoint
      form.model = provider.value.models[0]
    }
  } catch (cause) {
    loadError.value = cause.message || '读取配置失败'
  } finally {
    loading.value = false
  }
}

function validate() {
  if (
    !form.endpoint.trim() ||
    !form.model.trim() ||
    (!form.apiKey.trim() && (!reusableKey.value || form.clearApiKey))
  ) {
    error.value = provider.value
      ? '请填写当前服务商的 API 密钥。'
      : '请填写完整的接口地址、模型 ID 和 API 密钥。'
    return false
  }
  return true
}

async function save() {
  if (busy.value) return
  error.value = ''
  testMessage.value = ''
  if (form.enabled && !validate()) return
  saving.value = true
  try {
    const data = await saveAiSettings({
      ...form,
      clearApiKey: form.clearApiKey || !reusableKey.value,
    })
    hasApiKey.value = data.hasApiKey
    savedEndpoint.value = data.endpoint
    form.apiKey = ''
    form.clearApiKey = false
    showApiKey.value = false
    ElMessage.success('AI 服务配置已保存')
  } catch (cause) {
    error.value = cause.message
  } finally {
    saving.value = false
  }
}

async function test() {
  if (busy.value) return
  error.value = ''
  testMessage.value = ''
  if (!validate()) return
  testing.value = true
  try {
    const data = await testAiConnection({
      ...form,
      clearApiKey: form.clearApiKey || !reusableKey.value,
    })
    testMessage.value = data.message
  } catch (cause) {
    error.value = cause.message
  } finally {
    testing.value = false
  }
}

onMounted(load)
</script>

<template>
  <section class="ai-settings">
    <header class="settings-heading">
      <div>
        <p class="eyebrow">AI SERVICE CENTER</p>
        <h2>模型服务接入</h2>
        <p class="heading-copy">连接平台所需的大模型，让住客通过自然语言查找已上架房源。</p>
      </div>
      <span class="connection-pill" :class="`is-${connectionState.state}`">
        <i aria-hidden="true"></i>{{ connectionState.label }}
      </span>
    </header>

    <ol class="setup-steps" aria-label="接入流程">
      <li class="is-current"><span>1</span><strong>选择服务</strong></li>
      <li :class="{ 'is-current': configurationReady }"><span>2</span><strong>填写凭证</strong></li>
      <li :class="{ 'is-current': testMessage }"><span>3</span><strong>测试并启用</strong></li>
    </ol>

    <div v-if="loading" class="loading-panel" role="status" aria-live="polite">
      <span class="loading-mark" aria-hidden="true"></span>
      <div>
        <strong>正在读取服务配置</strong>
        <p>请稍候，正在安全加载模型与密钥状态。</p>
      </div>
    </div>
    <div v-else-if="loadError" class="load-error" role="alert">
      <div>
        <strong>配置读取失败</strong>
        <p>{{ loadError }}</p>
      </div>
      <button class="secondary-button" type="button" @click="load">重新读取</button>
    </div>

    <form v-else class="model-workspace" @submit.prevent="save">
      <aside class="provider-sidebar" aria-label="模型服务商">
        <div class="sidebar-heading">
          <span>服务商</span><small>{{ aiProviders.length + 1 }} 个接入方式</small>
        </div>
        <div class="provider-list" role="group" aria-label="选择服务商">
          <button
            v-for="item in aiProviders"
            :key="item.id"
            type="button"
            class="provider-choice"
            :class="{ selected: providerId === item.id }"
            :aria-pressed="providerId === item.id"
            :disabled="busy"
            @click="chooseProvider(item.id)"
          >
            <span class="provider-mark" :style="{ '--mark': item.accent, '--soft': item.soft }">{{
              item.mark
            }}</span>
            <span class="provider-name"
              ><strong>{{ item.label }}</strong
              ><small>{{ item.note }}</small></span
            >
            <ArrowRight class="provider-arrow" aria-hidden="true" />
          </button>
          <button
            type="button"
            class="provider-choice"
            :class="{ selected: providerId === 'custom' }"
            :aria-pressed="providerId === 'custom'"
            :disabled="busy"
            @click="chooseProvider('custom')"
          >
            <span class="provider-mark custom-mark">+</span>
            <span class="provider-name"><strong>自定义</strong><small>兼容服务</small></span>
            <ArrowRight class="provider-arrow" aria-hidden="true" />
          </button>
        </div>
        <div class="security-note">
          <Lock aria-hidden="true" />
          <span
            ><strong>凭证安全</strong
            ><small>API 密钥仅加密提交至后端，不写入浏览器缓存。</small></span
          >
        </div>
      </aside>

      <fieldset class="configuration-pane" :disabled="busy" aria-label="模型接入配置">
        <div class="provider-overview">
          <span
            class="provider-mark provider-mark--large"
            :style="{ '--mark': selectedProvider.accent, '--soft': selectedProvider.soft }"
            >{{ selectedProvider.mark }}</span
          >
          <div class="provider-overview-copy">
            <span>{{ selectedProvider.note }}</span>
            <h3>{{ selectedProvider.label }}</h3>
            <p>{{ selectedProvider.summary }}</p>
          </div>
          <label class="enable-control">
            <input v-model="form.enabled" type="checkbox" />
            <span aria-hidden="true"></span>
            <strong>{{ form.enabled ? '服务已启用' : '启用首页 AI 助手' }}</strong>
          </label>
        </div>

        <div class="config-section">
          <div class="section-title">
            <span>模型配置</span><small>系统会根据所选服务商自动补全标准接口</small>
          </div>
          <div class="config-grid">
            <template v-if="provider">
              <label class="config-field">
                <span>使用模型</span>
                <select v-model="form.model">
                  <option v-for="model in provider.models" :key="model" :value="model">
                    {{ model }}
                  </option>
                </select>
              </label>
              <div class="config-field">
                <span>接口地址</span>
                <div class="readonly-value">
                  <Connection aria-hidden="true" /><code>{{ form.endpoint }}</code>
                </div>
                <small>由系统维护，无需手动修改。</small>
              </div>
            </template>
            <template v-else>
              <label class="config-field">
                <span>完整接口地址</span>
                <input
                  v-model.trim="form.endpoint"
                  type="url"
                  maxlength="500"
                  placeholder="https://example.com/v1/chat/completions"
                />
                <small>仅支持 HTTPS，并需要兼容 Chat Completions 请求格式。</small>
              </label>
              <label class="config-field">
                <span>模型 ID</span>
                <input v-model.trim="form.model" maxlength="100" placeholder="例如 model-chat" />
                <small>填写接口实际使用的模型 ID，而不是产品展示名称。</small>
              </label>
            </template>
          </div>
        </div>

        <div class="config-section credentials-section">
          <div class="section-title">
            <span>访问凭证</span><small>密钥用于后端调用模型服务</small>
          </div>
          <label class="config-field key-field">
            <span>API 密钥</span>
            <div class="key-input-wrap">
              <Key aria-hidden="true" />
              <input
                v-model.trim="form.apiKey"
                :type="showApiKey ? 'text' : 'password'"
                autocomplete="new-password"
                maxlength="1000"
                :placeholder="
                  reusableKey ? '密钥已安全保存，留空表示继续使用' : '输入当前服务商的 API Key'
                "
              />
              <button
                type="button"
                :aria-label="showApiKey ? '隐藏密钥内容' : '显示密钥内容'"
                @click="showApiKey = !showApiKey"
              >
                <Hide v-if="showApiKey" aria-hidden="true" /><View v-else aria-hidden="true" />
              </button>
            </div>
            <small v-if="reusableKey" class="saved-key"
              ><CircleCheck aria-hidden="true" />已保存可用密钥；输入新值将替换原密钥。</small
            >
            <small v-else>密钥通过加密连接提交，页面不会回显已保存内容。</small>
          </label>
          <div class="credential-actions">
            <a v-if="provider" :href="provider.keyUrl" target="_blank" rel="noopener noreferrer"
              >前往 {{ provider.label }} 控制台获取密钥 <ArrowRight aria-hidden="true"
            /></a>
            <label v-if="reusableKey" class="clear-key"
              ><input v-model="form.clearApiKey" type="checkbox" />保存时清除已有密钥</label
            >
          </div>
          <p v-if="hasApiKey && !reusableKey" class="inline-notice">
            当前接口与已保存密钥不匹配，请为新服务重新填写密钥。
          </p>
        </div>

        <p v-if="error" class="result-message is-error error" role="alert">{{ error }}</p>
        <p v-if="testMessage" class="result-message is-success success" role="status">
          <CircleCheck aria-hidden="true" />{{ testMessage }}，保存配置后生效。
        </p>

        <footer class="configuration-footer">
          <div>
            <strong>连接检查</strong><span>建议先测试，确认服务地址、模型和额度均可用。</span>
          </div>
          <div class="actions">
            <button class="secondary-button" :disabled="busy" type="button" @click="test">
              <Connection aria-hidden="true" />{{ testing ? '正在测试…' : '测试连接' }}
            </button>
            <button class="primary-button" :disabled="busy" type="submit">
              {{ saving ? '正在保存…' : '保存并应用' }}<ArrowRight aria-hidden="true" />
            </button>
          </div>
        </footer>
      </fieldset>
    </form>
  </section>
</template>

<style scoped>
.ai-settings {
  width: min(100%, 1080px);
  color: #172239;
}
.settings-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
  margin-bottom: 26px;
}
.eyebrow {
  margin: 0 0 8px;
  color: #7151c8;
  font:
    700 11px/1.2 ui-monospace,
    SFMono-Regular,
    Consolas,
    monospace;
  letter-spacing: 0.14em;
}
.settings-heading h2 {
  margin: 0;
  font-size: clamp(28px, 3vw, 38px);
  line-height: 1.15;
  letter-spacing: -0.04em;
}
.heading-copy {
  max-width: 640px;
  margin: 12px 0 0;
  color: #66738a;
  font-size: 14px;
  line-height: 1.75;
}
.connection-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex: none;
  padding: 9px 13px;
  border: 1px solid #dfe4ec;
  border-radius: 999px;
  background: #fff;
  color: #657187;
  font-size: 12px;
  font-weight: 700;
}
.connection-pill i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #a7afbc;
}
.connection-pill.is-active i,
.connection-pill.is-success i {
  background: #21a77a;
  box-shadow: 0 0 0 4px #dff7ee;
}
.connection-pill.is-ready i,
.connection-pill.is-checking i {
  background: #e9a23b;
  box-shadow: 0 0 0 4px #fff3dc;
}
.connection-pill.is-error i {
  background: #d94d4d;
  box-shadow: 0 0 0 4px #fde8e8;
}
.setup-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin: 0 0 18px;
  padding: 0;
  list-style: none;
}
.setup-steps li {
  position: relative;
  display: flex;
  align-items: center;
  gap: 9px;
  color: #8a94a5;
  font-size: 12px;
}
.setup-steps li:not(:last-child)::after {
  content: '';
  height: 1px;
  flex: 1;
  margin: 0 16px;
  background: #dfe4ec;
}
.setup-steps span {
  display: grid;
  place-items: center;
  width: 23px;
  height: 23px;
  border: 1px solid #d4dae4;
  border-radius: 50%;
  background: #fff;
  font-size: 11px;
}
.setup-steps .is-current {
  color: #50369d;
}
.setup-steps .is-current span {
  border-color: #7151c8;
  background: #7151c8;
  color: #fff;
}
.loading-panel,
.load-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  min-height: 160px;
  padding: 32px;
  border: 1px solid #dfe4ec;
  border-radius: 16px;
  background: #fff;
}
.loading-panel {
  justify-content: center;
}
.loading-panel p,
.load-error p {
  margin: 5px 0 0;
  color: #758198;
  font-size: 13px;
}
.loading-mark {
  width: 28px;
  height: 28px;
  border: 2px solid #e3def2;
  border-top-color: #7151c8;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
.model-workspace {
  display: grid;
  grid-template-columns: 250px minmax(0, 1fr);
  overflow: hidden;
  min-height: 660px;
  border: 1px solid #dfe4ec;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 16px 45px rgba(34, 42, 70, 0.06);
}
.provider-sidebar {
  display: flex;
  flex-direction: column;
  padding: 22px 14px 18px;
  border-right: 1px solid #e4e8ef;
  background: #f8f9fc;
}
.sidebar-heading {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 0 8px 14px;
}
.sidebar-heading span {
  font-size: 13px;
  font-weight: 750;
}
.sidebar-heading small {
  color: #929bad;
  font-size: 11px;
}
.provider-list {
  display: grid;
  gap: 5px;
}
.provider-choice {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) 16px;
  align-items: center;
  gap: 10px;
  min-height: 58px;
  padding: 8px 10px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: #253148;
  text-align: left;
  cursor: pointer;
  transition:
    border-color 0.16s ease,
    background 0.16s ease,
    transform 0.16s ease;
}
.provider-choice:hover {
  border-color: #e0d9f3;
  background: #fff;
  transform: translateX(2px);
}
.provider-choice.selected {
  border-color: #d8cff0;
  background: #fff;
  box-shadow: 0 5px 16px rgba(84, 61, 151, 0.08);
}
.provider-choice:focus-visible,
.primary-button:focus-visible,
.secondary-button:focus-visible,
.key-input-wrap button:focus-visible {
  outline: 3px solid rgba(113, 81, 200, 0.28);
  outline-offset: 2px;
}
.provider-mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: var(--soft, #eef1f4);
  color: var(--mark, #526070);
  font:
    800 11px/1 ui-monospace,
    Consolas,
    monospace;
  letter-spacing: -0.02em;
}
.custom-mark {
  font-family: inherit;
  font-size: 21px;
  font-weight: 400;
}
.provider-name {
  display: grid;
  gap: 3px;
  min-width: 0;
}
.provider-name strong {
  font-size: 13px;
}
.provider-name small {
  overflow: hidden;
  color: #8993a4;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.provider-arrow {
  width: 14px;
  color: #a7afbc;
  opacity: 0;
  transition:
    opacity 0.16s ease,
    transform 0.16s ease;
}
.provider-choice.selected .provider-arrow,
.provider-choice:hover .provider-arrow {
  opacity: 1;
  transform: translateX(2px);
}
.security-note {
  display: flex;
  gap: 10px;
  margin-top: auto;
  padding: 16px 9px 2px;
  border-top: 1px solid #e3e7ee;
  color: #7d8798;
}
.security-note > svg {
  width: 17px;
  flex: none;
  color: #7151c8;
}
.security-note span {
  display: grid;
  gap: 5px;
}
.security-note strong {
  color: #525e72;
  font-size: 11px;
}
.security-note small {
  font-size: 10px;
  line-height: 1.55;
}
.configuration-pane {
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}
.provider-overview {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 25px 28px;
  border-bottom: 1px solid #e7eaf0;
}
.provider-mark--large {
  width: 58px;
  height: 58px;
  border-radius: 15px;
  font-size: 15px;
}
.provider-overview-copy span {
  color: #8791a4;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}
.provider-overview-copy h3 {
  margin: 4px 0 4px;
  font-size: 20px;
  letter-spacing: -0.02em;
}
.provider-overview-copy p {
  margin: 0;
  color: #758198;
  font-size: 12px;
}
.enable-control {
  display: flex;
  align-items: center;
  gap: 9px;
  cursor: pointer;
}
.enable-control input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
}
.enable-control > span {
  position: relative;
  width: 37px;
  height: 21px;
  border-radius: 999px;
  background: #c8cfda;
  transition: background 0.18s ease;
}
.enable-control > span::after {
  content: '';
  position: absolute;
  top: 3px;
  left: 3px;
  width: 15px;
  height: 15px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
  transition: transform 0.18s ease;
}
.enable-control input:checked + span {
  background: #7151c8;
}
.enable-control input:checked + span::after {
  transform: translateX(16px);
}
.enable-control input:focus-visible + span {
  outline: 3px solid rgba(113, 81, 200, 0.25);
  outline-offset: 2px;
}
.enable-control strong {
  font-size: 12px;
}
.config-section {
  padding: 24px 28px;
  border-bottom: 1px solid #e7eaf0;
}
.section-title {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 17px;
}
.section-title span {
  font-size: 13px;
  font-weight: 750;
}
.section-title small {
  color: #8a94a5;
  font-size: 11px;
}
.config-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}
.config-field {
  display: grid;
  align-content: start;
  gap: 8px;
  min-width: 0;
  color: #526076;
  font-size: 11px;
  font-weight: 650;
}
.config-field input,
.config-field select,
.readonly-value {
  width: 100%;
  min-height: 46px;
  box-sizing: border-box;
  border: 1px solid #d5dce7;
  border-radius: 9px;
  background: #fff;
  color: #1f2c43;
  font: 500 13px/1.4 inherit;
}
.config-field input,
.config-field select {
  padding: 0 13px;
}
.config-field input:focus,
.config-field select:focus {
  border-color: #7151c8;
  outline: 3px solid rgba(113, 81, 200, 0.12);
}
.config-field small {
  color: #9099aa;
  font-size: 10px;
  font-weight: 400;
  line-height: 1.5;
}
.readonly-value {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
  padding: 0 12px;
  background: #f7f8fa;
}
.readonly-value svg {
  width: 15px;
  flex: none;
  color: #7151c8;
}
.readonly-value code {
  overflow: hidden;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.key-field {
  max-width: 100%;
}
.key-input-wrap {
  position: relative;
}
.key-input-wrap > svg {
  position: absolute;
  top: 15px;
  left: 13px;
  z-index: 1;
  width: 16px;
  color: #8c96a7;
}
.key-input-wrap input {
  padding-right: 45px;
  padding-left: 40px;
}
.key-input-wrap button {
  position: absolute;
  top: 5px;
  right: 6px;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 7px;
  background: transparent;
  color: #7d8798;
  cursor: pointer;
}
.key-input-wrap button:hover {
  background: #f0edf8;
  color: #6044ac;
}
.key-input-wrap button svg {
  width: 17px;
}
.saved-key {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #218162 !important;
}
.saved-key svg {
  width: 13px;
}
.credential-actions {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  margin-top: 13px;
}
.credential-actions a {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #6044ac;
  font-size: 11px;
  font-weight: 650;
  text-decoration: none;
}
.credential-actions a:hover {
  text-decoration: underline;
  text-underline-offset: 3px;
}
.credential-actions a svg {
  width: 13px;
}
.clear-key {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #69758a;
  font-size: 10px;
}
.clear-key input {
  accent-color: #7151c8;
}
.inline-notice,
.result-message {
  margin: 14px 0 0;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 11px;
  line-height: 1.55;
}
.inline-notice {
  background: #fff7e8;
  color: #8c6228;
}
.result-message {
  display: flex;
  align-items: center;
  gap: 7px;
  margin: 18px 28px 0;
}
.result-message svg {
  width: 15px;
  flex: none;
}
.result-message.is-error {
  border: 1px solid #f2cccc;
  background: #fff2f2;
  color: #b43d3d;
}
.result-message.is-success {
  border: 1px solid #cdeadd;
  background: #effaf5;
  color: #21775a;
}
.configuration-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 28px 25px;
}
.configuration-footer > div:first-child {
  display: grid;
  gap: 4px;
}
.configuration-footer strong {
  font-size: 12px;
}
.configuration-footer span {
  color: #8a94a5;
  font-size: 10px;
}
.actions {
  display: flex;
  gap: 10px;
}
.primary-button,
.secondary-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  min-height: 42px;
  padding: 0 16px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 750;
  cursor: pointer;
}
.primary-button {
  border: 1px solid #7151c8;
  background: #7151c8;
  color: #fff;
  box-shadow: 0 7px 16px rgba(113, 81, 200, 0.18);
}
.primary-button:hover {
  border-color: #6044ac;
  background: #6044ac;
}
.secondary-button {
  border: 1px solid #cfd6e2;
  background: #fff;
  color: #354158;
}
.secondary-button:hover {
  border-color: #b9a9df;
  background: #faf8ff;
  color: #6044ac;
}
.primary-button svg,
.secondary-button svg {
  width: 15px;
}
button:disabled,
fieldset:disabled .enable-control {
  opacity: 0.58;
  cursor: not-allowed;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
@media (max-width: 900px) {
  .model-workspace {
    grid-template-columns: 1fr;
    min-height: 0;
  }
  .provider-sidebar {
    border-right: 0;
    border-bottom: 1px solid #e4e8ef;
  }
  .provider-list {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
  .provider-choice {
    grid-template-columns: 36px minmax(0, 1fr);
  }
  .provider-arrow,
  .security-note {
    display: none;
  }
}
@media (max-width: 620px) {
  .settings-heading {
    align-items: flex-start;
  }
  .connection-pill {
    margin-top: 4px;
    padding: 7px 9px;
  }
  .connection-pill {
    font-size: 0;
  }
  .setup-steps strong {
    font-size: 10px;
  }
  .setup-steps li:not(:last-child)::after {
    margin: 0 8px;
  }
  .model-workspace {
    border-radius: 13px;
  }
  .provider-sidebar {
    padding: 16px 10px;
  }
  .provider-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 6px;
  }
  .provider-choice {
    min-height: 54px;
    padding: 7px;
  }
  .provider-overview {
    grid-template-columns: 48px minmax(0, 1fr);
    gap: 12px;
    padding: 20px 16px;
  }
  .provider-mark--large {
    width: 48px;
    height: 48px;
  }
  .enable-control {
    grid-column: 1/-1;
    justify-content: space-between;
    margin-top: 5px;
    padding-top: 15px;
    border-top: 1px solid #e7eaf0;
  }
  .enable-control > span {
    order: 2;
  }
  .config-section {
    padding: 22px 16px;
  }
  .section-title {
    display: grid;
    gap: 5px;
  }
  .config-grid {
    grid-template-columns: 1fr;
  }
  .credential-actions,
  .configuration-footer {
    align-items: stretch;
    flex-direction: column;
  }
  .configuration-footer {
    padding: 20px 16px 22px;
  }
  .actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
  .result-message {
    margin-right: 16px;
    margin-left: 16px;
  }
}
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
  }
}
</style>
