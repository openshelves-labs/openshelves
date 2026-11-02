// Reads the versions the project actually uses and writes one shields.io
// endpoint file per README badge (https://shields.io/badges/endpoint-badge).
// The workflow publishes them to the `badges` branch, so the README badges
// always show the current version without the README ever being edited.
//
// Usage: node .github/scripts/version-badges.js <output-dir>

const fs   = require('fs');
const path = require('path');

const read = file => fs.readFileSync(file, 'utf8');

// Returns the first capture group of `re` in `file`, or stops the run if it's missing,
// so a moved or renamed setting fails loudly instead of publishing a wrong badge.
function find(file, re, what) {
    const m = read(file).match(re);
    if (!m) throw new Error(`Couldn't find the ${what} in ${file}`);
    return m[1];
}

function npmVersion(file, name) {
    const pkg = JSON.parse(read(file));
    const spec = pkg.dependencies?.[name] ?? pkg.devDependencies?.[name];
    if (!spec) throw new Error(`Couldn't find ${name} in ${file}`);
    return spec.replace(/^[\^~=v]/, '');
}

const BADGES = [
    {
        file: 'spring-boot', label: 'Spring Boot',
        version: () => find('backend/build.gradle', /id\s+['"]org\.springframework\.boot['"]\s+version\s+['"]([^'"]+)['"]/, 'Spring Boot plugin version'),
    },
    {
        file: 'java', label: 'Java',
        version: () => find('backend/build.gradle', /JavaLanguageVersion\.of\(\s*(\d+)\s*\)/, 'Java toolchain version'),
    },
    {
        file: 'nextjs', label: 'Next.js',
        version: () => npmVersion('frontend/package.json', 'next'),
    },
    {
        file: 'nodejs', label: 'Node.js',
        version: () => read('frontend/.nvmrc').trim().replace(/^v/, ''),
    },
];

const outDir = process.argv[2];
if (!outDir) throw new Error('Usage: node version-badges.js <output-dir>');
fs.mkdirSync(outDir, { recursive: true });

for (const b of BADGES) {
    const version = b.version();
    const badge = { schemaVersion: 1, label: b.label, message: version };
    fs.writeFileSync(path.join(outDir, `${b.file}.json`), JSON.stringify(badge, null, 2) + '\n');
    console.log(`${b.label}: ${version}`);
}
