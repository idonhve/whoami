<script setup lang="ts">
import { onMounted, ref } from 'vue'

import {
  createTechItem,
  deleteTechItem,
  fetchAdminTechStack,
  updateTechItem,
  type Proficiency,
  type TechItem,
  type TechItemCreate,
} from '@/api/tech'
import { PROFICIENCY_LABELS } from '@/api/tech'

/**
 * 后台技术栈管理（Spec 02）：新增/编辑/删除技术项。
 * 分类为自由字符串，提供常用值下拉（datalist）允许自定义，不硬编码枚举。
 * 改完保存后刷新 /tech 即见，无需发版。
 */
const COMMON_CATEGORIES = ['前端', '后端', '数据库', '工具', '其他']
const PROFICIENCY_OPTIONS: { value: Proficiency; label: string }[] = [
  { value: 'master', label: '精通' },
  { value: 'proficient', label: '熟练' },
  { value: 'familiar', label: '了解' },
]

const list = ref<TechItem[]>([])
const loading = ref(true)
const errorMsg = ref('')
const notice = ref('')

/**
 * 编辑表单的可变状态：icon 用 string（空 = 无图标），保存时才映射为 string|null。
 */
interface TechForm {
  name: string
  icon: string
  category: string
  proficiency: Proficiency
  weight: number
  sortOrder: number
}

// 编辑表单态：editingId 为空 = 新增；否则为编辑目标 id
const editingId = ref<number | null>(null)
const form = ref<TechForm>({
  name: '',
  icon: '',
  category: '',
  proficiency: 'proficient',
  weight: 5,
  sortOrder: 0,
})
const saving = ref(false)

const emptyForm = (): TechForm => ({
  name: '',
  icon: '',
  category: '',
  proficiency: 'proficient',
  weight: 5,
  sortOrder: 0,
})

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    list.value = await fetchAdminTechStack()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
  }
}

function startCreate() {
  editingId.value = null
  form.value = emptyForm()
  notice.value = ''
  errorMsg.value = ''
}

function startEdit(item: TechItem) {
  editingId.value = item.id
  form.value = {
    name: item.name,
    icon: item.icon ?? '',
    category: item.category,
    proficiency: item.proficiency,
    weight: item.weight,
    sortOrder: item.sortOrder,
  }
  notice.value = ''
  errorMsg.value = ''
}

function cancelEdit() {
  editingId.value = null
}

function validate(): string | null {
  if (!form.value.name.trim()) return '名称不能为空'
  if (form.value.name.trim().length > 50) return '名称不能超过 50 字符'
  if (!form.value.category.trim()) return '分类不能为空'
  if (form.value.category.trim().length > 20) return '分类不能超过 20 字符'
  const weight = Number(form.value.weight)
  if (!Number.isInteger(weight) || weight < 1 || weight > 100) return '权重必须是 1~100 的整数'
  return null
}

async function save() {
  if (saving.value) return
  const problem = validate()
  if (problem) {
    errorMsg.value = problem
    return
  }
  saving.value = true
  errorMsg.value = ''
  const payload: TechItemCreate = {
    name: form.value.name.trim(),
    icon: form.value.icon.trim() || null,
    category: form.value.category.trim(),
    proficiency: form.value.proficiency,
    weight: Number(form.value.weight),
    sortOrder: Number(form.value.sortOrder ?? 0),
  }
  try {
    if (editingId.value === null) {
      await createTechItem(payload)
      notice.value = '[ok] 已新增，前台刷新即见'
    } else {
      await updateTechItem(editingId.value, payload)
      notice.value = `[ok] ID ${editingId.value} 已保存，前台刷新即见`
    }
    editingId.value = null
    await load()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function remove(item: TechItem) {
  if (saving.value) return
  const confirmed = window.confirm(`确认删除技术项「${item.name}」？`)
  if (!confirmed) return
  saving.value = true
  errorMsg.value = ''
  try {
    await deleteTechItem(item.id)
    notice.value = `[ok] 已删除「${item.name}」，前台刷新即见`
    if (editingId.value === item.id) editingId.value = null
    await load()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '删除失败'
  } finally {
    saving.value = false
  }
}

function fmtIcon(icon: string | null) {
  return icon && icon.trim() ? icon : '(无)'
}

function itemLabel(value: Proficiency) {
  return PROFICIENCY_LABELS[value] ?? value
}

onMounted(load)
</script>

<template>
  <section class="tech-page">
    <header class="head">
      <h1 class="title">
        <span class="led" aria-hidden="true"></span>
        TECH STACK
      </h1>
      <p class="sub">$ tech-stack --admin · 增删改即时生效，前台刷新即见，无需发版</p>
    </header>

    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="errorMsg" class="error" role="alert">[error] {{ errorMsg }}</p>

    <form class="form" @submit.prevent="save">
      <div class="form-grid">
        <label class="field">
          <span>名称 *</span>
          <input
            v-model="form.name"
            type="text"
            maxlength="50"
            placeholder="Vue 3"
            spellcheck="false"
          />
        </label>
        <label class="field">
          <span>图标 (devicon 名)</span>
          <input v-model="form.icon" type="text" maxlength="50" placeholder="vuejs" spellcheck="false" />
        </label>
        <label class="field">
          <span>分类 *</span>
          <input v-model="form.category" list="tech-categories" maxlength="20" spellcheck="false" />
          <datalist id="tech-categories">
            <option v-for="c in COMMON_CATEGORIES" :key="c" :value="c" />
          </datalist>
        </label>
        <label class="field">
          <span>熟练度</span>
          <select v-model="form.proficiency">
            <option v-for="opt in PROFICIENCY_OPTIONS" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>权重 (1~100)</span>
          <input v-model.number="form.weight" type="number" min="1" max="100" step="1" />
        </label>
        <label class="field">
          <span>排序</span>
          <input v-model.number="form.sortOrder" type="number" step="1" />
        </label>
      </div>

      <div class="actions">
        <button class="neon-btn" type="submit" :disabled="saving">
          {{ saving ? 'SAVING…' : editingId === null ? '+ ADD' : 'SAVE' }}
        </button>
        <button v-if="editingId !== null" class="ghost-btn" type="button" :disabled="saving" @click="cancelEdit">
          CANCEL
        </button>
        <button class="ghost-btn" type="button" :disabled="saving" @click="startCreate">
          RESET
        </button>
      </div>
    </form>

    <p v-if="loading" class="loading">loading ...</p>

    <table v-else class="term-table">
      <thead>
        <tr>
          <th scope="col">ID</th>
          <th scope="col">名称</th>
          <th scope="col">图标</th>
          <th scope="col">分类</th>
          <th scope="col">熟练度</th>
          <th scope="col">权重</th>
          <th scope="col">排序</th>
          <th scope="col">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in list" :key="item.id">
          <td class="id">{{ item.id }}</td>
          <td class="name">{{ item.name }}</td>
          <td class="dim">{{ fmtIcon(item.icon) }}</td>
          <td class="cat">{{ item.category }}</td>
          <td>
            <span class="prof" :data-proficiency="item.proficiency">
              {{ itemLabel(item.proficiency) }}
            </span>
          </td>
          <td class="dim">{{ item.weight }}</td>
          <td class="dim">{{ item.sortOrder }}</td>
          <td class="ops">
            <button class="op-btn" :disabled="saving" @click="startEdit(item)">编辑</button>
            <button class="op-btn danger" :disabled="saving" @click="remove(item)">删除</button>
          </td>
        </tr>
        <tr v-if="!list.length">
          <td colspan="8" class="empty">暂无技术项，用上方表单新增</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.tech-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  font-family: var(--font-pixel);
  font-size: 16px;
  font-weight: 400;
  letter-spacing: 2px;
  color: var(--green);
  text-shadow: 0 0 10px var(--green-glow);
}

.led {
  width: 10px;
  height: 10px;
  background: var(--green);
  box-shadow: 0 0 10px var(--green-glow);
  animation: led-pulse 2.4s ease-in-out infinite;
}

.sub {
  margin: 0;
  color: var(--text-dim);
  font-family: var(--font-term);
  font-size: 20px;
  letter-spacing: 0.5px;
}

.notice {
  margin: 0;
  color: var(--green);
  font-size: 13px;
}

.error {
  margin: 0;
  color: var(--error);
  font-size: 13px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  background: var(--bg-panel);
  border: 2px solid var(--border);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: var(--text-dim);
}

.field input,
.field select {
  background: var(--bg);
  border: 2px solid var(--border);
  color: var(--text);
  font-family: var(--font-mono);
  font-size: 13px;
  padding: 7px 10px;
  outline: none;
  caret-color: var(--green);
  transition:
    border-color 0.2s,
    box-shadow 0.2s;
}

.field input:focus,
.field select:focus {
  border-color: var(--green);
  box-shadow: 0 0 12px var(--green-soft);
}

.actions {
  display: flex;
  gap: 10px;
}

.neon-btn {
  font-size: 11px;
  padding: 10px 20px;
}

.ghost-btn {
  background: transparent;
  border: 2px solid var(--border-bright);
  color: var(--text);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 12px;
  padding: 10px 16px;
  transition:
    border-color 0.2s,
    color 0.2s;
}

.ghost-btn:hover:not(:disabled) {
  border-color: var(--green);
  color: var(--green);
}

.loading {
  margin: 0;
  color: var(--text-dim);
}

.id {
  color: var(--text-dim);
}

.name {
  color: var(--text);
}

.cat {
  color: var(--cyan);
}

.dim {
  color: var(--text-dim);
}

.prof[data-proficiency='master'] {
  color: var(--green);
}

.prof[data-proficiency='proficient'] {
  color: var(--cyan);
}

.prof[data-proficiency='familiar'] {
  color: var(--amber);
}

.ops {
  white-space: nowrap;
}

.op-btn {
  background: transparent;
  border: 2px solid var(--border-bright);
  color: var(--text);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 12px;
  padding: 4px 12px;
  margin-right: 6px;
  transition:
    border-color 0.2s,
    color 0.2s,
    box-shadow 0.2s;
}

.op-btn:hover:not(:disabled) {
  border-color: var(--green);
  color: var(--green);
  box-shadow: 0 0 10px var(--green-soft);
}

.op-btn.danger:hover:not(:disabled) {
  border-color: var(--error);
  color: var(--error);
  box-shadow: 0 0 10px var(--error-soft);
}

.op-btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.empty {
  color: var(--text-dim);
  text-align: center;
}

@media (max-width: 760px) {
  .form-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 520px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .led {
    animation: none;
  }
  .title {
    text-shadow: none;
  }
}
</style>