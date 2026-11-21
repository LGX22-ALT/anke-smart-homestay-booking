<template>
  <footer class="site-footer">
    <div class="footer-inner">
      <div>
        <strong>民宿预约系统</strong>
        <p>酒店与民宿预订 · 城市旅行指南</p>
      </div>
      <nav aria-label="页脚导航">
        <RouterLink to="/search">查找住宿</RouterLink
        ><RouterLink to="/attractions">探索景点</RouterLink
        ><RouterLink to="/help">预订帮助</RouterLink><RouterLink to="/login">账户登录</RouterLink>
      </nav>
      <p class="copyright">© 2026 Hotel Out</p>
    </div>
  </footer>
</template>
<style scoped>
.site-footer {
  margin-top: 32px;
  padding: 32px 24px;
  background: white;
  border-top: 1px solid var(--line);
  color: var(--muted);
}
.footer-inner {
  display: flex;
  flex-wrap: wrap;
  gap: 24px 48px;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: auto;
}
strong {
  font-size: 16px;
  color: var(--ink);
}
p {
  margin: 6px 0 0;
  font-size: 12px;
}
nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
}
nav a {
  display: inline-flex;
  align-items: center;
  min-height: 44px;
  color: var(--muted);
  text-decoration: none;
  font-size: 13px;
}
nav a:hover {
  color: var(--brand);
  text-decoration: underline;
}
@media (max-width: 650px) {
  .footer-inner {
    align-items: start;
    flex-direction: column;
    gap: 16px;
  }
}
</style>
