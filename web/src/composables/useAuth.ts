import { ref } from 'vue'

/**
 * 访问认证：探测会话、密码登录、登出。
 * 会话以 HttpOnly cookie 维护，fetch 同源自动携带。
 */
export function useAuth() {
  const authenticated = ref(false)
  const checking = ref(true)

  /** 探测当前会话是否有效（页面加载时调用） */
  async function check(): Promise<void> {
    try {
      const res = await fetch('/api/auth/me')
      authenticated.value = res.ok
    } catch {
      authenticated.value = false
    } finally {
      checking.value = false
    }
  }

  /** 密码登录；成功返回 true */
  async function login(password: string): Promise<boolean> {
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ password }),
      })
      if (res.ok) {
        authenticated.value = true
        return true
      }
      return false
    } catch {
      return false
    }
  }

  /** 登出 */
  async function logout(): Promise<void> {
    try {
      await fetch('/api/auth/logout', { method: 'POST' })
    } finally {
      authenticated.value = false
    }
  }

  return { authenticated, checking, check, login, logout }
}
