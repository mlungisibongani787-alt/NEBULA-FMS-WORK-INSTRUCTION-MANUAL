const routes={};
export function register(route,loader){routes[route]=loader}
export async function go(route){const target=routes[route]?route:'dashboard';if(location.hash.slice(1)!==target)location.hash=target;await renderRoute(target)}
export async function renderRoute(route){const target=routes[route]?route:'dashboard';const app=document.querySelector('#app');app.innerHTML=window.RFID.shell(target);const mod=await routes[target]();const content=document.querySelector('#content');content.innerHTML=mod.render();mod.bind?.();window.RFID.bindGlobal();}
export function startRouter(){window.addEventListener('hashchange',()=>renderRoute(location.hash.slice(1)||'dashboard'));renderRoute(location.hash.slice(1)||'dashboard')}
