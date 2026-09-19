import express, { Router } from 'express';
import { PartialFailureError } from '@google-cloud/common/build/src/util';

/** Subset of `geoip-lite`'s lookup result that the router reads. */
export interface GeoLookupResult {
    range: [number, number];
    country: string;
    region: string;
    eu: string;
    timezone: string;
    city: string;
    ll: [number, number];
    metro: number;
    area: number;
}

export interface AnalyticsDependencies {
    /** Publishes the enriched event; `PubSub.topic('analytics')` in production. */
    publishJSON(event: object): Promise<unknown>;
    /** Inserts rows into the analytics table; BigQuery `Table#insert` in production. */
    insertRows(rows: object[], options: { ignoreUnknownValues: boolean }): Promise<unknown>;
    /** Verifies the Pub/Sub push JWT; `OAuth2Client#verifyIdToken` in production. */
    verifyIdToken(idToken: string): Promise<unknown>;
    /** Resolves an IP address to a location; `geoip-lite`'s `lookup` in production. */
    lookup(ip: string): GeoLookupResult | null;
    /** Shared secret expected in the `token` query parameter of Pub/Sub pushes. */
    verificationToken: string | undefined;
}

export function locationFromLookup(location: GeoLookupResult | null) {
    return location ? {
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
    } : null;
}

export function createAnalyticsRouter(deps: AnalyticsDependencies): Router {
    return express.Router()
        .use(express.json())
        .post('/', async (req, res) => {
            const ip = req.headers['x-forwarded-for'] as string ?? req.socket.remoteAddress ?? '127.0.0.1';
            await deps.publishJSON({
                ...req.body,
                location: locationFromLookup(deps.lookup(ip)),
            });
            res.status(201).send();
        })
        .post('/pubsub', async (req, res) => {
            // Verify that the request originates from the application.
            if (req.query.token !== deps.verificationToken) {
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
                await deps.verifyIdToken(token);

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
                await deps.insertRows([
                    {
                        ...messageData,
                        publish_timestamp: req.body.message.publishTime,
                    }
                ], {
                    ignoreUnknownValues: true,
                });
            } catch (e) {
                console.error(JSON.stringify(e));
                if (!(e instanceof PartialFailureError)) {
                    res.status(500).send();
                    return;
                }
            }

            res.status(200).send();
        });
}
