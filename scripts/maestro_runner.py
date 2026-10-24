#!/usr/bin/env python3
"""Pinned, isolated Maestro setup/run/verification on macOS and Windows (Python 3.10+)."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import sys
import time
import uuid
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
FLOWS = ROOT / 'playtests/maestro'
CONFIG = json.loads((FLOWS / 'toolchain.json').read_text())
STATE = ROOT / '.maestro-home'
WINDOWS = os.name == 'nt'


def checked(args, **kwargs):
    return subprocess.run([str(a) for a in args], cwd=ROOT, check=True, **kwargs)


def output(args, **kwargs):
    kwargs.setdefault("timeout", 30)
    return checked(args, capture_output=True, text=True, **kwargs).stdout.strip()


def java():
    candidates = [Path(os.environ.get('JAVA_HOME', '/nonexistent')) / 'bin' / ('java.exe' if WINDOWS else 'java')]
    candidates += [Path('C:/Program Files/Android/Android Studio/jbr/bin/java.exe')] if WINDOWS else [Path('/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/java')]
    found = shutil.which('java')
    if found:
        candidates.append(Path(found))
    for candidate in candidates:
        if candidate.is_file():
            version = subprocess.run([str(candidate), '-version'], capture_output=True, text=True)
            if version.returncode == 0:
                return candidate
    raise RuntimeError('Install Android Studio with its JBR or set JAVA_HOME to a working JDK (Gradle uses 21).')


def sdk():
    default = Path(os.environ.get('LOCALAPPDATA', '')) / 'Android/Sdk' if WINDOWS else Path.home() / 'Library/Android/sdk'
    root = Path(os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT') or default)
    adb = root / 'platform-tools' / ('adb.exe' if WINDOWS else 'adb')
    if not adb.is_file():
        raise RuntimeError('Install Android SDK platform-tools or set ANDROID_HOME.')
    return root, adb


def device(adb, requested):
    serial = requested or os.environ.get('ANDROID_SERIAL')
    devices = [line.split()[0] for line in output([adb, 'devices']).splitlines() if line.endswith('\tdevice')]
    if not serial:
        if len(devices) != 1:
            raise RuntimeError('Connect exactly one ready Android device, or supply --device SERIAL.')
        serial = devices[0]
    if serial not in devices:
        raise RuntimeError(f'Device {serial} is missing, offline, or unauthorized.')
    if output([adb, '-s', serial, 'shell', 'getprop', 'sys.boot_completed']) != '1':
        raise RuntimeError(f'Wait for Android to finish booting on {serial}.')
    return serial


def environment(jdk, sdk_root=None):
    env = os.environ.copy()
    env.update(JAVA_HOME=str(jdk.parent.parent), MAESTRO_CLI_NO_ANALYTICS='true',
               MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED='true', MAESTRO_DISABLE_UPDATE_CHECK='true')
    if sdk_root:
        env['ANDROID_HOME'] = str(sdk_root)
        env['PATH'] = str(sdk_root / 'platform-tools') + os.pathsep + env.get('PATH', '')
    return env


def installation():
    return ROOT / 'tools/maestro' / ('cli-' + CONFIG['version']) / 'maestro'


def setup(args):
    jdk = java()
    install = installation()
    if not (install / 'lib').is_dir():
        target = install.parent
        if target.exists():
            raise RuntimeError(f'Incomplete installation at {target}; move it aside before retrying.')
        archive = Path(args.archive).resolve() if args.archive else STATE / 'downloads' / ('maestro-' + CONFIG['version'] + '.zip')
        if not archive.exists():
            if args.archive:
                raise RuntimeError(f'Archive does not exist: {archive}')
            archive.parent.mkdir(parents=True, exist_ok=True)
            partial = archive.with_suffix('.zip.partial')
            with urllib.request.urlopen(CONFIG['archiveUrl'], timeout=60) as source, partial.open('wb') as dest:
                shutil.copyfileobj(source, dest)
            verify_archive(partial)
            partial.replace(archive)
        verify_archive(archive)
        with zipfile.ZipFile(archive) as bundle:
            for member in bundle.infolist():
                if not (target / member.filename).resolve().is_relative_to(target.resolve()):
                    raise RuntimeError('Archive contains an unsafe path.')
            bundle.extractall(target)
    STATE.mkdir(parents=True, exist_ok=True)
    version = output([jdk, f'-Duser.home={STATE}', '-classpath', install / 'lib/*', 'maestro.cli.AppKt', '--version'], env=environment(jdk))
    if version != CONFIG['version']:
        raise RuntimeError(f"Expected Maestro {CONFIG['version']}, got {version}")
    print(f'Maestro {version} ready.', flush=True)
    if args.install:
        sdk_root, adb = sdk()
        serial = device(adb, args.device)
        env = environment(jdk, sdk_root)
        env['ANDROID_SERIAL'] = serial
        checked([ROOT / ('gradlew.bat' if WINDOWS else 'gradlew'), ':app:installPlaytest', '--console=plain'], env=env)


def sha256_file(path):
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(chunk)
    return digest.hexdigest()


def flow_tree_sha256():
    files = sorted([*FLOWS.rglob("*.yaml"), FLOWS / "suites.json", FLOWS / "toolchain.json"])
    entries = {path.relative_to(FLOWS).as_posix(): sha256_file(path) for path in files}
    return hashlib.sha256(json.dumps(entries, sort_keys=True).encode()).hexdigest()


def verify_archive(archive):
    digest = sha256_file(archive)
    if digest != CONFIG['sha256']:
        raise RuntimeError('Maestro archive checksum does not match the pinned release.')



def capture_failure_log(adb, serial, path):
    with path.open('w') as log:
        try:
            subprocess.run([str(adb), '-s', serial, 'logcat', '-d', '-t', '1500'],
                           stdout=log, stderr=subprocess.STDOUT, timeout=10)
        except subprocess.TimeoutExpired:
            log.write('\nLogcat collection timed out after device disconnect.\n')


def report_passes(path):
    tree = ET.parse(path)
    cases = tree.findall('.//testcase')
    summaries = [tree.getroot(), *tree.findall('.//testsuite')]
    return (bool(cases) and not any(tree.findall('.//' + tag) for tag in ('failure', 'error', 'skipped'))
            and not any(int(node.get(key, '0')) for node in summaries for key in ('failures', 'errors', 'skipped')))


def run(args):
    jdk = java()
    if not (installation() / 'lib').is_dir():
        raise RuntimeError('Run python3 scripts/maestro_runner.py setup first.')
    cli = ['--version'] if getattr(args, 'version', False) else list(args.maestro_args)
    if cli and cli[0] == '--':
        cli.pop(0)
    if args.suite:
        if cli:
            raise RuntimeError('Use --suite or raw Maestro arguments, not both.')
        manifest = json.loads((FLOWS / 'suites.json').read_text())
        suite = manifest['suites'][args.suite]
        cli = ['test', str(FLOWS / suite['flow'])]
    testing = 'test' in cli and not any(a in cli for a in ('--help', '-h', '--version', '-v'))
    run_root = ROOT / 'playtests/artifacts/maestro' / (time.strftime('%Y%m%d-%H%M%S') + '-' + uuid.uuid4().hex[:8])
    run_root.mkdir(parents=True)
    temp = STATE / 'tmp' / run_root.name
    temp.mkdir(parents=True)
    env = environment(jdk)
    metadata = {'started': time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime()), 'platform': platform.platform(),
                'maestro': CONFIG['version'], 'appId': CONFIG['appId'], 'kind': 'device-test' if testing else 'cli-info', 'suite': args.suite, 'arguments': cli.copy(),
                'commit': output(['git', 'rev-parse', 'HEAD']), 'dirty': bool(output(['git', 'status', '--porcelain'])),
                'flowTreeSha256': flow_tree_sha256(), 'runnerSha256': sha256_file(Path(__file__)),
                'status': 'incomplete'}
    serial = None
    adb = None
    started = time.monotonic()
    try:
        if testing:
            if any(a.startswith(('--output', '--format', '--debug-output', '--test-output-dir')) for a in cli):
                raise RuntimeError('Report paths and format are managed by the runner.')
            for i, arg in enumerate(cli):
                if 'APP_ID=' in arg and arg.split('APP_ID=', 1)[1] != CONFIG['appId']:
                    raise RuntimeError('APP_ID must target the isolated .playtest app.')
                if arg in ('--device', '--udid'):
                    if i + 1 == len(cli):
                        raise RuntimeError(f'{arg} requires a serial.')
                    args.device = cli[i + 1]
                elif arg.startswith(('--device=', '--udid=')):
                    args.device = arg.split('=', 1)[1]
            sdk_root, adb = sdk()
            serial = device(adb, args.device)
            env = environment(jdk, sdk_root)
            if not output([adb, '-s', serial, 'shell', 'pm', 'path', CONFIG['appId']]).startswith('package:'):
                raise RuntimeError('Install the isolated app with setup --install --device SERIAL.')
            if not any(a.startswith(('--device', '--udid')) for a in cli):
                cli = ['--device', serial] + cli
            cli += ['-e', 'APP_ID=' + CONFIG['appId'], '--format', 'JUNIT', '--output', str(run_root / 'report.xml'),
                    '--debug-output', str(run_root), '--test-output-dir', str(run_root)]
            checked([adb, '-s', serial, 'shell', 'input', 'keyevent', 'KEYCODE_WAKEUP'], stdout=subprocess.DEVNULL)
            checked([adb, '-s', serial, 'shell', 'wm', 'dismiss-keyguard'], stdout=subprocess.DEVNULL)
            metadata['device'] = {key: output([adb, '-s', serial, 'shell', 'getprop', prop]) for key, prop in
                                  [('model', 'ro.product.model'), ('android', 'ro.build.version.release'), ('api', 'ro.build.version.sdk')]}
            metadata['device']['serial'] = serial
            package = output([adb, '-s', serial, 'shell', 'dumpsys', 'package', CONFIG['appId']])
            (run_root / 'installed-package.txt').write_text(package)
            metadata['package'] = [line.strip() for line in package.splitlines() if any(key in line for key in ('versionCode=', 'versionName=', 'lastUpdateTime='))]
            metadata['display'] = output([adb, '-s', serial, 'shell', 'wm', 'size'])
            metadata['fontScale'] = output([adb, '-s', serial, 'shell', 'settings', 'get', 'system', 'font_scale'])
            metadata['animations'] = {key: output([adb, '-s', serial, 'shell', 'settings', 'get', 'global', key])
                                      for key in ('window_animation_scale', 'transition_animation_scale', 'animator_duration_scale')}
            metadata['gameSettingsNote'] = 'Record in-game audio, tutorials and accessibility settings in the run template; device settings alone do not capture them.'
            apk = ROOT / 'app/build/outputs/apk/playtest/app-playtest.apk'
            if apk.is_file():
                metadata['localApkSha256'] = sha256_file(apk)
                metadata['localApkNote'] = 'Local build hash; installed version is recorded separately, not assumed identical.'
        command = [jdk, f'-Duser.home={STATE}', f'-Djava.io.tmpdir={temp}', '-classpath', installation() / 'lib/*', 'maestro.cli.AppKt', *cli]
        print(f'Artifacts: {run_root}', flush=True)
        default_timeout = {'smoke': 1200, 'campaign': 7200, 'focused': 5400, 'recovery': 1200}.get(args.suite, 900)
        timeout = int(os.environ.get('STARBORN_MAESTRO_TIMEOUT_SECONDS', str(default_timeout)))
        with (run_root / 'stdout.log').open('w') as stdout, (run_root / 'stderr.log').open('w') as stderr:
            process = subprocess.Popen([str(a) for a in command], cwd=ROOT, env=env, stdout=stdout, stderr=stderr)
            try:
                code = process.wait(timeout=timeout)
            except (subprocess.TimeoutExpired, KeyboardInterrupt):
                process.kill()
                process.wait()
                raise RuntimeError('Maestro timed out or was interrupted; this run is incomplete.')
        metadata['exitCode'] = code
        if code:
            raise RuntimeError(f'Maestro failed with exit {code}. See {run_root}')
        if testing and not report_passes(run_root / 'report.xml'):
            raise RuntimeError('Missing cases, failures, errors, or skips: this is not a passing run.')
        if testing and flow_tree_sha256() != metadata['flowTreeSha256']:
            raise RuntimeError('Flow configuration changed during this run; rerun before accepting the result.')
        metadata['status'] = 'passed'
        if not testing:
            print((run_root / 'stdout.log').read_text(), end='')
    except Exception as error:
        metadata['error'] = str(error)
        if adb and serial:
            capture_failure_log(adb, serial, run_root / 'logcat.txt')
        raise
    finally:
        metadata['finished'] = time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())
        metadata['elapsedSeconds'] = round(time.monotonic() - started, 2)
        (run_root / 'run.json').write_text(json.dumps(metadata, indent=2) + '\n')
    print('Passed. ' + str(run_root), flush=True)


def verify(args):
    node = shutil.which('node')
    if not node:
        raise RuntimeError('Install Node.js 22+ and run npm ci --ignore-scripts.')
    checked([node, 'scripts/validate-maestro.cjs'])
    checked([sys.executable, 'scripts/world1_checkpoints.py'])
    checked([sys.executable, '-m', 'unittest', 'discover', '-s', 'scripts', '-p', 'test_maestro_runner.py'])
    shell = shutil.which('pwsh') or (shutil.which('powershell') if WINDOWS else None)
    if not shell:
        raise RuntimeError('Install PowerShell 7 (pwsh) for the strict authored-selector validator.')
    checked([shell, '-NoProfile', '-File', 'scripts/validate_maestro_selectors.ps1', '-Strict'])
    jdk = java()
    sdk_root, _ = sdk()
    env = environment(jdk, sdk_root)
    gradle = ROOT / ('gradlew.bat' if WINDOWS else 'gradlew')
    if not args.skip_gradle:
        checked([gradle, ':app:validateWorld1Assets', ':app:runAssetIntegrity', '-PstarbornPowerShell=' + shell, '--console=plain'], env=env)
    if not args.skip_tests:
        checked([gradle, ':app:testDebugUnitTest', '--console=plain'], env=env)
    if not args.skip_lint:
        checked([gradle, ':app:lintDebug', '--console=plain'], env=env)
    if args.install:
        setup(args)
    if not args.skip_maestro:
        for name in ('smoke', 'campaign', 'focused', 'recovery'):
            run(argparse.Namespace(suite=name, device=args.device, maestro_args=[]))
    skipped = [name for name in ('gradle', 'tests', 'lint', 'maestro') if getattr(args, 'skip_' + name)]
    if skipped:
        print('Partial verification only; omitted stages: ' + ', '.join(skipped) + '. This is not full World 1 acceptance.')
    else:
        print('Automated verification passed once. Two clean device runs and phone acceptance remain required.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    for name in ('setup', 'run', 'verify'):
        sub = commands.add_parser(name)
        sub.add_argument('--device')
        if name == 'run':
            sub.add_argument('--version', action='store_true')
            sub.add_argument('--suite', choices=('smoke', 'campaign', 'focused', 'recovery'))
            sub.add_argument('maestro_args', nargs=argparse.REMAINDER)
        else:
            sub.add_argument('--install', action='store_true')
            sub.add_argument('--archive')
        if name == 'verify':
            for flag in ('gradle', 'tests', 'lint', 'maestro'):
                sub.add_argument('--skip-' + flag, action='store_true')
    args = parser.parse_args()
    try:
        if not CONFIG['appId'].endswith('.playtest'):
            raise RuntimeError('Toolchain must target the isolated .playtest app.')
        globals()[args.command](args)
    except (RuntimeError, OSError, ValueError, subprocess.CalledProcessError, ET.ParseError) as error:
        print(f'ERROR: {error}', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
