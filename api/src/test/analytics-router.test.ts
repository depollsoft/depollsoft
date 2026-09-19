import { after, before, beforeEach, describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { AddressInfo } from 'node:net';
import { Server } from 'node:http';
import express from 'express';
import { PartialFailureError } from '@google-cloud/common/build/src/util';
import { AnalyticsDependencies, GeoLookupResult, createAnalyticsRouter, locationFromLookup } from '../analytics-router';

const seattle: GeoLookupResult = {
    range: [1000, 2000],
    country: 'US',
    region: 'WA',
    eu: '0',
    timezone: 'America/Los_Angeles',
    city: 'Seattle',
    ll: [47.6, -122.3],
    metro: 819,
    area: 20,
};

interface Recorded {
    published: object[];
    inserted: { rows: object[]; options: { ignoreUnknownValues: boolean } }[];
    verifiedTokens: string[];
    lookedUp: string[];
}

function fakeDependencies(overrides: Partial<AnalyticsDependencies> = {}) {
    const recorded: Recorded = { published: [], inserted: [], verifiedTokens: [], lookedUp: [] };
    const deps: AnalyticsDependencies = {
        publishJSON: async (event) => { recorded.published.push(event); },
        insertRows: async (rows, options) => { recorded.inserted.push({ rows, options }); },
        verifyIdToken: async (token) => { recorded.verifiedTokens.push(token); },
        lookup: (ip) => { recorded.lookedUp.push(ip); return seattle; },
        verificationToken: 'secret',
        ...overrides,
    };
    return { deps, recorded };
}

describe('locationFromLookup', () => {
    it('returns null when geoip has no match', () => {
        assert.equal(locationFromLookup(null), null);
    });

    it('maps the geoip record into the analytics location shape', () => {
        assert.deepEqual(locationFromLookup({ ...seattle, eu: '1' }), {
            range: { low: 1000, high: 2000 },
            country: 'US',
            region: 'WA',
            eu: true,
            timezone: 'America/Los_Angeles',
            city: 'Seattle',
            coordinates: { latitude: 47.6, longitude: -122.3, radius: 20 },
            metro: 819,
        });
        assert.equal(locationFromLookup(seattle)?.eu, false);
    });
});

describe('analytics router', () => {
    let server: Server;
    let baseUrl: string;
    let current: ReturnType<typeof fakeDependencies>;

    before(async () => {
        const app = express();
        // Route through a fresh dependency set per test without rebuilding the server.
        app.use('/analytics', (req, res, next) => createAnalyticsRouter(current.deps)(req, res, next));
        server = app.listen(0);
        await new Promise<void>((resolve) => server.once('listening', resolve));
        baseUrl = `http://127.0.0.1:${(server.address() as AddressInfo).port}/analytics`;
    });

    after(() => new Promise<void>((resolve, reject) => server.close((e) => e ? reject(e) : resolve())));

    beforeEach(() => { current = fakeDependencies(); });

    const post = (path: string, body: unknown, headers: Record<string, string> = {}) =>
        fetch(baseUrl + path, {
            method: 'POST',
            headers: { 'content-type': 'application/json', ...headers },
            body: JSON.stringify(body),
        });

    describe('POST /', () => {
        it('publishes the event enriched with the caller location from X-Forwarded-For', async () => {
            const response = await post('/', { event: 'launch', app: 'pitchperfect' }, { 'x-forwarded-for': '8.8.8.8' });
            assert.equal(response.status, 201);
            assert.equal(await response.text(), '');
            assert.deepEqual(current.recorded.lookedUp, ['8.8.8.8']);
            assert.deepEqual(current.recorded.published, [{
                event: 'launch',
                app: 'pitchperfect',
                location: locationFromLookup(seattle),
            }]);
        });

        it('falls back to the socket address and publishes a null location when geoip has no match', async () => {
            current = fakeDependencies({ lookup: (ip) => { current.recorded.lookedUp.push(ip); return null; } });
            const response = await post('/', { event: 'launch' });
            assert.equal(response.status, 201);
            // Loopback may surface as IPv4-mapped IPv6 depending on the listener.
            assert.equal(current.recorded.lookedUp.length, 1);
            assert.match(current.recorded.lookedUp[0], /(^|:)127\.0\.0\.1$/);
            assert.deepEqual(current.recorded.published, [{ event: 'launch', location: null }]);
        });
    });

    describe('POST /pubsub', () => {
        const pushMessage = (data: object, publishTime = '2026-09-19T00:00:00Z') => ({
            message: { data: Buffer.from(JSON.stringify(data)).toString('base64'), publishTime },
        });
        const bearer = { authorization: 'Bearer jwt-from-pubsub' };

        it('rejects a push whose verification token does not match', async () => {
            const response = await post('/pubsub?token=wrong', pushMessage({}), bearer);
            assert.equal(response.status, 400);
            assert.equal(await response.text(), 'Invalid request');
            assert.deepEqual(current.recorded.verifiedTokens, []);
            assert.deepEqual(current.recorded.inserted, []);
        });

        it('rejects a push without an Authorization header', async () => {
            const response = await post('/pubsub?token=secret', pushMessage({}));
            assert.equal(response.status, 400);
            assert.equal(await response.text(), 'Invalid token');
            assert.deepEqual(current.recorded.inserted, []);
        });

        it('rejects a push whose JWT fails verification', async () => {
            current = fakeDependencies({ verifyIdToken: async () => { throw new Error('bad signature'); } });
            const response = await post('/pubsub?token=secret', pushMessage({}), bearer);
            assert.equal(response.status, 400);
            assert.equal(await response.text(), 'Invalid token');
            assert.deepEqual(current.recorded.inserted, []);
        });

        it('decodes the message and inserts it with the publish timestamp', async () => {
            const response = await post('/pubsub?token=secret', pushMessage({ event: 'launch', app: 'tagmaster' }), bearer);
            assert.equal(response.status, 200);
            assert.deepEqual(current.recorded.verifiedTokens, ['jwt-from-pubsub']);
            assert.deepEqual(current.recorded.inserted, [{
                rows: [{ event: 'launch', app: 'tagmaster', publish_timestamp: '2026-09-19T00:00:00Z' }],
                options: { ignoreUnknownValues: true },
            }]);
        });

        it('acknowledges the push when BigQuery reports a partial failure', async () => {
            current = fakeDependencies({
                insertRows: async () => { throw new PartialFailureError({ code: 200, errors: [], response: {} as never }); },
            });
            const response = await post('/pubsub?token=secret', pushMessage({ event: 'launch' }), bearer);
            assert.equal(response.status, 200);
        });

        it('returns 500 so Pub/Sub retries when the insert fails outright', async () => {
            current = fakeDependencies({ insertRows: async () => { throw new Error('BigQuery unavailable'); } });
            const response = await post('/pubsub?token=secret', pushMessage({ event: 'launch' }), bearer);
            assert.equal(response.status, 500);
            assert.equal(await response.text(), '');
        });
    });
});
