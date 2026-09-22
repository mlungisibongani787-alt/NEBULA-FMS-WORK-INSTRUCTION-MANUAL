import {WebSocketServer} from 'ws';
const port=Number(process.env.PORT||8787);const wss=new WebSocketServer({port});const clients=new Set();let readers=[];
const send=(ws,m)=>{if(ws.readyState===1)ws.send(JSON.stringify(m))};
function broadcast(m){for(const c of clients)send(c,m)}
wss.on('connection',ws=>{clients.add(ws);send(ws,{type:'status',state:'online',message:'RFID bridge server online'});ws.on('message',raw=>{try{const m=JSON.parse(raw);if(m.type==='hello')send(ws,{type:'status',state:'online',message:'Web bridge connected'});if(m.type==='reader-status'){readers=m.readers||[];broadcast({type:'readers',readers})}if(m.type==='tag'){broadcast(m)}if(m.type==='status')broadcast(m);if(m.type==='command')broadcast(m)}catch(e){send(ws,{type:'status',state:'error',message:e.message})}});ws.on('close',()=>clients.delete(ws))});console.log(`RFID SYSTEM bridge server listening on ws://0.0.0.0:${port}`);
