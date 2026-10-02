<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'

import {
  addCatalogTech,
  createCustomTech,
  deleteTechItem,
  fetchAdminTechStack,
  fetchTechCatalog,
  updateTechItem,
  type Proficiency,
  type TechCatalogItem,
  type TechItem,
  type TechItemCreate,
} from '@/api/tech'
import { PROFICIENCY_LABELS } from '@/api/tech'
import TechIcon from '@/components/tech/TechIcon.vue'

const COMMON_CATEGORIES = ['前端', '后端', '数据库', '中间件', '测试', '工具', '其他']
const PROFICIENCY_OPTIONS: { value: Proficiency; label: string }[] = [
  { value: 'master', label: '精通' },
  { value: 'proficient', label: '熟练' },
  { value: 'familiar', label: '了解' },
]
const MAX_ICON_BYTES = 512 * 1024
const ALLOWED_ICON_EXTENSIONS = /\.(png|jpe?g|webp)$/i

interface TechForm {
  name: string
  icon: string
  category: string
  proficiency: Proficiency
  weight: number
  sortOrder: number
}

interface CustomTechForm {
  name: string
  category: string
  proficiency: Proficiency
  weight: number
  sortOrder: number
}

const list = ref<TechItem[]>([])
const catalog = ref<TechCatalogItem[]>([])
const loading = ref(true)
const catalogLoading = ref(true)
const errorMsg = ref('')
const notice = ref('')
const saving = ref(false)
const editingId = ref<number | null>(null)
const showCustomForm = ref(false)
const categoryFilter = ref('全部')
const searchText = ref('')
const customFile = ref<File | null>(null)
const iconPreviewUrl = ref<string | null>(null)

const form = ref<TechForm>(emptyTechForm())
const customForm = ref<CustomTechForm>(emptyCustomTechForm())

const catalogCategories = computed(() => [
  '全部',
  ...new Set(catalog.value.map((item) => item.category)),
])
const filteredCatalog = computed(() => {
  const query = searchText.value.trim().toLocaleLowerCase()
  return catalog.value.filter((item) => {
    const categoryMatches = categoryFilter.value === '全部' || item.category === categoryFilter.value
    const searchMatches = !query || `${item.name} ${item.category}`.toLocaleLowerCase().includes(query)
    return categoryMatches && searchMatches
  })
})

function emptyTechForm(): TechForm {
  return { name: '', icon: '', category: '', proficiency: 'proficient', weight: 5, sortOrder: 0 }
}

function emptyCustomTechForm(): CustomTechForm {
  return {
    name: '',
    category: '后端',
    proficiency: 'familiar',
    weight: 5,
    sortOrder: 0,
  }
}

async function load() {
  loading.value = true
  catalogLoading.value = true
  errorMsg.value = ''
  try {
    const [items, catalogItems] = await Promise.all([fetchAdminTechStack(), fetchTechCatalog()])
    list.value = items
    catalog.value = catalogItems
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '加载失败'
  } finally {
    loading.value = false
    catalogLoading.value = false
  }
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
  form.value = emptyTechForm()
}

function validateFields(value: Pick<TechForm, 'name' | 'category' | 'weight'>): string | null {
  if (!value.name.trim()) return '名称不能为空'
  if (value.name.trim().length > 50) return '名称不能超过 50 字符'
  if (!value.category.trim()) return '分类不能为空'
  if (value.category.trim().length > 20) return '分类不能超过 20 字符'
  const weight = Number(value.weight)
  if (!Number.isInteger(weight) || weight < 1 || weight > 100) return '权重必须是 1~100 的整数'
  return null
}

async function saveEdit() {
  if (saving.value || editingId.value === null) return
  const problem = validateFields(form.value)
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
    await updateTechItem(editingId.value, payload)
    notice.value = `[ok] ID ${editingId.value} 已保存，前台刷新即见`
    cancelEdit()
    await load()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function addCatalogItem(item: TechCatalogItem) {
  if (saving.value || item.inStack) return
  saving.value = true
  errorMsg.value = ''
  try {
    await addCatalogTech(item.id, {
      proficiency: 'familiar',
      weight: 5,
      sortOrder: Math.max(0, ...list.value.map((entry) => entry.sortOrder)) + 1,
    })
    notice.value = `[ok] 已将「${item.name}」加入技术展示`
    await load()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '添加失败'
  } finally {
    saving.value = false
  }
}

function setCustomFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0] ?? null
  clearPreview()
  customFile.value = file
  errorMsg.value = ''
  if (file && file.size <= MAX_ICON_BYTES && ALLOWED_ICON_EXTENSIONS.test(file.name)) {
    iconPreviewUrl.value = URL.createObjectURL(file)
  }
}

function clearPreview() {
  if (iconPreviewUrl.value) URL.revokeObjectURL(iconPreviewUrl.value)
  iconPreviewUrl.value = null
}

function cancelCustomCreate() {
  showCustomForm.value = false
  customForm.value = emptyCustomTechForm()
  customFile.value = null
  clearPreview()
}

async function createCustomItem() {
  if (saving.value) return
  const problem = validateFields(customForm.value)
  if (problem) {
    errorMsg.value = problem
    return
  }
  if (!customFile.value) {
    errorMsg.value = '请选择 PNG、JPG、JPEG 或 WebP 图标'
    return
  }
  if (customFile.value.size > MAX_ICON_BYTES) {
    errorMsg.value = '图标不能超过 512 KB'
    return
  }
  if (!ALLOWED_ICON_EXTENSIONS.test(customFile.value.name)) {
    errorMsg.value = '图标格式仅支持 PNG、JPG、JPEG 或 WebP'
    return
  }

  saving.value = true
  errorMsg.value = ''
  try {
    await createCustomTech({
      file: customFile.value,
      name: customForm.value.name.trim(),
      category: customForm.value.category.trim(),
      proficiency: customForm.value.proficiency,
      weight: Number(customForm.value.weight),
      sortOrder: Math.max(0, ...list.value.map((entry) => entry.sortOrder)) + 1,
    })
    notice.value = `[ok] 已新建「${customForm.value.name.trim()}」并加入技术展示`
    cancelCustomCreate()
    await load()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '创建失败'
  } finally {
    saving.value = false
  }
}

async function remove(item: TechItem) {
  if (saving.value) return
  const confirmed = window.confirm(`确认从展示列表删除「${item.name}」？目录记录会保留。`)
  if (!confirmed) return
  saving.value = true
  errorMsg.value = ''
  try {
    await deleteTechItem(item.id)
    notice.value = `[ok] 已从展示列表移除「${item.name}」，目录记录仍保留`
    if (editingId.value === item.id) cancelEdit()
    await load()
  } catch (error) {
    errorMsg.value = error instanceof Error ? error.message : '删除失败'
  } finally {
    saving.value = false
  }
}

function itemLabel(value: Proficiency) {
  return PROFICIENCY_LABELS[value] ?? value
}

onMounted(load)
onUnmounted(clearPreview)
</script>

<template>
  <section class="tech-page">
    <header class="head">
      <h1 class="title">
        <span class="led" aria-hidden="true"></span>
        TECH STACK
      </h1>
      <p class="sub">$ tech-stack --catalog · 选择常用技术，或上传图标新建技术项</p>
    </header>

    <p v-if="notice" class="notice" role="status">{{ notice }}</p>
    <p v-if="errorMsg" class="error" role="alert">[error] {{ errorMsg }}</p>

    <section class="panel catalog-panel" aria-labelledby="catalog-title">
      <div class="panel-head">
        <div>
          <h2 id="catalog-title" class="section-title">JAVA FULL STACK CATALOG</h2>
          <p class="panel-description">从预置技术库添加到前台展示，默认熟练度「了解」、权重 5，可在下方调整。</p>
        </div>
        <span class="catalog-count">{{ catalog.length }} 项可选</span>
      </div>

      <div class="catalog-tools">
        <label class="field search-field">
          <span>搜索技术</span>
          <input v-model="searchText" type="search" placeholder="例如：Spring、MySQL、Docker" />
        </label>
        <label class="field category-field">
          <span>分类</span>
          <select v-model="categoryFilter">
            <option v-for="category in catalogCategories" :key="category" :value="category">
              {{ category }}
            </option>
          </select>
        </label>
        <button class="neon-btn custom-entry" type="button" @click="showCustomForm = !showCustomForm">
          {{ showCustomForm ? '收起新建' : '+ 新建技术栈' }}
        </button>
      </div>

      <p v-if="catalogLoading" class="loading">loading catalog ...</p>
      <div v-else-if="filteredCatalog.length" class="catalog-grid">
        <article v-for="item in filteredCatalog" :key="item.id" class="catalog-card">
          <TechIcon :icon="item.icon" :icon-url="item.iconUrl" :name="item.name" :size="28" />
          <div class="catalog-meta">
            <strong>{{ item.name }}</strong>
            <span>{{ item.category }}<template v-if="item.custom"> · 自建</template></span>
          </div>
          <button
            class="op-btn add-btn"
            type="button"
            :disabled="saving || item.inStack"
            @click="addCatalogItem(item)"
          >
            {{ item.inStack ? '已加入' : '加入展示' }}
          </button>
        </article>
      </div>
      <p v-else class="empty">没有匹配的技术项</p>
    </section>

    <form v-if="showCustomForm" class="form" @submit.prevent="createCustomItem">
      <div class="panel-head">
        <div>
          <h2 class="section-title">CUSTOM TECH</h2>
          <p class="panel-description">图标仅支持 PNG、JPG、JPEG、WebP；文件不超过 512 KB，图片尺寸不超过 1024 × 1024。</p>
        </div>
      </div>
      <div class="form-grid">
        <label class="field">
          <span>名称 *</span>
          <input v-model="customForm.name" type="text" maxlength="50" placeholder="例如：TiDB" />
        </label>
        <label class="field">
          <span>分类 *</span>
          <input v-model="customForm.category" list="custom-tech-categories" maxlength="20" />
          <datalist id="custom-tech-categories">
            <option v-for="category in COMMON_CATEGORIES" :key="category" :value="category" />
          </datalist>
        </label>
        <label class="field file-field">
          <span>图标文件 *</span>
          <input
            type="file"
            accept=".png,.jpg,.jpeg,.webp,image/png,image/jpeg,image/webp"
            @change="setCustomFile"
          />
          <small v-if="customFile">{{ customFile.name }} · {{ Math.ceil(customFile.size / 1024) }} KB</small>
        </label>
        <div v-if="iconPreviewUrl" class="icon-preview">
          <img :src="iconPreviewUrl" alt="自定义图标预览" />
          <span>图标预览</span>
        </div>
        <label class="field">
          <span>熟练度</span>
          <select v-model="customForm.proficiency">
            <option v-for="option in PROFICIENCY_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>权重 (1~100)</span>
          <input v-model.number="customForm.weight" type="number" min="1" max="100" step="1" />
        </label>
      </div>
      <div class="actions">
        <button class="neon-btn" type="submit" :disabled="saving">
          {{ saving ? 'SAVING…' : 'CREATE + ADD' }}
        </button>
        <button class="ghost-btn" type="button" :disabled="saving" @click="cancelCustomCreate">取消</button>
      </div>
    </form>

    <section class="stack-section" aria-labelledby="stack-title">
      <div class="stack-heading">
        <div>
          <h2 id="stack-title" class="section-title">CURRENT DISPLAY</h2>
          <p class="panel-description">当前展示 {{ list.length }} 项；编辑可调整名称、分类、熟练度、权重和排序。</p>
        </div>
      </div>

      <form v-if="editingId !== null" class="form edit-form" @submit.prevent="saveEdit">
        <div class="form-grid">
          <label class="field">
            <span>名称 *</span>
            <input v-model="form.name" type="text" maxlength="50" />
          </label>
          <label class="field">
            <span>分类 *</span>
            <input v-model="form.category" list="edit-tech-categories" maxlength="20" />
            <datalist id="edit-tech-categories">
              <option v-for="category in COMMON_CATEGORIES" :key="category" :value="category" />
            </datalist>
          </label>
          <label class="field">
            <span>熟练度</span>
            <select v-model="form.proficiency">
              <option v-for="option in PROFICIENCY_OPTIONS" :key="option.value" :value="option.value">
                {{ option.label }}
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
          <button class="neon-btn" type="submit" :disabled="saving">{{ saving ? 'SAVING…' : 'SAVE' }}</button>
          <button class="ghost-btn" type="button" :disabled="saving" @click="cancelEdit">取消</button>
        </div>
      </form>

      <p v-if="loading" class="loading">loading ...</p>
      <div v-else class="table-wrap">
        <table class="term-table">
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
              <td class="icon-cell">
                <TechIcon :icon="item.icon" :icon-url="item.iconUrl" :name="item.name" :size="24" />
                <span>{{ item.iconUrl ? '自定义图标' : item.icon || '无' }}</span>
              </td>
              <td class="cat">{{ item.category }}</td>
              <td><span class="prof" :data-proficiency="item.proficiency">{{ itemLabel(item.proficiency) }}</span></td>
              <td class="dim">{{ item.weight }}</td>
              <td class="dim">{{ item.sortOrder }}</td>
              <td class="ops">
                <button class="op-btn" type="button" :disabled="saving" @click="startEdit(item)">编辑</button>
                <button class="op-btn danger" type="button" :disabled="saving" @click="remove(item)">移除</button>
              </td>
            </tr>
            <tr v-if="!list.length">
              <td colspan="8" class="empty">暂未选择技术项，请从上方目录添加</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>
</template>

<style scoped>
.tech-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.head,
.panel-head,
.stack-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.head {
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

.sub,
.panel-description {
  margin: 0;
  color: var(--text-dim);
  font-size: 13px;
}

.sub {
  font-family: var(--font-term);
  font-size: 20px;
  letter-spacing: 0.5px;
}

.panel,
.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 16px;
  background: var(--bg-panel);
  border: 2px solid var(--border);
}

.section-title {
  margin: 0 0 6px;
  color: var(--cyan);
  font-family: var(--font-term);
  font-size: 22px;
  font-weight: 400;
  letter-spacing: 1px;
}

.catalog-count {
  flex: none;
  color: var(--green);
  font-family: var(--font-mono);
  font-size: 12px;
}

.catalog-tools {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(150px, 220px) auto;
  align-items: end;
  gap: 12px;
}

.field {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 6px;
  color: var(--text-dim);
  font-size: 12px;
}

.field input,
.field select {
  width: 100%;
  box-sizing: border-box;
  min-height: 42px;
  padding: 8px 10px;
  background: var(--bg);
  border: 2px solid var(--border);
  color: var(--text);
  font-family: var(--font-mono);
  font-size: 13px;
  outline: none;
  caret-color: var(--green);
}

.field input:focus-visible,
.field select:focus-visible {
  border-color: var(--green);
  box-shadow: 0 0 12px var(--green-soft);
}

.custom-entry,
.actions button {
  min-height: 44px;
}

.catalog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  max-height: 480px;
  overflow: auto;
  padding-right: 4px;
}

.catalog-card {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid var(--border);
  background: var(--bg);
}

.catalog-meta {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 3px;
}

.catalog-meta strong {
  overflow: hidden;
  color: var(--text);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.catalog-meta span,
.file-field small,
.icon-preview span {
  color: var(--text-dim);
  font-size: 11px;
}

.op-btn,
.ghost-btn {
  min-height: 40px;
  padding: 7px 12px;
  background: transparent;
  border: 2px solid var(--border-bright);
  color: var(--text);
  cursor: pointer;
  font-family: var(--font-mono);
  font-size: 12px;
  transition: border-color 0.2s, color 0.2s;
}

.op-btn:hover:not(:disabled),
.ghost-btn:hover:not(:disabled) {
  border-color: var(--green);
  color: var(--green);
}

.op-btn:focus-visible,
.ghost-btn:focus-visible,
.neon-btn:focus-visible {
  outline: 2px solid var(--cyan);
  outline-offset: 2px;
}

.op-btn:disabled,
.ghost-btn:disabled,
.neon-btn:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.add-btn {
  flex: none;
  min-width: 90px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.file-field input {
  padding: 5px;
}

.file-field input::file-selector-button {
  min-height: 28px;
  margin-right: 10px;
  padding: 4px 8px;
  border: 1px solid var(--border-bright);
  background: var(--bg-raised);
  color: var(--text);
  cursor: pointer;
}

.icon-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-top: 20px;
}

.icon-preview img {
  width: 44px;
  height: 44px;
  object-fit: contain;
  border: 1px solid var(--border-bright);
  background: var(--bg);
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.neon-btn {
  font-size: 11px;
  padding: 10px 18px;
}

.stack-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.icon-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 140px;
  color: var(--text-dim);
  font-size: 11px;
}

.loading,
.empty {
  margin: 0;
  color: var(--text-dim);
  text-align: center;
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

.id,
.dim {
  color: var(--text-dim);
}

.name {
  color: var(--text);
}

.cat {
  color: var(--cyan);
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

.ops .op-btn {
  margin-right: 6px;
}

.op-btn.danger:hover:not(:disabled) {
  border-color: var(--error);
  color: var(--error);
}

@media (max-width: 760px) {
  .catalog-grid {
    grid-template-columns: 1fr;
  }

  .form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .panel-head,
  .stack-heading {
    flex-direction: column;
  }

  .catalog-tools,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .custom-entry {
    width: 100%;
  }

  .catalog-card {
    flex-wrap: wrap;
  }

  .catalog-meta {
    flex-basis: calc(100% - 42px);
  }

  .add-btn {
    margin-left: 38px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .led {
    animation: none;
  }

  .title {
    text-shadow: none;
  }

  .op-btn,
  .ghost-btn {
    transition: none;
  }
}
</style>
