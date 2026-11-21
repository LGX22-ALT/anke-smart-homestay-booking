<template>
  <div class="app-shell">
    <RouterView />
  </div>
</template>

<style>
:root {
  color: var(--ink);
  background: #f5f7fa;
  font-family: var(--font-ui);
  font-synthesis: none;
  text-rendering: optimizeLegibility;
}

* {
  box-sizing: border-box;
}

html,
body,
#app {
  min-width: 320px;
  min-height: 100%;
  margin: 0;
}

body {
  min-height: 100vh;
}

button,
input {
  font: inherit;
}

.app-shell {
  min-height: 100vh;
}
</style>
