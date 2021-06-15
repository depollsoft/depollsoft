import express from 'express';
import { OAuth2Client } from 'google-auth-library';
import { PubSub } from '@google-cloud/pubsub';
import { BigQuery } from '@google-cloud/bigquery';
import { lookup } from 'geoip-lite';

const authClient = new OAuth2Client();
const pubSub = new PubSub();
const bigQuery = new BigQuery();
const analyticsTable = bigQuery.dataset('app_analytics').table('events');
const { PUBSUB_VERIFICATION_TOKEN } = process.env;

const router = express.Router()
    .use(express.json())
    .post('/', async (req, res) => {
        console.log(JSON.stringify(req, null, '  '));
        await pubSub.topic('analytics').publishJSON({
            ...req.body,
            location: lookup(req.ip),
        });
        res.status(201).send();
    })
    .post('/pubsub', async (req, res) => {
        // Verify that the request originates from the application.
        if (req.query.token !== PUBSUB_VERIFICATION_TOKEN) {
            res.status(400).send('Invalid request');
            return;
        }

        // Verify that the push request originates from Cloud Pub/Sub.
        try {
            // Get the Cloud Pub/Sub-generated JWT in the "Authorization" header.
            const bearer = req.header('Authorization');
            if (!bearer) {
                throw 'Invalid token';
            }
            const [, token] = bearer.match(/Bearer (.*)/) || ['', ''];

            // Verify and decode the JWT.
            // Note: For high volume push requests, it would save some network
            // overhead if you verify the tokens offline by decoding them using
            // Google's Public Cert; caching already seen tokens works best when
            // a large volume of messages have prompted a single push server to
            // handle them, in which case they would all share the same token for
            // a limited time window.
            const ticket = await authClient.verifyIdToken({
                idToken: token,
            });

            const claim = ticket.getPayload();

            // IMPORTANT: you should validate claim details not covered
            // by signature and audience verification above, including:
            //   - Ensure that `claim.email` is equal to the expected service
            //     account set up in the push subscription settings.
            //   - Ensure that `claim.email_verified` is set to true.
        } catch (e) {
            res.status(400).send('Invalid token');
            return;
        }

        // The message is a unicode string encoded in base64.
        const message = Buffer.from(req.body.message.data, 'base64').toString(
            'utf-8'
        );

        const messageData = JSON.parse(message);

        await analyticsTable.insert([
            {
                ...messageData,
                publish_timestamp: req.body.message.publishTime,
            }
        ]);

        res.status(200).send();
    });

export const analyticsRouter = router;
export default router;