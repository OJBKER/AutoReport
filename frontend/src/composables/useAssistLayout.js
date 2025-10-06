import { ref } from 'vue'
/**
 * useAssistLayout
 * 管理组件块顺序与布局模式，并持久化 localStorage。
 * 用法示例：
 *   const { blocks, layoutMode, load, toggle, onBlocksReordered } = useAssistLayout()
 *   onMounted(load)
 *   <div v-for="b in blocks" :key="b.id">...</div>
 */
export function useAssistLayout(options = {}) {
  const LS_KEY_ORDER = options.keyOrder || 'assistBlocksOrder'
  const LS_KEY_MODE = options.keyMode || 'assistLayoutMode'

  const blocks = ref([
    { id: 'viewer', title: '模板视图', type: 'viewer' },
    { id: 'deepseek', title: 'Deepseek 测试', type: 'deepseek' },
    { id: 'docgen', title: '文档生成', type: 'docgen' }
  ])
  const layoutMode = ref('vertical')

  function load() {
    try {
      const order = JSON.parse(localStorage.getItem(LS_KEY_ORDER) || '[]')
      if (Array.isArray(order) && order.length) {
        const map = new Map(blocks.value.map(b => [b.id, b]))
        const arr = []
        order.forEach(i => { if (map.has(i)) arr.push(map.get(i)) })
        blocks.value.forEach(b => { if (!order.includes(b.id)) arr.push(b) })
        if (arr.length) blocks.value = arr
      }
    } catch (_) { }
    try {
      const m = localStorage.getItem(LS_KEY_MODE)
      if (m === 'horizontal' || m === 'vertical') layoutMode.value = m
    } catch (_) { }
  }

  function persist() {
    try { localStorage.setItem(LS_KEY_ORDER, JSON.stringify(blocks.value.map(b => b.id))) } catch (_) { }
    try { localStorage.setItem(LS_KEY_MODE, layoutMode.value) } catch (_) { }
  }

  function toggle() {
    layoutMode.value = layoutMode.value === 'vertical' ? 'horizontal' : 'vertical'
    persist()
  }

  function onBlocksReordered() {
    // 拖拽指令触发时列表已经变更，这里只需持久化
    persist()
  }

  return { blocks, layoutMode, load, persist, toggle, onBlocksReordered }
}
