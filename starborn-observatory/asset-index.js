// Shared read-only asset index for Observatory workspaces.
export async function createAssetIndex(base='../app/src/main/assets/') {
  const files={worlds:'worlds',hubs:'hubs',nodes:'hub_nodes',rooms:'rooms',quests:'quests',enemies:'enemies',items:'items',skills:'skills',characters:'characters',statuses:'statuses',events:'events',milestones:'milestones',cinematics:'cinematics'};
  const index={};
  await Promise.all(Object.entries(files).map(async([type,file])=>{try{index[type]=await fetch(base+file+'.json').then(r=>r.json())}catch{index[type]=[]}}));
  const records=()=>Object.entries(index).flatMap(([type,items])=>(Array.isArray(items)?items:[]).map(data=>({type,id:data.id||'',name:data.title||data.name||data.id||'Untitled',data})));
  const relationType=(from,to)=>{const rules={quests:{rooms:'occurs_in',milestones:'sets_or_requires',enemies:'encounters',items:'rewards',cinematics:'launches'},rooms:{enemies:'contains',items:'contains',quests:'hosts',cinematics:'presents'},enemies:{items:'drops',skills:'uses'},skills:{statuses:'applies'},cinematics:{audio:'presents_with'}};return rules[from]?.[to]||'references'};
  const links=()=>{const all=records(),byId=new Map(all.map(x=>[x.id,x]));return all.flatMap(source=>{const found=new Set();const walk=v=>{if(typeof v==='string'&&byId.has(v)&&v!==source.id)found.add(v);else if(Array.isArray(v))v.forEach(walk);else if(v&&typeof v==='object')Object.values(v).forEach(walk)};walk(source.data);return [...found].map(target=>({from:source,to:byId.get(target),type:relationType(source.type,target.type)}))})};
  return {records,links,relationType,data:index};
}
