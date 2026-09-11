"""Build inert APK fixtures using the existing Android SDK and a temporary rotation key."""
from pathlib import Path
import argparse
import os
import subprocess
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--output', type=Path, default=Path('/tmp/tankobun-security-qa/fixtures'))
args = parser.parse_args()
output = args.output
output.mkdir(parents=True, exist_ok=True)
jbr = Path('/Applications/Android Studio.app/Contents/jbr/Contents/Home')
sdk = Path.home() / 'Library/Android/sdk'
build = sdk / 'build-tools/36.1.0'
env = dict(os.environ, JAVA_HOME=str(jbr))

def run(*command):
    subprocess.run(list(map(str, command)), check=True, env=env, stdout=subprocess.DEVNULL)

debug = Path.home() / '.android/debug.keystore'
rotation = output / 'rotation.jks'
if not rotation.exists():
    run(jbr / 'bin/keytool', '-genkeypair', '-keystore', rotation, '-storepass', 'android',
        '-keypass', 'android', '-alias', 'rotation', '-keyalg', 'RSA', '-keysize', '2048',
        '-validity', '3650', '-dname', 'CN=Fictional Security QA')
lineage = output / 'rotation.lineage'
run(build / 'apksigner', 'rotate', '--out', lineage, '--old-signer', '--ks', debug,
    '--ks-pass', 'pass:android', '--new-signer', '--ks', rotation, '--ks-pass', 'pass:android')

cases = {
    'valid': ('com.tankobun.app.novelqa', 50, '4.2.2'),
    'wrong-package': ('com.example.securityfixture', 50, '4.2.2'),
    'downgrade': ('com.tankobun.app.novelqa', 48, '4.2.2'),
    'wrong-version': ('com.tankobun.app.novelqa', 51, '4.2.2'),
    'wrong-name': ('com.tankobun.app.novelqa', 50, 'different'),
    'wrong-signer': ('com.tankobun.app.novelqa', 50, '4.2.2'),
    'rotated': ('com.tankobun.app.novelqa', 50, '4.2.2'),
    'multiple-signers': ('com.tankobun.app.novelqa', 50, '4.2.2'),
    'unsigned': ('com.tankobun.app.novelqa', 50, '4.2.2'),
}
for name, (package, version, version_name) in cases.items():
    manifest = output / f'{name}.xml'
    manifest.write_text(f'<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="{package}" android:versionCode="{version}" android:versionName="{version_name}"><uses-sdk android:minSdkVersion="29" android:targetSdkVersion="36"/><application android:label="Inert security fixture" android:hasCode="false"/></manifest>')
    unsigned = output / f'{name}-unsigned.apk'
    target = output / f'{name}.apk'
    run(build / 'aapt2', 'link', '-o', unsigned, '--manifest', manifest, '-I', sdk / 'platforms/android-36/android.jar')
    if name == 'unsigned':
        target.write_bytes(unsigned.read_bytes())
        continue
    key = rotation if name == 'wrong-signer' else debug
    command = [build / 'apksigner', 'sign', '--ks', key, '--ks-pass', 'pass:android', '--out', target]
    if name == 'rotated':
        command += ['--next-signer', '--ks', rotation, '--ks-pass', 'pass:android', '--lineage', lineage, '--rotation-min-sdk-version', '28']
    if name == 'multiple-signers':
        command += ['--v3-signing-enabled', 'false', '--v4-signing-enabled', 'false', '--next-signer', '--ks', rotation, '--ks-pass', 'pass:android']
    run(*command, unsigned)
    run(build / 'apksigner', 'verify', target)
tampered = output / 'tampered.apk'
tampered.write_bytes((output / 'valid.apk').read_bytes())
with zipfile.ZipFile(tampered, 'a') as archive:
    archive.writestr('tampered.txt', 'The signed bytes have been changed.')
print(output)
