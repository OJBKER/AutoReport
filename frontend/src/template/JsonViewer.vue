<template>
  <div class="json-viewer">
    <h2>实验报告模板填写</h2>
    <form @submit.prevent>
      <div v-if="jsonData">
        <div v-for="field in jsonData.header" :key="field.key" class="form-row">
          <label :for="field.key">{{ field.label }}：</label>
          <input v-if="field.type === 'text'" :id="field.key" v-model="form[field.key]" type="text" />
        </div>
        <div v-for="section in jsonData.sections" :key="section.key" class="form-section">
          <label :for="section.key" style="font-weight:bold;display:block;margin-bottom:4px;">
            {{ section.title }}
            <span v-if="section.default" style="color:#888;font-size:13px;margin-left:8px;">（说明：{{ Array.isArray(section.default) ? section.default.join('，') : section.default }}）</span>
          </label>
          <textarea :id="section.key" v-model="form[section.key]" rows="4" class="unified-textarea" v-autoresize="{ max: 480 }" @click="openBigEditor(section.key)"></textarea>
        </div>
        <div v-for="field in jsonData.footer" :key="field.key" class="form-row">
          <label :for="field.key">{{ field.label }}：</label>
          <input v-if="field.type === 'text'" :id="field.key" v-model="form[field.key]" type="text" />
        </div>
      </div>
      <div v-if="error" style="color:red;">{{ error }}</div>
    </form>

    <!-- 放大编辑弹窗 -->
    <div v-if="showBigEditor" class="big-editor-mask" @keydown.esc="cancelBigEditor">
      <div class="big-editor-container">
        <div class="big-editor-header">
          <span>放大编辑</span>
          <button type="button" class="close-btn" @click="cancelBigEditor">×</button>
        </div>
        <div class="big-editor-body">
          <textarea v-model="bigEditorValue" class="big-editor-textarea"></textarea>
        </div>
        <div class="big-editor-footer">
          <button type="button" class="be-btn secondary" @click="cancelBigEditor">取消</button>
          <button type="button" class="be-btn primary" @click="confirmBigEditor">确定编辑</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, defineExpose, watch } from 'vue'
import autoResize from '../directives/autoresize.js'

const emit = defineEmits(['template-loaded'])

const props = defineProps({
  jsonUrl: { type: String, default: '' }
})

const jsonData = ref(null)
const error = ref('')
const form = ref({})
let fieldTypeMap = {}

// 放大编辑状态
const showBigEditor = ref(false)
const bigEditorKey = ref('')
const bigEditorValue = ref('')

function openBigEditor(key){
  if(!key) return;
  bigEditorKey.value = key;
  bigEditorValue.value = form.value[key] || '';
  showBigEditor.value = true;
  // 下一帧聚焦
  requestAnimationFrame(()=>{
    const el=document.querySelector('.big-editor-textarea');
    if(el) { el.focus(); el.selectionStart = el.value.length; el.selectionEnd = el.value.length; }
  });
}
function confirmBigEditor(){
  if(bigEditorKey.value && bigEditorKey.value in form.value){
    form.value[bigEditorKey.value] = bigEditorValue.value;
  }
  showBigEditor.value=false;
}
function cancelBigEditor(){ showBigEditor.value=false; }

async function loadTemplate(url){ if(!url) return false; try{ const res=await fetch(url+'?_='+Date.now()); if(!res.ok) throw new Error('模板文件获取失败'); const data=await res.json(); jsonData.value=data; form.value={}; fieldTypeMap={}; [...(data.header||[]),...(data.sections||[]),...(data.footer||[])].forEach(f=>{ form.value[f.key]=''; fieldTypeMap[f.key]=f.type }); emit('template-loaded', url); return true } catch(err){ error.value=err.message; return false } }

onMounted(() => { if (props.jsonUrl) loadTemplate(props.jsonUrl) })
watch(() => props.jsonUrl, (nv, ov) => { if (nv && nv !== ov) loadTemplate(nv) })

function fillForm(obj){
  if(!jsonData.value||!obj) return;
  Object.entries(obj).forEach(([k,v])=>{
    if(!(k in form.value)) return;
    const t=fieldTypeMap[k];
    if(t==='list'){
      if(Array.isArray(v)) form.value[k]=v.join('\n');
      else if(typeof v==='string') form.value[k]=v; 
      else if(v==null) form.value[k]='';
      else form.value[k]=String(v);
    } else if(t==='textarea') {
      form.value[k]= typeof v==='string'?v:(v==null?'':JSON.stringify(v));
    } else if(t==='text') {
      form.value[k]= typeof v==='string'?v: (v==null?'':String(v));
    } else {
      form.value[k]= typeof v==='string'?v: (v==null?'':JSON.stringify(v));
    }
  });
}

function getFormData(){
  const out={};
  Object.entries(form.value).forEach(([k,v])=>{
    const t=fieldTypeMap[k];
    if(t==='list'){
      if(typeof v==='string') out[k]=v.split(/\n+/).map(s=>s.trim()).filter(Boolean); else if(Array.isArray(v)) out[k]=v; else out[k]=[];
    } else {
      out[k]=v;
    }
  });
  return out;
}
function getTemplateSchema(){ return jsonData.value ? JSON.parse(JSON.stringify(jsonData.value)) : null }

defineExpose({ getFormData, fillForm, getTemplateSchema, loadTemplate, openBigEditor })
</script>

<script>
export default { directives: { autoresize: autoResize } }
</script>

<style scoped>
 input[type="text"] {
   display: block;
   width: 70%;
   max-width: 400px;
   font-size: 16px;
   padding: 10px 12px;
   border: 1.5px solid #b3c2d4;
   border-radius: 8px;
   box-shadow: 0 1px 4px #e0e7ef;
   background: #f8fbff;
   transition: border-color 0.2s, box-shadow 0.2s;
   margin: 0 auto 16px auto;
   box-sizing: border-box;
 }
 input[type="text"]:focus {
   border-color: #409eff;
   box-shadow: 0 2px 8px #cbe6ff;
   outline: none;
   background: #fff;
 }
 .json-viewer {
   width: 95%; /* changed from 66.666% to 95% per requirement */
   margin: 40px auto;
   padding: 24px;
   background: #fff;
   border-radius: 8px;
   box-shadow: 0 2px 12px #eee;
   box-sizing: border-box;
 }

 @media (max-width: 900px) {
   .json-viewer {
     width: 98%;
     padding: 8px;
   }
   textarea {
     width: 98%;
     max-width: 98%;
   }
 }

 textarea {
   display: block;
   width: 90%;
   max-width: 700px;
   min-height: 80px;
   font-size: 16px;
   padding: 12px 14px;
   border: 1.5px solid #b3c2d4;
   border-radius: 8px;
   box-shadow: 0 1px 4px #e0e7ef;
   background: #f8fbff;
   transition: border-color 0.2s, box-shadow 0.2s;
   margin: 0 auto 16px auto;
  resize: none;
   box-sizing: border-box;
 }
 textarea:focus {
   border-color: #409eff;
   box-shadow: 0 2px 8px #cbe6ff;
   outline: none;
   background: #fff;
 }
pre {
  background: #f0f8ff;
  border-radius: 6px;
  padding: 12px;
  margin-top: 8px;
  font-size: 15px;
  white-space: pre-wrap;
}

/* Big editor modal */
.big-editor-mask { position:fixed; inset:0; background:rgba(0,0,0,.45); display:flex; align-items:center; justify-content:center; z-index:9999; padding:30px; box-sizing:border-box; }
.big-editor-container { width: min(1200px, 94%); height: min(760px, 90%); background:#fff; border-radius:12px; box-shadow:0 8px 32px rgba(0,0,0,.22); display:flex; flex-direction:column; overflow:hidden; animation: be-pop .25s ease; }
.big-editor-header { padding:14px 18px; font-weight:600; font-size:15px; background:linear-gradient(135deg,#eef3f9,#dfe8f2); display:flex; justify-content:space-between; align-items:center; }
.close-btn { background:transparent; border:none; font-size:22px; line-height:1; cursor:pointer; color:#555; }
.close-btn:hover { color:#111; }
.big-editor-body { flex:1; padding:16px 18px 6px; overflow:auto; }
.big-editor-textarea { width:100%; height:100%; min-height:100%; resize:none; font-size:16px; line-height:1.55; font-family:inherit; border:1px solid #b3c2d4; border-radius:8px; padding:14px 16px; box-sizing:border-box; background:#f8fbff; box-shadow:0 1px 4px #e0e7ef inset; }
.big-editor-textarea:focus { outline:none; border-color:#409eff; box-shadow:0 0 0 2px rgba(64,158,255,.25); background:#fff; }
.big-editor-footer { padding:12px 18px 16px; display:flex; justify-content:flex-end; gap:12px; background:#f6f8fa; border-top:1px solid #e2e8ef; }
.be-btn { padding:8px 18px; font-size:14px; border-radius:6px; border:1px solid transparent; cursor:pointer; font-weight:500; }
.be-btn.secondary { background:#e7ecf2; border-color:#d0d7df; }
.be-btn.secondary:hover { background:#dbe2ea; }
.be-btn.primary { background:#409eff; color:#fff; }
.be-btn.primary:hover { background:#3087dd; }
@keyframes be-pop { from { transform:translateY(12px); opacity:0; } to { transform:translateY(0); opacity:1; } }
</style>
