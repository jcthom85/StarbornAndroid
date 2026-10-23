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
if (failures.length) { console.error(failures.join('\n')); process.exitCode = 1; }
else console.log(`Maestro structure passed: ${files.length} flows, ${references} references. UI behavior requires device testing.`);
