import { ref, onMounted, onBeforeUnmount } from 'vue'
/**
 * Generic dropdown controller.
 * @param {Object} opts
 * @param {Ref<HTMLElement|null>} opts.wrapperRef - container element ref.
 * @param {Function} [opts.beforeOpen] - return false to cancel open.
 * @param {Function} [opts.onOpen] - called after show set true.
 * @param {Function} [opts.onClose] - called after show set false.
 * @param {boolean} [opts.closeOnEsc=true] - close when pressing ESC.
 */
export function useDropdown(opts={}){
  const { wrapperRef, beforeOpen, onOpen, onClose, closeOnEsc=true } = opts
  const show = ref(false)
  function open(){
    if(show.value) return
    if(beforeOpen && beforeOpen()===false) return
    show.value = true
    onOpen && onOpen()
  }
  function close(){
    if(!show.value) return
    show.value = false
    onClose && onClose()
  }
  function toggle(){ show.value ? close() : open() }
  function onDocClick(e){
    if(!show.value) return
    const el = wrapperRef?.value
    if(el && !el.contains(e.target)) close()
  }
  function onKey(e){ if(closeOnEsc && e.key==='Escape' && show.value) close() }
  onMounted(()=>{ document.addEventListener('click', onDocClick); document.addEventListener('keydown', onKey) })
  onBeforeUnmount(()=>{ document.removeEventListener('click', onDocClick); document.removeEventListener('keydown', onKey) })
  return { show, open, close, toggle }
}
export default useDropdown
