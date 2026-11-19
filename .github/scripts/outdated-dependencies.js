// Scans the repo for Gradle, npm and Docker files and toolchain pins (Java,
// Node.js, Gradle), checks them for available updates, and keeps a single
// GitHub issue up to date with the results.
// Informational only: opens no branches or PRs.

const { spawnSync } = require('child_process');
const fs   = require('fs');
const path = require('path');


// ---------- config ----------

const ROOT      = process.cwd();
const SKIP_DIRS = new Set([
    'node_modules', 'build', 'target', 'dist', 'out', 'docs', '.git', '.gradle', '.next', '.idea',
]);

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


// ---------- helpers: versions ----------

const vkey = v => (v.match(/\d+/g) || []).map(Number);

function compareKeys(a, b) {
    for (let i = 0; i < Math.max(a.length, b.length); i++) {
        const d = (a[i] ?? 0) - (b[i] ?? 0);
        if (d !== 0) return d;
    }
    return 0;
}

const newest = versions => versions.reduce((a, b) => (compareKeys(vkey(a), vkey(b)) >= 0 ? a : b));

const isMajorBump = (cur, latest) => (vkey(latest)[0] ?? 0) > (vkey(cur)[0] ?? 0);

function isStable(v) {
    if (/(^|[.\-_])(release|final|ga)($|[.\-_])/i.test(v)) return true;
    return /^[0-9,.v-]+(-r)?$/.test(v);
}

function latestStable(versions) {
    const stable = versions.filter(v => isStable(v) && vkey(v).length);
    return stable.length ? newest(stable) : null;
}


// ---------- helpers: markdown ----------

function table(headers, rows) {
    const lines = [
        `| ${headers.join(' | ')} |`,
        `|${headers.map(() => '---|').join('')}`,
        ...rows.map(r => `| ${r.join(' | ')} |`),
    ];
    return lines.join('\n');
}

const plural = (n, one, many = one + 's') => `${n} ${n === 1 ? one : many}`;
const code     = v => `\`${v}\``;

const majorNote   = (cur, latest) => (isMajorBump(cur, latest) ? ' _(major version, read the release notes first)_' : '');
const bump        = (cur, latest) => `${cur} → **${latest}**${isMajorBump(cur, latest) ? ' _(major)_' : ''}`;
const shortName   = coord => coord.split(':').pop();

// Name linked to its registry page, with the full name on hover when it's shortened.
const link      = (text, url, title) => `[${code(text)}](${url}${title ? ` "${title}"` : ''})`;
const ver       = v => (v && v !== '?' ? `/${v}` : '');
const mavenLink = (coord, v) => link(shortName(coord), `https://mvnrepository.com/artifact/${coord.replace(':', '/')}${ver(v)}`, coord);
const npmLink   = (name, v) => link(name, `https://www.npmjs.com/package/${name}${v && v !== '?' ? `/v/${v}` : ''}`);
const shell       = (...lines) => '```sh\n' + lines.join('\n') + '\n```';

// "How to update" under a section. Several steps become bullets; continuation
// lines are indented so code blocks stay inside their bullet.
function howTo(steps) {
    if (!steps.length) return '';
    if (steps.length === 1) {
        // "**Java**: in …" reads as "**How to update Java:** in …".
        const m = steps[0].match(/^\*\*([^*]+)\*\*: ([\s\S]*)$/);
        const [what, rest] = m ? [` ${m[1]}`, m[2]] : ['', steps[0]];
        return `\n**How to update${what}:** ${rest.replace(/^In /, 'in ')}\n`;
    }
    return '\n**How to update:**\n\n' + steps.map(st => '- ' + st.split('\n').join('\n  ')).join('\n') + '\n';
}

// " All are in `file`." when every item comes from the same file.
function whereNote(files, n) {
    const uniq = [...new Set(files.filter(Boolean).map(file => code(rel(file))))];
    if (uniq.length !== 1) return '';
    return ` ${n === 1 ? "It's" : n === 2 ? 'Both are' : 'All are'} in ${uniq[0]}.`;
}

function logBlock(log, runUrl) {
    const tail = log.length > MAX_LOG_CHARS ? '…\n' + log.slice(-MAX_LOG_CHARS) : log;
    return `\n<details><summary>Log</summary>\n\n\`\`\`text\n${tail}\n\`\`\`\n\n[Full run log](${runUrl})\n</details>\n`;
}


// ---------- helpers: HTTP and errors ----------

async function httpGet(url) {
    const res = await fetch(url, {
        headers: { 'User-Agent': 'dependency-report' },
        signal:  AbortSignal.timeout(30000),
    });
    if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
    return res.text();
}

function withLog(message, log) {
    const e = new Error(message);
    e.log = log;
    return e;
}


// ---------- helpers: file discovery ----------

const rel = p => path.relative(ROOT, p).split(path.sep).join('/') || '.';

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


// ---------- check: toolchain (Java, Node.js, Gradle) ----------

// Each check returns { md, count, blocked, items, show } for its report section,
// or null to skip it. Sections with nothing ready or blocked are left out unless `show` is set.
// They are listed in the order they appear in the report.

const JAVA_PIN_RES = [
    /JavaLanguageVersion\.of\(\s*(\d+)\s*\)/g,
    /(?:source|target)Compatibility\s*=\s*(?:JavaVersion\.VERSION_)?['"]?(\d+)/g,
];
const NODE_PIN_FILES = new Set(['.nvmrc', '.node-version']);
const GRADLE_DIST_RE = /gradle-(\d[\w.\-]*?)-(?:bin|all)\.zip/;

async function javaReleases() {
    const d = JSON.parse(await httpGet('https://api.adoptium.net/v3/info/available_releases'));
    return { lts: String(d.most_recent_lts), latest: String(d.most_recent_feature_release) };
}

async function nodeReleases() {
    const list = JSON.parse(await httpGet('https://nodejs.org/dist/index.json'));
    const strip = r => r.version.replace(/^v/, '');
    const stable = v => /^\d+\.\d+\.\d+$/.test(v);   // skip prereleases such as 27.0.0-alpha
    return {
        lts:    newest(list.filter(r => r.lts).map(strip).filter(stable)),
        latest: newest(list.map(strip).filter(stable)),
    };
}

async function gradleReleases() {
    const d = JSON.parse(await httpGet('https://services.gradle.org/versions/current'));
    return { lts: null, latest: d.version };
}

// Java and Node.js are compared by major version, Gradle by full version.
function toolStatus(pin, r, byMajor) {
    const key = v => (byMajor ? vkey(v).slice(0, 1) : vkey(v));
    if (!vkey(pin).length) return { text: 'Floating', outdated: false };
    if (compareKeys(key(pin), key(r.latest)) >= 0) return { text: '✅ Latest', outdated: false };
    if (r.lts && compareKeys(key(pin), key(r.lts)) === 0) return { text: '✅ LTS', outdated: false };
    return { text: '⬆️ Update', outdated: true };
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
        return `**Node.js**: set ${where}${extra} to ${choice}, run \`nvm use\`, then reinstall with \`npm ci\`.`;
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
    const items = [];
    const rows = pins.map(({ tool, file, pin }) => {
        const r = releases[tool];
        if (!r) return [tool, `**${pin}**`, '–', '–', '⚠️ Lookup failed'];
        const st = toolStatus(pin, r, sources[tool].byMajor);
        if (st.outdated) {
            count++;
            steps.push(toolchainStep(tool, file, pin, r));
            items.push({ name: `${tool} (${rel(file)})`, from: pin, to: r.latest });
        }
        return [tool, `**${pin}**`, r.lts ?? '–', r.latest, st.text];
    });
    // Always shown: it's also a reference for which versions we're on.
    return {
        md: table(['Tool', 'In use', 'Latest LTS', 'Latest', ''], rows) + '\n' + howTo(steps),
        count,
        items,
        show: true,
    };
}


// ---------- check: Gradle dependencies ----------

// First Gradle file under `dir` that mentions `group:name`, and whether it
// pins a version there. Versions from a BOM or a transitive dependency aren't.
function gradleDeclaration(dir, coord) {
    const file = allFiles().find(f => f.startsWith(dir + path.sep)
        && GRADLE_FILE.test(path.basename(f))
        && fs.readFileSync(f, 'utf8').includes(coord));
    const pinned = !!file && fs.readFileSync(file, 'utf8').includes(coord + ':');
    return { file, pinned };
}

// Runs the `dependencyUpdates` task (ben-manes versions plugin) and returns its JSON report.
function gradleReport(dir) {
    const run = spawnSync('./gradlew',
        ['dependencyUpdates', '-DoutputFormatter=json', '--console=plain'],
        { cwd: dir, stdio: 'inherit' });
    if (run.status !== 0) throw new Error(`gradlew exited with ${run.status}`);

    return JSON.parse(fs.readFileSync(path.join(dir, 'build/dependencyUpdates/report.json'), 'utf8'));
}



async function checkGradleDeps(dir) {
    const data = gradleReport(dir);

    const outdated = (data.outdated?.dependencies || []).map(d => {
        const av = d.available || {};
        const coord = `${d.group}:${d.name}`;
        return { coord, cur: d.version, latest: av.release || av.milestone || av.integration || '?',
                 ...gradleDeclaration(dir, coord) };
    });

    // Only versions pinned in our own build files can be bumped here. The rest get
    // their version from elsewhere, so they're shown as blocked and not counted.
    const updatable  = outdated.filter(d => d.pinned);
    const viaBom     = outdated.filter(d => !d.pinned && d.file);
    const undeclared = outdated.filter(d => !d.file);
    const blocked    = [...viaBom, ...undeclared];
    for (const d of viaBom) d.by = (await bomFor(dir, d.coord)) || 'a BOM';
    for (const d of undeclared) d.by = '_not declared here_';

    const parts = [];
    if (updatable.length) {
        parts.push(table(['Dependency', 'Current → Latest'], updatable.map(d => [mavenLink(d.coord, d.latest), bump(d.cur, d.latest)])));
        const steps = updatable.map(d =>
            `In ${code(rel(d.file))}, change ${code(`${d.coord}:${d.cur}`)} to ${code(`${d.coord}:${d.latest}`)}.${majorNote(d.cur, d.latest)}`);
        steps.push('Check that everything still builds and re-run the check:\n'
            + shell(`cd ${rel(dir)}`, './gradlew build dependencyUpdates'));
        parts.push(howTo(steps));
    }
    if (blocked.length) {
        let intro = viaBom.length
            ? 'Their versions come from BOMs we import, so they move when that BOM is bumped.'
            : "They aren't declared in our build files; they come in through other dependencies.";
        if (viaBom.length && undeclared.length) intro += ' The ones _not declared here_ come in through other dependencies.';
        parts.push(`${updatable.length ? '**Blocked:** ' : ''}${intro}${whereNote(viaBom.map(d => d.file), viaBom.length)}\n\n`
            + table(['Dependency', 'Current → Latest', 'Blocked by'],
                blocked.map(d => [mavenLink(d.coord, d.latest), bump(d.cur, d.latest), d.by])) + '\n');
    }

    const unresolved = data.unresolved?.count || 0;
    if (unresolved) {
        parts.push(`_${plural(unresolved, "dependency couldn't", "dependencies couldn't")} be resolved, so they weren't checked._\n`);
    }

    return {
        md: parts.join('\n'),
        count: updatable.length,
        blocked: blocked.length,
        items: updatable.map(d => ({ name: d.coord, from: d.cur, to: d.latest })),
        show: unresolved > 0,
    };
}


// ---------- check: Gradle plugins and BOMs ----------

const PLUGIN_PORTAL = 'https://plugins.gradle.org/m2';
const MAVEN_CENTRAL = 'https://repo.maven.apache.org/maven2';

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
    const items = [];
    for (const { file, id, cur } of found) {
        try {
            const latest = latestStable(
                await mavenVersions(PLUGIN_PORTAL, id, `${id}.gradle.plugin`));
            if (latest && compareKeys(vkey(latest), vkey(cur)) > 0) {
                rows.push([link(id, `https://plugins.gradle.org/plugin/${id}/${latest}`), bump(cur, latest)]);
                items.push({ name: id, from: cur, to: latest });
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
        md: table(['Plugin', 'Current → Latest'], rows) + '\n' + howTo(steps),
        count: rows.length,
        items,
    };
}

async function checkBoms() {
    const found = [];
    for (const file of findFiles(n => GRADLE_FILE.test(n))) {
        for (const m of fs.readFileSync(file, 'utf8').matchAll(BOM_RE)) {
            found.push({ file, g: m[1], a: m[2], cur: m[3] });
        }
    }
    if (!found.length) return null;

    const rows = [];
    const steps = [];
    const items = [];
    for (const { file, g, a, cur } of found) {
        try {
            const latest = latestStable(await mavenVersions(MAVEN_CENTRAL, g, a));
            if (latest && compareKeys(vkey(latest), vkey(cur)) > 0) {
                rows.push([mavenLink(`${g}:${a}`, latest), bump(cur, latest)]);
                items.push({ name: `${g}:${a}`, from: cur, to: latest });
                steps.push(`In ${code(rel(file))}, change ${code(`mavenBom '${g}:${a}:${cur}'`)} to ${code(`'${g}:${a}:${latest}'`)}.${majorNote(cur, latest)}`);
            }
        } catch (e) {
            console.error(`bom ${g}:${a}: ${e.message}`);
        }
    }
    return {
        md: 'Bumping a BOM also moves every library it manages.\n\n'
            + table(['BOM', 'Current → Latest'], rows) + '\n' + howTo(steps),
        count: rows.length,
        items,
    };
}


// ---------- BOM lookup ----------

// Fills the "Blocked by" column for libraries whose version comes from a BOM.
// Reads each imported BOM's pom from Maven Central, following its parents and
// the BOMs it imports. Best effort: anything unresolved shows as "a BOM".

const BOM_RE = /mavenBom\s*\(?\s*['"]([^:'"]+):([^:'"]+):([^'"]+)['"]/g;
const SPRING_BOOT_RE = /id\s*\(?\s*['"]org\.springframework\.boot['"]\s*\)?\s*version\s*['"]([^'"]+)['"]/;

// BOMs in the order they apply. With the dependency-management plugin, the
// Spring Boot plugin imports its BOM implicitly, and a later import wins.
function declaredBoms(dir) {
    const texts = allFiles()
        .filter(f => f.startsWith(dir + path.sep) && GRADLE_FILE.test(path.basename(f)))
        .map(f => fs.readFileSync(f, 'utf8'));
    const boms = [];
    for (const text of texts) {
        const boot = text.match(SPRING_BOOT_RE);
        if (boot && text.includes('io.spring.dependency-management')) {
            boms.push({ g: 'org.springframework.boot', a: 'spring-boot-dependencies', v: boot[1] });
        }
    }
    for (const text of texts) {
        for (const m of text.matchAll(BOM_RE)) boms.push({ g: m[1], a: m[2], v: m[3] });
    }
    return boms;
}

// 'spring-boot-dependencies' -> 'Spring Boot', 'testcontainers-bom' -> 'Testcontainers'
const bomName = a => a.replace(/-(dependencies|bom)$/, '').split('-')
    .map(w => w[0].toUpperCase() + w.slice(1)).join(' ');

async function bomFor(dir, coord) {
    let found = null;
    for (const b of declaredBoms(dir)) {
        if ((await pomInfo(b.g, b.a, b.v)).managed.has(coord)) found = bomName(b.a);
    }
    return found;
}

const pomCache = new Map();

function pomInfo(g, a, v, depth = 0) {
    const key = `${g}:${a}:${v}`;
    if (!pomCache.has(key)) {
        pomCache.set(key, loadPom(g, a, v, depth).catch(e => {
            console.error(`pom ${key}: ${e.message}`);
            return { props: {}, managed: new Set() };
        }));
    }
    return pomCache.get(key);
}

// { props, managed } for a pom: its properties and every group:artifact it manages.
async function loadPom(g, a, v, depth) {
    const props = {};
    const managed = new Set();
    if (depth > 5 || /\$\{/.test(v)) return { props, managed };

    const url = `${MAVEN_CENTRAL}/${g.replace(/\./g, '/')}/${a}/${v}/${a}-${v}.pom`;
    const xml = (await httpGet(url)).replace(/<!--[\s\S]*?-->/g, '');
    const tag = (text, t) => (text.match(new RegExp(`<${t}>\\s*([^<]*?)\\s*</${t}>`)) || [])[1];

    const parent = xml.match(/<parent>([\s\S]*?)<\/parent>/)?.[1];
    if (parent) {
        const p = await pomInfo(tag(parent, 'groupId'), tag(parent, 'artifactId'), tag(parent, 'version'), depth + 1);
        Object.assign(props, p.props);
        p.managed.forEach(c => managed.add(c));
    }
    const own = xml.match(/<properties>([\s\S]*?)<\/properties>/)?.[1] || '';
    for (const m of own.matchAll(/<([\w.\-]+)>([^<]*)<\/\1>/g)) props[m[1]] = m[2].trim();
    Object.assign(props, { 'project.groupId': g, 'project.artifactId': a, 'project.version': v });

    const sub = text => {
        for (let i = 0; i < 3; i++) text = (text || '').replace(/\$\{([^}]+)\}/g, (m, k) => props[k] ?? m);
        return text;
    };
    const deps = xml.match(/<dependencyManagement>\s*<dependencies>([\s\S]*?)<\/dependencies>/)?.[1] || '';
    const imports = [];
    for (const [, dep] of deps.matchAll(/<dependency>([\s\S]*?)<\/dependency>/g)) {
        const dg = sub(tag(dep, 'groupId'));
        const da = sub(tag(dep, 'artifactId'));
        if (tag(dep, 'scope') === 'import') imports.push(pomInfo(dg, da, sub(tag(dep, 'version')), depth + 1));
        else managed.add(`${dg}:${da}`);
    }
    for (const i of await Promise.all(imports)) i.managed.forEach(c => managed.add(c));
    return { props, managed };
}


// ---------- check: npm packages ----------

// What a peer range allows, in words: '^8 || ^9' -> 'up to 9', '>=4.8.4 <6.1.0' -> 'below 6.1'.
function rangeHint(range) {
    const below = range.match(/<\s*(\d+(?:\.\d+)*)/);
    if (below) return `below ${below[1].replace(/(\.0)+$/, '')}`;
    const majors = range.split('||').map(p => vkey(p)[0]).filter(n => n !== undefined);
    return majors.length ? `up to ${Math.max(...majors)}` : code(range);
}

const blockedBy = r => `${code(r.by)} (supports ${rangeHint(r.range)})`;

// Packages the Next.js upgrade codemod moves together with `next`.
const NEXT_FAMILY = new Set(['next', 'eslint-config-next', 'react', 'react-dom', '@types/react', '@types/react-dom']);

const manifestPath = dir => code(rel(path.join(dir, 'package.json')));

// npm prints a peer clash in one of two shapes; both yield [name, range, required by].
const PEER_CONFLICT_RES = [
    /Conflicting peer dependency: (\S+)@\S+\s[\s\S]*?peer \S+@"([^"]+)" from (\S+)/,
    /Could not resolve dependency:\s+(?:npm error\s+)?peer (\S+)@"([^"]+)" from (\S+)/,
];

// Dry-runs `npm install name@version` with strict peer checks; nothing is written.
// (Without --strict-peer-deps npm only warns and installs anyway.)
// Returns { ok: true } if it would install cleanly, otherwise { ok: false, reason },
// where `reason` is { by, range } (which package needs which range), or null when
// npm failed for some unrelated reason (network, an older conflict in the tree, ...).
function npmProbe(dir, name, version) {
    const args = ['install', '--dry-run', '--no-save', '--strict-peer-deps',
        '--ignore-scripts', '--no-audit', '--no-fund', `${name}@${version}`];
    const p = spawnSync('npm', args, { cwd: dir, encoding: 'utf8' });
    if (p.status === 0) return { ok: true };

    const err = p.stderr || '';
    for (const re of PEER_CONFLICT_RES) {
        const m = err.match(re);
        // Only count it if the clash is about this package, not some older conflict in the tree.
        if (m && m[1] === name) return { ok: false, reason: { by: m[3].replace(/(.)@[^@]*$/, '$1'), range: m[2] } };
    }

    console.error(`npm dry-run of ${name}@${version} failed:\n${err}`);
    return { ok: false, reason: null };
}

// When the latest version is blocked, finds the newest release that does install:
// tries the newest stable release of each lower major, down to the one in use.
// Returns that version, or null if nothing newer than `wanted` installs.
function newestInstallable(dir, o) {
    const p = spawnSync('npm', ['view', o.name, 'versions', '--json'], { cwd: dir, encoding: 'utf8' });
    if (p.status !== 0) {
        console.error(`npm view ${o.name} versions failed:\n${p.stderr}`);
        return null;
    }

    const all    = [].concat(JSON.parse(p.stdout || '[]'));
    const stable = all.filter(v => /^\d+\.\d+\.\d+$/.test(v));
    const major  = v => vkey(v)[0] ?? 0;

    for (let m = major(o.latest) - 1; m >= major(o.cur); m--) {
        const candidates = stable.filter(v => major(v) === m && compareKeys(vkey(v), vkey(o.wanted)) > 0);
        if (!candidates.length) continue;

        const v = newest(candidates);
        if (npmProbe(dir, o.name, v).ok) return v;
    }
    return null;
}

// Update steps for the packages that can move now.
function npmSteps(dir, outdated) {
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
    for (const [list, one, many] of [
        [beyond.filter(o => !isMajorBump(o.cur, o.latest)), 'needs a range change', 'need a range change'],
        [beyond.filter(o =>  isMajorBump(o.cur, o.latest)), 'is a **major** upgrade, so read its changelog first',
                                                            'are **major** upgrades, so read their changelogs first'],
    ]) {
        if (!list.length) continue;
        const label = list.length === 1 ? one : many;
        const cmds = [install(list.filter(o => !isExact(o)), false), install(list.filter(isExact), true)].filter(Boolean);
        steps.push(`${list.map(o => code(o.name)).join(', ')} ${label}:\n` + shell(cd, ...cmds));
    }

    const overridden = rest.filter(o => pkg.overrides && o.name in pkg.overrides);
    if (overridden.length) {
        steps.push(`${overridden.map(o => code(o.name)).join(', ')} ${overridden.length === 1 ? 'is' : 'are'} also listed under `
            + `\`overrides\` in ${manifestPath(dir)}; update ${overridden.length === 1 ? 'it' : 'them'} there too.`);
    }
    if (steps.length) steps.push('Then check that it still lints and builds:\n' + shell(cd, 'npm run lint && npm run build'));
    return steps;
}

// Installs packages if they aren't yet; `npm outdated` and the dry-runs need node_modules.
function npmInstall(dir) {
    if (fs.existsSync(path.join(dir, 'node_modules'))) return;

    const hasLock = fs.existsSync(path.join(dir, 'package-lock.json'));
    const args    = [hasLock ? 'ci' : 'install', '--ignore-scripts', '--no-audit', '--no-fund'];
    const install = spawnSync('npm', args, { cwd: dir, encoding: 'utf8' });
    if (install.status === 0) return;

    const err = (install.stderr || '').trim();
    console.error(err);
    const msg = err.split('\n')
        .filter(l => l.startsWith('npm error') && !/complete log|_logs\//.test(l))
        .slice(0, 3)
        .map(l => l.replace(/^npm error\s*/, ''))
        .join(' ') || 'see job log';
    throw withLog(`npm ${args[0]} failed: ${msg}`, err);
}

// `npm outdated` as a list of { name, cur, wanted, latest }, sorted by name.
function npmOutdated(dir) {
    const p = spawnSync('npm', ['outdated', '--json'], { cwd: dir, encoding: 'utf8' });
    if (p.status > 1) {    // 1 just means "something is outdated"
        const err = (p.stderr || '').trim();
        throw withLog(err.split('\n')[0] || `npm exited with ${p.status}`, err);
    }

    const data = JSON.parse(p.stdout || '{}');
    return Object.keys(data).sort().map(name => {
        const info = Array.isArray(data[name]) ? data[name][0] : data[name];
        return { name, cur: info.current ?? '?', wanted: info.wanted ?? '?', latest: info.latest ?? '?' };
    });
}

async function checkNpm(dir) {
    npmInstall(dir);
    const outdated = npmOutdated(dir);

    // Try each update that needs a range change; a peer conflict means it's blocked.
    // The Next.js family is skipped when `next` moves, since the codemod bumps them together.
    const nextMoves = outdated.some(o => o.name === 'next' && o.latest !== o.cur);
    for (const o of outdated) {
        if (o.latest === o.wanted || (nextMoves && NEXT_FAMILY.has(o.name))) continue;

        const probe = npmProbe(dir, o.name, o.latest);
        if (probe.ok || !probe.reason) continue;

        o.blockedBy = probe.reason;
        o.fallback  = newestInstallable(dir, o);   // e.g. a lower major that still fits
    }

    // Updatable = has somewhere to move now. A blocked package may still move to an
    // older major that installs, or at least within its current range.
    const updatable = outdated
        .map(o => (o.blockedBy ? { ...o, latest: o.fallback || o.wanted } : o))
        .filter(o => o.latest !== o.cur);
    const blocked  = outdated.filter(o => o.blockedBy && !updatable.some(u => u.name === o.name));
    const manifest = [path.join(dir, 'package.json')];

    const parts = [];
    if (updatable.length) {
        // A package whose latest is blocked can still move to an older release that installs.
        const rows = updatable.map(u => {
            const o = outdated.find(x => x.name === u.name);
            const note = o.blockedBy ? `<br><sub>${o.latest} is blocked by ${blockedBy(o.blockedBy)}</sub>` : '';
            return [npmLink(u.name, u.latest), bump(u.cur, u.latest) + note];
        });
        parts.push(table(['Package', 'Current → Latest'], rows) + '\n' + howTo(npmSteps(dir, updatable)));
    }
    if (blocked.length) {
        parts.push(`${updatable.length ? '**Blocked:** ' : ''}Other packages we use haven't caught up with ${blocked.length === 1 ? 'this one' : 'these'} yet.`
            + `${whereNote(manifest, blocked.length)}\n\n`
            + table(['Package', 'Current → Latest', 'Blocked by'],
                blocked.map(o => [npmLink(o.name, o.latest), bump(o.cur, o.latest), blockedBy(o.blockedBy)])) + '\n');
    }

    return {
        md: parts.join('\n'),
        count: updatable.length,
        blocked: blocked.length,
        items: updatable.map(o => ({ name: o.name, from: o.cur, to: o.latest })),
    };
}


// ---------- check: Docker images ----------

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
    const items = [];
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
                const hub = parsed.repo.startsWith('library/') ? `_/${image}` : `r/${parsed.repo}`;
                rows.push([link(image, `https://hub.docker.com/${hub}`), bump(parsed.tag, better[0].t)]);
                items.push({ name: `${image} (${rel(file)})`, from: parsed.tag, to: better[0].t });
                steps.push(`In ${code(rel(file))}, change ${code(`${image}:${parsed.tag}`)} to ${code(`${image}:${better[0].t}`)}.`
                    + majorNote(parsed.tag, better[0].t));
            }
        }
    }
    return {
        md: table(['Image', 'Current → Newer tag'], rows) + '\n' + howTo(steps),
        count: rows.length,
        items,
    };
}


// ---------- report ----------

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

function issueTitle(total, blocked, failed) {
    const broken = failed ? `⚠️ ${plural(failed, 'check')} failed` : '';
    let status;
    if (total)        status = `⬆️ ${plural(total, 'update')} available${broken ? ` · ${broken}` : ''}`;
    else if (failed)  status = broken;
    else if (blocked) status = `✅ No action needed · ${plural(blocked, 'update')} blocked`;
    else              status = '✅ All up to date';
    return `Dependencies: ${status}`;
}

function opening(total, blocked, failed) {
    const broken = failed ? ` ${plural(failed, 'check')} didn't finish, see below.` : '';
    if (total) {
        return `**${plural(total, 'update')} ready to go.**`
            + (blocked ? ` ${blocked} more ${blocked === 1 ? 'is' : 'are'} blocked.` : '') + broken;
    }
    if (blocked) {
        return `**Nothing to act on right now.** ${plural(blocked, 'newer version is', 'newer versions are')} out, `
            + `but ${blocked === 1 ? "it's" : 'all of them are'} blocked.${broken}`;
    }
    if (failed) return `**${plural(failed, "check didn't", "checks didn't")} finish**, so some areas couldn't be checked.`;
    return '**Everything is up to date.** Nothing to do this time 🎉';
}


// Runs every section's check. A check that throws is reported in its own
// section instead of stopping the whole run. Areas with nothing to report only
// appear in the summary table.
async function runChecks(core, runUrl) {
    const report = { results: [], summary: [], labels: new Set(), total: 0, blocked: 0, failed: 0,
                     state: { items: {}, failed: [] } };

    for (const { emoji, title, label, run } of buildSections()) {
        const name  = `${emoji} ${title}`;
        const ready = n => (label === 'toolchain' ? plural(n, 'update') : `${n} ready`);
        try {
            const result = await run();
            if (!result) continue;
            const { count, blocked = 0 } = result;

            report.total   += count;
            report.blocked += blocked;
            if (count) report.labels.add(label);
            for (const it of result.items || []) report.state.items[`${title}|${it.name}`] = { ...it, section: title };

            const status = [count && `⬆️ ${ready(count)}`, blocked && `⚠️ ${blocked} blocked`].filter(Boolean);
            report.summary.push([name, status.join(' · ') || '✅ Up to date']);

            if (!count && !blocked && !result.show) continue;
            const suffix = [count && ready(count), blocked && `${blocked} blocked`].filter(Boolean).join(', ');
            report.results.push(`### ${name}${suffix ? `: ${suffix}` : ''}\n\n${result.md}\n`);
        } catch (e) {
            core.warning(`[${title}] failed: ${e.message}`);
            let md = `### ${name}: check failed\n\nThis check didn't finish: ${code(e.message)}\n`;
            if (e.log) md += logBlock(e.log, runUrl);

            report.summary.push([name, '❌ Check failed']);
            report.results.push(md);
            report.failed++;
            report.state.failed.push(title);
        }
    }
    return report;
}

// When the check ran, in IST, e.g. "24 Oct 2026, 1:11 PM".
function checkedAt(now = new Date()) {
    const zone = { timeZone: 'Asia/Kolkata' };
    return new Intl.DateTimeFormat('en-GB', { ...zone, day: 'numeric', month: 'short', year: 'numeric' }).format(now)
        + ', ' + new Intl.DateTimeFormat('en-US', { ...zone, hour: 'numeric', minute: '2-digit', hour12: true }).format(now);
}

// The issue body: opening line, at-a-glance table, every section, footer.
function buildBody({ results, summary, total, blocked, failed }, context, runUrl) {
    const trigger = {
        schedule:          'scheduled run',
        push:              `push to \`${context.ref.replace('refs/heads/', '')}\``,
        workflow_dispatch: 'manual run',
    }[context.eventName] || context.eventName;

    // Scheduled runs may not carry repository details, so fall back to plain words.
    const branch = context.payload?.repository?.default_branch;
    const defaultBranch = branch ? `\`${branch}\`` : 'the default branch';

    const body = [
        '## 👋 Outdated dependencies check-in',
        '',
        opening(total, blocked, failed),
        '',
        `<sub>Checked ${checkedAt()} IST · ${trigger} · [view run](${runUrl})</sub>`,
        '',
        ...(summary.length
            ? [table(['Area', 'Status'], summary), '']
            : ['_No toolchain pins or Gradle, npm or Docker files found in the repository._', '']),
        results.join('\n'),
        '---',
        '',
        `<sub>Informational only, no branches or PRs are opened. Refreshed on relevant pushes to ${defaultBranch} and every evening.</sub>`,
    ].join('\n');

    return body.length > MAX_BODY_CHARS
        ? body.slice(0, MAX_BODY_CHARS) + '\n\n_Report truncated; see the run log for the rest._'
        : body;
}


// ---------- issue sync ----------

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

// ---------- comments ----------

// Last run's findings, kept in a hidden marker at the end of the issue body
// so each run can tell what's new and what's gone since the previous one.
const STATE_RE = /<!-- dependency-state:(.*?) -->/s;
const stateMarker = state => `\n<!-- dependency-state:${JSON.stringify(state)} -->`;

function readState(body) {
    try { return JSON.parse((body || '').match(STATE_RE)?.[1]); } catch { return null; }
}

const pick = list => list[Math.floor(Math.random() * list.length)];

// ['a', 'b', 'c'] -> 'a, b and c'
const joinWords = list => (list.length > 1 ? `${list.slice(0, -1).join(', ')} and ${list.at(-1)}` : list[0]);

// Bullet list, capped so a big first run doesn't produce a wall of text.
function bulletList(lines, max = 8) {
    const shown = lines.slice(0, max).map(l => `- ${l}`);
    if (lines.length > max) shown.push(`- …and ${lines.length - max} more, see the description`);
    return shown.join('\n');
}

// What changed since the last run. Items in a section that failed this time
// aren't counted as resolved, since that section simply wasn't checked.
function diffState(prev, cur) {
    const curFailed = new Set(cur.failed);
    const added = [], moved = [], resolved = [];
    for (const [key, it] of Object.entries(cur.items)) {
        const old = prev.items[key];
        if (!old) added.push(it);
        else if (old.to !== it.to) moved.push({ ...it, was: old.to });
    }
    for (const [key, it] of Object.entries(prev.items)) {
        if (!cur.items[key] && !curFailed.has(it.section)) resolved.push(it);
    }
    return {
        added, moved, resolved,
        broke: cur.failed.filter(t => !prev.failed.includes(t)),
        fixed: prev.failed.filter(t => !curFailed.has(t)),
    };
}

// A short, conversational comment about what changed. Returns '' if nothing did.
function changeComment(d, action) {
    const parts = [];

    if (d.added.length === 1) {
        const it = d.added[0];
        parts.push(pick([
            `🆕 New update: ${code(it.name)} can move from ${code(it.from)} to ${code(it.to)}.`,
            `🆕 ${code(it.name)} ${code(it.to)} is out; we're on ${code(it.from)}.`,
        ]));
    } else if (d.added.length) {
        parts.push(pick([
            `🆕 ${d.added.length} new updates since the last check:`,
            '🆕 New updates are available:',
        ]) + '\n\n' + bulletList(d.added.map(it => `${code(it.name)}: ${code(it.from)} → ${code(it.to)}`)));
    }

    if (d.moved.length) {
        const lines = d.moved.map(it => `${code(it.name)} is now at ${code(it.to)} (was ${code(it.was)})`);
        parts.push(d.moved.length === 1
            ? `🔄 A newer release came out for a pending update: ${lines[0]}.`
            : '🔄 Newer releases came out for some pending updates:\n\n' + bulletList(lines));
    }

    if (d.resolved.length) {
        const names = d.resolved.map(it => code(it.name));
        parts.push(d.resolved.length === 1
            ? pick([`✅ ${names[0]} is up to date now.`, `✅ ${names[0]} has been updated, thanks.`])
            : d.resolved.length <= 4
                ? pick([`✅ ${joinWords(names)} are up to date now.`, `✅ Updated since the last check: ${joinWords(names)}.`])
                : `✅ ${d.resolved.length} updates are done:\n\n` + bulletList(names));
    }

    if (d.broke.length) {
        parts.push(`⚠️ The ${joinWords(d.broke)} ${d.broke.length > 1 ? 'checks' : 'check'} didn't finish this time, `
            + 'so that area wasn\'t checked. The error is in the description.');
    }
    if (d.fixed.length) {
        parts.push(`🔧 The ${joinWords(d.fixed)} ${d.fixed.length > 1 ? 'checks are' : 'check is'} working again.`);
    }

    if (action === 'close') {
        parts.push(pick([
            "🎉 That's everything for now, so I'm closing this. I'll reopen it when new updates come in.",
            "🎉 Nothing left to act on, so I'm closing this. It will reopen when new updates are available.",
        ]));
    } else if (action === 'reopen' && parts.length) {
        parts.unshift('🔔 Reopening: there are updates to act on again.');
    }
    return parts.join('\n\n');
}

// Used when there's no saved state yet (first run with this version), so there's nothing to compare against.
const FALLBACK = {
    close:  "🎉 Nothing left to act on, so I'm closing this. I'll reopen it when new updates come in.",
    reopen: '🔔 Reopening: new updates are available. Details are in the description.',
};


// One issue is reused for good and refreshed on every run, open or closed.
// The first run always creates it, closed straight away if there's nothing to act on.
// It's open while there's something to act on, closed when there isn't
// (blocked updates may still be listed), and reopened when updates show up again.
async function syncIssue(github, context, { title, body, managed, clean, state }) {
    body += stateMarker(state);
    const { data: issues } = await github.rest.issues.listForRepo({
        ...context.repo, labels: ISSUE_LABEL, state: 'all', sort: 'created', direction: 'desc', per_page: 20,
    });
    const existing = issues.find(i => !i.pull_request); // Dependabot PRs share the label

    if (!existing) {
        const { data } = await github.rest.issues.create({
            ...context.repo, title, body, labels: [ISSUE_LABEL, ...managed],
        });
        if (!clean) return `opened ${data.html_url}`;
        // Nothing to act on yet: keep the issue (and its saved state) but close it quietly.
        await github.rest.issues.update({
            ...context.repo, issue_number: data.number, state: 'closed', state_reason: 'completed',
        });
        return `opened and closed ${data.html_url}`;
    }

    // Keep labels added by hand; only the managed labels are recalculated.
    const kept = existing.labels
        .map(l => (typeof l === 'string' ? l : l.name))
        .filter(n => !MANAGED_LABELS.includes(n));
    const issue = { ...context.repo, issue_number: existing.number };
    const wasOpen = existing.state === 'open';
    const action  = clean ? (wasOpen ? 'close' : null) : (wasOpen ? null : 'reopen');

    const prev = readState(existing.body);
    const comment = prev ? changeComment(diffState(prev, state), action) : (FALLBACK[action] || '');

    if (clean) {
        await github.rest.issues.update({ ...issue, title, body, labels: kept });
        if (!wasOpen) return `refreshed ${existing.html_url}, stays closed`;

        await github.rest.issues.createComment({ ...issue, body: comment });
        await github.rest.issues.update({ ...issue, state: 'closed', state_reason: 'completed' });
        return `closed ${existing.html_url}`;
    }

    await github.rest.issues.update({
        ...issue, title, body, state: 'open', labels: [...new Set([...kept, ...managed])],
    });
    if (comment) await github.rest.issues.createComment({ ...issue, body: comment });
    return `${wasOpen ? 'updated' : 'reopened'} ${existing.html_url}${comment ? ', commented' : ''}`;
}


// ---------- entry point ----------

module.exports = async ({ github, context, core }) => {
    core.info('Scanning the repository for toolchain pins and Gradle, npm and Docker files');
    const runUrl = `${context.serverUrl}/${context.repo.owner}/${context.repo.repo}/actions/runs/${context.runId}`;

    const report = await runChecks(core, runUrl);
    const title  = issueTitle(report.total, report.blocked, report.failed);
    const body   = buildBody(report, context, runUrl);
    await core.summary.addRaw(body).write();

    if (report.failed) report.labels.add(FAILED_LABEL);
    await ensureLabels(github, context);

    const outcome = await syncIssue(github, context, {
        title, body, managed: [...report.labels].sort(), clean: !report.total && !report.failed, state: report.state,
    });
    core.info(`Issue ${outcome} (${title})`);
};
