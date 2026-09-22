export const $=(s,p=document)=>p.querySelector(s);export const $$=(s,p=document)=>[...p.querySelectorAll(s)];
export const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
export const num=v=>Number(String(v??0).replace(/,/g,''))||0;
export const uid=p=>`${p}-${Date.now()}-${Math.random().toString(36).slice(2,7)}`;
export function download(name,text,type='text/plain'){const a=document.createElement('a');a.href=URL.createObjectURL(new Blob([text],{type}));a.download=name;a.click();setTimeout(()=>URL.revokeObjectURL(a.href),500)}
export function csvRows(text){const rows=[];let row=[],cell='',q=false;for(let i=0;i<text.length;i++){const c=text[i],n=text[i+1];if(c==='"'&&q&&n==='"'){cell+='"';i++;continue}if(c==='"'){q=!q;continue}if(c===','&&!q){row.push(cell);cell='';continue}if((c==='\n'||c==='\r')&&!q){if(c==='\r'&&n==='\n')i++;row.push(cell);if(row.some(x=>x.trim()))rows.push(row);row=[];cell='';continue}cell+=c}row.push(cell);if(row.some(x=>x.trim()))rows.push(row);return rows}
export function rowsToObjects(rows){if(!rows.length)return [];const headers=rows[0].map(x=>String(x).trim());return rows.slice(1).map(r=>Object.fromEntries(headers.map((h,i)=>[h,r[i]??''])))}
export function fmt(n){return new Intl.NumberFormat().format(num(n))}
