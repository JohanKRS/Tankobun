"""Build the original external APK fixture using existing local Android tooling."""
from pathlib import Path
import os
import subprocess
import zipfile
import argparse

parser = argparse.ArgumentParser()
parser.add_argument('--version', type=int, default=1)
parser.add_argument('--package', default='eu.kanade.tachiyomi.extension.en.preferencesqa')
parser.add_argument('--output', type=Path, default=Path('/tmp/tankobun-apk-preferences-qa/extension'))
args = parser.parse_args()

root = Path(__file__).resolve().parents[2]
output = args.output
output.mkdir(parents=True, exist_ok=True)
jbr = Path(os.environ.get('JAVA_HOME', '/Applications/Android Studio.app/Contents/jbr/Contents/Home'))
sdk = Path(os.environ.get('ANDROID_HOME', str(Path.home() / 'Library/Android/sdk')))
build = sdk / 'build-tools/36.1.0'
cache = Path.home() / '.gradle/caches/modules-2/files-2.1'
pref = next((cache / 'androidx.preference/preference/1.2.1').rglob('*.aar'))
with zipfile.ZipFile(pref) as archive:
    (output / 'preference.jar').write_bytes(archive.read('classes.jar'))
kotlin = next((cache / 'org.jetbrains.kotlin/kotlin-stdlib/2.4.10').rglob('*.jar'))
classpath = [sdk / 'platforms/android-36/android.jar',
    root / 'core/extensions/build/intermediates/compile_library_classes_jar/debug/bundleLibCompileToJarDebug/classes.jar',
    output / 'preference.jar', kotlin]
(output / 'classes').mkdir(exist_ok=True)
subprocess.run([str(jbr / 'bin/javac'), '--release', '17', '-classpath', ':'.join(map(str, classpath)),
    '-d', str(output / 'classes'), str(root / 'tools/apk-preferences-qa/PreferenceSourceFactory.java')], check=True)
env = dict(os.environ, JAVA_HOME=str(jbr))
subprocess.run([str(build / 'd8'), '--lib', str(classpath[0]), '--classpath', str(classpath[1]),
    '--classpath', str(classpath[2]), '--min-api', '29', '--output', str(output),
    *map(str, (output / 'classes').rglob('*.class'))], check=True, env=env)
manifest = (root / 'tools/apk-preferences-qa/AndroidManifest.xml').read_text()
manifest = manifest.replace('package="eu.kanade.tachiyomi.extension.en.preferencesqa"', f'package="{args.package}"')
manifest = manifest.replace('android:versionCode="1"', f'android:versionCode="{args.version}"').replace('android:versionName="1.0"', f'android:versionName="{args.version}.0"')
manifest = manifest.replace('android:value=".PreferenceSourceFactory"', 'android:value="eu.kanade.tachiyomi.extension.en.preferencesqa.PreferenceSourceFactory"')
(output / 'AndroidManifest.xml').write_text(manifest)
subprocess.run([str(build / 'aapt2'), 'link', '-o', str(output / 'unsigned.apk'), '--manifest',
    str(output / 'AndroidManifest.xml'), '-I', str(classpath[0])], check=True)
with zipfile.ZipFile(output / 'unsigned.apk', 'a') as archive:
    archive.write(output / 'classes.dex', 'classes.dex')
subprocess.run([str(build / 'apksigner'), 'sign', '--ks', str(Path.home() / '.android/debug.keystore'),
    '--ks-pass', 'pass:android', '--key-pass', 'pass:android', '--out', str(output / 'preferences-qa.apk'),
    str(output / 'unsigned.apk')], check=True, env=env)
print(output / 'preferences-qa.apk')
