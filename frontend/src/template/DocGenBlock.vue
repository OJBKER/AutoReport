<template>
  <div class="docgen-panel">
    <h2>DOC 文档生成</h2>
    <p class="docgen-desc">将当前表单内容导出为 docx（后端生成）。</p>
    <div class="docgen-actions">
      <button class="docgen-btn primary" @click="handleExport(false)" :disabled="exporting">{{ exporting? '生成中...' : '生成 DOCX' }}</button>
      <button class="docgen-btn" @click="handleExport(true)" :disabled="exporting">{{ exporting? '...' : '仅预览 JSON' }}</button>
    </div>
    <div v-if="error" class="docgen-error">{{ error }}</div>
    <pre v-if="preview && showPreview" class="docgen-preview" @click="showPreview=false" title="点击收起">{{ preview }}</pre>
    <div v-else-if="preview && !showPreview" class="docgen-collapsed" @click="showPreview=true">预览已生成（点击展开）</div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getCsrfToken } from '../utils/csrf.js'

const props = defineProps({
  formData: { type: Object, default: null },
  templateType: { type: String, default: null }
})

const emit = defineEmits(['toast'])

const exporting = ref(false)
const error = ref('')
const preview = ref('')
const showPreview = ref(false)

async function handleExport(previewOnly=false){
  error.value=''
  if(!props.formData){ emit('toast','当前无表单数据'); return }
  try {
    exporting.value=true
    const csrfToken = await getCsrfToken()
    const url = previewOnly? '/api/report/export/preview' : '/api/report/export'
    const res = await fetch(url, {
      method:'POST',
      headers:{ 'Content-Type':'application/json','X-CSRF-TOKEN':csrfToken },
      body: JSON.stringify({ templateType: props.templateType || null, data: props.formData })
    })
    if(!res.ok) throw new Error('HTTP '+res.status)
    if(previewOnly){
      const data = await res.json().catch(()=>null)
      if(!data) throw new Error('解析预览失败')
      preview.value = JSON.stringify(data, null, 2)
      showPreview.value = true
      emit('toast','预览已生成')
    } else {
      const blob = await res.blob()
      const urlObj = URL.createObjectURL(blob)
      const a = document.createElement('a')
      const ts = new Date().toISOString().replace(/[:T]/g,'-').slice(0,19)
      a.href = urlObj
      a.download = (props.templateType || 'report')+'-'+ts+'.docx'
      document.body.appendChild(a)
      a.click()
      a.remove()
      URL.revokeObjectURL(urlObj)
      emit('toast','DOCX 已下载')
    }
  } catch(e){
    error.value= String(e)
    emit('toast','文档导出失败: '+e)
  } finally {
    exporting.value=false
  }
}

</script>

<style scoped>
.docgen-panel h2 { margin:0 0 10px; font-size:1.15rem; }
.docgen-desc { margin:0 0 12px; font-size:13px; color: var(--color-text-secondary,#666); }
.docgen-actions { display:flex; gap:12px; margin-bottom:12px; flex-wrap:wrap; }
.docgen-btn { padding:8px 16px; border-radius:6px; border:1px solid #d0d7df; background:#eef2f6; cursor:pointer; font-size:13px; }
.docgen-btn.primary { background:#409eff; color:#fff; border-color:#409eff; }
.docgen-btn.primary:hover { background:#3188de; }
.docgen-btn:hover { background:#dde5ec; }
.docgen-error { color:#d93025; font-size:12px; margin-bottom:8px; }
.docgen-preview { background:#0f172a; color:#d1e7ff; padding:12px 14px; border-radius:8px; max-height:280px; overflow:auto; font-size:12px; line-height:1.45; cursor:pointer; }
.docgen-collapsed { font-size:12px; color:#409eff; cursor:pointer; }
</style>
