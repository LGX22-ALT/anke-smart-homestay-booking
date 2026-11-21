<script setup>
import ProfileEditor from '@/components/business/ProfileEditor.vue'
</script>
<template>
  <section class="merchant-profile">
    <header>
      <h1>个人资料</h1>
      <p>设置头像和个人名称，方便住客认识你。</p>
    </header>
    <ProfileEditor />
  </section>
</template>
<style scoped>
.merchant-profile {
  max-width: 1000px;
  margin-inline: auto;
  padding: 32px;
  background: white;
  border: 1px solid var(--line);
  border-radius: 10px;
}
header {
  margin-bottom: 28px;
}
h1 {
  margin: 0 0 8px;
  font-size: 26px;
}
header p {
  margin: 0;
  color: var(--muted);
  font-size: 14px;
  line-height: 1.7;
}
@media (max-width: 600px) {
  .merchant-profile {
    padding: 20px 16px;
  }
}
</style>
