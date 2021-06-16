import express from 'express';
import { OAuth2Client } from 'google-auth-library';
import { PubSub } from '@google-cloud/pubsub';
import { BigQuery } from '@google-cloud/bigquery';
import { lookup } from 'geoip-lite';
import { PartialFailureError } from '@google-cloud/common/build/src/util';

const authClient = new OAuth2Client();
const pubSub = new PubSub();
const bigQuery = new BigQuery();
const analyticsTable = bigQuery.dataset('app_analytics').table('events');
const { PUBSUB_VERIFICATION_TOKEN } = process.env;

const router = express.Router()
    .use(express.json())
    .post('/', async (req, res) => {
        const ip = req.headers['x-forwarded-for'] as string ?? req.socket.remoteAddress ?? '127.0.0.1';
        const location = lookup(ip);
        await pubSub.topic('analytics').publishJSON({
            ...req.body,
            location: location ? {
                range: {
                    low: location.range[0],
                    high: location.range[1]
                },
                country: location.country,
                region: location.region,
                eu: location.eu == '1',
                timezone: location.timezone,
                city: location.city,
                coordinates: {
                    latitude: location.ll[0],
                    longitude: location.ll[1],
                    radius: location.area
                },
                metro: location.metro
            } : null,
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

        try {
            await analyticsTable.insert([
                {
                    ...messageData,
                    publish_timestamp: req.body.message.publishTime,
                }
            ], {
                ignoreUnknownValues: true,
            });
        } catch (e) {
            if (e instanceof PartialFailureError) {
                console.error(e);
            } else {
                throw e;
            }
        }

        res.status(200).send();
    });

export const analyticsRouter = router;
export default router;