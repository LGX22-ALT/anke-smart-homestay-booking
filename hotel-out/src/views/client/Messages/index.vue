<script setup>
import NavHeader from '@/components/common/NavHeader.vue'
import MessageInbox from '@/components/business/MessageInbox.vue'
</script>

<template>
  <div class="messages-page">
    <NavHeader />
    <main class="messages-main">
      <header class="messages-heading">
        <h1>消息中心</h1>
        <span>入住咨询与商家回复</span>
      </header>
      <MessageInbox />
    </main>
  </div>
</template>

<style scoped>
.messages-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 24px 32px;
}
.messages-heading {
  display: flex;
  align-items: baseline;
  gap: 16px;
  margin-bottom: 20px;
}
.messages-heading h1 {
  margin: 0;
  font-size: 26px;
  color: var(--ink);
}
.messages-heading span {
  font-size: 13px;
  color: var(--muted);
}
@media (max-width: 768px) {
  .messages-main {
    padding: 16px 12px;
  }
  .messages-heading {
    margin-bottom: 12px;
  }
  .messages-heading h1 {
    font-size: 22px;
  }
  .messages-heading span {
    font-size: 12px;
  }
}
</style>
