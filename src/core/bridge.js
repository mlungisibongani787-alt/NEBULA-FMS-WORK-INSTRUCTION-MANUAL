let socket=null;let listeners=new Set();let lastStatus={state:'offline',message:'Bridge not connected'};
export function bridgeStatus(){return lastStatus}
export function onBridge(fn){listeners.add(fn);return()=>listeners.delete(fn)}
function emit(v){lastStatus=v;listeners.forEach(f=>f(v));window.dispatchEvent(new CustomEvent('rfid-bridge',{detail:v}))}
export async function connectBridge(url){disconnectBridge();return new Promise((resolve)=>{try{socket=new WebSocket(url.replace(/^http/,'ws'));socket.onopen=()=>{emit({state:'online',message:'Bridge connected',url});socket.send(JSON.stringify({type:'hello',client:'RFID SYSTEM',version:2}));resolve(true)};socket.onmessage=e=>{try{const m=JSON.parse(e.data);window.dispatchEvent(new CustomEvent('rfid-message',{detail:m}));if(m.type==='status')emit(m); }catch{}};socket.onerror=()=>{emit({state:'error',message:'Bridge connection failed',url});resolve(false)};socket.onclose=()=>emit({state:'offline',message:'Bridge disconnected',url})}catch(e){emit({state:'error',message:e.message,url});resolve(false)}})}
export function disconnectBridge(){try{socket?.close()}catch{}socket=null;emit({state:'offline',message:'Bridge disconnected'})}
export function bridgeCommand(command,payload={}){if(!socket||socket.readyState!==1)throw new Error('RFID bridge is not connected');socket.send(JSON.stringify({type:'command',command,payload}))}
