<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import UserAvatar from '@/components/common/UserAvatar.vue'
import { DEFAULT_AVATARS } from '@/constants/avatars'
import { getProfile, saveProfile, uploadAvatar } from '@/services/dataSource'
import { useUserStore } from '@/stores'
import { getSessionIdentity } from '@/utils/auth'
import { validateAvatarFile, validateProfileName } from '@/utils/profileValidation'

const props = defineProps({ allowPhone: { type: Boolean, default: false } })
const userStore = useUserStore()
const identity = getSessionIdentity()
const form = reactive({ name: '', avatar: '', phone: '' })
const loading = ref(true)
const loadError = ref('')
const saving = ref(false)
const error = ref('')
const savedMessage = ref('')
const pendingFile = ref(null)
const uploadedUrl = ref('')
const fileInput = ref(null)
const localPreview = ref('')
const preview = computed(() => localPreview.value || form.avatar)
let disposed = false
const isCurrent = () => !disposed && identity === getSessionIdentity()

function releasePreview() {
  if (localPreview.value) URL.revokeObjectURL(localPreview.value)
  localPreview.value = ''
}
function chooseDefault(avatar) {
  releasePreview()
  pendingFile.value = null
  uploadedUrl.value = ''
  form.avatar = avatar.url
  error.value = ''
  savedMessage.value = ''
}
function chooseFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  error.value = validateAvatarFile(file)
  savedMessage.value = ''
  if (error.value) return
  releasePreview()
  pendingFile.value = file
  uploadedUrl.value = ''
  localPreview.value = URL.createObjectURL(file)
}
async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const profile = (await getProfile()) || userStore.userInfo
    if (!isCurrent()) return
    userStore.updateProfileInfo(profile, identity)
    Object.assign(form, {
      name: profile.name || '',
      avatar: profile.avatar || '',
      phone: profile.phone || '',
    })
  } catch (cause) {
    if (isCurrent()) loadError.value = cause.message || '个人资料加载失败，请重试'
  } finally {
    if (isCurrent()) loading.value = false
  }
}
async function submit() {
  if (saving.value || loading.value || !isCurrent()) return
  error.value = validateProfileName(form.name)
  if (!error.value && props.allowPhone && !/^1[3-9]\d{9}$/.test(form.phone.trim()))
    error.value = '请输入正确的 11 位手机号'
  savedMessage.value = ''
  if (error.value) return
  saving.value = true
  try {
    let avatar = form.avatar
    if (pendingFile.value) {
      if (!uploadedUrl.value) uploadedUrl.value = (await uploadAvatar(pendingFile.value)).url
      if (!isCurrent()) return
      avatar = uploadedUrl.value
    }
    const data = { name: form.name.trim(), avatar: avatar || DEFAULT_AVATARS[0].url }
    if (props.allowPhone) data.phone = form.phone.trim()
    const saved = await saveProfile(data)
    if (!isCurrent()) return
    userStore.updateProfileInfo(saved, identity)
    Object.assign(form, {
      name: saved.name,
      avatar: saved.avatar,
      phone: saved.phone || form.phone,
    })
    pendingFile.value = null
    uploadedUrl.value = ''
    releasePreview()
    savedMessage.value = '个人资料已保存'
  } catch (cause) {
    if (isCurrent()) error.value = cause.message || '保存失败，请重试；已填写的内容会保留'
  } finally {
    if (isCurrent()) saving.value = false
  }
}
onMounted(load)
onBeforeUnmount(() => {
  disposed = true
  releasePreview()
})
</script>

<template>
  <div class="identity-editor" :aria-busy="loading || saving">
    <p v-if="loading" class="identity-note" role="status">正在加载个人资料…</p>
    <div v-else-if="loadError" class="identity-load-error" role="alert">
      <p>{{ loadError }}</p>
      <button type="button" @click="load">重新加载</button>
    </div>
    <form v-else @submit.prevent="submit">
      <fieldset :disabled="saving">
        <legend class="visually-hidden">编辑个人资料</legend>
        <div class="avatar-edit-row">
          <UserAvatar class="avatar-preview" :src="preview" :name="form.name" label="头像预览" />
          <div>
            <strong>个人头像</strong>
            <p class="identity-note">JPG、PNG，最大 5MB；图片会居中裁切为正方形。</p>
            <input
              ref="fileInput"
              class="visually-hidden"
              type="file"
              accept="image/jpeg,image/png"
              aria-label="上传头像文件"
              tabindex="-1"
              @change="chooseFile"
            />
            <button class="avatar-upload" type="button" @click="fileInput.click()">
              <Upload />上传头像
            </button>
          </div>
        </div>
        <div class="default-avatar-section">
          <h2>默认头像 <span>选择一款，快速设置</span></h2>
          <div class="default-avatar-list" role="group" aria-label="选择默认头像">
            <button
              v-for="avatar in DEFAULT_AVATARS"
              :key="avatar.url"
              type="button"
              :aria-label="`使用${avatar.name}头像`"
              :aria-pressed="!pendingFile && form.avatar === avatar.url"
              @click="chooseDefault(avatar)"
            >
              <UserAvatar :src="avatar.url" :name="avatar.name" /><span>{{ avatar.name }}</span>
            </button>
          </div>
        </div>
        <div class="identity-fields">
          <label for="profile-name">个人名称</label>
          <div class="name-field">
            <input
              id="profile-name"
              v-model="form.name"
              autocomplete="nickname"
              maxlength="64"
              required
              aria-describedby="profile-name-hint"
              @input="savedMessage = ''"
            /><span>{{ form.name.length }}/64</span>
          </div>
          <p id="profile-name-hint" class="identity-note">
            用于页面上的个人展示，支持中文、字母和数字。
          </p>
          <template v-if="allowPhone">
            <label for="profile-phone">手机号</label>
            <input
              id="profile-phone"
              v-model="form.phone"
              type="tel"
              autocomplete="tel"
              maxlength="11"
              required
              @input="savedMessage = ''"
            />
            <p class="identity-note">
              实名认证：{{ userStore.userInfo?.verified ? '已认证' : '未认证' }}
            </p>
          </template>
        </div>
        <p v-if="pendingFile" class="identity-note">已选择 {{ pendingFile.name }}，保存后生效。</p>
        <p v-if="error" class="identity-error" role="alert">{{ error }}</p>
        <p v-if="savedMessage" class="identity-success" role="status">{{ savedMessage }}</p>
        <div class="identity-actions">
          <button type="submit" :disabled="saving">{{ saving ? '正在保存…' : '保存修改' }}</button
          ><span class="identity-note">头像和名称将在保存后更新</span>
        </div>
      </fieldset>
    </form>
  </div>
</template>

<style scoped>
.identity-editor {
  max-width: 760px;
  color: var(--ink);
}
fieldset {
  margin: 0;
  padding: 0;
  min-width: 0;
  border: 0;
}
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
}
.avatar-edit-row {
  display: flex;
  gap: 24px;
  align-items: center;
  padding: 8px 0 28px;
}
.avatar-preview {
  width: 88px;
  height: 88px;
}
.avatar-edit-row strong {
  font-size: 16px;
}
.identity-note {
  margin: 8px 0;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.7;
  overflow-wrap: anywhere;
}
button {
  cursor: pointer;
  font: inherit;
}
.avatar-upload,
.identity-load-error button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 44px;
  padding: 8px 16px;
  border: 1px solid var(--line);
  border-radius: 6px;
  color: var(--ink);
  background: white;
}
.avatar-upload svg {
  width: 17px;
  height: 17px;
}
.default-avatar-section {
  padding: 24px 0;
  border-block: 1px solid var(--line);
}
.default-avatar-section h2 {
  margin: 0 0 16px;
  font-size: 15px;
}
.default-avatar-section h2 span {
  margin-left: 10px;
  color: var(--muted);
  font-size: 13px;
  font-weight: 400;
}
.default-avatar-list {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.default-avatar-list button {
  display: grid;
  gap: 7px;
  justify-items: center;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--muted);
  font-size: 12px;
}
.default-avatar-list .account-avatar {
  width: 48px;
  height: 48px;
}
.default-avatar-list button[aria-pressed='true'] {
  border-color: var(--brand);
  color: var(--brand);
  background: var(--brand-soft);
}
.default-avatar-list button:hover {
  background: var(--brand-soft);
}
.identity-fields {
  max-width: 440px;
  padding-top: 28px;
}
.identity-fields label {
  display: block;
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
}
.identity-fields input {
  width: 100%;
  min-width: 0;
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid var(--line);
  border-radius: 6px;
  color: var(--ink);
  background: white;
  font: inherit;
}
.name-field {
  position: relative;
}
.name-field input {
  padding-right: 62px;
}
.name-field span {
  position: absolute;
  right: 12px;
  top: 14px;
  color: var(--muted);
  font-size: 12px;
}
.identity-fields .identity-note {
  margin-bottom: 22px;
}
.identity-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  margin-top: 28px;
}
.identity-actions button {
  min-height: 44px;
  padding: 10px 28px;
  border: 0;
  border-radius: 6px;
  background: var(--brand);
  color: white;
}
.identity-actions button:hover:not(:disabled) {
  background: var(--brand-hover);
}
button:focus-visible,
input:focus-visible {
  outline: 2px solid var(--brand);
  outline-offset: 3px;
}
button:disabled {
  opacity: 0.6;
  cursor: wait;
}
.identity-error,
.identity-load-error {
  color: #b42318;
  font-size: 14px;
}
.identity-success {
  color: #167451;
  font-size: 14px;
}
@media (max-width: 600px) {
  .avatar-edit-row {
    gap: 16px;
    align-items: flex-start;
  }
  .avatar-preview {
    width: 64px;
    height: 64px;
  }
  .default-avatar-list {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 8px;
  }
  .identity-actions {
    align-items: flex-start;
    flex-direction: column;
    gap: 0;
  }
}
</style>
