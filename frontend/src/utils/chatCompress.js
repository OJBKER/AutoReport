/**
 * Chat 压缩总览（简要，纯 JSON 版，无 Base64）：
 * 输入：完整 messages (system / user / assistant / __FORM_CONTEXT__ 混合)。
 * 1) 丢弃 system 与 __FORM_CONTEXT__。
 * 2) 用户保留 -> ['u', text]
 * 3) 助手解析 JSON 提取 talk（缺失则跳过）-> ['a', talk]
 * 4) 生成对象 { v:1, t: [...] }，直接 JSON.stringify 后前缀 'CT1:'
 * 解压：去掉前缀直接 JSON.parse；助手消息还原为 {talk: text}。
 * 目的：最小体积且便于直接查看与调试；已放弃 Base64 封装（数据库重置，无需兼容旧格式）。
 */
/**
 * Chat compression (SIMPLE FINAL):
 * 仅保留用户输入与 AI talk：CT1:{"v":1,"t":[["u",userText],["a",talk]]}
 */
const PREFIX_TALK_ONLY = 'CT1:'

// legacy helpers removed (C1/C2)

/**
 * Talk-only compression (CT1):
 * 保留顺序，只记录：
 *   用户: ['u', text]
 *   助手: ['a', talkString]
 * 丢弃：system、表单上下文(__FORM_CONTEXT__)以及助手 JSON 里除 talk 外的字段。
 * 如果助手消息无法解析出 talk，则跳过该条（可选策略：也可退化存 raw，但这里为了体积直接丢弃）。
 */
export function compressChatHistory(messages){
  try {
    if(!Array.isArray(messages) || !messages.length) return ''
    const FORM_PREFIX='__FORM_CONTEXT__'
    const out=[]
    for(const m of messages){
      if(!m || typeof m.content!=='string') continue
      if(m.role==='system') continue
      if(m.role==='user'){
        if(m.content.startsWith(FORM_PREFIX)) continue // 丢弃表单上下文
        out.push(['u', m.content])
      } else if(m.role==='assistant'){
        // 解析 JSON 抽取 talk
        let talk = null
        try {
          if(m.content.trim().startsWith('{')){
            const obj = JSON.parse(m.content)
            if(obj && typeof obj==='object' && Object.prototype.hasOwnProperty.call(obj,'talk')){
              const tv = obj.talk
              if(Array.isArray(tv)) talk = tv.join('\n')
              else if(typeof tv==='string') talk = tv
              else if(tv!=null) talk = JSON.stringify(tv)
            }
          }
        } catch { /* ignore */ }
        if(talk==null) continue // 丢弃无法提取的助手消息
        out.push(['a', talk])
      }
    }
    if(!out.length) return ''
    const json = JSON.stringify({ v:1, t: out })
    return PREFIX_TALK_ONLY + json
  } catch(e){
    console.warn('[compressChatHistoryTalkOnly] failed', e)
    return ''
  }
}

/**
 * Advanced compression (C2):
 *  - Assistant messages: extract JSON, separate talk, delta against previous full (excluding talk)
 *  - Form context messages (prefix __FORM_CONTEXT__): only store diff vs last form context object
 *  - User normal messages: role code + content
 *  Structure:
 *    { v:2, a:[ entries... ], f: optional system hash placeholder }
 *  Entry variants (array form for minimal overhead):
 *    ['u', content]
 *    ['f', formDeltaObject]  // form context diff
 *    ['a', 'F', fullObjWithoutTalk, talk?]  // first assistant full
 *    ['a', 'D', deltaObj, talk?]            // assistant delta
 *    ['a', 'S', talk?]                      // assistant same structure only talk changed
 *    ['a', 'R', rawString]                  // fallback raw unparsed assistant
 */

/**
 * Detect if string is a compressed chat history produced by compressChatHistory.
 * @param {string} str 
 */
export function isCompressedChat(str){ return typeof str==='string' && str.startsWith(PREFIX_TALK_ONLY) }

/**
 * Decompress back to canonical message array.
 * @param {string} packed 
 * @returns {Array<{role:string, content:string}>|null}
 */
export function decompressChatHistory(packed){
  try {
    if(!isCompressedChat(packed)) return null
    if(packed.startsWith(PREFIX_TALK_ONLY)){
      const jsonPart = packed.slice(PREFIX_TALK_ONLY.length).trim()
      if(!jsonPart.startsWith('{')) return null
      const obj = JSON.parse(jsonPart)
      if(!obj || obj.v!==1 || !Array.isArray(obj.t)) return null
      const restored=[]
      for(const entry of obj.t){
        if(!Array.isArray(entry) || entry.length!==2) continue
        const [r,text] = entry
        if(r==='u') restored.push({ role:'user', content: text })
        else if(r==='a') restored.push({ role:'assistant', content: JSON.stringify({ talk: text }) })
      }
      return restored
    }
    return null
  } catch(e){
    console.warn('[decompressChatHistory] failed', e)
    return null
  }
}

/**
 * Safe helper: auto choose raw JSON.parse fallback.
 * @param {string} raw maybe compressed or plain JSON array string
 */
export function parseMaybeCompressed(raw){
  if(!raw) return null
  if(isCompressedChat(raw)) return decompressChatHistory(raw)
  try { const arr = JSON.parse(raw); return Array.isArray(arr) ? arr : null } catch { return null }
}

// convenience export list (optional)
export default { compressChatHistory, decompressChatHistory, parseMaybeCompressed, isCompressedChat }
