<script setup>
import MessageInbox from '@/components/business/MessageInbox.vue'
</script>

<template>
  <div class="bo-page message-page">
    <header class="bo-page-head">
      <div>
        <h1>住客私信</h1>
        <p>查看入住咨询，直接回复住客。</p>
      </div>
    </header>
    <MessageInbox merchant />
  </div>
</template>
