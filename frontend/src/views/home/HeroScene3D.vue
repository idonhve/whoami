<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as THREE from 'three'

import { cssVar } from '@/utils/cssVar'
import { prefersReducedMotion } from '@/utils/motion'

/**
 * 首页 Hero 的 3D 锚点（本页唯一重动效）：
 * 线框二十面体 + 粒子点云，霓虹绿加法混合。
 * 由 HeroSection 以异步组件懒加载，降级模式下不加载本组件。
 * reduced-motion：渲染单帧静态画面，不开动画循环。
 */

const container = ref<HTMLDivElement | null>(null)

let renderer: THREE.WebGLRenderer | null = null
let raf = 0
let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  const el = container.value
  if (!el) return

  const scene = new THREE.Scene()
  const camera = new THREE.PerspectiveCamera(55, 1, 0.1, 100)
  camera.position.set(0, 0, 6)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setClearColor(0x000000, 0)
  el.appendChild(renderer.domElement)

  // 颜色取自设计 token（WebGL 无法直接引用 CSS 变量）
  const colorGreen = new THREE.Color(cssVar('--green', '#00ff9c'))
  const colorCyan = new THREE.Color(cssVar('--cyan', '#2bd9ff'))

  // 线框二十面体
  const geo = new THREE.IcosahedronGeometry(2.1, 1)
  const wire = new THREE.LineSegments(
    new THREE.WireframeGeometry(geo),
    new THREE.LineBasicMaterial({ color: colorGreen, transparent: true, opacity: 0.55 }),
  )
  scene.add(wire)

  // 粒子点云
  const dotCount = 420
  const positions = new Float32Array(dotCount * 3)
  for (let i = 0; i < dotCount; i++) {
    const r = 2.6 + Math.random() * 1.8
    const theta = Math.random() * Math.PI * 2
    const phi = Math.acos(2 * Math.random() - 1)
    positions[i * 3] = r * Math.sin(phi) * Math.cos(theta)
    positions[i * 3 + 1] = r * Math.sin(phi) * Math.sin(theta)
    positions[i * 3 + 2] = r * Math.cos(phi)
  }
  const dotGeo = new THREE.BufferGeometry()
  dotGeo.setAttribute('position', new THREE.BufferAttribute(positions, 3))
  const dots = new THREE.Points(
    dotGeo,
    new THREE.PointsMaterial({
      color: colorCyan,
      size: 0.045,
      transparent: true,
      opacity: 0.8,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    }),
  )
  scene.add(dots)

  const resize = () => {
    if (!renderer || !el) return
    const w = el.clientWidth
    const h = el.clientHeight
    if (w === 0 || h === 0) return
    renderer.setSize(w, h)
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2))
    camera.aspect = w / h
    camera.updateProjectionMatrix()
  }
  resize()
  resizeObserver = new ResizeObserver(resize)
  resizeObserver.observe(el)

  if (prefersReducedMotion()) {
    // 静态单帧
    wire.rotation.set(0.4, 0.6, 0)
    resize()
    renderer.render(scene, camera)
    return
  }

  const animate = () => {
    raf = requestAnimationFrame(animate)
    wire.rotation.y += 0.0028
    wire.rotation.x += 0.0011
    dots.rotation.y -= 0.0016
    renderer?.render(scene, camera)
  }
  animate()
})

onBeforeUnmount(() => {
  if (raf) cancelAnimationFrame(raf)
  resizeObserver?.disconnect()
  renderer?.dispose()
  renderer?.domElement.remove()
  renderer = null
})
</script>

<template>
  <div ref="container" class="hero-scene" aria-hidden="true"></div>
</template>

<style scoped>
.hero-scene {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.hero-scene :deep(canvas) {
  display: block;
}
</style>
