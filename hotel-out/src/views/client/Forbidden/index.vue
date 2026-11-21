<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const sourcePath = computed(() => route.query.from || '受保护页面')
</script>

<template>
  <main class="status-page">
    <section class="status-card" aria-labelledby="forbidden-title">
      <p class="eyebrow">ACCESS CONTROL</p>
      <h1 id="forbidden-title">暂无访问权限</h1>
      <p class="description">
        当前账号无法访问
        <code>{{ sourcePath }}</code>
        。如需继续，请切换到具备对应角色的账号。
      </p>
      <RouterLink class="primary-link" to="/"> 返回首页 </RouterLink>
    </section>
  </main>
</template>

<style scoped>
.status-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  padding: 24px;
  background: #f5f7fa;
}

.status-card {
  width: min(100%, 560px);
  padding: clamp(32px, 7vw, 64px);
  border: 1px solid var(--line);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 24px 70px rgb(16 42 67 / 10%);
}

.eyebrow {
  margin: 0 0 16px;
  color: #d64545;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

h1 {
  margin: 0;
  color: var(--ink);
  font-size: clamp(36px, 7vw, 56px);
  line-height: 1.1;
}

.description {
  margin: 24px 0 32px;
  color: #52606d;
  line-height: 1.8;
}

code {
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--ink);
  background: #fcebea;
  word-break: break-all;
}

.primary-link {
  display: inline-flex;
  align-items: center;
  min-height: 42px;
  padding: 0 18px;
  border-radius: 4px;
  color: #fff;
  background: var(--ink);
  text-decoration: none;
}

.primary-link:focus-visible,
.primary-link:hover {
  background: #243b53;
}
</style>
