import { ref, onBeforeUnmount } from 'vue'
/**
 * useToast
 * 统一的轻量提示组件逻辑。
 * 用法示例：
 *   import { useToast } from '@/composables/useToast'
 *   const { show, message, toast, close } = useToast()
 *   toast('保存成功')
 */
export function useToast(defaultDuration = 2600) {
  const show = ref(false)
  const message = ref('')
  let timer = null

  function toast(msg, duration = defaultDuration) {
    message.value = msg
    show.value = true
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => { show.value = false }, duration)
  }

  function close() {
    show.value = false
    if (timer) { clearTimeout(timer); timer = null }
  }

  onBeforeUnmount(() => { if (timer) clearTimeout(timer) })

  return { show, message, toast, close }
}
