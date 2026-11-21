export const AVATAR_MAX_BYTES = 5 * 1024 * 1024

export function validateProfileName(name) {
  if (!String(name || '').trim()) return '请输入个人名称'
  if (String(name).trim().length > 64) return '个人名称不能超过 64 个字符'
  return ''
}

export function validateAvatarFile(file) {
  if (!file || file.size === 0) return '图片文件为空，请重新选择'
  if (!['image/jpeg', 'image/png'].includes(file.type)) return '请选择 JPG 或 PNG 图片'
  if (file.size > AVATAR_MAX_BYTES) return '头像不能超过 5MB，请选择较小的图片'
  return ''
}
