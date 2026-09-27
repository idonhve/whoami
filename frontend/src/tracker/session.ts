/**
 * 访客会话标识（Spec 05）：crypto.randomUUID() 生成，存 sessionStorage。
 * 键名 'whoami:sid' 是跨模块约定（命令面板等也读此键），不得改动。
 * 与 F1 开机动画偏好（bootSession.ts 的 whoami:boot:*）无关，勿混淆。
 */

export const SESSION_KEY = 'whoami:sid'

/** 取当前会话 id；不存在则生成并写入 sessionStorage（同一浏览器标签页内复用） */
export function getSessionId(): string {
  let id: string | null = null
  try {
    id = sessionStorage.getItem(SESSION_KEY)
  } catch {
    // sessionStorage 不可用（隐私模式等）：本次生成临时 id，不持久化
  }
  if (!id) {
    id = crypto.randomUUID()
    try {
      sessionStorage.setItem(SESSION_KEY, id)
    } catch {
      // 持久化失败不影响本次上报
    }
  }
  return id
}
