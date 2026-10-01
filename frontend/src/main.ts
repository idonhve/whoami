import '@fontsource/press-start-2p'
import '@fontsource/vt323'
import { createPinia } from 'pinia'
import { createApp } from 'vue'

import App from './App.vue'
import { installRouteTransition } from './composables/routeTransition'
import router from './router'
import { installTracker } from './tracker'
import './styles/global.css'

installRouteTransition(router)
// 访客埋点 SDK（Spec 05，全站横切件）：会话进入/离开 + 路由 page_view 自动上报
installTracker(router)

createApp(App).use(createPinia()).use(router).mount('#app')
