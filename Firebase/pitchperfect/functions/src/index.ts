import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { MongoClient } from 'mongodb';
import fetch from 'node-fetch'


admin.initializeApp();

async function doParseImport(
    collectionName: string,
    lastUpdateField: string,
    batchSize: number,
    processBatch: (batch: any[], isFirstTime: boolean) => Promise<void>) {
    const mongoClient = await MongoClient.connect(functions.config().parse.databaseurl);
    const db = mongoClient.db('pitchperfect-azure-2');
    const collection = db.collection(collectionName);

    const lastWriteRef = admin.firestore().doc('appData/parseImport');

    let count = 0;
    let isFirstTime = true;
    do {
        const lastWrite = await lastWriteRef.get();
        let prevTimestamp = admin.firestore.Timestamp.fromMillis(0);
        if (lastWrite) {
            const value = lastWrite.get(lastUpdateField) as admin.firestore.Timestamp;
            if (value) {
                prevTimestamp = value;
                isFirstTime = false;
            }
        }

        const entries = await collection.find({})
            .sort({ '_updated_at': 1 })
            .filter({ '_updated_at': { '$gt': prevTimestamp.toDate() } })
            .limit(batchSize)
            .toArray();
        count = entries.length;

        await processBatch(entries, isFirstTime);

        prevTimestamp = admin.firestore.Timestamp.fromMillis(
            Math.max(prevTimestamp.toMillis(), ...entries.map(v => v._updated_at.getTime())));

        await lastWriteRef.set({
            [lastUpdateField]: prevTimestamp
        }, { merge: true });
        console.log(`Imported ${count} ${collectionName} changes`);
    } while (count > 0);
    await mongoClient.close();
}

exports.parseImport = functions.runWith({
    timeoutSeconds: 180
}).pubsub.schedule('every 5 minutes').onRun(async context => {
    const promises = [];
    promises.push(doParseImport('_User', 'lastUserUpdate', 100, async (users, isFirstTime) => {
        if (isFirstTime) {
            // clear auth users
            while (true) {
                const results = await admin.auth().listUsers(1000);
                if (results.users.length === 0) {
                    break;
                }
                try {
                    await admin.auth().deleteUsers(results.users.map(user => user.uid));
                } catch (e) {
                    console.error(e);
                    console.error("Waiting 1 second");
                    await new Promise(r => setTimeout(r, 1000));
                }
                console.log(`Deleted ${results.users.length} users from Auth`);
            }
        }

        const imports: admin.auth.UserImportRecord[] = [];
        const userPreferenceBatch = admin.firestore().batch();

        for (const user of users) {
            const toggleNotes = user.ToggleNote || false;
            const wakeLock = user.WakeLock || false;

            if (!user._auth_data_facebook) {
                console.log('User missing auth data: ');
                console.log(user);
                continue;
            }
            userPreferenceBatch.set(
                admin.firestore().doc(`users/${user._id}`),
                { wakeLock, toggleNotes },
                { merge: true });

            let existingUser = null;
            try {
                existingUser = await admin.auth().getUser(user._id);
            } catch { }
            if (!existingUser) {
                imports.push({
                    uid: user._id,
                    providerData: [{
                        providerId: 'facebook.com',
                        uid: user._auth_data_facebook.id
                    }],
                });
            }
        }

        await userPreferenceBatch.commit();
        await admin.auth().importUsers(imports);
    }));

    promises.push(doParseImport('SongList', 'lastSongUpdate', 500, async (songLists, isFirstTime) => {
        const songPromises = [];
        for (const songList of songLists) {
            const owner = songList._p_owner.split('$')[1];
            songPromises.push(admin.firestore().doc(`users/${owner}/songLists/default`).set({
                name: "Default",
                songs: songList.songs['*items']
            }, { merge: true }));
        }
        await Promise.all(songPromises);
    }));
    await Promise.all(promises);
});

exports.exchangeAuthToken = functions.https.onCall(async (data, context) => {
    const result = await fetch(`${functions.config().parse.baseurl}/users/me`, {
        headers: {
            'X-Parse-Application-Id': functions.config().parse.appid,
            'X-Parse-Master-Key': functions.config().parse.masterkey,
            'X-Parse-Session-Token': data.token
        },
        method: 'GET'
    });
    if (!result.ok) {
        throw new functions.https.HttpsError('permission-denied', result.statusText);
    }
    const user = await result.json();
    return { token: await admin.auth().createCustomToken(user.objectId) };
});