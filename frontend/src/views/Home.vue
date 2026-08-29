<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const PHRASES = ['终端 / 像素 / 赛博科技', '个人主页系统构建中', 'F1-F12 模块陆续上线']

const typed = ref('')
let timer: number | undefined
let phraseIdx = 0
let charIdx = 0
let deleting = false

function tick() {
  const phrase = PHRASES[phraseIdx]
  if (!deleting) {
    charIdx += 1
    typed.value = phrase.slice(0, charIdx)
    if (charIdx === phrase.length) {
      deleting = true
      timer = window.setTimeout(tick, 2200)
      return
    }
  } else {
    charIdx -= 1
    typed.value = phrase.slice(0, charIdx)
    if (charIdx === 0) {
      deleting = false
      phraseIdx = (phraseIdx + 1) % PHRASES.length
    }
  }
  timer = window.setTimeout(tick, deleting ? 26 : 110)
}

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    typed.value = PHRASES[0]
    return
  }
  tick()
})

onBeforeUnmount(() => {
  if (timer !== undefined) window.clearTimeout(timer)
})
</script>

<template>
  <main class="home">
    <div class="hud-frame"></div>

    <header class="hud-top">
      <span class="hud-item">SYS v1.0.0 // M1</span>
      <span class="hud-item">zh-CN · UTF-8</span>
    </header>

    <section class="hero">
      <p class="prompt">$ whoami --init</p>
      <h1 class="logo" data-text="whoami">whoami</h1>
      <p class="typed-line">
        <span>{{ typed }}</span><span class="cursor" aria-hidden="true">▌</span>
      </p>

      <div class="boot-panel">
        <p class="boot-line ok">[ OK ] auth / jwt .............. ONLINE</p>
        <p class="boot-line ok">[ OK ] db / mysql-8 ............ ONLINE</p>
        <p class="boot-line wait">[WAIT] homepage / spec-01 ...... PENDING</p>
      </div>

      <RouterLink class="neon-btn" to="/admin/login">ENTER ADMIN &gt;</RouterLink>
    </section>

    <footer class="hud-bottom">
      <span class="hud-item">© 2026 whoami</span>
      <span class="hud-item">EST. 2026 // BUILDING</span>
    </footer>
  </main>
</template>

<style scoped>
.home {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(ellipse 55% 38% at 50% 42%, rgba(0, 255, 156, 0.07), transparent 70%),
    radial-gradient(circle, rgba(43, 217, 255, 0.05) 1px, transparent 1px);
  background-size:
    100% 100%,
    30px 30px;
}

.hud-top,
.hud-bottom {
  position: absolute;
  left: 34px;
  right: 34px;
  display: flex;
  justify-content: space-between;
  color: var(--text-dim);
  font-size: 11px;
  letter-spacing: 1px;
}

.hud-top {
  top: 26px;
}

.hud-bottom {
  bottom: 26px;
}

.hero {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 26px;
  padding: 80px 20px;
  text-align: center;
}

.prompt {
  margin: 0;
  color: var(--text-dim);
  font-family: var(--font-term);
  font-size: 22px;
  animation: rise-in 0.5s ease-out both;
}

.logo {
  position: relative;
  margin: 0;
  font-family: var(--font-pixel);
  font-size: clamp(30px, 6.5vw, 58px);
  font-weight: 400;
  letter-spacing: 2px;
  color: var(--green);
  text-shadow:
    0 0 10px var(--green-glow),
    0 0 36px rgba(0, 255, 156, 0.18);
  animation: rise-in 0.5s ease-out 0.08s both;
}

/* 故障艺术：双层错位剪影，平时隐藏，周期性闪现 */
.logo::before,
.logo::after {
  content: attr(data-text);
  position: absolute;
  inset: 0;
  opacity: 0;
  pointer-events: none;
}

.logo::before {
  color: var(--cyan);
  animation: glitch-a 6s steps(1) infinite;
}

.logo::after {
  color: var(--magenta);
  animation: glitch-b 6s steps(1) infinite;
}

.logo:hover {
  animation: logo-jitter 0.28s steps(2) infinite;
}

@keyframes logo-jitter {
  50% {
    text-shadow:
      3px 0 var(--magenta),
      -3px 0 var(--cyan),
      0 0 10px var(--green-glow);
  }
}

@keyframes glitch-a {
  0%,
  90% {
    opacity: 0;
  }
  90.5% {
    opacity: 0.85;
    clip-path: inset(14% 0 72% 0);
    transform: translateX(-5px);
  }
  91.5% {
    clip-path: inset(62% 0 22% 0);
    transform: translateX(4px);
  }
  92.5% {
    opacity: 0;
  }
}

@keyframes glitch-b {
  0%,
  90% {
    opacity: 0;
  }
  90.5% {
    opacity: 0.85;
    clip-path: inset(58% 0 26% 0);
    transform: translateX(5px);
  }
  91.5% {
    clip-path: inset(8% 0 80% 0);
    transform: translateX(-4px);
  }
  92.5% {
    opacity: 0;
  }
}

.typed-line {
  margin: 0;
  min-height: 22px;
  color: var(--text);
  font-family: var(--font-term);
  font-size: 22px;
  animation: rise-in 0.5s ease-out 0.16s both;
}

.cursor {
  display: inline-block;
  color: var(--green);
  animation: cursor-blink 1s steps(1) infinite;
}

.boot-panel {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 14px 20px;
  border-left: 2px solid var(--border-bright);
  background: rgba(9, 15, 22, 0.6);
  text-align: left;
  animation: rise-in 0.5s ease-out 0.24s both;
}

.boot-line {
  margin: 0;
  font-family: var(--font-term);
  font-size: 19px;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.boot-line.ok {
  color: var(--text-dim);
}

.boot-line.wait {
  color: var(--amber);
}

.hero > .neon-btn {
  animation: rise-in 0.5s ease-out 0.32s both;
}

@media (max-width: 560px) {
  .hud-top,
  .hud-bottom {
    left: 24px;
    right: 24px;
  }

  .boot-panel {
    max-width: 100%;
    overflow-x: auto;
  }
}
</style>
