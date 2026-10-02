<script setup lang="ts">
import { computed } from 'vue'

import { apiUrl } from '@/api/base'

import { iconPath } from './iconPaths'

/**
 * 技术图标：用 devicon 图标名渲染为内联 SVG（不用 emoji）。
 * 已知图标名走 iconPaths 映射；未知名渲染中性回退占位（首字符）。
 */
const props = withDefaults(
  defineProps<{ icon?: string | null; iconUrl?: string | null; name?: string | null; size?: number }>(),
  { size: 30 },
)

const path = computed(() => iconPath(props.icon))
const deviconNames = new Set([
  'apachekafka-original', 'axios-plain', 'bootstrap-original', 'css3-original',
  'docker-original', 'elasticsearch-original', 'git-original', 'github-original',
  'gitlab-original', 'gradle-original', 'groovy-original', 'hibernate-original',
  'html5-original', 'intellij-original', 'java-original', 'jenkins-original',
  'jquery-original', 'junit-original', 'kotlin-original', 'kubernetes-plain',
  'linux-original', 'mariadb-original', 'maven-original', 'mongodb-original',
  'mysql-original', 'nginx-original', 'nodejs-original', 'oracle-original',
  'postman-original', 'postgresql-original', 'prometheus-original',
  'quarkus-original', 'rabbitmq-original', 'react-original', 'redis-original',
  'sonarqube-original', 'spring-original', 'sqlite-original', 'swagger-original',
  'tailwindcss-original', 'thymeleaf-original', 'tomcat-original',
  'typescript-original', 'javascript-original', 'vitejs-original', 'vuejs-original',
  'vscode-original',
])
const deviconAliases: Record<string, string> = {
  springboot: 'spring-original',
  springmvc: 'spring-original',
  springsecurity: 'spring-original',
  springcloud: 'spring-original',
  juc: 'java-original',
  jvm: 'java-original',
}
const deviconSrc = computed(() => {
  const raw = props.icon?.trim().toLowerCase()
  if (!raw) return null
  const key = deviconAliases[raw] ?? raw
  if (!deviconNames.has(key)) return null
  const base = import.meta.env.BASE_URL.replace(/\/+$/, '')
  return `${base}/tech-icons/${key}.svg`
})
const imageSrc = computed(() => (props.iconUrl ? apiUrl(props.iconUrl) : null))
const fallback = computed(() => {
  const name = props.icon?.trim()
  return name ? name[0].toUpperCase() : '?' // ASCII，像素感占位
})
const label = computed(() => props.name?.trim() || props.icon?.trim() || 'tech')
</script>

<template>
  <img
    v-if="imageSrc"
    class="tech-icon-image"
    :src="imageSrc"
    :width="size"
    :height="size"
    :alt="label"
    loading="lazy"
    data-testid="tech-icon-image"
  />
  <img
    v-else-if="deviconSrc"
    class="tech-icon-image devicon-image"
    :src="deviconSrc"
    :width="size"
    :height="size"
    :alt="label"
    loading="lazy"
    data-testid="tech-icon-devicon"
  />
  <svg
    v-else
    class="tech-icon"
    :width="size"
    :height="size"
    :viewBox="path ? '0 0 24 24' : '0 0 28 28'"
    role="img"
    :aria-label="label"
    data-testid="tech-icon"
  >
    <path v-if="path" :d="path" fill="currentColor" />
    <g v-else class="tech-icon--fallback" :data-testid="`fallback-${fallback}`">
      <rect x="1.5" y="5" width="25" height="18" rx="2" />
      <text
        x="14"
        y="19"
        text-anchor="middle"
        font-size="14"
        font-family="var(--font-pixel), monospace"
        fill="currentColor"
      >
        {{ fallback }}
      </text>
    </g>
  </svg>
</template>

<style scoped>
.tech-icon-image {
  display: inline-block;
  flex: none;
  object-fit: contain;
}

.tech-icon {
  display: inline-block;
  flex: none;
  filter: drop-shadow(0 0 6px var(--green-glow));
  color: var(--green);
  transition:
    color 0.2s,
    filter 0.2s;
}

.tech-icon--fallback {
  stroke: var(--border-bright);
  stroke-width: 1.5;
  color: var(--text-dim);
}
</style>
