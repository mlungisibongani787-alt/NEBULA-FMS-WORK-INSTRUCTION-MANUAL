export const demoData={
  site:'Meadowdale HQ',
  readers:[],
  asns:[{asn:'0000000108',owner:'NIKE',po:'4503450687',lpn:'LPN0000001619',location:'STAGE',sku:'651995-340-XL',description:'Retail product',expected:12,received:0,status:'New',epcs:Array.from({length:12},(_,i)=>`EPC-${String(i+1).padStart(4,'0')}`)}],
  inventory:[],
  locations:[{id:'STAGE',name:'STAGE',zone:'Inbound',expected:0},{id:'A01-01-01',name:'A01-01-01',zone:'A',expected:0},{id:'A01-01-02',name:'A01-01-02',zone:'A',expected:0}],
  cycleCounts:[],
  investigations:[],
  events:[]
};

export function makeEpcs(sku,qty){return Array.from({length:Math.max(0,Number(qty)||0)},(_,i)=>`EPC-${sku.replace(/[^A-Za-z0-9]/g,'').slice(0,8)}-${String(i+1).padStart(5,'0')}`)}
