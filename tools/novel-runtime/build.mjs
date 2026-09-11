import { build } from 'esbuild';
import { mkdirSync, writeFileSync, readFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';

const target = resolve('../../core/extensions/src/main/assets/novel');
mkdirSync(target, { recursive: true });
await build({
  entryPoints: ['runtime.js'], outfile: `${target}/runtime.js`, bundle: true,
  platform: 'browser', format: 'iife', target: ['chrome80'], minify: true,
  legalComments: 'eof',
});
const lock = JSON.parse(readFileSync('package-lock.json', 'utf8'));
let notices = 'Software dependencies of the novel plugin runtime. No reading sources are bundled.\n\n';
for (const [path, pkg] of Object.entries(lock.packages)) {
  if (!path || pkg.dev || pkg.optional) continue;
  notices += `${path.replace(/^node_modules\//, '')} ${pkg.version}\n`;
  const license = ['LICENSE', 'LICENSE.md', 'LICENSE.txt', 'LICENSE-MIT.txt', 'License.txt', 'LICENSE-MIT'].find(name => existsSync(`${path}/${name}`));
  if (!license) {
    const fallback = `licenses/${path.split('/').at(-1)}-LICENSE`;
    if (!existsSync(fallback)) throw new Error(`Missing license for ${path}`);
    notices += readFileSync(fallback, 'utf8') + '\n\n';
    continue;
  }
  notices += readFileSync(`${path}/${license}`, 'utf8') + '\n\n';
}
writeFileSync(`${target}/LICENSES.txt`, notices);
