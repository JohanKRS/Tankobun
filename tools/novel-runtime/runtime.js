import { load } from 'cheerio';
import { Parser } from 'htmlparser2';
import dayjs from 'dayjs';
import * as urlencode from 'urlencode';
import { gcm } from '@noble/ciphers/aes.js';
import { utf8ToBytes, bytesToUtf8 } from '@noble/ciphers/utils.js';
import protobuf from 'protobufjs';
import { transformChapter } from './chapter.js';

const pending = new Map();
let sequence = 0;
const bytes64 = bytes => { let s = ''; for (const b of bytes) s += String.fromCharCode(b); return btoa(s); };
const from64 = value => Uint8Array.from(atob(value), c => c.charCodeAt(0));
async function fetchApi(input, init = {}, encoding) {
  const request = new Request(input, init);
  const body = ['GET', 'HEAD'].includes(request.method) ? null : bytes64(new Uint8Array(await request.arrayBuffer()));
  const id = ++sequence;
  const payload = await new Promise((resolve, reject) => {
    pending.set(id, { resolve, reject });
    TankobunNative.fetch(JSON.stringify({ id, url: request.url, method: request.method, headers: Object.fromEntries(request.headers), body, encoding }));
  });
  const bytes = from64(payload.body);
  const response = new Response([204,205,304].includes(payload.status) ? null : bytes, { status: payload.status, headers: payload.headers });
  Object.defineProperty(response, 'url', { value: payload.url });
  if (encoding) response.text = async () => payload.text;
  return response;
}
const fetchText = async (url, init, encoding) => {
  try {
    const response = await fetchApi(url, init, encoding);
    return response.ok ? await response.text() : '';
  } catch { return ''; }
};
async function fetchProto(spec, url, init = {}) {
  const root = protobuf.parse(spec.proto).root;
  const requestType = root.lookupType(spec.requestType);
  const invalid = requestType.verify(spec.requestData || {});
  if (invalid) throw new Error(invalid);
  const encoded = requestType.encode(requestType.create(spec.requestData || {})).finish();
  const framed = new Uint8Array(encoded.length + 5);
  new DataView(framed.buffer).setUint32(1, encoded.length); framed.set(encoded, 5);
  const response = await fetchApi(url, { ...init, method: init.method || 'POST', body: framed });
  const result = new Uint8Array(await response.arrayBuffer());
  const length = new DataView(result.buffer).getUint32(1);
  if (result[0] !== 0 || length > result.length - 5) throw new Error('Unsupported protobuf response');
  const type = root.lookupType(spec.responseType);
  return type.toObject(type.decode(result.slice(5, length + 5)));
}
const NovelStatus = { Unknown:'Unknown', Ongoing:'Ongoing', Completed:'Completed', Licensed:'Licensed', PublishingFinished:'Publishing Finished', Cancelled:'Cancelled', OnHiatus:'On Hiatus', STUB:'STUB', Inactive:'Inactive' };
const FilterTypes = { TextInput:'Text', Picker:'Picker', CheckboxGroup:'Checkbox', Switch:'Switch', ExcludableCheckboxGroup:'XCheckbox' };
globalThis.TankobunRuntime = {
  reply(payload) {
    const call = pending.get(payload.id); if (!call) return;
    pending.delete(payload.id);
    payload.error ? call.reject(new Error(payload.error)) : call.resolve(payload);
  },
  async run(code, operation, args, saved = {}, configured = {}, web = {}, chapterAssets = {}) {
    const items = { ...saved };
    const storage = {
      set(key, value, expires) { items[key] = { value, created: new Date().toISOString(), ...(expires == null ? {} : { expires: +expires }) }; },
      get(key, raw = false) { const item = items[key]; if (!item) return; if (item.expires != null && item.expires < Date.now()) { delete items[key]; return; } return raw ? item : item.value; },
      delete(key) { delete items[key]; }, clearAll() { for (const key of Object.keys(items)) delete items[key]; }, getAllKeys() { return Object.keys(items); },
    };
    for (const [key, value] of Object.entries(configured)) storage.set(key, value);
    let plugin;
    const browserStorage = () => {
      try { return web.origins?.[new URL(plugin?.site || web.site).origin]; } catch { return undefined; }
    };
    const modules = {
      cheerio:{ load }, htmlparser2:{ Parser }, dayjs, urlencode:{ encode:urlencode.encode, decode:urlencode.decode },
      '@libs/novelStatus':{ NovelStatus }, '@libs/filterInputs':{ FilterTypes },
      '@libs/fetch':{ fetchApi, fetchText, fetchProto }, '@libs/isAbsoluteUrl':{ isUrlAbsolute: value => /^[a-z][a-z\d+.-]*:/i.test(value) },
      '@libs/defaultCover':{ defaultCover:'' }, '@libs/aes':{ gcm }, '@libs/utils':{ utf8ToBytes, bytesToUtf8 },
      '@libs/storage':{ storage, localStorage:{ get:() => browserStorage()?.local }, sessionStorage:{ get:() => browserStorage()?.session } },
    };
    const require = name => { if (!(name in modules)) throw new Error('Unsupported LNReader module: ' + name); return modules[name]; };
    const instantiate = () => new Function('require', 'module', 'const exports=module.exports;\n' + code + '\n;return exports.default;')(require, { exports:{} });
    plugin = instantiate();
    if (!plugin || typeof plugin.parseChapter !== 'function') throw new Error('Invalid LNReader plugin');
    let defaultsAdded = false;
    for (const [key, setting] of Object.entries(plugin.pluginSettings || {})) {
      if (storage.get(key) === undefined) { storage.set(key, setting.value); defaultsAdded = true; }
    }
    if (defaultsAdded || (web.site && plugin.site !== web.site)) plugin = instantiate();
    let value;
    if (operation === 'metadata') value = { id:plugin.id, name:plugin.name, version:plugin.version, site:plugin.site, imageRequestInit:plugin.imageRequestInit, pluginSettings:plugin.pluginSettings, webStorageUtilized:plugin.webStorageUtilized, hasResolveUrl:typeof plugin.resolveUrl === 'function' };
    else if (operation === 'details') {
      value = await plugin.parseNovel(args[0]);
      const count = args[1] === false ? 1 : Number(value.totalPages || 1);
      if (!Number.isFinite(count) || count > 500) throw new Error('Chapter list exceeds supported pagination limit');
      if (count > 1 && typeof plugin.parsePage !== 'function') throw new Error('Plugin is missing parsePage');
      for (let page = 2; page <= count; page++) {
        const next = await plugin.parsePage(args[0], String(page));
        value.chapters.push(...next.chapters);
      }
      value.chapters = [...new Map(value.chapters.map(chapter => [chapter.path, chapter])).values()];
    } else if (operation === 'popularNovels') {
      const filters = Object.fromEntries(Object.entries(plugin.filters || {}).map(([key, filter]) => [key, { type:filter.type, value:filter.value }]));
      value = await plugin.popularNovels(args[0], { showLatestNovels:false, filters, ...args[1] });
    } else {
      if (typeof plugin[operation] !== 'function') throw new Error('Unsupported LNReader operation: ' + operation);
      value = await plugin[operation](...args);
    }
    let changedWebsite;
    if (operation === 'parseChapter' && (chapterAssets.javascript || chapterAssets.css)) {
      globalThis.fetch = fetchApi;
      const url = plugin.resolveUrl ? await plugin.resolveUrl(args[0], false) : new URL(args[0], plugin.site || web.site).href;
      const website = browserStorage() || {local:{},session:{}};
      const transformed = await transformChapter(value, chapterAssets, { url, path:args[0], pluginId:plugin.id, website }, () => pending.size > 0);
      value = transformed.html;
      if (JSON.stringify(website) !== JSON.stringify(transformed.website)) changedWebsite = {origin:new URL(plugin.site || web.site).origin,...transformed.website};
    }
    return { value, storage:items, website:changedWebsite };
  },
};
