<template>
  <div class="docgen-panel">
    <h2>DOC 文档生成</h2>
    <p class="docgen-desc">将当前表单内容导出为 docx（后端生成）。</p>
    <div class="docgen-actions">
      <button class="docgen-btn primary" @click="handleExport(false)" :disabled="exporting">{{ exporting? '生成中...' : '生成 DOCX' }}</button>
      <button class="docgen-btn" @click="handleExport(true)" :disabled="exporting">{{ exporting? '...' : '预览 JSON' }}</button>
      <button class="docgen-btn" @click="previewHtml" :disabled="exporting || htmlLoading">{{ htmlLoading? '加载中...' : '预览 DOC 样式' }}</button>
      <button v-if="htmlPreview" class="docgen-btn" @click="toggleHtmlMode">{{ showHtml ? '收起 HTML' : '展开 HTML' }}</button>
    </div>
    <div v-if="error" class="docgen-error">{{ error }}</div>
    <pre v-if="preview && showPreview" class="docgen-preview" @click="showPreview=false" title="点击收起">{{ preview }}</pre>
    <div v-else-if="preview && !showPreview" class="docgen-collapsed" @click="showPreview=true">JSON 预览已生成（点击展开）</div>
    <div v-if="htmlPreview" class="html-preview-wrapper" :class="{ collapsed: !showHtml }">
      <div class="html-preview-bar">
        <span>HTML 预览</span>
        <div class="flex-gap"></div>
        <button class="mini-btn" @click="showHtml=!showHtml">{{ showHtml? '收起' : '展开' }}</button>
        <button class="mini-btn" @click="openInNewTab">新标签打开</button>
      </div>
      <iframe v-show="showHtml" ref="iframeRef" class="html-iframe" sandbox="allow-same-origin" title="DOC 预览"></iframe>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import http from '@/api/http'

const props = defineProps({
  formData: { type: Object, default: null },
  templateCode: { type: [Number, String], default: null }
})

const emit = defineEmits(['toast'])

const exporting = ref(false)
const error = ref('')
const preview = ref('')
const showPreview = ref(false)
const htmlLoading = ref(false)
const htmlPreview = ref('') // 存储 HTML 字符串
const showHtml = ref(true)
const iframeRef = ref(null)

async function handleExport(previewOnly=false){
  error.value=''
  if(!props.formData){ emit('toast','当前无表单数据'); return }
  try {
    exporting.value=true
    const url = previewOnly? '/api/report/export/preview' : '/api/report/export'
    if(previewOnly){
      const { data } = await http.post(url, { templateCode: props.templateCode!=null?Number(props.templateCode):null, data: props.formData })
      if(!data) throw new Error('解析预览失败')
      preview.value = JSON.stringify(data, null, 2)
      showPreview.value = true
      emit('toast','预览已生成')
    } else {
      const resp = await http.post(url, { templateCode: props.templateCode!=null?Number(props.templateCode):null, data: props.formData }, { responseType:'blob' })
      const blob = resp.data
      const urlObj = URL.createObjectURL(blob)
      const a = document.createElement('a')
      const ts = new Date().toISOString().replace(/[:T]/g,'-').slice(0,19)
      a.href = urlObj
      a.download = ((props.templateCode!=null?('C'+props.templateCode):'report'))+'-'+ts+'.docx'
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

async function previewHtml(){
  error.value=''
  if(!props.formData){ emit('toast','当前无表单数据'); return }
  htmlLoading.value=true
  try {
    const { data } = await http.post('/api/report/export/preview/html', { templateCode: props.templateCode!=null?Number(props.templateCode):null, data: props.formData }, { responseType:'text' })
    // data 是字符串（由于 responseType text），直接使用
    htmlPreview.value = typeof data === 'string'? data : String(data)
    // 写入 iframe
    if(iframeRef.value){
      const doc = iframeRef.value.contentDocument || iframeRef.value.contentWindow?.document
      if(doc){
        doc.open();
        doc.write(htmlPreview.value);
        doc.close();
      }
    }
    showHtml.value = true
    emit('toast','HTML 预览已生成')
  } catch(e){
    error.value = 'HTML 预览失败: '+e
    emit('toast', error.value)
  } finally { htmlLoading.value=false }
}
function toggleHtmlMode(){ showHtml.value=!showHtml.value }
function openInNewTab(){ if(!htmlPreview.value) return; const w = window.open('about:blank','_blank'); if(w){ w.document.write(htmlPreview.value); w.document.close(); } }

// 重置预览（供外部调用）
function resetPreview(){
  preview.value=''
  showPreview.value=false
  htmlPreview.value=''
  showHtml.value=true
  error.value=''
  // 清空 iframe 内容以释放内存
  if(iframeRef.value){
    try { const doc = iframeRef.value.contentDocument || iframeRef.value.contentWindow?.document; if(doc){ doc.open(); doc.write('<!DOCTYPE html><html><head><meta charset="utf-8"></head><body style="font:12px sans-serif;color:#888;padding:8px;">(预览已重置)</body></html>'); doc.close(); } } catch(_){}
  }
}

defineExpose({ resetPreview })

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
.html-preview-wrapper { margin-top:14px; border:1px solid #d0d7df; border-radius:10px; background:#f8fafc; box-shadow:0 1px 2px rgba(0,0,0,.05); }
.html-preview-wrapper.collapsed { background:#f1f5f9; }
.html-preview-bar { display:flex; align-items:center; gap:8px; padding:6px 10px; font-size:12px; background:#e2e8f0; border-bottom:1px solid #d0d7df; border-top-left-radius:10px; border-top-right-radius:10px; }
.html-iframe { width:100%; min-height:480px; border:none; border-bottom-left-radius:10px; border-bottom-right-radius:10px; background:#fff; }
.mini-btn { padding:4px 10px; font-size:12px; border:1px solid #cbd5e1; background:#fff; border-radius:4px; cursor:pointer; }
.mini-btn:hover { background:#e2e8f0; }
.flex-gap { flex:1; }
</style>
