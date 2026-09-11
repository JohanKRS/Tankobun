import test from 'node:test';
import assert from 'node:assert/strict';
import './runtime.js';
const runtime = globalThis.TankobunRuntime;
const fixture = `
const {load} = require('cheerio');
const {storage} = require('@libs/storage');
exports.default = {
 id:'fixture',version:'1.0.0',
 async parseChapter() {return '<p>Original fictional chapter.</p>';},
 async parseNovel(path) {return {path,name:'The Paper Observatory',chapters:[{path:'one',name:'1'}],totalPages:3};},
 async parsePage(path,page) {return {chapters:[{path:page,name:page}]};},
 async searchNovels(query,page) {
  storage.set('query',query);
  const $ = load('<article><a data-id="f">The Paper Observatory</a></article>');
  return $('article > a[data-id]').map((_,el)=>({name:$(el).text(),path:$(el).attr('data-id')})).get();
 }
};`;
test('compiled CommonJS plugin uses real selectors and persistent storage', async () => {
 const result = await runtime.run(fixture,'searchNovels',['paper',1]);
 assert.equal(result.value[0].name,'The Paper Observatory');
 assert.equal(result.storage.query.value,'paper');
});
test('all paginated chapters are returned once', async () => {
 const result=await runtime.run(fixture,'details',['book']);
 assert.deepEqual(result.value.chapters.map(x=>x.path),['one','2','3']);
});
test('unsupported modules fail explicitly', async () => {
 await assert.rejects(runtime.run("require('node:fs');"+fixture,'metadata',[]), /Unsupported LNReader module/);
});
test('fetch bridge preserves POST body, request and response headers', async () => {
 globalThis.TankobunNative={fetch(json) {
  const request=JSON.parse(json);
  assert.equal(request.method,'POST');
  assert.equal(atob(request.body),'hello');
  assert.equal(request.headers['x-fixture'],'test');
  runtime.reply({id:request.id,status:201,url:request.url,headers:{'content-type':'application/json'},body:btoa('{"ok":true}')});
 }};
 const code=`const {fetchApi}=require('@libs/fetch');exports.default={parseChapter:async()=>{const r=await fetchApi('https://example.invalid/fiction',{method:'POST',body:'hello',headers:{'X-Fixture':'test'}});return [r.status,await r.json()]}};`;
 assert.deepEqual((await runtime.run(code,'parseChapter',[])).value,[201,{ok:true}]);
});
test('expired data is removed, per-call storage remains isolated', async()=>{
 const code=`const {storage}=require('@libs/storage');exports.default={parseChapter:()=>{storage.set('expired',true,1);return storage.get('expired') || 'gone'}};`;
 const result=await runtime.run(code,'parseChapter',[],{});
 assert.equal(result.value,'gone');assert.deepEqual(result.storage,{});
});

test('community URL encoding module exposes encode and decode', async()=>{
 const code=`const {encode,decode}=require('urlencode');exports.default={parseChapter:()=>[encode('hello world'),decode('hello%20world')]};`;
 assert.deepEqual((await runtime.run(code,'parseChapter',[])).value,['hello%20world','hello world']);
});

test('fetchText returns an empty string for HTTP and network failures', async()=>{
 const code=`const {fetchText}=require('@libs/fetch');exports.default={parseChapter:()=>fetchText('https://example.invalid/fiction')};`;
 globalThis.TankobunNative={fetch(json){const request=JSON.parse(json);runtime.reply({id:request.id,status:503,url:request.url,headers:{},body:btoa('unavailable')});}};
 assert.equal((await runtime.run(code,'parseChapter',[])).value,'');
 globalThis.TankobunNative={fetch(json){runtime.reply({id:JSON.parse(json).id,error:'offline'});}};
 assert.equal((await runtime.run(code,'parseChapter',[])).value,'');
});

test('popular and latest requests receive the plugin filter defaults', async()=>{
 const code=`exports.default={filters:{status:{type:'Picker',value:'ongoing'}},parseChapter:()=>'',popularNovels:(page,options)=>[page,options]};`;
 assert.deepEqual((await runtime.run(code,'popularNovels',[1])).value,[1,{showLatestNovels:false,filters:{status:{type:'Picker',value:'ongoing'}}}]);
 assert.equal((await runtime.run(code,'popularNovels',[2,{showLatestNovels:true}])).value[1].showLatestNovels,true);
});

test('settings defaults and saved values are available to class initializers', async()=>{
 const code=`const {storage}=require('@libs/storage');exports.default=new class{quality=storage.get('quality');pluginSettings={quality:{type:'Select',value:'high'}};parseChapter(){return this.quality}};`;
 const first=await runtime.run(code,'parseChapter',[]);
 assert.equal(first.value,'high');
 assert.equal((await runtime.run(code,'parseChapter',[],first.storage,{quality:'low'})).value,'low');
});

test('website storage follows the plugin origin, including configured source URLs', async()=>{
 const code=`const {storage,localStorage,sessionStorage}=require('@libs/storage');exports.default=new class{site=storage.get('url')||'https://example.invalid';cached=localStorage.get()?.marker;parseChapter(){return [this.cached,sessionStorage.get()?.page]}};`;
 const web={site:'https://example.invalid',origins:{'https://example.invalid':{local:{marker:'source'},session:{page:'chapter'}},'https://alternate.invalid':{local:{marker:'alternate'},session:{page:'other'}}}};
 assert.deepEqual((await runtime.run(code,'parseChapter',[],{},{},web)).value,['source','chapter']);
 assert.deepEqual((await runtime.run(code,'parseChapter',[],{},{url:'https://alternate.invalid'},web)).value,['alternate','other']);
 assert.deepEqual((await runtime.run(code,'parseChapter',[],{},{url:'https://unknown.invalid'},web)).value,[undefined,undefined]);
});
