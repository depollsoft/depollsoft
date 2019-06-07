import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';
import { MongoClient } from 'mongodb';


admin.initializeApp();

async function doParseImport(
    collectionName: string,
    lastUpdateField: string,
    batchSize: number,
    processBatch: (batch: any[]) => Promise<void>) {
    const mongoClient = await MongoClient.connect(functions.config().parse.databaseurl, {
        useNewUrlParser: true
    });
    const db = mongoClient.db('pitchperfect-azure-2');
    const collection = db.collection(collectionName);

    const lastWriteRef = admin.firestore().doc('appData/parseImport');

    let count = 0;
    do {
        const lastWrite = await lastWriteRef.get();
        let prevTimestamp = admin.firestore.Timestamp.fromMillis(0);
        if (lastWrite) {
            const value = lastWrite.get(lastUpdateField) as admin.firestore.Timestamp;
            if (value) {
                prevTimestamp = value;
            }
        }

        const entries = await collection.find({})
            .sort({ '_updated_at': 1 })
            .filter({ '_updated_at': { '$gt': prevTimestamp.toDate() } })
            .limit(batchSize)
            .toArray();
        count = entries.length;

        await processBatch(entries);

        prevTimestamp = admin.firestore.Timestamp.fromMillis(
            Math.max(prevTimestamp.toMillis(), ...entries.map(v => v._updated_at.getTime())));

        await lastWriteRef.set({
            [lastUpdateField]: prevTimestamp
        }, { merge: true });
        console.log(`Imported ${count} ${collectionName} changes`);
    } while (count > 0);
}

exports.parseImport = functions.pubsub.schedule('every 5 minutes').onRun(async context => {
    const promises = [];
    promises.push(doParseImport('_User', 'lastUserUpdate', 500, async (users) => {
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

            imports.push({
                uid: user._id,
                providerData: [{
                    providerId: 'facebook.com',
                    uid: user._auth_data_facebook.id
                }],

            });
        }

        await userPreferenceBatch.commit();
        await admin.auth().importUsers(imports);
    }));

    promises.push(doParseImport('SongList', 'lastSongUpdate', 500, async (songLists) => {
        const songPromises = [];
        for (const songList of songLists) {
            const batch = admin.firestore().batch();
            const owner = songList._p_owner.split('$')[1];
            batch.set(admin.firestore().doc(`users/${owner}/songLists/default`), {
                name: "Default"
            }, { merge: true });

            let order = 0;
            for (const song of songList.songs['*items']) {
                batch.set(admin.firestore().doc(`users/${owner}/songLists/default/songs/${song.Id}`), {
                    name: song.Name || "",
                    key: song.Key,
                    order: order++
                }, { merge: true })
            }
            songPromises.push(batch.commit());
        }
        await Promise.all(songPromises);
    }));
    await Promise.all(promises);
});