import { createApp } from 'vue'
import { ElDialog, ElMessage } from 'element-plus'
import 'element-plus/es/components/dialog/style/css'
import 'element-plus/es/components/message/style/css'
import './assets/backoffice.css'
import './assets/platform.css'
import './assets/reference-layout.css'
import './assets/responsive.css'

import App from './App.vue'
import router from './router'
import { pinia } from './stores'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.component('ElDialog', ElDialog)
app.config.globalProperties.$message = ElMessage
app.mount('#app')
