import * as functions from "firebase-functions/v1";
import { getAuth } from "firebase-admin/auth";
import { initializeApp } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

initializeApp();

const auth = getAuth();
const firestore = getFirestore();

exports.deleteUser = functions.https.onCall(async (_, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError(
            "permission-denied",
            "No user logged in",
        );
    }
    await firestore.recursiveDelete(
        firestore.doc(`/users/${context.auth.uid}`),
    );
    await auth.deleteUser(context.auth.uid);
    return {};
});
