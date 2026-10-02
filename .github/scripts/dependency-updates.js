// Scans the repo for Gradle, npm and Docker files, checks them for available
// updates, and keeps a single GitHub issue up to date with the results.
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
const AREA_LABELS    = ['backend', 'frontend', 'docker'];
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

async function checkGradleDeps(dir) {
    const run = spawnSync('./gradlew',
        ['dependencyUpdates', '-DoutputFormatter=json', '--console=plain'],
        { cwd: dir, stdio: 'inherit' });
    if (run.status !== 0) throw new Error(`gradlew exited with ${run.status}`);

    const data = JSON.parse(
        fs.readFileSync(path.join(dir, 'build/dependencyUpdates/report.json'), 'utf8'));

    const parts = [];
    let count = 0;

    const g = data.gradle || {};
    if (g.current?.isUpdateAvailable) {
        parts.push(`**Gradle wrapper:** ${g.running.version} → ${g.current.version}\n`);
        count++;
    }

    const rows = (data.outdated?.dependencies || []).map(d => {
        const av = d.available || {};
        const latest = av.release || av.milestone || av.integration || '?';
        return [`\`${d.group}:${d.name}\``, d.version, latest];
    });
    count += rows.length;
    parts.push(rows.length
        ? table(['Dependency', 'Current', 'Latest'], rows)
        : 'All resolved dependencies are up to date.');

    const unresolved = data.unresolved?.count || 0;
    if (unresolved) {
        parts.push(`\n_${unresolved} dependencies could not be resolved by the plugin._`);
    }
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
    for (const { file, id, cur } of found) {
        try {
            const latest = latestStable(
                await mavenVersions(PLUGIN_PORTAL, id, `${id}.gradle.plugin`));
            if (latest && compareKeys(vkey(latest), vkey(cur)) > 0) {
                rows.push([`\`${id}\``, cur, latest, `\`${rel(file)}\``]);
            }
        } catch (e) {
            console.error(`plugin ${id}: ${e.message}`);
        }
    }
    return {
        md: rows.length
            ? table(['Plugin', 'Current', 'Latest', 'Declared in'], rows)
            : 'All plugins are up to date.',
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
    for (const { file, g, a, cur } of found) {
        try {
            const latest = latestStable(await mavenVersions(MAVEN_CENTRAL, g, a));
            if (latest && compareKeys(vkey(latest), vkey(cur)) > 0) {
                rows.push([`\`${g}:${a}\``, cur, latest, `\`${rel(file)}\``]);
            }
        } catch (e) {
            console.error(`bom ${g}:${a}: ${e.message}`);
        }
    }
    return {
        md: rows.length
            ? table(['BOM', 'Current', 'Latest', 'Declared in'], rows)
            : 'All BOMs are up to date.',
        count: rows.length,
    };
}

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
    const rows = Object.keys(data).sort().map(name => {
        const info = Array.isArray(data[name]) ? data[name][0] : data[name];
        return [`\`${name}\``, info.current ?? '?', (info.wanted === info.current ? '—' : (info.wanted ?? '?')), info.latest ?? '?'];
    });
    return {
        md: rows.length
            ? table(['Package', 'Current', 'Compatible', 'Latest'], rows)
            : 'All npm packages are up to date.',
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
                rows.push([`\`${rel(file)}\``, `\`${ref}\``, `\`${better[0].t}\``]);
            }
        }
    }
    return {
        md: rows.length
            ? table(['File', 'Current', 'Newer tag'], rows)
            : 'All pinned Docker Hub images are up to date.',
        count: rows.length,
    };
}


// ---------- sections (built from whatever the scan finds) ----------

function buildSections() {
    const sections = [];

    for (const dir of gradleProjects()) {
        sections.push({
            title: `Gradle dependencies (${rel(dir)})`,
            label: 'backend',
            run:   () => checkGradleDeps(dir),
        });
    }
    sections.push({ title: 'Gradle plugins', label: 'backend', run: checkGradlePlugins });
    sections.push({ title: 'Managed BOMs',   label: 'backend', run: checkBoms });

    for (const dir of npmProjects()) {
        sections.push({
            title: `npm packages (${rel(dir)})`,
            label: 'frontend',
            run:   () => checkNpm(dir),
        });
    }

    sections.push({ title: 'Docker images', label: 'docker', run: checkDocker });
    return sections;
}


// ---------- issue handling ----------

async function ensureLabels(github, context, names) {
    for (const name of names) {
        try {
            await github.rest.issues.createLabel({ ...context.repo, name, color: '0E8A16' });
        } catch (e) {
            if (e.status !== 422) throw e; // 422 = already exists
        }
    }
}

async function upsertIssue(github, context, body, areaLabels) {
    const { data: open } = await github.rest.issues.listForRepo({
        ...context.repo, labels: ISSUE_LABEL, state: 'open', per_page: 10,
    });
    const existing = open.find(i => !i.pull_request);

    if (!existing) {
        const created = await github.rest.issues.create({
            ...context.repo,
            title:  'Dependency updates available',
            body,
            labels: [ISSUE_LABEL, ...areaLabels],
        });
        return created.data.html_url;
    }

    // Keep labels added by hand; only the managed area labels are recalculated.
    const kept = existing.labels
        .map(l => (typeof l === 'string' ? l : l.name))
        .filter(n => !AREA_LABELS.includes(n));
    const updated = await github.rest.issues.update({
        ...context.repo,
        issue_number: existing.number,
        body,
        labels: [...new Set([...kept, ...areaLabels])],
    });
    return updated.data.html_url;
}


// ---------- entry point ----------

module.exports = async ({ github, context, core }) => {
    core.info('Scanning the repository for Gradle, npm and Docker files');

    const runUrl = `${context.serverUrl}/${context.repo.owner}/${context.repo.repo}/actions/runs/${context.runId}`;

    const results = [];
    const labels  = new Set();
    let total  = 0;
    let failed = 0;

    for (const { title, label, run } of buildSections()) {
        try {
            const result = await run();
            if (!result) continue;
            total += result.count;
            if (result.count) labels.add(label);
            results.push(`### ${title}\n${result.md}\n`);
        } catch (e) {
            core.warning(`[${title}] failed: ${e.message}`);
            let md = `### ${title}\n⚠️ Check failed: \`${e.message}\`\n`;
            if (e.log) md += logBlock(e.log, runUrl);
            results.push(md);
            failed++;
        }
    }

    if (!results.length) {
        results.push('_No Gradle, npm or Docker files found in the repository._\n');
    }

    const stamp = new Intl.DateTimeFormat('en-CA', {
        timeZone: 'Asia/Kolkata', year: 'numeric', month: '2-digit', day: '2-digit',
        hour: '2-digit', minute: '2-digit', hourCycle: 'h23',
    }).format(new Date()).replace(', ', ' ');

    let body = [
        `_Last checked ${stamp} IST. Informational only; no PRs are opened._`,
        '',
        `**${total} update(s) available**${failed ? ` · ⚠️ ${failed} check(s) failed` : ''}`,
        '',
        ...results,
    ].join('\n');

    if (body.length > MAX_BODY_CHARS) {
        body = body.slice(0, MAX_BODY_CHARS) + '\n\n_Report truncated._';
    }

    await core.summary.addRaw(body).write();

    // 'github-actions' is not used by this script; it's created here for Dependabot's PR labels.
    await ensureLabels(github, context, [ISSUE_LABEL, ...AREA_LABELS, 'github-actions']);
    const url = await upsertIssue(github, context, body, [...labels].sort());
    core.info(`Report updated: ${url} (${total} update(s), ${failed} failed check(s))`);
};
