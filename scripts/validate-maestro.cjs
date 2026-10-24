// Structural checks only. Device runs are required to validate UI selectors.
const fs = require('node:fs');
const path = require('node:path');
const YAML = require('yaml');
const root = path.resolve(__dirname, '..');
const flowRoot = path.join(root, 'playtests/maestro');
const files = [];
function scan(dir) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const file = path.join(dir, entry.name);
    if (entry.isDirectory()) scan(file);
    else if (entry.name.endsWith('.yaml')) files.push(file);
  }
}
scan(flowRoot);
const failures = [];
const edges = new Map();
let references = 0;
function fail(file, message) { failures.push(`${path.relative(root, file)}: ${message}`); }
function inspect(value, file) {
  if (!value || typeof value !== 'object') return;
  for (const [key, child] of Object.entries(value)) {
    if (key === 'runFlow' || key === 'runScript') {
      const relative = typeof child === 'string' ? child : child?.file;
      if (relative) {
        references++;
        const target = path.resolve(path.dirname(file), relative);
        if (!target.startsWith(flowRoot + path.sep)) fail(file, `reference leaves flow directory: ${relative}`);
        else if (!fs.existsSync(target)) fail(file, `missing reference: ${relative}`);
        else if (key === 'runFlow') edges.get(file).push(target);
      }
    }
    inspect(child, file);
  }
}
for (const file of files) {
  edges.set(file, []);
  const docs = YAML.parseAllDocuments(fs.readFileSync(file, 'utf8'));
  for (const doc of docs) for (const error of doc.errors) fail(file, error.message);
  if (docs.some(doc => doc.errors.length)) continue;
  const values = docs.map(doc => doc.toJSON());
  if (values.length !== 2 || values[0]?.appId !== '${APP_ID}' || !Array.isArray(values[1])) {
    fail(file, 'expected appId: ${APP_ID}, document separator, and command list');
    continue;
  }
  inspect(values[1], file);
}
function visit(file, stack) {
  if (stack.includes(file)) { fail(file, 'recursive runFlow cycle'); return; }
  for (const target of edges.get(file) || []) visit(target, [...stack, file]);
}
for (const file of files) visit(file, []);
const config = JSON.parse(fs.readFileSync(path.join(flowRoot, 'toolchain.json'), 'utf8'));
if (!config.appId.endsWith('.playtest')) failures.push('Toolchain must target isolated .playtest app.');
// Named suites form the supported graph; historical files still receive structure checks.
const manifest = JSON.parse(fs.readFileSync(path.join(flowRoot, 'suites.json'), 'utf8'));
const requiredSuites = ['smoke', 'campaign', 'focused', 'recovery'];
if (JSON.stringify(Object.keys(manifest.suites).sort()) !== JSON.stringify(requiredSuites.sort())) failures.push('Expected smoke, campaign, focused and recovery suite entrypoints.');
const supported = new Set();
function collect(file, visited = new Set()) {
  if (visited.has(file)) return visited;
  visited.add(file);
  for (const child of edges.get(file) || []) collect(child, visited);
  return visited;
}
for (const [name, suite] of Object.entries(manifest.suites)) {
  const file = path.resolve(flowRoot, suite.flow);
  if (!files.includes(file)) { failures.push(`Unknown suite entrypoint: ${name}: ${suite.flow}`); continue; }
  for (const member of collect(file)) supported.add(member);
}
const campaign = collect(path.resolve(flowRoot, manifest.suites.campaign.flow));
let campaignLaunches = 0;
for (const file of campaign) {
  const raw = fs.readFileSync(file, 'utf8');
  if (/Debug Win|Instant Win|💥 Win/i.test(raw.replace(/^\s*#.*$/gm, ''))) fail(file, 'campaign cannot use assisted combat');
  const commands = YAML.parseAllDocuments(raw)[1]?.toJSON();
  function checkCommands(value) {
    if (!value || typeof value !== 'object') return;
    if (value.launchApp) {
      campaignLaunches++;
      if (value.launchApp.clearState !== true) fail(file, 'campaign must start from fresh isolated app state');
    }
    if (value.tapOn?.id && /^(scenario-|title-scenarios)/.test(value.tapOn.id)) fail(file, 'continuous campaign cannot launch checkpoints');
    for (const child of Object.values(value)) checkCommands(child);
  }
  checkCommands(commands);
}
if (campaignLaunches !== 1) failures.push(`Continuous campaign must have exactly one app launch/reset; found ${campaignLaunches}.`);
// Scenario IDs are checked against the current catalog, not historical display titles.
const catalog = [
  'gameRuntime/src/main/kotlin/com/example/starborn/debug/DebugTestRegistry.kt',
  'gameRuntime/src/main/kotlin/com/example/starborn/debug/DebugCampaignScenarios.kt'
].map(file => fs.readFileSync(path.join(root, file), 'utf8')).join('\n');
for (const file of supported) {
  const docs = YAML.parseAllDocuments(fs.readFileSync(file, 'utf8'));
  function checkScenarios(value) {
    if (!value || typeof value !== 'object') return;
    const npc = value.env?.NPC;
    if (npc && !npc.includes('${')) {
      const npcs = JSON.parse(fs.readFileSync(path.join(root, 'app/src/main/assets/npcs.json'), 'utf8'));
      if (!npcs.some(entry => entry.id === npc)) fail(file, `unknown NPC ID: ${npc}`);
    }
    const action = value.env?.ACTION;
    if (action && !action.includes('${')) {
      const rooms = JSON.parse(fs.readFileSync(path.join(root, 'app/src/main/assets/rooms.json'), 'utf8'));
      if (!rooms.some(room => (room.actions?.some(entry => entry.name === action) || Object.values(room.special_exits || {}).includes(action)))) fail(file, `unknown room action: ${action}`);
    }
    const scenario = value.env?.SCENARIO;
    if (scenario && !scenario.includes('${') && !catalog.includes(`"${scenario}"`)) fail(file, `unknown scenario ID: ${scenario}`);
    for (const child of Object.values(value)) checkScenarios(child);
  }
  checkScenarios(docs[1]?.toJSON());
}
// Tutorial modal controls pause ATB and must remain tied to real continuation labels.
const tutorialFlow = YAML.parseAllDocuments(fs.readFileSync(path.join(flowRoot, 'world1/_shared/combat_tutorial.yaml'), 'utf8'))[1].toJSON();
const tutorialCopy = fs.readFileSync(path.join(root, 'gameRuntime/src/main/kotlin/com/example/starborn/feature/combat/presentation/CombatTutorialCopy.kt'), 'utf8');
for (const label of tutorialFlow[0].repeat.while.visible.split('|')) {
  if (!tutorialCopy.includes(JSON.stringify(label))) failures.push(`Combat tutorial continuation label drifted: ${label}`);
}
// Adaptive combat must still use real, current authored ability names.
const skills = JSON.parse(fs.readFileSync(path.join(root, 'app/src/main/assets/skills.json'), 'utf8'));
for (const name of ['Arc Tether', 'Hydraulic Kick', 'Blast Wave']) {
  if (!skills.some(skill => skill.name === name)) failures.push(`World 1 combat ability drifted: ${name}`);
}
// Counter-tune step counts must change when authored initial/target values change.
const puzzle = JSON.parse(fs.readFileSync(path.join(root, 'app/src/main/assets/tuning_puzzles.json'), 'utf8')).find(p => p.id === 'w1_echo_counter_tune');
const solution = YAML.parseAllDocuments(fs.readFileSync(path.join(flowRoot, 'world1/_shared/counter_tune.yaml'), 'utf8'))[1].toJSON();
const counts = solution.filter(command => command.repeat).map(command => command.repeat.times);
const expected = puzzle.sliders.map((dial, i) => (dial.target - dial.initial) / (i === 2 ? 5 : 1));
if (JSON.stringify(counts) !== JSON.stringify(expected)) failures.push('World 1 counter-tune solution has drifted from authored values.');
const checkpointManifest = JSON.parse(fs.readFileSync(path.join(root, 'app/src/debug/assets/playtest/world1/manifest.json'), 'utf8'));
const crypto = require('node:crypto');
const currentCatalogs = fs.readdirSync(path.join(root, 'app/src/main/assets')).filter(name => name.endsWith('.json')).sort();
if (JSON.stringify(currentCatalogs) !== JSON.stringify(Object.keys(checkpointManifest.sources).sort())) failures.push('Checkpoint source catalog membership changed; recapture earned states.');
for (const [name, digest] of Object.entries(checkpointManifest.sources)) {
  const source = fs.readFileSync(path.join(root, 'app/src/main/assets', name));
  if (crypto.createHash('sha256').update(source).digest('hex') !== digest) failures.push(`Earned checkpoints are stale: ${name}`);
}
for (const [id, checkpoint] of Object.entries(checkpointManifest.checkpoints)) {
  const snapshot = fs.readFileSync(path.join(root, 'app/src/debug/assets/playtest/world1', checkpoint.file));
  if (crypto.createHash('sha256').update(snapshot).digest('hex') !== checkpoint.sha256) failures.push(`Checkpoint changed without route evidence: ${id}`);
}
if (failures.length) { console.error(failures.join('\n')); process.exitCode = 1; }
else console.log(`Maestro structure passed: ${files.length} flows, ${references} references. ${supported.size} supported dependencies across four named suites. UI behavior requires device testing.`);
