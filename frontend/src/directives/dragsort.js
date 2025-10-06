// v-drag-sort directive
// Usage: v-drag-sort="{ list, key: 'id', storageKey: 'assistBlocksOrder', dragClass, overClass }"
// list: reactive array (passed from <script setup>)
// key: unique key property in each item
// storageKey: optional localStorage key to persist order
// Emits custom events via dispatch on element: 'drag-sort-update'

function applyPersist(list, keyField, storageKey){
  if(!storageKey) return;
  try{ const order = JSON.parse(localStorage.getItem(storageKey)||'[]'); if(Array.isArray(order)&&order.length){
    const map = new Map(list.map(i=>[i[keyField], i]));
    const reordered=[];
    order.forEach(k=>{ if(map.has(k)) reordered.push(map.get(k)); });
    list.forEach(it=>{ if(!order.includes(it[keyField])) reordered.push(it); });
    if(reordered.length) list.splice(0, list.length, ...reordered);
  }}catch(_){ }
}
function savePersist(list, keyField, storageKey){ if(!storageKey) return; try{ localStorage.setItem(storageKey, JSON.stringify(list.map(i=>i[keyField])))}catch(_){ } }

export default {
  mounted(el, binding){
    const opts = binding.value || {};
    const maybeList = opts.list;
    const isRef = maybeList && typeof maybeList === 'object' && 'value' in maybeList && Array.isArray(maybeList.value);
    const list = isRef ? maybeList.value : maybeList;
    if(!Array.isArray(list)){ console.warn('[v-drag-sort] list must be an array or ref(array)'); return }
    const keyField = opts.key || 'id';
    const storageKey = opts.storageKey || null;
    const dragClass = opts.dragClass || 'dragging';
    const overClass = opts.overClass || 'over';
    const syncDom = opts.syncDom !== false; // default true
  const edgeThreshold = typeof opts.edgeThreshold === 'number' ? opts.edgeThreshold : null; // px within edge to allow drag
  const requireGrabCursor = !!opts.requireGrabCursor; // only start drag if computed cursor is grab/grabbing
  const handlesSelector = typeof opts.handles === 'string' ? opts.handles : null; // CSS selector for drag handles

    applyPersist(list, keyField, storageKey);

  let draggingKey = null;

    function clearOver(){
      el.querySelectorAll('[data-drag-key].'+overClass).forEach(n=> n.classList.remove(overClass));
    }
    function handleDragStart(e){
      const block = e.target.closest('[data-drag-key]');
      if(!block) return;
      if(handlesSelector){
        const handle = e.target.closest(handlesSelector);
        if(!handle || !block.contains(handle)){
          e.preventDefault();
          e.stopPropagation();
          return;
        }
      }
      if(requireGrabCursor){
        const targetEl = e.target instanceof Element ? e.target : block;
        const cs = window.getComputedStyle(targetEl);
        const cursor = cs.cursor;
        const cursorOk = cursor && /grab|grabbing|move/i.test(cursor);
        if(!cursorOk){
          e.preventDefault();
          e.stopPropagation();
          return;
        }
      }
      if(edgeThreshold != null && !handlesSelector){
        const rect = block.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;
        const withinLeft = x <= edgeThreshold;
        const withinRight = (rect.width - x) <= edgeThreshold;
        const withinTop = y <= edgeThreshold;
        const withinBottom = (rect.height - y) <= edgeThreshold;
        if(!(withinLeft || withinRight || withinTop || withinBottom)){
          // Cancel drag if not from edge: allow text selection / normal interaction
          e.preventDefault();
          e.stopPropagation();
          return;
        }
      }
      draggingKey = block.getAttribute('data-drag-key');
      block.classList.add(dragClass);
      e.dataTransfer && e.dataTransfer.setData('text/plain', draggingKey);
    }
    function handleDragEnter(e){
      const block = e.target.closest('[data-drag-key]');
      if(!block) return;
      const key = block.getAttribute('data-drag-key');
      if(key && key !== draggingKey){
        block.classList.add(overClass);
      }
    }
    function handleDrop(e){
      e.preventDefault();
      const block = e.target.closest('[data-drag-key]');
      if(!block) return;
      const toKey = block.getAttribute('data-drag-key');
      if(!draggingKey || !toKey || draggingKey===toKey) return;
      const fromIndex = list.findIndex(i=> i[keyField]===draggingKey);
      const toIndexRaw = list.findIndex(i=> i[keyField]===toKey);
      if(fromIndex===-1 || toIndexRaw===-1) return;
      let toIndex = toIndexRaw;
      // Adjust index when moving downward so semantic ordering feels natural
      if(toIndex > fromIndex) toIndex = toIndex; // can tweak if wanting before/after semantics
      const [mv] = list.splice(fromIndex,1);
      list.splice(toIndex,0,mv);
      // Force reactivity for ref case (clone assignment)
      if(isRef){ maybeList.value = list.slice(); }
      // Fallback DOM sync (helps if framework reuse prevents visual move)
      if(syncDom){
        try {
          const childrenMap = {};
          Array.from(el.children).forEach(ch=>{
            if(ch.nodeType===1 && ch.hasAttribute('data-drag-key')){
              childrenMap[ch.getAttribute('data-drag-key')] = ch;
            }
          });
          list.forEach(item=>{
            const k = item[keyField];
            const node = childrenMap[k];
            if(node){ el.appendChild(node); }
          });
        } catch(domErr){ /* silent */ }
      }
      savePersist(list, keyField, storageKey);
      el.dispatchEvent(new CustomEvent('drag-sort-update', { detail: { order: list.map(i=>i[keyField]) }}));
      clearOver();
    }
    function handleDragEnd(){
      const actives = el.querySelectorAll('[data-drag-key].'+dragClass);
      actives.forEach(b=> b.classList.remove(dragClass));
      draggingKey=null; clearOver();
    }
    function preventDefault(e){ e.preventDefault(); }

    el.__dragSort = { handleDragStart, handleDragEnter, handleDrop, handleDragEnd, preventDefault };
    el.addEventListener('dragstart', handleDragStart);
    el.addEventListener('dragenter', handleDragEnter);
    el.addEventListener('dragover', preventDefault);
    el.addEventListener('drop', handleDrop);
    el.addEventListener('dragend', handleDragEnd);
  },
  updated(){},
  unmounted(el){
    const ds = el.__dragSort; if(!ds) return;
    el.removeEventListener('dragstart', ds.handleDragStart);
    el.removeEventListener('dragenter', ds.handleDragEnter);
    el.removeEventListener('dragover', ds.preventDefault);
    el.removeEventListener('drop', ds.handleDrop);
    el.removeEventListener('dragend', ds.handleDragEnd);
    delete el.__dragSort;
  }
};
