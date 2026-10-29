// Browser smoke checks using an isolated headless Chrome profile and the DevTools protocol.
// Run with an already-serving URL: node sites/starborn-playtest/check.mjs <url>
import { spawn } from 'node:child_process';
import { mkdtemp, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import assert from 'node:assert/strict';
const url = process.argv[2] || 'http://127.0.0.1:4173/';
const profile = await mkdtemp(path.join(tmpdir(), 'starborn-guide-qa-'));
const chromePath = process.env.CHROME_PATH || (process.platform === 'darwin' ? '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' : process.platform === 'win32' ? 'C:/Program Files/Google/Chrome/Application/chrome.exe' : 'google-chrome');
const chrome = spawn(chromePath, ['--headless=new', '--remote-debugging-port=9427', `--user-data-dir=${profile}`, '--no-first-run', '--no-default-browser-check', 'about:blank'], { stdio: 'ignore', windowsHide: true });
let socket;
try {
  let targets;
  for (let i = 0; i < 50; i++) {
    try { targets = await (await fetch('http://127.0.0.1:9427/json')).json(); break; } catch { await new Promise(resolve => setTimeout(resolve, 200)); }
  }
  assert(targets?.length, 'Chrome starts');
  socket = new WebSocket(targets.find(t => t.type === 'page').webSocketDebuggerUrl);
  await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
  let serial = 0;
  const pending = new Map();
  const errors = [];
  socket.onmessage = event => {
    const msg = JSON.parse(event.data);
    if (msg.method === 'Runtime.exceptionThrown') errors.push(msg.params.exceptionDetails);
    if (msg.id && pending.has(msg.id)) { const { resolve, reject } = pending.get(msg.id); pending.delete(msg.id); if (msg.error) reject(new Error(JSON.stringify(msg.error))); else resolve(msg.result); }
  };
  const send = (method, params = {}) => new Promise((resolve, reject) => { const id = ++serial; pending.set(id, { resolve, reject }); socket.send(JSON.stringify({ id, method, params })); });
  const evaluate = async expression => { const result = await send('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true }); if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails)); return result.result.value; };
  await send('Runtime.enable');
  await send('Page.enable');
  await send('Emulation.setDeviceMetricsOverride', { width: 1440, height: 1000, deviceScaleFactor: 1, mobile: false });
  await send('Page.navigate', { url: url.replace(/#.*$/, '') + '#world-2/' });
  async function waitFor(expression) { for (let i = 0; i < 100; i++) { if (await evaluate(expression)) return; await new Promise(resolve => setTimeout(resolve, 100)); } throw new Error(`Timed out: ${expression}`); }
  await waitFor('document.querySelector("#article h2") !== null');
  await waitFor('document.querySelector(".brand-logo")?.naturalWidth > 0');
  assert.equal(await evaluate('GUIDES.length'), 2);
  assert.equal(await evaluate('world'), 2, 'world-2 root link selects World 2 on a fresh visit');
  assert.equal(await evaluate('GUIDES[1].sections.some(s => s.blocks.some(b => b.text.includes("report_to_zeke")))'), true, 'World 2 includes the current Botanist return task');
  await evaluate('switchWorld(1)');
  assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth'), true, 'desktop has no horizontal overflow');
  const desktopShot = await send('Page.captureScreenshot', { format: 'png' });
  const desktopScreenshot = path.join(profile, 'desktop.png');
  await writeFile(desktopScreenshot, Buffer.from(desktopShot.data, 'base64'));
  await evaluate('showSection(2)');
  await waitFor('document.querySelector("input[data-step]") !== null');
  await waitFor('restoring === false');
  const firstStep = await evaluate('document.querySelector("input[data-step]").dataset.step');
  await evaluate('document.querySelector("input[data-step]").click()');
  assert.equal(await evaluate(`JSON.parse(localStorage.getItem(STORE)).worlds[1].checked.includes('${firstStep}')`), true, 'checkmarks saved');
  await evaluate('window.scrollTo(0, 650)');
  await new Promise(resolve => setTimeout(resolve, 350));
  await send('Page.reload');
  await waitFor('document.querySelector("input[data-step]")?.checked === true');
  await waitFor('window.scrollY > 500');
  await evaluate('document.querySelector("#search").value="Chime"; document.querySelector("#search").dispatchEvent(new Event("input"))');
  assert.equal(await evaluate('document.querySelectorAll("#results button").length > 0'), true, 'search returns matches');
  await evaluate(`document.querySelector('[data-result-world="2"]').click()`);
  await waitFor('world === 2 && document.querySelector("#search-results").hidden');
  assert.equal(await evaluate(`document.querySelector('[data-world="2"]').getAttribute('aria-pressed')`), 'true', 'cross-world search navigation');
  await evaluate('document.querySelector("#theme").click()');
  assert.equal(await evaluate('document.documentElement.classList.contains("light")'), true, 'theme toggle');
  await evaluate('document.querySelector("#theme").click()');
  await send('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true });
  await evaluate('document.querySelector("#contents").open=false; window.scrollTo(0,0)');
  assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth'), true, 'mobile has no horizontal overflow');
  const shot = await send('Page.captureScreenshot', { format: 'png' });
  const screenshot = path.join(profile, 'mobile.png');
  await writeFile(screenshot, Buffer.from(shot.data, 'base64'));
  await send('Emulation.setDeviceMetricsOverride', { width: 320, height: 740, deviceScaleFactor: 1, mobile: true });
  assert.equal(await evaluate('document.documentElement.scrollWidth <= innerWidth'), true, 'narrow phone has no horizontal overflow');
  await waitFor('document.querySelector("#offline").textContent.includes("Ready")');
  await send('Network.enable');
  await send('Network.emulateNetworkConditions', { offline: true, latency: 0, downloadThroughput: 0, uploadThroughput: 0 });
  await send('Page.reload');
  await waitFor('document.querySelector("#article h2") !== null');
  assert.equal(await evaluate('GUIDES.length'), 2, 'both worlds available after offline reload');
  assert.deepEqual(errors, [], 'no uncaught browser errors');
  console.log(JSON.stringify({ passed: ['World 2 root link', 'current Botanist return task', 'logo loaded', 'desktop layout', 'checkmarks persist', 'position restored', 'search', 'cross-world navigation', 'theme', 'mobile layout', 'offline reload', 'no browser errors'], screenshot, desktopScreenshot }, null, 2));
  await send('Browser.close');
} finally { socket?.close(); chrome.kill(); }
