// Read-only mirror of Starborn's authored condition vocabulary.
export function evaluateCondition(condition, state){
  if(!condition)return true;
  const tokens=Array.isArray(condition)?condition:condition.split(',').map(x=>x.trim()).filter(Boolean);
  return tokens.every(token=>{const [type,...rest]=token.split(':');const value=rest.join(':');switch(type.toLowerCase()){
    case 'milestone':case 'milestone_set':return state.milestones.has(value);
    case 'milestone_not_set':return !state.milestones.has(value);
    case 'quest':case 'quest_active':return state.activeQuests.has(value);
    case 'quest_completed':return state.completedQuests.has(value);
    case 'quest_stage':{const p=value.split(':');return state.questStages?.get(p[0])===p[1]}
    case 'quest_stage_not':{const p=value.split(':');return state.questStages?.get(p[0])!==p[1]}
    case 'quest_task_completed':case 'task_completed':return state.completedTasks?.has(value);
    case 'quest_task_not_completed':case 'task_not_completed':return !state.completedTasks?.has(value);
    case 'quest_failed':return state.failedQuests.has(value);
    case 'quest_not_started':return !state.activeQuests.has(value)&&!state.completedQuests.has(value)&&!state.failedQuests.has(value);
    case 'event_completed':return state.events.has(value);
    case 'event_not_completed':return !state.events.has(value);
    case 'tutorial_completed':return state.tutorials.has(value);
    case 'tutorial_not_completed':return !state.tutorials.has(value);
    case 'item':case 'has_item':case 'item_in_inventory':return state.items.has(value);
    case 'item_not':case 'has_not_item':case 'not_has_item':return !state.items.has(value);
    default:return {unknown:true,token};
  }});
}
export function explainCondition(condition,state){const result=evaluateCondition(condition,state);return {met:result===true,unknown:Array.isArray(result)?result:[],condition};}
