import { execFileSync, spawnSync } from 'node:child_process';
import { mkdtemp, cp } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const home = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(home, '../..');
function run(command, args, cwd = root, input) {
  return execFileSync(command, args, { cwd, input, encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'], windowsHide: true }).trim();
}
run(process.execPath, [path.join(home, 'build.mjs')]);
const repository = JSON.parse(run('gh', ['repo', 'view', '--json', 'nameWithOwner,url']));
const endpoint = `repos/${repository.nameWithOwner}/pages`;
const existing = spawnSync('gh', ['api', endpoint], { cwd: root, encoding: 'utf8', windowsHide: true });
if (existing.status !== 0 && !existing.stderr.includes('404')) throw new Error(existing.stderr);
const pages = existing.status === 0 ? JSON.parse(existing.stdout) : null;
if (pages && (pages.source?.branch !== 'gh-pages' || pages.source?.path !== '/')) throw new Error('Existing Pages configuration differs. Review it before publishing.');
const remote = run('git', ['remote', 'get-url', 'origin']);
const branch = run('git', ['ls-remote', '--heads', remote, 'gh-pages']);
const staging = await mkdtemp(path.join(tmpdir(), 'starborn-guide-publish-'));
run('git', ['init', '-b', 'gh-pages', staging]);
run('git', ['remote', 'add', 'origin', remote], staging);
if (branch) {
  run('git', ['fetch', '--depth=1', 'origin', 'gh-pages'], staging);
  run('git', ['checkout', '-B', 'gh-pages', 'FETCH_HEAD'], staging);
}
await cp(path.join(home, 'dist'), staging, { recursive: true });
run('git', ['add', '--', 'index.html', 'sw.js', '.nojekyll'], staging);
if (run('git', ['status', '--porcelain'], staging)) {
  run('git', ['-c', 'user.name=Starborn Guide Publisher', '-c', 'user.email=guide-publisher@users.noreply.github.com', 'commit', '-m', 'Publish Starborn World 1 and World 2 playtest companion'], staging);
  run('git', ['push', 'origin', 'HEAD:refs/heads/gh-pages'], staging);
}
if (!pages) run('gh', ['api', '--method', 'POST', endpoint, '-f', 'build_type=legacy', '-f', 'source[branch]=gh-pages', '-f', 'source[path]=/']);
console.log(JSON.stringify({ commit: run('git', ['rev-parse', 'HEAD'], staging), site: JSON.parse(run('gh', ['api', endpoint])), staging }, null, 2));
