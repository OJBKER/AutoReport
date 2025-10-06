// Simple auto-resize textarea directive
// Usage: v-autoresize or v-autoresize="{ max: 320 }"
// Will adjust height on:
//  - mount
//  - input event
//  - value update (via component updates)

function resize(el, max){
  if(!el) return;
  el.style.height = 'auto';
  const h = el.scrollHeight;
  el.style.height = Math.min(h, max || Infinity) + 'px';
}

export default {
  mounted(el, binding){
    const opts = binding.value || {};
    const max = typeof opts === 'number' ? opts : (opts.max || opts.maxHeight || 0);
    el.__autoResizeMax = max > 0 ? max : Infinity;
    el.__autoResizeHandler = () => resize(el, el.__autoResizeMax);
    // Listen to user input
    el.addEventListener('input', el.__autoResizeHandler);
    // Initial pass after potential async content fill
    requestAnimationFrame(el.__autoResizeHandler);
  },
  updated(el){
    // When v-model value changes programmatically
    if(el.__autoResizeHandler) el.__autoResizeHandler();
  },
  unmounted(el){
    if(el.__autoResizeHandler){
      el.removeEventListener('input', el.__autoResizeHandler);
      delete el.__autoResizeHandler;
    }
    delete el.__autoResizeMax;
  }
};
