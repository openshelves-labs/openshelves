// Scans the repo for Gradle, npm and Docker files and toolchain pins (Java,
// Node.js, Gradle), checks them for available updates, and keeps a single
// GitHub issue up to date with the results.
// Informational only: opens no branches or PRs.

const { spawnSync } = require('child_process');
const fs   = require('fs');
const path = require('path');

const ROOT      = process.cwd();
const SKIP_DIRS = new Set([
    'node_modules', 'build', 'target', 'dist', 'out', 'docs', '.git', '.gradle', '.next', '.idea',
]);

const PLUGIN_PORTAL = 'https://plugins.gradle.org/m2';
const MAVEN_CENTRAL = 'https://repo.maven.apache.org/maven2';

const ISSUE_LABEL    = 'dependency-updates';
const FAILED_LABEL   = 'check-failed';

// Every label this workflow uses, with the colour and description it should have.
const LABELS = {
    [ISSUE_LABEL]:    { color: 'bfd4f2', description: 'Dependency update report' },
    'toolchain':      { color: 'f5e6c4', description: 'Java, Node.js or Gradle version is behind' },
    'backend':        { color: 'c2e0c6', description: 'Gradle libraries, plugins or BOMs have updates' },
    'frontend':       { color: 'd8cff0', description: 'npm packages have updates' },
    'docker':         { color: 'c9e4f0', description: 'Docker images have newer tags' },
    [FAILED_LABEL]:   { color: 'f2c4c0', description: 'A dependency check failed; see the issue for details' },
    // Not used by this script; created here for Dependabot's PR labels.
    'github-actions': { color: 'e1e4e8', description: 'GitHub Actions version updates' },
};

// Recalculated on every run; any other label on the issue is left alone.
const MANAGED_LABELS = ['toolchain', 'backend', 'frontend', 'docker', FAILED_LABEL];
const MAX_BODY_CHARS = 60000;
const MAX_LOG_CHARS  = 5000;

const GRADLE_FILE = /^(build|settings)\.gradle(\.kts)?$/;


// ---------- helpers ----------

const rel = p => path.relative(ROOT, p).split(path.sep).join('/') || '.';

async function httpGet(url) {
    const res = await fetch(url, {
        headers: { 'User-Agent': 'dependency-report' },
        signal:  AbortSignal.timeout(30000),
    });
    if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
    return res.text();
}

function table(headers, rows) {
    const lines = [
        `| ${headers.join(' | ')} |`,
        `|${headers.map(() => '---|').join('')}`,
        ...rows.map(r => `| ${r.join(' | ')} |`),
    ];
    return lines.join('\n');
}

const plural = (n, one, many = one + 's') => `${n} ${n === 1 ? one : many}`;
const allGood  = msg => `✅ ${msg} Nothing to do here.`;
const code     = v => `\`${v}\``;

const isMajorBump = (cur, latest) => (vkey(latest)[0] ?? 0) > (vkey(cur)[0] ?? 0);
const majorNote   = (cur, latest) => (isMajorBump(cur, latest) ? ' ⚠️ _major version, read the release notes first_' : '');
const shell       = (...lines) => '```sh\n' + lines.join('\n') + '\n```';

// "How to update" tip box shown under a section; each step is one bullet.
// Every line needs a '>' prefix to stay inside GitHub's [!TIP] alert.
function howTo(steps) {
    if (!steps.length) return '';
    const bullets = steps.map(st => '- ' + st.split('\n').join('\n  '));
    const lines = ['[!TIP]', '**🛠️ How to update**', '', ...bullets.join('\n').split('\n')];
    return '\n' + lines.map(l => (l ? `> ${l}` : '>')).join('\n') + '\n';
}

function withLog(message, log) {
    const e = new Error(message);
    e.log = log;
    return e;
}

function logBlock(log, runUrl) {
    const tail = log.length > MAX_LOG_CHARS ? '…\n' + log.slice(-MAX_LOG_CHARS) : log;
    return `\n<details><summary>Log</summary>\n\n\`\`\`text\n${tail}\n\`\`\`\n\n[Full run log](${runUrl})\n</details>\n`;
}

const vkey = v => (v.match(/\d+/g) || []).map(Number);

function compareKeys(a, b) {
    for (let i = 0; i < Math.max(a.length, b.length); i++) {
        const d = (a[i] ?? 0) - (b[i] ?? 0);
        if (d !== 0) return d;
    }
    return 0;
}

function isStable(v) {
    if (/(^|[.\-_])(release|final|ga)($|[.\-_])/i.test(v)) return true;
    return /^[0-9,.v-]+(-r)?$/.test(v);
}

function latestStable(versions) {
    const stable = versions.filter(v => isStable(v) && vkey(v).length);
    if (!stable.length) return null;
    return stable.reduce((a, b) => (compareKeys(vkey(a), vkey(b)) >= 0 ? a : b));
}

const versionCache = new Map();

async function mavenVersions(base, group, artifact) {
    const url = `${base}/${group.replace(/\./g, '/')}/${artifact}/maven-metadata.xml`;
    if (!versionCache.has(url)) {
        const xml  = await httpGet(url);
        const list = (xml.match(/<versions>([\s\S]*?)<\/versions>/) || [])[1] || '';
        versionCache.set(url, [...list.matchAll(/<version>([^<]+)<\/version>/g)].map(m => m[1]));
    }
    return versionCache.get(url);
}


// ---------- file discovery ----------

let fileCache;

function allFiles() {
    if (fileCache) return fileCache;
    fileCache = [];
    const walk = dir => {
        for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
            const full = path.join(dir, entry.name);
            if (entry.isDirectory()) {
                if (!SKIP_DIRS.has(entry.name)) walk(full);
            } else if (entry.isFile()) {
                fileCache.push(full);
            }
        }
    };
    walk(ROOT);
    fileCache.sort();
    return fileCache;
}

const findFiles = test => allFiles().filter(f => test(path.basename(f)));

function gradleProjects() {
    return findFiles(n => n === 'gradlew')
        .map(f => path.dirname(f))
        .filter(dir => fs.readdirSync(dir).some(n => GRADLE_FILE.test(n)));
}

function npmProjects() {
    const dirs = findFiles(n => n === 'package.json').map(f => path.dirname(f));
    const workspaceRoots = dirs.filter(d => {
        try {
            return !!JSON.parse(fs.readFileSync(path.join(d, 'package.json'), 'utf8')).workspaces;
        } catch {
            return false;
        }
    });
    // Workspace packages are covered by their root, so skip them here.
    return dirs.filter(d => !workspaceRoots.some(r => r !== d && d.startsWith(r + path.sep)));
}


// ---------- checks (each returns { md, count } or null to skip) ----------

// First Gradle file under `dir` that mentions `group:name`, and whether it
// pins a version there. Versions from a BOM or a transitive dependency aren't.
function gradleDeclaration(dir, coord) {
    const file = allFiles().find(f => f.startsWith(dir + path.sep)
        && GRADLE_FILE.test(path.basename(f))
        && fs.readFileSync(f, 'utf8').includes(coord));
    const pinned = !!file && fs.readFileSync(file, 'utf8').includes(coord + ':');
    return { file, pinned };
}

async function checkGradleDeps(dir) {
    const run = spawnSync('./gradlew',
        ['dependencyUpdates', '-DoutputFormatter=json', '--console=plain'],
        { cwd: dir, stdio: 'inherit' });
    if (run.status !== 0) throw new Error(`gradlew exited with ${run.status}`);

    const data = JSON.parse(
        fs.readFileSync(path.join(dir, 'build/dependencyUpdates/report.json'), 'utf8'));

    const parts = [];
    let count = 0;

    const outdated = (data.outdated?.dependencies || []).map(d => {
        const av = d.available || {};
        const coord = `${d.group}:${d.name}`;
        return { coord, cur: d.version, latest: av.release || av.milestone || av.integration || '?',
                 ...gradleDeclaration(dir, coord) };
    });
    const rows = outdated.map(d => [code(d.coord), d.cur, `**${d.latest}**`,
        d.file ? code(rel(d.file)) : '_transitive / BOM_']);
    count += rows.length;
    parts.push(rows.length
        ? `${plural(rows.length, 'library has', 'libraries have')} a newer release out:\n\n`
            + table(['Dependency', 'Current', 'Latest', 'Declared in'], rows)
        : allGood('Every library is already on its latest release.'));

    const unresolved = data.unresolved?.count || 0;
    if (unresolved) {
        parts.push(`\n> 🤷 ${plural(unresolved, 'dependency', 'dependencies')} couldn't be resolved, so I couldn't check them.`);
    }

    const steps = [];
    for (const d of outdated.filter(x => x.pinned)) {
        steps.push(`In ${code(rel(d.file))}, change ${code(`${d.coord}:${d.cur}`)} to ${code(`${d.coord}:${d.latest}`)}.${majorNote(d.cur, d.latest)}`);
    }
    const managed = outdated.filter(x => !x.pinned);
    if (managed.length) {
        steps.push(`${managed.map(d => code(d.coord)).join(', ')} ${managed.length === 1 ? 'gets its' : 'get their'} version from a BOM `
            + '(usually Spring Boot) or another library. Bump that instead of pinning a version here, '
            + 'or wait for the next Spring Boot release to pick it up.');
    }
    if (steps.length) steps.push('Check that everything still builds and re-run the check:\n'
        + shell(`cd ${rel(dir)}`, './gradlew build dependencyUpdates'));
    parts.push(howTo(steps));
    return { md: parts.join('\n'), count };
}

async function checkGradlePlugins() {
    const found = [];
    const re = /id\s*\(?\s*['"]([^'"]+)['"]\s*\)?\s*version\s*['"]([^'"]+)['"]/g;
    for (const file of findFiles(n => GRADLE_FILE.test(n))) {
        for (const m of fs.readFileSync(file, 'utf8').matchAll(re)) {
            found.push({ file, id: m[1], cur: m[2] });
        }
    }
    if (!found.length) return null;

    const rows = [];
    const steps = [];
    for (const { file, id, cur } of found) {
        try {
            const latest = latestStable(
                await mavenVersions(PLUGIN_PORTAL, id, `${id}.gradle.plugin`));
            if (latest && compareKeys(vkey(latest), vkey(cur)) > 0) {
                rows.push([code(id), cur, `**${latest}**`, code(rel(file))]);
                let step = `In ${code(rel(file))}, change ${code(`id '${id}' version '${cur}'`)} to ${code(`'${latest}'`)}.${majorNote(cur, latest)}`;
                if (id === 'org.springframework.boot') {
                    step += ' This also moves every library Spring Boot manages, so skim the '
                        + `[release notes](https://github.com/spring-projects/spring-boot/releases/tag/v${latest}).`;
                }
                steps.push(step);
            }
        } catch (e) {
            console.error(`plugin ${id}: ${e.message}`);
        }
    }
    return {
        md: rows.length
            ? `${plural(rows.length, 'plugin')} can be bumped:\n\n`
                + table(['Plugin', 'Current', 'Latest', 'Declared in'], rows) + '\n' + howTo(steps)
            : allGood('All Gradle plugins are current.'),
        count: rows.length,
    };
}

async function checkBoms() {
    const found = [];
    const re = /mavenBom\s*\(?\s*['"]([^:'"]+):([^:'"]+):([^'"]+)['"]/g;
    for (const file of findFiles(n => GRADLE_FILE.test(n))) {
        for (const m of fs.readFileSync(file, 'utf8').matchAll(re)) {
            found.push({ file, g: m[1], a: m[2], cur: m[3] });
        }
    }
    if (!found.length) return null;

    const rows = [];
    const steps = [];
    for (const { file, g, a, cur } of found) {
        try {
            const latest = latestStable(await mavenVersions(MAVEN_CENTRAL, g, a));
            if (latest && compareKeys(vkey(latest), vkey(cur)) > 0) {
                rows.push([code(`${g}:${a}`), cur, `**${latest}**`, code(rel(file))]);
                steps.push(`In ${code(rel(file))}, change ${code(`mavenBom '${g}:${a}:${cur}'`)} to ${code(`'${g}:${a}:${latest}'`)}.${majorNote(cur, latest)}`);
            }
        } catch (e) {
            console.error(`bom ${g}:${a}: ${e.message}`);
        }
    }
    return {
        md: rows.length
            ? `${plural(rows.length, 'BOM has', 'BOMs have')} a newer version. Bumping it also moves every library it manages:\n\n`
                + table(['BOM', 'Current', 'Latest', 'Declared in'], rows) + '\n' + howTo(steps)
            : allGood('All BOMs are current.'),
        count: rows.length,
    };
}

// Packages the Next.js upgrade codemod moves together with `next`.
const NEXT_FAMILY = new Set(['next', 'eslint-config-next', 'react', 'react-dom', '@types/react', '@types/react-dom']);

function npmHowTo(dir, outdated) {
    const pkg   = JSON.parse(fs.readFileSync(path.join(dir, 'package.json'), 'utf8'));
    const specs = { ...pkg.dependencies, ...pkg.devDependencies };
    const cd    = `cd ${rel(dir)}`;
    const steps = [];

    let rest = outdated;
    if (outdated.some(o => o.name === 'next' && o.latest !== o.cur)) {
        const n = outdated.find(o => o.name === 'next');
        steps.push(`**Next.js ${n.cur} → ${n.latest}**: use the official upgrade codemod. It bumps \`next\`, `
            + `\`eslint-config-next\`, React and the React types together and applies any code changes the release needs.${majorNote(n.cur, n.latest)}\n`
            + shell(cd, 'npx @next/codemod@canary upgrade latest'));
        rest = outdated.filter(o => !NEXT_FAMILY.has(o.name));
    }

    const inRange = rest.filter(o => o.wanted !== o.cur);
    if (inRange.length) {
        steps.push(`${inRange.map(o => code(o.name)).join(', ')} can move within the current range:\n`
            + shell(cd, 'npm update'));
    }

    const beyond  = rest.filter(o => o.latest !== o.wanted);
    const install = (list, exact) => list.length
        ? `npm install ${exact ? '--save-exact ' : ''}${list.map(o => `${o.name}@${o.latest}`).join(' ')}` : null;
    const isExact = o => /^\d/.test(specs[o.name] || '');
    for (const [list, label] of [
        [beyond.filter(o => !isMajorBump(o.cur, o.latest)), 'need a range change'],
        [beyond.filter(o =>  isMajorBump(o.cur, o.latest)), 'are **major** upgrades, so read their changelogs first'],
    ]) {
        if (!list.length) continue;
        const cmds = [install(list.filter(o => !isExact(o)), false), install(list.filter(isExact), true)].filter(Boolean);
        steps.push(`${list.map(o => code(o.name)).join(', ')} ${label}:\n` + shell(cd, ...cmds));
    }

    const overridden = rest.filter(o => pkg.overrides && o.name in pkg.overrides);
    if (overridden.length) {
        steps.push(`${overridden.map(o => code(o.name)).join(', ')} ${overridden.length === 1 ? 'is' : 'are'} also listed under `
            + `\`overrides\` in ${manifestPath(dir)}; update ${overridden.length === 1 ? 'it' : 'them'} there too.`);
    }
    if (steps.length) steps.push('Then check that it still lints and builds:\n' + shell(cd, 'npm run lint && npm run build'));
    return howTo(steps);
}

const manifestPath = dir => code(rel(path.join(dir, 'package.json')));

async function checkNpm(dir) {
    if (!fs.existsSync(path.join(dir, 'node_modules'))) {
        const hasLock = fs.existsSync(path.join(dir, 'package-lock.json'));
        const args = [hasLock ? 'ci' : 'install', '--ignore-scripts', '--no-audit', '--no-fund'];
        const install = spawnSync('npm', args, { cwd: dir, encoding: 'utf8' });
        if (install.status !== 0) {
            const err = (install.stderr || '').trim();
            console.error(err);
            const msg = err.split('\n')
                .filter(l => l.startsWith('npm error') && !/complete log|_logs\//.test(l))
                .slice(0, 3)
                .map(l => l.replace(/^npm error\s*/, ''))
                .join(' ') || 'see job log';
            throw withLog(`npm ${args[0]} failed: ${msg}`, err);
        }
    }

    const p = spawnSync('npm', ['outdated', '--json'], { cwd: dir, encoding: 'utf8' });
    if (p.status > 1) {
        const err = (p.stderr || '').trim();
        throw withLog(err.split('\n')[0] || `npm exited with ${p.status}`, err);
    }

    const data = JSON.parse(p.stdout || '{}');
    const manifest = code(rel(path.join(dir, 'package.json')));
    const outdated = Object.keys(data).sort().map(name => {
        const info = Array.isArray(data[name]) ? data[name][0] : data[name];
        return { name, cur: info.current ?? '?', wanted: info.wanted ?? '?', latest: info.latest ?? '?' };
    });
    const rows = outdated.map(o => [code(o.name), o.cur, o.wanted === o.cur ? '—' : o.wanted, `**${o.latest}**`, manifest]);
    return {
        md: rows.length
            ? `${plural(rows.length, 'package')} behind. _Compatible_ is what \`npm update\` would give you within the current range; _Latest_ may need a range change:\n\n`
                + table(['Package', 'Current', 'Compatible', 'Latest', 'Declared in'], rows) + '\n' + npmHowTo(dir, outdated)
            : allGood('Every npm package is on its latest version.'),
        count: rows.length,
    };
}

// --- Docker ---

const FROM_RE  = /^\s*FROM\s+(?:--platform=\S+\s+)?(\S+)(?:\s+AS\s+(\S+))?/i;
const IMAGE_RE = /^\s*image:\s*["']?([^\s"']+)/;
const TAG_RE   = /^(v?)(\d+(?:\.\d+)*)(.*)$/;
const hubCache = new Map();

const isDockerFile = n => /^Dockerfile/.test(n)
    || /\.dockerfile$/.test(n)
    || /^(docker-)?compose.*\.ya?ml$/.test(n);

function dockerRefs(file) {
    const refs = [];
    const stages = new Set();
    for (const line of fs.readFileSync(file, 'utf8').split('\n')) {
        let m = line.match(FROM_RE);
        if (m) {
            const [, ref, alias] = m;
            if (!stages.has(ref) && ref !== 'scratch') refs.push(ref);
            if (alias) stages.add(alias);
            continue;
        }
        m = line.match(IMAGE_RE);
        if (m) refs.push(m[1]);
    }
    return refs;
}

function dockerParse(ref) {
    if (ref.includes('$') || ref.includes('@')) return null;
    const slash = ref.lastIndexOf('/');
    const head  = slash >= 0 ? ref.slice(0, slash) : '';
    const last  = ref.slice(slash + 1);
    const colon = last.indexOf(':');
    const img   = colon >= 0 ? last.slice(0, colon) : last;
    const tag   = colon >= 0 ? last.slice(colon + 1) : '';

    const first = head.split('/')[0];
    if (head && (first.includes('.') || first.includes(':'))) return null; // not Docker Hub
    if (!tag || tag === 'latest') return null;

    const name = head ? `${head}/${img}` : img;
    return { repo: name.includes('/') ? name : `library/${name}`, tag };
}

function tagShape(tag) {
    const m = tag.match(TAG_RE);
    if (!m) return null;
    return { prefix: m[1], ver: m[2].split('.').map(Number), suffix: m[3] };
}

async function hubTags(repo) {
    if (hubCache.has(repo)) return hubCache.get(repo);
    const tags = [];
    let url = `https://hub.docker.com/v2/repositories/${repo}/tags?page_size=100&ordering=last_updated`;
    for (let i = 0; i < 3 && url; i++) {
        const data = JSON.parse(await httpGet(url));
        tags.push(...data.results.map(t => t.name));
        url = data.next;
    }
    hubCache.set(repo, tags);
    return tags;
}

async function checkDocker() {
    const files = findFiles(isDockerFile);
    if (!files.length) return null;

    const rows = [];
    const steps = [];
    const seen = new Set();
    for (const file of files) {
        for (const ref of dockerRefs(file)) {
            const key = `${file}|${ref}`;
            const parsed = dockerParse(ref);
            if (!parsed || seen.has(key)) continue;
            seen.add(key);

            const cur = tagShape(parsed.tag);
            if (!cur) continue;

            let tags;
            try {
                tags = await hubTags(parsed.repo);
            } catch (e) {
                console.error(`docker ${parsed.repo}: ${e.message}`);
                continue;
            }

            const better = tags
                .map(t => ({ t, s: tagShape(t) }))
                .filter(({ s }) => s && s.prefix === cur.prefix && s.suffix === cur.suffix
                    && s.ver.length === cur.ver.length && compareKeys(s.ver, cur.ver) > 0)
                .sort((a, b) => compareKeys(b.s.ver, a.s.ver));

            if (better.length) {
                const image = parsed.repo.replace(/^library\//, '');
                rows.push([code(image), parsed.tag, `**${better[0].t}**`, code(rel(file))]);
                steps.push(`In ${code(rel(file))}, change ${code(`${image}:${parsed.tag}`)} to ${code(`${image}:${better[0].t}`)}.`
                    + majorNote(parsed.tag, better[0].t));
            }
        }
    }
    return {
        md: rows.length
            ? `${plural(rows.length, 'image has', 'images have')} a newer tag on Docker Hub:\n\n`
                + table(['Image', 'Current', 'Newer tag', 'Declared in'], rows) + '\n' + howTo(steps)
            : allGood('All pinned Docker Hub images are current.'),
        count: rows.length,
    };
}


// --- Toolchain (Java, Node.js, Gradle) ---

const JAVA_PIN_RES = [
    /JavaLanguageVersion\.of\(\s*(\d+)\s*\)/g,
    /(?:source|target)Compatibility\s*=\s*(?:JavaVersion\.VERSION_)?['"]?(\d+)/g,
];
const NODE_PIN_FILES = new Set(['.nvmrc', '.node-version']);
const GRADLE_DIST_RE = /gradle-(\d[\w.\-]*?)-(?:bin|all)\.zip/;

const newest = versions => versions.reduce((a, b) => (compareKeys(vkey(a), vkey(b)) >= 0 ? a : b));

async function javaReleases() {
    const d = JSON.parse(await httpGet('https://api.adoptium.net/v3/info/available_releases'));
    return { lts: String(d.most_recent_lts), latest: String(d.most_recent_feature_release) };
}

async function nodeReleases() {
    const list = JSON.parse(await httpGet('https://nodejs.org/dist/index.json'));
    const strip = r => r.version.replace(/^v/, '');
    return {
        lts:    newest(list.filter(r => r.lts).map(strip)),
        latest: newest(list.map(strip)),
    };
}

async function gradleReleases() {
    const d = JSON.parse(await httpGet('https://services.gradle.org/versions/current'));
    return { lts: null, latest: d.version };
}

// Java and Node.js are compared by major version, Gradle by full version.
function toolStatus(pin, r, byMajor) {
    const key = v => (byMajor ? vkey(v).slice(0, 1) : vkey(v));
    if (!vkey(pin).length) return { text: 'floating', outdated: false };
    if (compareKeys(key(pin), key(r.latest)) >= 0) return { text: '✅ latest', outdated: false };
    if (r.lts && compareKeys(key(pin), key(r.lts)) === 0) return { text: '✅ LTS', outdated: false };
    return { text: '⬆️ update', outdated: true };
}

function toolchainStep(tool, file, pin, r) {
    const where  = code(rel(file));
    const major  = v => String(vkey(v)[0]);
    const choice = r.lts && major(r.lts) !== major(r.latest)
        ? `the LTS (**${major(r.lts)}**) or the latest (**${major(r.latest)}**)`
        : `**${major(r.latest)}**`;
    if (tool === 'Java') {
        return `**Java**: in ${where}, change ${code(`JavaLanguageVersion.of(${pin})`)} to ${choice}. `
            + 'The foojay resolver downloads the new JDK on the next build; also switch the project SDK in your IDE.';
    }
    if (tool === 'Node.js') {
        const dir = path.dirname(file);
        const extra = fs.existsSync(path.join(dir, 'package.json'))
            && JSON.parse(fs.readFileSync(path.join(dir, 'package.json'), 'utf8')).engines?.node
            ? ` and \`engines.node\` in ${manifestPath(dir)}` : '';
        return `**Node.js**: set ${where}${extra} to ${choice}, run \`nvm use\`, then reinstall with \`npm ci\`. `
            + 'If the README has a Node.js badge, update it too.';
    }
    return `**Gradle ${pin} → ${r.latest}**: run the wrapper task, then run it once more so the wrapper scripts and jar come from the new version:\n`
        + shell(`cd ${rel(path.dirname(path.dirname(path.dirname(file))))}`,
                `./gradlew wrapper --gradle-version ${r.latest}`, './gradlew wrapper');
}

async function checkToolchain() {
    const pins = [];

    for (const file of findFiles(n => GRADLE_FILE.test(n))) {
        const text = fs.readFileSync(file, 'utf8');
        const found = new Set(JAVA_PIN_RES.flatMap(re => [...text.matchAll(re)].map(m => m[1])));
        for (const v of found) pins.push({ tool: 'Java', file, pin: v });
    }
    for (const file of findFiles(n => NODE_PIN_FILES.has(n))) {
        const v = fs.readFileSync(file, 'utf8').trim().replace(/^v/, '');
        if (v) pins.push({ tool: 'Node.js', file, pin: v });
    }
    for (const dir of gradleProjects()) {
        const file = path.join(dir, 'gradle/wrapper/gradle-wrapper.properties');
        if (!fs.existsSync(file)) continue;
        const m = fs.readFileSync(file, 'utf8').match(GRADLE_DIST_RE);
        if (m) pins.push({ tool: 'Gradle', file, pin: m[1] });
    }
    if (!pins.length) return null;

    const sources = {
        'Java':    { fetch: javaReleases,   byMajor: true  },
        'Node.js': { fetch: nodeReleases,   byMajor: true  },
        'Gradle':  { fetch: gradleReleases, byMajor: false },
    };
    const releases = {};
    for (const tool of new Set(pins.map(p => p.tool))) {
        try {
            releases[tool] = await sources[tool].fetch();
        } catch (e) {
            console.error(`toolchain ${tool}: ${e.message}`);
        }
    }

    let count = 0;
    const steps = [];
    const rows = pins.map(({ tool, file, pin }) => {
        const r = releases[tool];
        if (!r) return [tool, pin, '?', '?', '⚠️ lookup failed', code(rel(file))];
        const st = toolStatus(pin, r, sources[tool].byMajor);
        if (st.outdated) {
            count++;
            steps.push(toolchainStep(tool, file, pin, r));
        }
        return [tool, `**${pin}**`, r.lts ?? '—', r.latest, st.text, code(rel(file))];
    });
    const intro = count
        ? `${plural(count, 'tool is', 'tools are')} behind both the latest LTS and the latest release:`
        : 'All pinned tools are on either the latest LTS or the latest release 👍';
    return {
        md: `${intro}\n\n` + table(['Tool', 'Pinned', 'Latest LTS', 'Latest', 'Status', 'Declared in'], rows) + '\n' + howTo(steps),
        count,
    };
}

// ---------- sections (built from whatever the scan finds) ----------

// Section title from the project folder: ('frontend', 'packages') -> 'Frontend packages'.
// The repo root has no folder name, so it falls back to `rootTitle`.
function projectTitle(dir, noun, rootTitle) {
    const name = rel(dir);
    return name === '.' ? rootTitle : `${name[0].toUpperCase()}${name.slice(1)} ${noun}`;
}

function buildSections() {
    const sections = [
        { emoji: '🧰', title: 'Toolchain', label: 'toolchain', run: checkToolchain },
    ];

    for (const dir of gradleProjects()) {
        sections.push({
            emoji: '☕', title: projectTitle(dir, 'dependencies', 'Gradle dependencies'), label: 'backend',
            run:   () => checkGradleDeps(dir),
        });
    }
    sections.push({ emoji: '🔌', title: 'Gradle plugins', label: 'backend', run: checkGradlePlugins });
    sections.push({ emoji: '📋', title: 'Managed BOMs',   label: 'backend', run: checkBoms });

    for (const dir of npmProjects()) {
        sections.push({
            emoji: '📦', title: projectTitle(dir, 'packages', 'npm packages'), label: 'frontend',
            run:   () => checkNpm(dir),
        });
    }

    sections.push({ emoji: '🐳', title: 'Docker images', label: 'docker', run: checkDocker });
    return sections;
}


// ---------- issue handling ----------

async function ensureLabels(github, context) {
    for (const [name, { color, description }] of Object.entries(LABELS)) {
        try {
            await github.rest.issues.createLabel({ ...context.repo, name, color, description });
        } catch (e) {
            if (e.status !== 422) throw e; // 422 = already exists, so bring it in line
            await github.rest.issues.updateLabel({ ...context.repo, name, color, description });
        }
    }
}

async function upsertIssue(github, context, title, body, managed) {
    const { data: open } = await github.rest.issues.listForRepo({
        ...context.repo, labels: ISSUE_LABEL, state: 'open', per_page: 10,
    });
    const existing = open.find(i => !i.pull_request);

    if (!existing) {
        const created = await github.rest.issues.create({
            ...context.repo,
            title,
            body,
            labels: [ISSUE_LABEL, ...managed],
        });
        return created.data.html_url;
    }

    // Keep labels added by hand; only the managed labels are recalculated.
    const kept = existing.labels
        .map(l => (typeof l === 'string' ? l : l.name))
        .filter(n => !MANAGED_LABELS.includes(n));
    const updated = await github.rest.issues.update({
        ...context.repo,
        issue_number: existing.number,
        title,
        body,
        labels: [...new Set([...kept, ...managed])],
    });
    return updated.data.html_url;
}


// ---------- entry point ----------

// ['backend', 'frontend', 'toolchain'] -> 'backend, frontend & toolchain'
function areaList(areas) {
    const a = [...new Set(areas)].sort();
    return a.length > 1 ? `${a.slice(0, -1).join(', ')} & ${a.at(-1)}` : (a[0] || '');
}

function issueTitle(total, failed, areas, failedAreas) {
    const broken = failed ? `⚠️ ${plural(failed, 'check')} failed in ${areaList(failedAreas)}` : '';
    if (total)  return `⬆️ ${plural(total, 'dependency update')} in ${areaList(areas)}${broken ? ` · ${broken}` : ''}`;
    if (failed) return broken;
    return '✅ All dependencies are up to date';
}

function opening(total, failed) {
    if (!total && !failed) return 'Good news: everything is up to date. Nothing to do this time 🎉';
    const parts = [];
    if (total)  parts.push(`I found **${plural(total, 'update')}** worth a look`);
    if (failed) parts.push(`**${plural(failed, 'check')}** didn't finish, so some areas couldn't be checked`);
    return parts.join(', and ') + '. Details are below.';
}

module.exports = async ({ github, context, core }) => {
    core.info('Scanning the repository for toolchain pins and Gradle, npm and Docker files');

    const runUrl = `${context.serverUrl}/${context.repo.owner}/${context.repo.repo}/actions/runs/${context.runId}`;

    const results = [];
    const summary = [];
    const labels  = new Set();
    const failedAreas = new Set();
    let total  = 0;
    let failed = 0;

    for (const { emoji, title, label, run } of buildSections()) {
        const heading = `### ${emoji} ${title}`;
        try {
            const result = await run();
            if (!result) continue;
            total += result.count;
            if (result.count) labels.add(label);
            summary.push([emoji, title, result.count ? `⬆️ ${result.count}` : '✅ 0']);
            results.push(`${heading}\n\n${result.md}\n`);
        } catch (e) {
            core.warning(`[${title}] failed: ${e.message}`);
            let md = `${heading}\n\n⚠️ This check didn't finish: ${code(e.message)}\n`;
            if (e.log) md += logBlock(e.log, runUrl);
            summary.push([emoji, title, '⚠️ failed']);
            failedAreas.add(label);
            results.push(md);
            failed++;
        }
    }

    const stamp = new Intl.DateTimeFormat('en-GB', {
        timeZone: 'Asia/Kolkata', day: 'numeric', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
    }).format(new Date());

    const trigger = {
        schedule:          'the weekly schedule',
        push:              `a push to \`${context.ref.replace('refs/heads/', '')}\``,
        workflow_dispatch: 'a manual run',
    }[context.eventName] || context.eventName;

    let body = [
        '## 👋 Dependency check-in',
        '',
        `${opening(total, failed)}`,
        '',
        `🕒 Checked on **${stamp} IST**, triggered by ${trigger}.`,
        '',
        ...(summary.length
            ? ['#### At a glance', '', table(['', 'Area', 'Updates'], summary), '', '---', '']
            : ['_No toolchain pins or Gradle, npm or Docker files found in the repository._', '']),
        // GitHub collapses blank lines, so <br> is what actually adds space.
        results.join('\n<br>\n\n'),
        '---',
        '',
        `<sub>📝 This is an informational report; no branches or PRs are opened. It's refreshed on every relevant push to \`main\` and every Friday evening. [View the run](${runUrl}).</sub>`,
    ].join('\n');

    if (body.length > MAX_BODY_CHARS) {
        body = body.slice(0, MAX_BODY_CHARS) + '\n\n_Report truncated; see the run log for the rest._';
    }

    const title = issueTitle(total, failed, labels, failedAreas);
    await core.summary.addRaw(body).write();

    if (failed) labels.add(FAILED_LABEL);
    await ensureLabels(github, context);
    const url = await upsertIssue(github, context, title, body, [...labels].sort());
    core.info(`Report updated: ${url} (${title})`);
};
