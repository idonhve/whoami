import '@fontsource/press-start-2p'
import '@fontsource/vt323'
import { createPinia } from 'pinia'
import { createApp } from 'vue'

import App from './App.vue'
import { installRouteTransition } from './composables/routeTransition'
import router from './router'
import './styles/global.css'

installRouteTransition(router)

createApp(App).use(createPinia()).use(router).mount('#app')
