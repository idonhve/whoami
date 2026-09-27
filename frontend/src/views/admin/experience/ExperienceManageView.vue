<script setup lang="ts">
import { onMounted, ref } from 'vue'

import {
  createExperience,
  deleteExperience,
  fetchAdminExperiences,
  updateExperience,
  type Experience,
  type ExperienceCreate,
} from '@/api/experience'
import { validateRadar } from '@/components/experience/experienceFormat'

/**
 * 后台经历管理（Spec 09）：经历卡 CRUD（公司/职位/时间/战果数组/雷达维度数组/技术标签/展开要点/排序）。
 * 雷达维度可自定义 3~8 个，校验维度名 ≤20 不重复、score 0~100 整数；
 * techTags / highlights 以换行分隔输入，保存时拆成数组。
 */
interface AchievementForm {
  value: string
  context: string
}
interface RadarForm {
  dimension: string
  score: number
}
interface ExForm {
  company: string
  title: string
  startDate: string
  endDate: string
  sortOrder: number
  achievements: AchievementForm[]
  radar: RadarForm[]
  techTagsText: string
  highlightsText: string
}

const list = ref<Experience[]>([])
const loading = ref(true)
const errorMsg = ref('')
const notice = ref('')

const editingId = ref<number | null>(null)
const saving = ref(false)

const emptyForm = (): ExForm => ({
  company: '',
  title: '',
  startDate: '',
  endDate: '',
  sortOrder: 0,
  achievements: [],
  radar: [],
  techTagsText: '',
  highlightsText: '',
})
const form = ref<ExForm>(emptyForm())

function splitLines(text: string): string[] {
  return text
    .split(/\r?\n/)
    .map((s) => s.trim())
    .filter(Boolean)
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    list.value = await fetchAdminExperiences()
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

function startEdit(item: Experience) {
  editingId.value = item.id
  form.value = {
    company: item.company,
    title: item.title,
    startDate: item.startDate ?? '',
    endDate: item.endDate ?? '',
    sortOrder: item.sortOrder ?? 0,
    achievements: item.achievements.map((a) => ({ value: a.value, context: a.context ?? '' })),
    radar: item.radar.map((r) => ({ dimension: r.dimension, score: Number(r.score) })),
    techTagsText: (item.techTags ?? []).join('\n'),
    highlightsText: (item.highlights ?? []).join('\n'),
  }
  notice.value = ''
  errorMsg.value = ''
}

function cancelEdit() {
  editingId.value = null
}

function addAchievement() {
  if (form.value.achievements.length >= 6) {
    errorMsg.value = '战果最多 6 条'
    return
  }
  form.value.achievements.push({ value: '', context: '' })
}
function removeAchievement(index: number) {
  form.value.achievements.splice(index, 1)
}

function addRadar() {
  if (form.value.radar.length >= 8) {
    errorMsg.value = '雷达维度最多 8 个'
    return
  }
  form.value.radar.push({ dimension: '', score: 50 })
}
function removeRadar(index: number) {
  form.value.radar.splice(index, 1)
}

function validate(): string | null {
  const f = form.value
  if (!f.company.trim()) return '公司不能为空'
  if (f.company.trim().length > 50) return '公司不能超过 50 字符'
  if (!f.title.trim()) return '职位不能为空'
  if (f.title.trim().length > 50) return '职位不能超过 50 字符'
  if (!f.startDate) return '入职时间必填'
  if (f.endDate && f.endDate < f.startDate) return '结束时间不能早于入职时间'
  if (f.achievements.some((a) => !a.value.trim())) return '战果 value 不能为空'
  if (f.achievements.some((a) => a.value.length > 20)) return '战果 value 最长 20'
  if (f.achievements.some((a) => a.context.length > 50)) return '战果 context 最长 50'
  const radar = f.radar.map((r) => ({ dimension: r.dimension.trim(), score: Number(r.score) }))
  const radarErr = validateRadar(radar)
  if (radarErr) return radarErr
  const tags = splitLines(f.techTagsText)
  if (tags.length > 12) return '技术标签最多 12 个'
  if (tags.some((t) => t.length > 30)) return '单个技术标签最长 30'
  const highlights = splitLines(f.highlightsText)
  if (highlights.length > 10) return '扩展要点最多 10 条'
  if (highlights.some((h) => h.length > 50)) return '单条扩展要点最长 50'
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
  const payload: ExperienceCreate = {
    company: form.value.company.trim(),
    title: form.value.title.trim(),
    startDate: form.value.startDate,
    endDate: form.value.endDate || null,
    achievements: form.value.achievements
      .filter((a) => a.value.trim())
      .map((a) => ({ value: a.value.trim(), context: a.context.trim() })),
    radar: form.value.radar.map((r) => ({ dimension: r.dimension.trim(), score: Number(r.score) })),
    techTags: splitLines(form.value.techTagsText),
    highlights: splitLines(form.value.highlightsText),
    sortOrder: Number(form.value.sortOrder ?? 0),
  }
  try {
    if (editingId.value === null) {
      await createExperience(payload)
      notice.value = '[ok] 已新增，前台刷新即见'
    } else {
      await updateExperience(editingId.value, payload)
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

async function remove(item: Experience) {
  if (saving.value) return
  const confirmed = window.confirm(`确认删除经历「${item.company} · ${item.title}」？`)
  if (!confirmed) return
  saving.value = true
  errorMsg.value = ''
  try {
    await deleteExperience(item.id)
    notice.value = `[ok] 已删除「${item.company} · ${item.title}」，前台刷新即见`
    if (editingId.value === item.id) editingId.value = null
    await load()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '删除失败'
  } finally {
    saving.value = false
  }
}

function rangeText(item: Experience): string {
  const end = item.endDate ? item.endDate.slice(0, 7).replace('-', '.') : '至今'
  return `${item.startDate.slice(0, 7).replace('-', '.')} – ${end}`
}

onMounted(load)
</script>

<template>
  <section class="exp-page">
    <header class="head">
      <h1 class="title">
        <span class="led" aria-hidden="true"></span>
        EXPERIENCE
      </h1>
      <p class="sub">$ experience --admin · 增删改即时生效，前台刷新即见，无需发版</p>
    </header>

    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="errorMsg" class="error" role="alert">[error] {{ errorMsg }}</p>

    <form class="form" @submit.prevent="save">
      <div class="form-grid">
        <label class="field">
          <span>公司 *</span>
          <input
            v-model="form.company"
            type="text"
            maxlength="50"
            placeholder="某公司"
            spellcheck="false"
          />
        </label>
        <label class="field">
          <span>职位 *</span>
          <input
            v-model="form.title"
            type="text"
            maxlength="50"
            placeholder="前端工程师"
            spellcheck="false"
          />
        </label>
        <label class="field">
          <span>入职 (YYYY-MM-DD) *</span>
          <input v-model="form.startDate" type="date" />
        </label>
        <label class="field">
          <span>离职 (空 = 至今)</span>
          <input v-model="form.endDate" type="date" />
        </label>
        <label class="field">
          <span>排序</span>
          <input v-model.number="form.sortOrder" type="number" step="1" />
        </label>
      </div>

      <!-- 战果数组 -->
      <fieldset class="block">
        <legend>战果（≤6 条 · value ≤20 / context ≤50）</legend>
        <div v-for="(a, i) in form.achievements" :key="i" class="row">
          <input
            v-model="a.value"
            type="text"
            maxlength="20"
            placeholder="300%、50w+…"
            spellcheck="false"
          />
          <input
            v-model="a.context"
            type="text"
            maxlength="50"
            placeholder="一句话语境"
            spellcheck="false"
          />
          <button class="mini-btn danger" type="button" @click="removeAchievement(i)">删</button>
        </div>
        <button class="mini-btn" type="button" @click="addAchievement">+ 战果</button>
      </fieldset>

      <!-- 雷达维度数组（3~8） -->
      <fieldset class="block">
        <legend>雷达维度（3~8 个 · 维度名 ≤20 不重复 · 分值 0~100）</legend>
        <div v-for="(r, i) in form.radar" :key="i" class="row">
          <input
            v-model="r.dimension"
            type="text"
            maxlength="20"
            placeholder="架构能力"
            spellcheck="false"
          />
          <input v-model.number="r.score" type="number" min="0" max="100" step="1" />
          <button class="mini-btn danger" type="button" @click="removeRadar(i)">删</button>
        </div>
        <button class="mini-btn" type="button" @click="addRadar">+ 维度</button>
      </fieldset>

      <!-- 技术标签 / 展开要点：换行分隔 -->
      <div class="textarea-grid">
        <label class="field">
          <span>技术标签（换行分隔，≤12 个 · 每个 ≤30）</span>
          <textarea
            v-model="form.techTagsText"
            rows="4"
            spellcheck="false"
            placeholder="vue3&#10;spring&#10;mysql"
          />
        </label>
        <label class="field">
          <span>展开要点（换行分隔，≤10 条 · 每条 ≤50）</span>
          <textarea
            v-model="form.highlightsText"
            rows="4"
            spellcheck="false"
            placeholder="推动核心模块重构…&#10;主导数据看板上线…"
          />
        </label>
      </div>

      <div class="actions">
        <button class="neon-btn" type="submit" :disabled="saving">
          {{ saving ? 'SAVING…' : editingId === null ? '+ ADD' : 'SAVE' }}
        </button>
        <button
          v-if="editingId !== null"
          class="ghost-btn"
          type="button"
          :disabled="saving"
          @click="cancelEdit"
        >
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
          <th scope="col">公司 / 职位</th>
          <th scope="col">时间</th>
          <th scope="col">战果</th>
          <th scope="col">雷达</th>
          <th scope="col">标签</th>
          <th scope="col">排序</th>
          <th scope="col">操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in list" :key="item.id">
          <td class="id">{{ item.id }}</td>
          <td>
            <div class="row-cell">
              <span class="name">{{ item.company }}</span
              ><span class="dim">{{ item.title }}</span>
            </div>
          </td>
          <td class="dim">{{ rangeText(item) }}</td>
          <td class="dim">{{ item.achievements.map((a) => a.value).join(' · ') || '—' }}</td>
          <td class="dim">{{ item.radar.length }} 维</td>
          <td class="dim">{{ item.techTags.length }}</td>
          <td class="dim">{{ item.sortOrder }}</td>
          <td class="ops">
            <button class="op-btn" :disabled="saving" @click="startEdit(item)">编辑</button>
            <button class="op-btn danger" :disabled="saving" @click="remove(item)">删除</button>
          </td>
        </tr>
        <tr v-if="!list.length">
          <td colspan="8" class="empty">暂无经历，用上方表单新增</td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.exp-page {
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
  gap: 16px;
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
.field select,
.field textarea {
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
.field textarea {
  resize: vertical;
  line-height: 1.5;
}
.field input:focus,
.field select:focus,
.field textarea:focus {
  border-color: var(--green);
  box-shadow: 0 0 12px var(--green-soft);
}
.block {
  border: 1px dashed var(--border-bright);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.block legend {
  padding: 0 6px;
  color: var(--text-dim);
  font-size: 12px;
}
.row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.row input[type='text'] {
  flex: 1;
  background: var(--bg);
  border: 2px solid var(--border);
  color: var(--text);
  font-family: var(--font-mono);
  font-size: 13px;
  padding: 6px 10px;
  outline: none;
}
.row input[type='number'] {
  width: 90px;
  background: var(--bg);
  border: 2px solid var(--border);
  color: var(--text);
  font-family: var(--font-mono);
  font-size: 13px;
  padding: 6px 8px;
  outline: none;
}
.mini-btn {
  background: transparent;
  border: 2px solid var(--border-bright);
  color: var(--text);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 12px;
  padding: 6px 12px;
  white-space: nowrap;
  transition:
    border-color 0.2s,
    color 0.2s;
}
.mini-btn:hover {
  border-color: var(--green);
  color: var(--green);
}
.mini-btn.danger:hover {
  border-color: var(--error);
  color: var(--error);
}
.textarea-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
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
.row-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.name {
  color: var(--green);
}
.cat {
  color: var(--cyan);
}
.dim {
  color: var(--text-dim);
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
  .textarea-grid {
    grid-template-columns: 1fr;
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
