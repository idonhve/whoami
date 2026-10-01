import { ref } from 'vue'
import { defineStore } from 'pinia'

import { meApi, refreshApi, loginApi, type AdminInfo } from '@/api/auth'
import { TOKEN_KEY } from '@/api/http'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
  const admin = ref<AdminInfo | null>(null)

  async function login(username: string, password: string) {
    const result = await loginApi(username, password)
    token.value = result.token
    localStorage.setItem(TOKEN_KEY, result.token)
  }

  async function refresh() {
    const result = await refreshApi()
    token.value = result.token
    localStorage.setItem(TOKEN_KEY, result.token)
  }

  async function fetchMe() {
    admin.value = await meApi()
  }

  function logout() {
    token.value = null
    admin.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  return { token, admin, login, refresh, fetchMe, logout }
})
