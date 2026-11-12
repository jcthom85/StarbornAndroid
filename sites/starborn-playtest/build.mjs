import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const home = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(home, '../..');
const integrated = process.argv.includes('--integrated-world3');
const outputDirectory = path.join(home, 'dist', ...(integrated ? ['world-3-integrated'] : []));
const guides = [];
const integratedStepIds = integrated
  ? JSON.parse(await readFile(path.join(home, 'src/world3-integrated-step-ids.json'), 'utf8')) : {};
for (const world of (integrated ? [3] : [1, 2, 3, 4, 5])) {
  const guideFile = integrated ? 'WORLD_3_INTEGRATED_COMPLETIONIST_WALKTHROUGH.md' : `WORLD_${world}_COMPLETIONIST_WALKTHROUGH.md`;
  const source = await readFile(path.join(root, 'docs/playtest/walkthroughs', guideFile), 'utf8');
  const sections = [];
  let section = { title: 'Before you start', blocks: [] };
  let paragraph = [];
  let nextStep = 0;
  const flush = () => {
    if (!paragraph.length) return;
    const text = paragraph.join(' ');
    const heading = /^(#{3,4}) /.exec(text);
    const step = /^(\d+)\. /.exec(text);
    const checklist = world >= 3 && /^- \[ \] /.test(text);
    if (step) nextStep = Math.max(nextStep, Number(step[1]));
    const stableKey = section.title.replace(/^\d+\. /, '') + '\n' + text.replace(/^\d+\. /, '');
    const id = (integrated && step ? integratedStepIds[stableKey] : null)
      || createHash('sha256').update(`${section.title}\n${text}`).digest('hex').slice(0, 16);
    section.blocks.push({ id, text: heading ? text.slice(heading[0].length) : text, heading: heading?.[1].length || 0, step: step ? Number(step[1]) : checklist ? ++nextStep : null });
    paragraph = [];
  };
  for (const raw of source.split(/\r?\n/)) {
    const line = raw.trimEnd();
    if (line.startsWith('# ')) continue;
    if (line.startsWith('## ')) {
      flush();
      if (section.blocks.length) sections.push(section);
      section = { title: line.slice(3), blocks: [] };
    } else if (!line.trim()) flush();
    else if (/^(#{3,4} |\d+\. |- )/.test(line)) { flush(); paragraph.push(line); flush(); }
    else paragraph.push(line);
  }
  flush();
  if (section.blocks.length) sections.push(section);
  guides.push({ world, ...(world === 3 && !integrated ? { progressKey: '3-rebuilt-2026-11-07' } : {}), revision: createHash('sha256').update(source).digest('hex').slice(0, 12), sections });
}
// Preserve published chapters and checklist IDs when adding later worlds.
const preserveIndex = process.argv.indexOf('--preserve-existing');
if (!integrated && preserveIndex >= 0) {
  const existingHtml = await readFile(process.argv[preserveIndex + 1], 'utf8');
  const existing = JSON.parse(existingHtml.match(/const GUIDES = (.*?);\n/)[1]);
  for (const prior of existing.filter(g => g.world <= 4)) {
    const index = guides.findIndex(g => g.world === prior.world);
    if (index >= 0) guides[index] = prior;
  }
}
const data = JSON.stringify(guides).replaceAll('<', '\\u003c');
let template = await readFile(path.join(home, 'src/index.html'), 'utf8');
let app = await readFile(path.join(home, 'src/app.js'), 'utf8');
if (integrated) {
  app = app.replace("const STORE = 'starborn-companion-v1';", "const STORE = 'starborn-companion-world3-integrated-v1';")
    .replace("? state.world : 1;", "? state.world : 3;");
  template = template.replace(/<div class="worlds"[^>]*>.*?<\/div>/,
    '<div class="worlds" role="group" aria-label="Guide edition"><a href="../#world-3/">Original World 3 guide</a><button data-world="3">World 3 · Integrated route</button></div>')
    .replace('Search all worlds', 'Search this guide')
    .replace('Full spoilers · Source review: Oct 21, 2026<br>Game version 1.3.94 (178)', 'Separate edition · Nov 6, 2026<br>Game version 1.3.110 (194)')
    .replace('Progress is separate from your game save and other devices.', 'This edition has its own checklist. Your original guide progress is preserved.');
}
const css = await readFile(path.join(home, 'src/style.css'), 'utf8');
const logo = await readFile(path.join(root, 'app/src/main/res/drawable-nodpi/title_logo_starborn.webp'));
const html = template.replace('/* GUIDE_STYLES */', css).replace('/* GUIDE_LOGO */', `data:image/webp;base64,${logo.toString('base64')}`).replace('/* GUIDE_DATA */', `const GUIDES = ${data};`).replace('/* GUIDE_APP */', app);
const version = createHash('sha256').update(html).digest('hex').slice(0, 12);
await mkdir(outputDirectory, { recursive: true });
await writeFile(path.join(outputDirectory, 'index.html'), html);
await writeFile(path.join(outputDirectory, '.nojekyll'), '');
await writeFile(path.join(outputDirectory, 'sw.js'), `const CACHE = '${integrated ? 'starborn-integrated-guide-' : 'starborn-guide-'}${version}';
self.addEventListener('install', event => event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(['./', './index.html'])).then(() => self.skipWaiting())));
self.addEventListener('activate', event => event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(key => key.startsWith('${integrated ? 'starborn-integrated-guide-' : 'starborn-guide-'}') && key !== CACHE).map(key => caches.delete(key)))).then(() => self.clients.claim())));
self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET' || new URL(event.request.url).origin !== self.location.origin) return;
  if (event.request.mode === 'navigate') event.respondWith(fetch(event.request).then(response => { if (response.ok) { const copy = response.clone(); event.waitUntil(caches.open(CACHE).then(cache => cache.put('./index.html', copy))); } return response; }).catch(() => caches.match('./index.html')));
});
`);
console.log(`Built ${guides.length} worlds, ${guides.reduce((n, g) => n + g.sections.length, 0)} sections, ${guides.reduce((n, g) => n + g.sections.flatMap(s => s.blocks).filter(b => b.step !== null).length, 0)} steps → ${path.join(outputDirectory, 'index.html')}`);
