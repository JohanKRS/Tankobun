// LNReader chapter scripts operate on this DOM before Tankobun renders native text.
export async function transformChapter(html, assets, context, hasPendingRequests) {
  const browser = JSON.parse(JSON.stringify(context.website || {local:{},session:{}}));
  const storage = values => new Proxy({
    get length() { return Object.keys(values).length; },
    key: index => Object.keys(values)[index] ?? null,
    getItem: key => Object.prototype.hasOwnProperty.call(values, key) ? String(values[key]) : null,
    setItem: (key, value) => { Object.defineProperty(values, String(key), {value:String(value),writable:true,enumerable:true,configurable:true}); },
    removeItem: key => { delete values[key]; },
    clear: () => { for (const key of Object.keys(values)) delete values[key]; },
  }, {
    get: (target, key) => key in target ? Reflect.get(target,key) : values[key],
    set: (target, key, value) => { target.setItem(key,value); return true; },
    deleteProperty: (target, key) => { target.removeItem(key); return true; },
    ownKeys: () => Object.keys(values),
    getOwnPropertyDescriptor: (_, key) => Object.prototype.hasOwnProperty.call(values,key) ? {value:values[key],configurable:true,enumerable:true,writable:true} : undefined,
  });
  Object.defineProperty(globalThis, 'localStorage', {value:storage(browser.local),configurable:true});
  Object.defineProperty(globalThis, 'sessionStorage', {value:storage(browser.session),configurable:true});
  const root = document.createElement('div');
  root.id = 'LNReader-chapter';
  const inert = new DOMParser().parseFromString(String(html), 'text/html');
  inert.querySelectorAll('script,iframe,object,embed,base,link').forEach(node => node.remove());
  inert.querySelectorAll('*').forEach(node => {
    for (const attr of [...node.attributes]) if (/^on/i.test(attr.name)) node.removeAttribute(attr.name);
  });
  root.innerHTML = inert.body.innerHTML;
  document.body.replaceChildren(root);
  const base = document.createElement('base'); base.href = context.url; document.head.append(base);
  if (assets.css) { const style = document.createElement('style'); style.textContent = assets.css; document.head.append(style); }
  globalThis.initialReaderConfig = { chapter:{ path:context.path }, novel:{ pluginId:context.pluginId }, readerSettings:{} };
  const originalTimeout = globalThis.setTimeout;
  const originalClear = globalThis.clearTimeout;
  const originalInterval = globalThis.setInterval;
  const originalClearInterval = globalThis.clearInterval;
  const intervals = new Set();
  const timers = new Set();
  let changedAt = Date.now();
  let failure;
  const onError = event => { failure = event.error || new Error(event.message || 'Chapter script failed'); event.preventDefault(); };
  const onRejection = event => { failure = event.reason || new Error('Chapter script failed'); event.preventDefault(); };
  const observer = new MutationObserver(() => { changedAt = Date.now(); });
  observer.observe(document.documentElement, { childList:true, subtree:true, characterData:true, attributes:true });
  globalThis.setTimeout = (callback, delay, ...args) => {
    const id = originalTimeout(() => {
      timers.delete(id);
      try { typeof callback === 'function' ? callback(...args) : (0, eval)(callback); } catch (error) { failure = error; }
    }, delay);
    timers.add(id); return id;
  };
  globalThis.clearTimeout = id => { timers.delete(id); originalClear(id); };
  globalThis.setInterval = (callback, delay, ...args) => {
    const id = originalInterval(() => {
      timers.delete(id);
      try { typeof callback === 'function' ? callback(...args) : (0, eval)(callback); } catch (error) { failure = error; }
    }, delay);
    intervals.add(id); timers.add(id); return id;
  };
  globalThis.clearInterval = id => { timers.delete(id); intervals.delete(id); originalClearInterval(id); };
  globalThis.addEventListener('error', onError);
  globalThis.addEventListener('unhandledrejection', onRejection);
  try {
    if (assets.javascript) {
      const script = document.createElement('script'); script.textContent = assets.javascript; document.body.append(script);
      // Scripts are injected after the real blank document has loaded.
      document.dispatchEvent(new Event('DOMContentLoaded', { bubbles:true }));
      globalThis.dispatchEvent(new Event('load'));
    }
    const deadline = Date.now() + 15_000;
    while (true) {
      await new Promise(resolve => originalTimeout(resolve, 50));
      if (failure) throw failure;
      if (!timers.size && !hasPendingRequests() && Date.now() - changedAt >= 200) break;
      if (Date.now() >= deadline) throw new Error('Chapter script did not finish');
    }
    const chapter = document.getElementById('LNReader-chapter');
    if (!chapter) throw new Error('Chapter script removed the reading container');
    // Preserve source CSS that hides non-reading elements, without imposing its typography.
    [...chapter.querySelectorAll('*')].reverse().forEach(node => {
      const style = getComputedStyle(node);
      if (node.hidden || style.display === 'none' || style.visibility === 'hidden') node.remove();
    });
    chapter.querySelectorAll('script,style,iframe,object,embed,link').forEach(node => node.remove());
    if (!chapter.textContent.trim()) throw new Error('Chapter script returned empty text');
    return {html:chapter.innerHTML,website:browser};
  } finally {
    observer.disconnect();
    globalThis.setTimeout = originalTimeout; globalThis.clearTimeout = originalClear;
    globalThis.setInterval = originalInterval; globalThis.clearInterval = originalClearInterval;
    for (const timer of intervals) originalClearInterval(timer);
    for (const timer of timers) originalClear(timer);
    globalThis.removeEventListener('error', onError);
    globalThis.removeEventListener('unhandledrejection', onRejection);
  }
}
