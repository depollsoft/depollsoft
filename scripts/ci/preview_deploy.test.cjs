const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const {updateDeployComment} = require('./preview_deploy.cjs');

const root = path.resolve(__dirname, '../..');
const workflow = fs.readFileSync(path.join(root, '.github/workflows/deploy-pr-preview.yml'), 'utf8');
const context = {repo: {owner: 'owner', repo: 'repo'}, runId: 77, actor: 'actor'};
const marker = '<!-- preview-deploy-status -->';
const meta = (key, value) => `<!-- preview-deploy-${key}:${value} -->`;
const existing = (values, id = 10) => ({
    id, body: [marker, ...Object.entries(values).map(([k, v]) => meta(k, v))].join('\n'),
});
const preview = (sha, url, firebase = '✅ Published') => ({
    html_url: url,
    body: `<!-- Sticky Pull Request Commentpreview-build -->
<!-- preview-build-source-sha:${sha} -->
| **Firebase App Distribution** | ${firebase} |`,
});

function fakeGitHub(comments = []) {
    const calls = [];
    return {
        calls,
        rest: {issues: {
            listComments: 'listComments',
            createComment: async args => { calls.push(['create', args]); },
            updateComment: async args => { calls.push(['update', args]); },
        }},
        paginate: async (endpoint) => endpoint === 'listComments' ? comments : [
            {id: 1, content: 'eyes', user: {login: 'github-actions[bot]'}},
            {id: 2, content: 'rocket', user: {login: 'human'}},
        ],
        request: async (endpoint, args) => { calls.push([endpoint, args]); },
    };
}

async function runJob(job, {inputs = {}, results = {}, sha = '', build = '', comments = []} = {}) {
    const body = workflow.split(`  comment-${job}:\n`)[1].split(/^  [a-z-]+:/m)[0];
    let script = body.split('          script: |\n')[1]
        .split('\n').map(line => line.replace(/^\x20{12}/, '')).join('\n');
    script = script.replace(/'\$\{\{ needs\.([a-z-]+)\.result \}\}'/g,
                            (_, name) => JSON.stringify(results[name] || 'skipped'));
    assert.ok(!script.includes('${{'), 'all GitHub expressions must be supplied by the fixture');
    const github = fakeGitHub(comments);
    const warnings = [];
    const AsyncFunction = Object.getPrototypeOf(async () => {}).constructor;
    await new AsyncFunction('github', 'context', 'core', 'require', 'process', script)(
        github, context, {warning: message => warnings.push(message)}, require,
        {env: {GITHUB_WORKSPACE: root, DEPLOY_INPUTS: JSON.stringify(inputs),
               DEPLOY_SHA: sha, DEPLOY_BUILD_NUMBER: build}});
    return {github, warnings, comment: github.calls.find(([op]) => op === 'create' || op === 'update')};
}

test('start updates only the requested lock and uses the SHA-matched preview', async () => {
    const {github, comment} = await runJob('start', {
        inputs: {'pr-number': '5', 'request-comment-id': ' 123 ', 'request-comment-user': 'requester',
                 'preview-build-number': '9', 'request-preview-comment-url': 'fallback'},
        sha: 'selected', build: '10',
        comments: [existing({'request-key': 'comment-123'}), existing({'request-key': 'comment-456'}, 20),
                   preview('selected', 'matched-url'), preview('different', 'wrong-url')],
    });
    assert.equal(comment[0], 'update');
    assert.equal(comment[1].comment_id, 10);
    assert.match(comment[1].body, /matched-url/);
    assert.doesNotMatch(comment[1].body, /wrong-url|fallback/);
    assert.ok(comment[1].body.includes(meta('state', 'in_progress')));
    assert.ok(comment[1].body.includes(meta('request-key', 'comment-123')));
    assert.match(comment[1].body, /monotonic store build numbers/);
    assert.match(comment[1].body, /Firebase App Distribution\*\* \| ✅ Published/);
    assert.equal(github.calls.length, 1);
});

test('workflow dispatch creates a workflow-keyed lock without a command reaction', async () => {
    const {comment} = await runJob('start', {inputs: {'pr-number': '5'}});
    assert.equal(comment[0], 'create');
    assert.equal(comment[1].issue_number, 5);
    assert.ok(comment[1].body.includes(meta('request-key', 'workflow-77')));
    assert.match(comment[1].body, /@actor via workflow dispatch/);
    assert.match(comment[1].body, /Awaiting publication/);
});

test('finish reconstructs missing inputs from its own existing comment after validation failure', async () => {
    const saved = existing({
        'request-key': 'comment-123', 'requested-by': 'saved-user', 'requested-at': 'saved-time',
        'last-command-comment-id': '123', 'request-comment-url': 'command-url',
        'preview-comment-url': 'saved-preview', 'source-sha': 'saved-sha',
        'build-number': '12', 'requested-build-number': '11',
    });
    const {comment, github} = await runJob('finish', {
        inputs: {'pr-number': '5', 'request-comment-id': '123'}, comments: [saved],
        results: {'validate-pr': 'failure'},
    });
    assert.equal(comment[0], 'update');
    for (const value of ['saved-user', 'saved-time', 'command-url', 'saved-preview', 'saved-sha']) {
        assert.ok(comment[1].body.includes(value), value);
    }
    assert.ok(comment[1].body.includes(meta('state', 'failed')));
    assert.match(comment[1].body, /PR validation failed/);
    assert.match(comment[1].body, /Not started/);
    assert.equal(github.calls.at(-1)[1].content, '-1');
});

test('finish preserves success, failure, cancellation and SHA-verification results', async () => {
    const success = {'validate-pr': 'success', 'deploy-android': 'success',
                     'deploy-ios': 'success', 'verify-pr-source': 'success'};
    const cases = [
        [success, 'success', /Deployed to TestFlight/, 'rocket'],
        [{...success, 'deploy-android': 'failure'}, 'failed', /Build or store deploy failed/, '-1'],
        [{...success, 'deploy-ios': 'cancelled'}, 'cancelled', /Deploy cancelled/, null],
        [{...success, 'verify-pr-source': 'failure'}, 'failed', /did not match/, '-1'],
    ];
    for (const [results, state, status, reaction] of cases) {
        const {comment, github} = await runJob('finish', {
            inputs: {'pr-number': '5', 'request-comment-id': '123'}, results,
        });
        assert.ok(comment[1].body.includes(meta('state', state)));
        assert.match(comment[1].body, status);
        const posts = github.calls.filter(([endpoint]) => endpoint.startsWith('POST'));
        assert.equal(posts.length, reaction ? 1 : 0);
        if (reaction) {
            assert.equal(posts[0][1].content, reaction);
        }
        const deletes = github.calls.filter(([endpoint]) => endpoint.startsWith('DELETE'));
        assert.deepEqual(deletes.map(([, args]) => args.reaction_id), [1], 'human reactions remain untouched');
    }
});

test('explicit inputs and validated outputs win over old metadata', async () => {
    const github = fakeGitHub([existing({'request-key': 'comment-123', 'source-sha': 'old',
                                        'build-number': '1', 'requested-by': 'old-user'})]);
    const {body} = await updateDeployComment({
        github, context, inputs: {'pr-number': '5', 'request-comment-id': '123',
                                 'source-sha': 'input', 'request-comment-user': 'new-user'},
        validatedSha: 'validated', buildNumber: '2',
        state: 'in_progress', status: 'running', androidStatus: 'android', iosStatus: 'ios',
        nextStep: 'next',
    });
    assert.ok(body.includes(meta('source-sha', 'validated')));
    assert.ok(body.includes(meta('build-number', '2')));
    assert.ok(body.includes(meta('requested-by', 'new-user')));
});

test('comment helpers use the trusted workflow revision even when validation fails', () => {
    for (const phase of ['start', 'finish']) {
        const job = workflow.split(`  comment-${phase}:\n`)[1].split(/^  [a-z-]+:/m)[0];
        assert.ok(job.includes('ref: ${{ github.workflow_sha }}'));
        assert.match(job, /contents: read/);
        assert.doesNotMatch(job, /ref:.*deploy-sha/);
        if (phase === 'finish') {
            assert.match(job, /if: always\(\)/);
        }
    }
});
