const KEY='rfid-system-v2';
const seed={site:'Meadowdale HQ',asnLines:[],inventory:[],locations:[],cycleCounts:[],investigations:[],events:[],settings:{varianceTolerance:0,readerPower:2200,beeper:'MEDIUM',bridgeUrl:'ws://127.0.0.1:8787/rfid/',autoSync:true},devices:{bridge:'offline',readers:[]}};
let state=load();
function load(){try{const raw=localStorage.getItem(KEY);return raw?merge(seed,JSON.parse(raw)):structuredClone(seed)}catch{return structuredClone(seed)}}
function merge(a,b){const o={...a,...b};for(const k of Object.keys(a)){if(a[k]&&typeof a[k]==='object'&&!Array.isArray(a[k]))o[k]=merge(a[k],b?.[k]||{});else if(!(k in b))o[k]=a[k]}return o}
function save(){localStorage.setItem(KEY,JSON.stringify(state));window.dispatchEvent(new CustomEvent('rfid-state',{detail:state}))}
export const store={
 get(){return state}, set(patch){state=merge(state,patch);save();return state}, reset(){state=structuredClone(seed);save();return state},
 addEvent(type,message,meta={}){state.events.unshift({id:crypto.randomUUID(),time:new Date().toISOString(),type,message,...meta});state.events=state.events.slice(0,500);save()},
 upsertInventory(items){const map=new Map(state.inventory.map(x=>[x.epc,x]));for(const item of items)map.set(item.epc,{...(map.get(item.epc)||{}),...item});state.inventory=[...map.values()];save()},
 addCycleCount(c){state.cycleCounts.unshift(c);save()}, addInvestigation(c){state.investigations.unshift(c);save()}
};
