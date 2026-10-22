import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { createHash } from 'node:crypto';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const home = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(home, '../..');
const guides = [];
for (const world of [1, 2]) {
  const source = await readFile(path.join(root, `docs/playtest/walkthroughs/WORLD_${world}_COMPLETIONIST_WALKTHROUGH.md`), 'utf8');
  const sections = [];
  let section = { title: 'Before you start', blocks: [] };
  let paragraph = [];
  const flush = () => {
    if (!paragraph.length) return;
    const text = paragraph.join(' ');
    const heading = /^(#{3,4}) /.exec(text);
    const step = /^(\d+)\. /.exec(text);
    const id = createHash('sha256').update(`${section.title}\n${text}`).digest('hex').slice(0, 16);
    section.blocks.push({ id, text: heading ? text.slice(heading[0].length) : text, heading: heading?.[1].length || 0, step: step ? Number(step[1]) : null });
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
  guides.push({ world, revision: createHash('sha256').update(source).digest('hex').slice(0, 12), sections });
}
const data = JSON.stringify(guides).replaceAll('<', '\\u003c');
const template = await readFile(path.join(home, 'src/index.html'), 'utf8');
const app = await readFile(path.join(home, 'src/app.js'), 'utf8');
const css = await readFile(path.join(home, 'src/style.css'), 'utf8');
const logo = await readFile(path.join(root, 'app/src/main/res/drawable-nodpi/title_logo_starborn.webp'));
const html = template.replace('/* GUIDE_STYLES */', css).replace('/* GUIDE_LOGO */', `data:image/webp;base64,${logo.toString('base64')}`).replace('/* GUIDE_DATA */', `const GUIDES = ${data};`).replace('/* GUIDE_APP */', app);
const version = createHash('sha256').update(html).digest('hex').slice(0, 12);
await mkdir(path.join(home, 'dist'), { recursive: true });
await writeFile(path.join(home, 'dist/index.html'), html);
await writeFile(path.join(home, 'dist/.nojekyll'), '');
await writeFile(path.join(home, 'dist/sw.js'), `const CACHE = 'starborn-guide-${version}';
self.addEventListener('install', event => event.waitUntil(caches.open(CACHE).then(cache => cache.addAll(['./', './index.html'])).then(() => self.skipWaiting())));
self.addEventListener('activate', event => event.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(key => key.startsWith('starborn-guide-') && key !== CACHE).map(key => caches.delete(key)))).then(() => self.clients.claim())));
self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET' || new URL(event.request.url).origin !== self.location.origin) return;
  if (event.request.mode === 'navigate') event.respondWith(fetch(event.request).then(response => { if (response.ok) { const copy = response.clone(); event.waitUntil(caches.open(CACHE).then(cache => cache.put('./index.html', copy))); } return response; }).catch(() => caches.match('./index.html')));
});
`);
console.log(`Built ${guides.length} worlds, ${guides.reduce((n, g) => n + g.sections.length, 0)} sections, ${guides.reduce((n, g) => n + g.sections.flatMap(s => s.blocks).filter(b => b.step !== null).length, 0)} steps → ${path.join(home, 'dist/index.html')}`);
