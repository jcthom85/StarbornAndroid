const $ = selector => document.querySelector(selector);
const STORE = 'starborn-companion-v1';
let storageAvailable = true;
let state;
try { state = JSON.parse(localStorage.getItem(STORE)) || {}; } catch { state = {}; storageAvailable = false; }
if (!state || typeof state !== 'object' || Array.isArray(state)) state = {};
state.worlds ||= {};
let world = state.world === 2 ? 2 : 1;
let sectionIndex = 0;
let restoring = false;
let scrollTimer;
const escape = text => text.replace(/[&<>"']/g, ch => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[ch]));
const plain = text => text.replace(/\*\*|`/g, '').replace(/\[([^\]]+)\]\([^)]+\)/g, '$1');
function inline(text) {
  return escape(text).replace(/`([^`]+)`|\*\*(.+?)\*\*|\[([^\]]+)\]\([^)]+\)/g, (_, code, bold, label) => code ? `<code>${code}</code>` : bold ? `<strong>${bold}</strong>` : label);
}
function guide() { return GUIDES.find(g => g.world === world); }
function progressState() {
  const g = guide();
  const current = state.worlds[world];
  if (!current || typeof current !== 'object' || Array.isArray(current)) state.worlds[world] = {};
  const p = state.worlds[world];
  if (!Array.isArray(p.checked)) p.checked = [];
  const valid = new Set(g.sections.flatMap(s => s.blocks).filter(b => b.step !== null).map(b => b.id));
  p.checked = p.checked.filter(id => valid.has(id));
  if (!Number.isInteger(p.section) || p.section < 0 || p.section >= g.sections.length) p.section = 0;
  if (!Number.isFinite(p.scroll) || p.scroll < 0) p.scroll = 0;
  return p;
}
function save() {
  state.world = world;
  try { localStorage.setItem(STORE, JSON.stringify(state)); } catch { storageAvailable = false; }
  $('#saved-status').textContent = storageAvailable ? 'Saved in this browser' : 'Storage unavailable · progress lasts this visit';
}
function updateProgress() {
  const total = guide().sections.flatMap(s => s.blocks).filter(b => b.step !== null).length;
  const done = progressState().checked.length;
  $('#progress-text').textContent = `${done} / ${total} steps complete`;
  $('#progress').max = total;
  $('#progress').value = done;
}
function setHash(block) {
  history.replaceState(null, '', `#world-${world}/chapter-${sectionIndex + 1}${block ? '/' + block : ''}`);
}
function showSection(index, { scroll = 0, block = null, focus = false } = {}) {
  clearTimeout(scrollTimer);
  restoring = true;
  const g = guide();
  const p = progressState();
  sectionIndex = Math.max(0, Math.min(index, g.sections.length - 1));
  p.section = sectionIndex;
  p.scroll = scroll;
  const section = g.sections[sectionIndex];
  $('#search-results').hidden = true;
  $('#article').hidden = false;
  $('.page-nav').hidden = false;
  $('#article').innerHTML = `<h2>${escape(section.title)}</h2>` + section.blocks.map(b => {
    if (b.heading) return `<h${b.heading} id="${b.id}">${inline(b.text)}</h${b.heading}>`;
    if (b.step !== null) {
      const checked = p.checked.includes(b.id);
      return `<label id="${b.id}" class="step${checked ? ' checked' : ''}"><input type="checkbox" data-step="${b.id}" aria-label="Complete step ${b.step}" ${checked ? 'checked' : ''}><span><span class="step-number">${String(b.step).padStart(2, '0')}</span>${inline(b.text.replace(/^\d+\. /, ''))}</span></label>`;
    }
    const bullet = b.text.startsWith('- ');
    const checkpoint = /^\*\*Check before/i.test(b.text);
    return `<p id="${b.id}" class="${checkpoint ? 'checkpoint' : bullet ? 'bullet' : ''}">${inline(bullet ? b.text.slice(2) : b.text)}</p>`;
  }).join('');
  $('#chapters').innerHTML = g.sections.map((s, i) => `<a href="#world-${world}/chapter-${i + 1}" data-chapter="${i}" ${i === sectionIndex ? 'aria-current="page"' : ''}>${escape(s.title)}</a>`).join('');
  $('#world-label').textContent = `WORLD ${world}`;
  $('#chapter-label').textContent = `WORLD ${world} / FIELD NOTES`;
  $('#page-number').textContent = `${sectionIndex + 1} of ${g.sections.length}`;
  $('#previous').disabled = sectionIndex === 0;
  $('#next').disabled = sectionIndex === g.sections.length - 1;
  document.querySelectorAll('[data-world]').forEach(button => button.setAttribute('aria-pressed', Number(button.dataset.world) === world));
  document.title = `World ${world} · ${section.title} · Starborn`;
  updateProgress();
  setHash(block);
  save();
  if (focus) $('#reader').focus({ preventScroll: true });
  requestAnimationFrame(() => requestAnimationFrame(() => {
    if (block && document.getElementById(block)) {
      const target = document.getElementById(block);
      target.scrollIntoView({ block: 'center' });
      target.classList.add('highlight');
    } else window.scrollTo(0, scroll);
    restoring = false;
  }));
}
function rememberPosition() {
  if (restoring || !$('#search-results').hidden) return;
  progressState().scroll = window.scrollY;
  save();
}
function switchWorld(number) {
  rememberPosition();
  world = number;
  const p = progressState();
  $('#search').value = '';
  showSection(p.section, { scroll: p.scroll });
}
function search() {
  const query = $('#search').value.trim().toLocaleLowerCase();
  if (!query) { showSection(sectionIndex, { scroll: progressState().scroll }); return; }
  if ($('#search-results').hidden) rememberPosition();
  $('#search-results').hidden = false;
  $('#article').hidden = true;
  $('.page-nav').hidden = true;
  const terms = query.split(/\s+/);
  const matches = [];
  for (const g of GUIDES) for (const [si, s] of g.sections.entries()) {
    if (terms.every(t => plain(s.title).toLocaleLowerCase().includes(t))) matches.push({ world: g.world, si, title: s.title, text: 'Open this chapter', id: '' });
    for (const b of s.blocks) if (terms.every(t => plain(b.text).toLocaleLowerCase().includes(t))) matches.push({ world: g.world, si, title: s.title, text: plain(b.text), id: b.id });
  }
  $('#result-count').textContent = matches.length ? `${matches.length} matches across both worlds${matches.length > 100 ? ' · Showing the first 100. Refine your search for more.' : ''}` : 'No matches. Try a room name, quest, item, or internal ID.';
  $('#results').innerHTML = matches.slice(0, 100).map(m => {
    const at = Math.max(0, m.text.toLocaleLowerCase().indexOf(terms[0]) - 70);
    return `<button data-result-world="${m.world}" data-result-section="${m.si}" data-result-block="${m.id}"><small>WORLD ${m.world} · ${escape(m.title)}</small><p>${at ? '…' : ''}${escape(m.text.slice(at, at + 280))}${m.text.length > at + 280 ? '…' : ''}</p></button>`;
  }).join('');
}
function route() {
  const match = /^#world-([12])(?:\/chapter-(\d+)(?:\/([a-f0-9]+))?|\/?)$/.exec(location.hash);
  if (!match) return false;
  world = Number(match[1]);
  const index = match[2] ? Number(match[2]) - 1 : progressState().section;
  if (index < 0 || index >= guide().sections.length) return false;
  const p = progressState();
  showSection(index, { scroll: p.section === index && !match[3] ? p.scroll : 0, block: match[3] });
  return true;
}
document.querySelectorAll('[data-world]').forEach(button => button.addEventListener('click', () => switchWorld(Number(button.dataset.world))));
$('#chapters').addEventListener('click', event => {
  const link = event.target.closest('[data-chapter]');
  if (!link) return;
  event.preventDefault();
  rememberPosition();
  $('#search').value = '';
  if (matchMedia('(max-width:760px)').matches) $('#contents').open = false;
  showSection(Number(link.dataset.chapter), { focus: true });
});
$('#article').addEventListener('change', event => {
  const id = event.target.dataset.step;
  if (!id) return;
  const p = progressState();
  p.checked = p.checked.filter(item => item !== id);
  if (event.target.checked) p.checked.push(id);
  event.target.closest('.step').classList.toggle('checked', event.target.checked);
  updateProgress();
  save();
});
$('#previous').addEventListener('click', () => showSection(sectionIndex - 1, { focus: true }));
$('#next').addEventListener('click', () => showSection(sectionIndex + 1, { focus: true }));
$('#search').addEventListener('input', search);
$('#close-search').addEventListener('click', () => { $('#search').value = ''; showSection(sectionIndex, { scroll: progressState().scroll, focus: true }); });
$('#results').addEventListener('click', event => {
  const result = event.target.closest('[data-result-world]');
  if (!result) return;
  world = Number(result.dataset.resultWorld);
  $('#search').value = '';
  if (matchMedia('(max-width:760px)').matches) $('#contents').open = false;
  showSection(Number(result.dataset.resultSection), { block: result.dataset.resultBlock, focus: true });
});
$('#reset').addEventListener('click', () => {
  if (!confirm(`Clear all checked steps for World ${world}? Your reading position will stay saved.`)) return;
  progressState().checked = [];
  showSection(sectionIndex, { scroll: window.scrollY });
});
function applyTheme() {
  document.documentElement.classList.toggle('light', state.theme === 'light');
  $('#theme').textContent = state.theme === 'light' ? 'Dark mode' : 'Light mode';
  $('#theme').setAttribute('aria-label', `Switch to ${state.theme === 'light' ? 'dark' : 'light'} mode`);
}
$('#theme').addEventListener('click', () => { state.theme = state.theme === 'light' ? 'dark' : 'light'; applyTheme(); save(); });
$('.brand').addEventListener('click', event => { event.preventDefault(); $('#search').value = ''; showSection(0); });
document.addEventListener('keydown', event => {
  if (event.key === '/' && !['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName)) { event.preventDefault(); $('#search').focus(); }
  if (event.key === 'Escape' && $('#search').value) { $('#search').value = ''; search(); }
});
window.addEventListener('scroll', () => { clearTimeout(scrollTimer); scrollTimer = setTimeout(rememberPosition, 200); }, { passive: true });
window.addEventListener('pagehide', rememberPosition);
document.addEventListener('visibilitychange', () => { if (document.hidden) rememberPosition(); });
window.addEventListener('hashchange', () => { $('#search').value = ''; route(); });
if (matchMedia('(max-width:760px)').matches) $('#contents').open = false;
applyTheme();
if (!route()) { const p = progressState(); showSection(p.section, { scroll: p.scroll }); }
if ('serviceWorker' in navigator && ['https:', 'http:'].includes(location.protocol)) {
  navigator.serviceWorker.register('./sw.js').then(() => navigator.serviceWorker.ready).then(() => { $('#offline').textContent = 'Ready to read offline'; }).catch(() => { $('#offline').textContent = 'Online reading · offline setup unavailable'; });
} else $('#offline').textContent = location.protocol === 'file:' ? 'Self-contained offline copy' : 'Online reading';
