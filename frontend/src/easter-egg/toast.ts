/**
 * F11 秘籍触发的终端风短提示（Spec 11）：`> konami ✓`。
 * 用全局 CSS 变量 token 自绘（首次调用注入一个 style 标签），不引组件库；
 * 纯装饰层：aria-hidden、pointer-events:none，不抢焦点、不改变 DOM 语义。
 */

const TOAST_CLASS = 'konami-toast'
const TOAST_STYLE_ID = 'konami-toast-style'
/** 提示停留时长（reduced-motion 下动画降级为直出直隐，停留时长不变） */
const TOAST_MS = 2400
/** 渐出动画时长（与样式表 keyframe 一致） */
const TOAST_OUT_MS = 320

function ensureToastStyle(): void {
  if (document.getElementById(TOAST_STYLE_ID)) return
  const style = document.createElement('style')
  style.id = TOAST_STYLE_ID
  style.textContent = `
.konami-toast {
  position: fixed;
  left: 50%;
  bottom: 48px;
  transform: translateX(-50%);
  /* 位于全局 CRT 质感层(9999)之下的瞬时全局层 */
  z-index: 9998;
  padding: 10px 18px;
  border: 2px solid var(--green);
  background: var(--bg-raised);
  color: var(--green);
  font-family: var(--font-term);
  font-size: 22px;
  letter-spacing: 1px;
  text-shadow: 0 0 8px var(--green-glow);
  box-shadow: 0 0 18px var(--green-glow), inset 0 0 12px var(--green-soft);
  pointer-events: none;
  animation: konami-toast-in 0.18s steps(2) both;
}
.konami-toast.is-out {
  animation: konami-toast-out ${TOAST_OUT_MS}ms steps(3) both;
}
@keyframes konami-toast-in {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
}
@keyframes konami-toast-out {
  from {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
  }
  to {
    opacity: 0;
    transform: translateX(-50%) translateY(10px);
  }
}
@media (prefers-reduced-motion: reduce) {
  .konami-toast,
  .konami-toast.is-out {
    animation: none;
  }
}`
  document.head.appendChild(style)
}

function removeToasts(): void {
  document.querySelectorAll(`.${TOAST_CLASS}`).forEach((el) => el.remove())
}

/** 显示终端风短提示；重复触发时替换旧提示，不叠加（秘籍可重复玩） */
export function showKonamiToast(text = '> konami ✓'): void {
  ensureToastStyle()
  removeToasts()
  const el = document.createElement('div')
  el.className = TOAST_CLASS
  el.setAttribute('aria-hidden', 'true')
  el.textContent = text
  document.body.appendChild(el)
  window.setTimeout(() => {
    el.classList.add('is-out')
    window.setTimeout(() => el.remove(), TOAST_OUT_MS)
  }, TOAST_MS)
}
