<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Cpu, Connection, Edit, Delete, VideoPlay, View, Hide } from '@element-plus/icons-vue'
import { aiProviders } from '@/constants/aiProviders'
import {
  listAiConnections,
  saveAiConnection,
  activateAiConnection,
  deleteAiConnection,
  testSavedAiConnection,
  testAiDraft,
} from '@/api/modules/ai'

const items = ref([])
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const dialog = ref(false)
const editing = ref(null)
const showKey = ref(false)
const feedback = ref('')
const form = reactive({
  name: '',
  provider: 'deepseek',
  baseUrl: '',
  model: '',
  apiKey: '',
  enabled: true,
  maxTokens: 4096,
  temperature: 0.6,
})
const active = computed(() => items.value.find((i) => i.active))
const selected = computed(() => aiProviders.find((i) => i.id === form.provider))
const changedEndpoint = computed(
  () => editing.value && editing.value.baseUrl !== form.baseUrl.replace(/\/$/, ''),
)
async function load() {
  loading.value = true
  try {
    items.value = await listAiConnections()
    error.value = ''
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
function preset(id) {
  const p = aiProviders.find((i) => i.id === id)
  form.provider = id
  form.baseUrl = p ? p.endpoint.replace(/\/chat\/completions$/, '') : ''
  form.model = p?.models[0] || ''
  form.name = p ? `${p.label} · ${form.model}` : ''
  form.apiKey = ''
  feedback.value = ''
}
function open(item) {
  editing.value = item || null
  if (item) Object.assign(form, item, { apiKey: '' })
  else {
    Object.assign(form, { enabled: true, maxTokens: 4096, temperature: 0.6 })
    preset('deepseek')
  }
  feedback.value = ''
  showKey.value = false
  dialog.value = true
}
async function run(action) {
  if (busy.value) return
  busy.value = true
  feedback.value = ''
  try {
    await action()
  } catch (e) {
    if (dialog.value) feedback.value = e.message
    else ElMessage.error(e.message)
  } finally {
    busy.value = false
  }
}
function validate() {
  if (!form.name.trim() || !form.model.trim() || !form.baseUrl.trim())
    throw new Error('请填写配置名称、基础地址和模型 ID')
  if (!form.apiKey && (!editing.value?.hasApiKey || changedEndpoint.value))
    throw new Error('请填写当前接口的 API 密钥')
}
function save() {
  return run(async () => {
    validate()
    await saveAiConnection(editing.value?.id, { ...form })
    dialog.value = false
    await load()
    ElMessage.success('模型连接已保存')
  })
}
function testDraft() {
  return run(async () => {
    validate()
    const r = await testAiDraft(editing.value?.id, { ...form })
    feedback.value = `${r.message} · ${r.latencyMs} ms`
  })
}
function test(item) {
  return run(async () => {
    const r = await testSavedAiConnection(item.id)
    ElMessage.success(`${item.name}：${r.message} · ${r.latencyMs} ms`)
  })
}
function activate(item) {
  return run(async () => {
    await activateAiConnection(item.id)
    await load()
    ElMessage.success('已切换当前模型')
  })
}
async function remove(item) {
  try {
    await ElMessageBox.confirm(`删除“${item.name}”及其保存的密钥？`, '删除模型连接', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  return run(async () => {
    await deleteAiConnection(item.id)
    await load()
  })
}
onMounted(load)
</script>

<template>
  <section class="connection-library" aria-label="模型连接管理">
    <div class="library-banner">
      <div class="banner-icon"><Cpu /></div>
      <div class="active-copy">
        <small>当前生效模型</small>
        <h2>{{ active?.name || '尚未接入模型' }}</h2>
        <p>
          {{
            active
              ? `${active.model} · ${active.baseUrl}`
              : '添加连接后，即可为首页 AI 助手提供服务'
          }}
        </p>
      </div>
      <button class="connect-button" :disabled="busy" @click="open()"><Plus />接入模型</button>
    </div>
    <div class="list-heading">
      <div>
        <h3>模型连接</h3>
        <p>保存多组配置，按需切换服务。每个连接独立管理访问凭证。</p>
      </div>
      <span>{{ items.length }} 个连接</span>
    </div>
    <p v-if="error" class="library-error" role="alert">
      {{ error }} <button @click="load">重新加载</button>
    </p>
    <div v-if="loading" class="library-empty" role="status">正在加载模型连接…</div>
    <div v-else-if="!items.length && !error" class="library-empty">
      <Connection />
      <h3>连接你的第一个模型</h3>
      <p>选择服务商，填入 API Key，即可测试连接。</p>
      <button class="connect-button" @click="open()">接入模型</button>
    </div>
    <div v-else class="connections-table">
      <div class="table-head">
        <span>配置名称</span><span>模型与接口</span><span>状态</span><span>操作</span>
      </div>
      <article
        v-for="item in items"
        :key="item.id"
        class="connection-row"
        :class="{ 'is-active': item.active }"
      >
        <div class="connection-name">
          <span class="row-mark"><Cpu /></span>
          <div>
            <strong>{{ item.name }}</strong
            ><small>OpenAI 兼容接口</small>
          </div>
        </div>
        <div class="model-cell">
          <strong>{{ item.model }}</strong
          ><span :title="item.baseUrl">{{ item.baseUrl }}</span>
        </div>
        <div>
          <span class="state-tag" :class="{ enabled: item.enabled }">{{
            item.active
              ? item.enabled
                ? '当前使用'
                : '当前已停用'
              : item.enabled
                ? '可切换'
                : '已停用'
          }}</span>
        </div>
        <div class="row-actions">
          <button :disabled="busy" @click="test(item)"><Connection />测试</button
          ><button :disabled="busy || item.active || !item.enabled" @click="activate(item)">
            <VideoPlay />切换</button
          ><button :disabled="busy" @click="open(item)"><Edit />编辑</button
          ><button
            class="danger"
            :disabled="busy || item.active"
            :title="item.active ? '请先切换其他模型' : '删除连接'"
            @click="remove(item)"
          >
            <Delete />删除
          </button>
        </div>
      </article>
    </div>

    <el-dialog
      v-model="dialog"
      :title="editing ? '编辑模型连接' : '接入模型'"
      width="760px"
      class="model-connection-dialog"
      :close-on-click-modal="false"
      :close-on-press-escape="!busy"
      :show-close="!busy"
      :before-close="
        (done) => {
          if (!busy) done()
        }
      "
      destroy-on-close
    >
      <form class="connection-form" @submit.prevent="save">
        <fieldset :disabled="busy">
          <div class="quick-presets">
            <span>快速接入</span
            ><button
              v-for="p in aiProviders"
              :key="p.id"
              type="button"
              :class="{ selected: form.provider === p.id }"
              @click="preset(p.id)"
            >
              {{ p.label }}</button
            ><button
              type="button"
              :class="{ selected: form.provider === 'custom' }"
              @click="preset('custom')"
            >
              自定义
            </button>
          </div>
          <div class="modal-grid">
            <label
              >配置名称<input
                v-model="form.name"
                maxlength="80"
                required
                placeholder="例如：日常问答 · DeepSeek"
            /></label>
            <label>接入类型<input value="OpenAI 兼容接口" readonly /></label>
            <label class="full"
              >接口基础地址<input
                v-model.trim="form.baseUrl"
                type="url"
                maxlength="500"
                required
                placeholder="https://api.example.com/v1"
                @input="
                  () => {
                    form.apiKey = ''
                    feedback = ''
                  }
                "
              /><small
                >已提供服务商默认地址，可自行修改。填写基础地址或完整聊天接口均可。</small
              ></label
            >
            <label class="full"
              >API Key
              <div class="secret-input">
                <input
                  v-model.trim="form.apiKey"
                  :type="showKey ? 'text' : 'password'"
                  autocomplete="new-password"
                  maxlength="1000"
                  :placeholder="
                    editing?.hasApiKey && !changedEndpoint
                      ? '已保存密钥，留空保持不变'
                      : '请输入当前接口的 API Key'
                  "
                /><button
                  type="button"
                  :aria-label="showKey ? '隐藏密钥' : '显示密钥'"
                  @click="showKey = !showKey"
                >
                  <Hide v-if="showKey" /><View v-else />
                </button>
              </div>
              <small v-if="changedEndpoint">接口地址已更改，请重新填写对应密钥。</small></label
            >
            <label
              >对话模型 ID<input
                v-model.trim="form.model"
                list="connection-models"
                maxlength="100"
                required
                placeholder="填写实际 API 模型 ID" /><datalist id="connection-models">
                <option
                  v-for="model in selected?.models || []"
                  :key="model"
                  :value="model"
                /></datalist
            ></label>
            <label
              >最大输出 Token<input
                v-model.number="form.maxTokens"
                type="number"
                min="256"
                max="16384"
                required
            /></label>
            <label
              >生成温度
              <span class="temperature-control"
                ><input
                  v-model.number="form.temperature"
                  type="range"
                  min="0"
                  max="2"
                  step="0.1"
                /><output>{{ form.temperature }}</output></span
              ></label
            >
            <label
              >配置状态<span class="enabled-check"
                ><input
                  v-model="form.enabled"
                  type="checkbox"
                  :disabled="editing?.active"
                />启用此连接</span
              ></label
            >
          </div>
          <a
            v-if="selected"
            class="console-link"
            :href="selected.keyUrl"
            target="_blank"
            rel="noopener noreferrer"
            >前往 {{ selected.label }} 控制台获取 API Key ↗</a
          >
          <p class="key-explanation">已保存密钥不会返回浏览器。切换基础地址后，需重新填写密钥。</p>
          <p v-if="feedback" role="status" class="draft-feedback">{{ feedback }}</p>
        </fieldset>
        <div class="modal-actions">
          <button type="button" :disabled="busy" @click="testDraft">
            {{ busy ? '处理中…' : '测试连接' }}</button
          ><span></span><button type="button" :disabled="busy" @click="dialog = false">取消</button
          ><button class="connect-button" type="submit" :disabled="busy">保存配置</button>
        </div>
      </form>
    </el-dialog>
  </section>
</template>

<style scoped>
.connection-library {
  width: 100%;
  max-width: 1250px;
  color: #202938;
}
.library-banner {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 26px 30px;
  background: #292638;
  color: #fff;
  border-radius: 12px;
}
.banner-icon {
  display: grid;
  place-items: center;
  width: 60px;
  height: 60px;
  flex: none;
  background: #544077;
  border-radius: 12px;
}
.banner-icon svg {
  width: 30px;
}
.active-copy {
  min-width: 0;
  flex: 1;
}
.active-copy small {
  color: #c9bedf;
  font-size: 12px;
}
.active-copy h2 {
  margin: 8px 0;
  font-size: 23px;
  overflow-wrap: anywhere;
}
.active-copy p {
  margin: 0;
  color: #b8b3c9;
  font-size: 12px;
  overflow-wrap: anywhere;
}
.connect-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 18px;
  border: 0;
  border-radius: 7px;
  background: #7151c8;
  color: #fff;
  font-weight: 650;
  font-size: 13px;
  white-space: nowrap;
  cursor: pointer;
}
.connect-button svg {
  width: 17px;
}
.list-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 28px 0 16px;
  gap: 14px;
}
.list-heading h3 {
  font-size: 17px;
  margin: 0 0 7px;
}
.list-heading p {
  font-size: 12px;
  color: #687488;
  margin: 0;
}
.list-heading > span {
  font-size: 12px;
  color: #687488;
  white-space: nowrap;
}
.connections-table {
  border: 1px solid #dce1e8;
  border-radius: 10px;
  overflow: hidden;
  background: white;
}
.table-head,
.connection-row {
  display: grid;
  grid-template-columns: minmax(180px, 1.1fr) minmax(200px, 1.3fr) 95px minmax(255px, 1fr);
  gap: 18px;
  align-items: center;
  padding: 20px;
}
.table-head {
  background: #f8f9fb;
  color: #748093;
  font-size: 12px;
  padding-top: 14px;
  padding-bottom: 14px;
}
.connection-row {
  border-top: 1px solid #e7eaf0;
  min-height: 68px;
}
.connection-row.is-active {
  background: #fbfaff;
}
.connection-name {
  display: flex;
  gap: 12px;
  align-items: center;
  min-width: 0;
}
.connection-name strong,
.model-cell strong {
  font-size: 13px;
  overflow-wrap: anywhere;
}
.connection-name small,
.model-cell > span {
  display: block;
  font-size: 11px;
  color: #8390a0;
  margin-top: 7px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.connection-name > div,
.model-cell {
  min-width: 0;
}
.row-mark {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  flex: none;
  background: #f0edf8;
  color: #7151c8;
  border-radius: 8px;
}
.row-mark svg {
  width: 19px;
}
.state-tag {
  font-size: 11px;
  padding: 5px 8px;
  background: #eef0f4;
  color: #6b7788;
  border-radius: 5px;
  white-space: nowrap;
}
.state-tag.enabled {
  background: #ecf7f1;
  color: #26765a;
}
.row-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.row-actions button {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: 0;
  color: #5c526f;
  font-size: 12px;
  padding: 6px 0;
  cursor: pointer;
}
.row-actions svg {
  width: 14px;
}
.row-actions .danger {
  color: #b64343;
}
button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.library-empty {
  display: grid;
  justify-items: center;
  padding: 55px 20px;
  background: white;
  border: 1px dashed #dce1e8;
  border-radius: 12px;
  color: #718096;
  font-size: 13px;
}
.library-empty > svg {
  width: 34px;
  color: #7151c8;
}
.library-error {
  color: #b42318;
}
.connection-form fieldset {
  border: 0;
  margin: 0;
  padding: 0;
  min-width: 0;
}
.quick-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-bottom: 24px;
}
.quick-presets > span {
  font-size: 12px;
  color: #758094;
  margin-right: 6px;
}
.quick-presets button {
  border: 1px solid #dce1e8;
  background: white;
  padding: 8px 12px;
  border-radius: 6px;
  color: #526173;
  cursor: pointer;
}
.quick-presets .selected {
  border-color: #7151c8;
  background: #f3effb;
  color: #6246a0;
}
.modal-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px 24px;
}
.modal-grid label {
  display: grid;
  gap: 9px;
  font-size: 13px;
  color: #526173;
  min-width: 0;
}
.modal-grid .full {
  grid-column: 1/-1;
}
.modal-grid input:not([type='checkbox']):not([type='range']) {
  box-sizing: border-box;
  width: 100%;
  height: 44px;
  padding: 0 12px;
  border: 1px solid #cfd7e3;
  border-radius: 6px;
  font-size: 14px;
  color: #202938;
  background: #fff;
}
.modal-grid input[readonly] {
  background: #f7f8fa !important;
  color: #7c8796 !important;
}
.modal-grid small {
  font-size: 11px;
  color: #8994a3;
  line-height: 1.6;
}
.secret-input {
  position: relative;
}
.secret-input input {
  padding-right: 45px !important;
}
.secret-input button {
  position: absolute;
  right: 6px;
  top: 5px;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  background: none;
  border: 0;
  color: #718096;
  cursor: pointer;
}
.secret-input svg {
  width: 18px;
}
.temperature-control {
  display: flex;
  align-items: center;
  gap: 15px;
  height: 44px;
}
.temperature-control input {
  width: 100%;
  accent-color: #7151c8;
}
.temperature-control output {
  min-width: 25px;
}
.enabled-check {
  display: flex;
  align-items: center;
  gap: 9px;
  height: 44px;
}
.enabled-check input {
  accent-color: #7151c8;
}
.console-link {
  display: inline-block;
  font-size: 12px;
  color: #6246a0;
  margin-top: 24px;
}
.key-explanation {
  font-size: 11px;
  line-height: 1.6;
  color: #8994a3;
}
.draft-feedback {
  padding: 12px;
  background: #f3effb;
  color: #6246a0;
  font-size: 13px;
  border-radius: 6px;
}
.modal-actions {
  position: sticky;
  bottom: -1px;
  z-index: 2;
  background: #fff;
  padding-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 10px;
  border-top: 1px solid #e3e7ee;
  margin-top: 22px;
  padding-top: 20px;
}
.modal-actions > span {
  flex: 1;
}
.modal-actions > button:not(.connect-button) {
  padding: 11px 15px;
  border: 1px solid #dce1e8;
  border-radius: 6px;
  color: #526173;
  background: white;
  cursor: pointer;
}
input:focus-visible,
button:focus-visible {
  outline: 2px solid #7151c8;
  outline-offset: 2px;
}
@media (max-width: 1100px) {
  .table-head {
    display: none;
  }
  .connection-row {
    grid-template-columns: 1fr 1fr;
  }
  .row-actions {
    justify-content: flex-end;
  }
}
@media (max-width: 600px) {
  .library-banner {
    padding: 20px;
    gap: 12px;
    flex-wrap: wrap;
  }
  .banner-icon {
    width: 42px;
    height: 42px;
  }
  .active-copy h2 {
    font-size: 19px;
  }
  .library-banner > .connect-button {
    width: 100%;
    margin-top: 8px;
  }
  .connection-row {
    grid-template-columns: 1fr;
    padding: 18px;
    gap: 14px;
  }
  .row-actions {
    justify-content: flex-start;
    gap: 20px;
  }
  .modal-grid {
    grid-template-columns: 1fr;
  }
  .modal-grid .full {
    grid-column: auto;
  }
  .modal-actions {
    flex-wrap: wrap;
  }
  .list-heading p {
    line-height: 1.7;
  }
  .row-actions button {
    min-height: 36px;
  }
}
@media (prefers-reduced-motion: reduce) {
  * {
    transition: none !important;
  }
}
</style>
<style>
.model-connection-dialog {
  max-width: calc(100vw - 28px);
  border-radius: 12px;
  padding: 24px !important;
}
.model-connection-dialog .el-dialog__body {
  max-height: 73vh;
  overflow: auto;
}
.model-connection-dialog .el-dialog__title {
  font-size: 22px;
  font-weight: 650;
}
.model-connection-dialog .el-dialog__header {
  padding-bottom: 24px;
}
</style>
