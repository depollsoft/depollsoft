import { OAuth2Client } from 'google-auth-library';
import { PubSub } from '@google-cloud/pubsub';
import { BigQuery } from '@google-cloud/bigquery';
import { lookup } from 'geoip-lite';
import { createAnalyticsRouter } from './analytics-router';

// Production wiring. The router itself lives in analytics-router.ts so it can
// be unit tested without constructing Google Cloud clients.
const authClient = new OAuth2Client();
const pubSub = new PubSub();
const bigQuery = new BigQuery();
const analyticsTable = bigQuery.dataset('app_analytics').table('events');
const { PUBSUB_VERIFICATION_TOKEN } = process.env;

const router = createAnalyticsRouter({
    // A fresh Topic per request, exactly as before: a shared Topic instance
    // would batch concurrent publishes into one RPC.
    publishJSON: (event) => pubSub.topic('analytics').publishJSON(event),
    insertRows: (rows, options) => analyticsTable.insert(rows, options),
    verifyIdToken: (idToken) => authClient.verifyIdToken({ idToken }),
    lookup,
    verificationToken: PUBSUB_VERIFICATION_TOKEN,
});

export const analyticsRouter = router;
export default router;
