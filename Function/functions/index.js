"use strict"

const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp(functions.config().firebase);

exports.sendNotification = functions.database.ref("/Notifications/{userId}/{notificationId}").onWrite((change, context) => {
    
    const userId = context.params.userId;
    const notificationId = context.params.notificationId;
    let fromUserId;

    console.log("Notification being sent to:", userId);

    if (!change.after.exists) {
        return console.log("A notification has been deleted from the database:", notificationId);
    }

    return admin.database().ref(`/Notifications/${userId}/${notificationId}`).once("value").then((fromUserResult) => {
        const fromUserId = fromUserResult.val().from;
        console.log("You have new notification from:", fromUserId);

        const userQuery = admin.database().ref(`Users/${fromUserId}/name`).once("value");
        const deviceToken = admin.database().ref(`/Users/${userId}/deviceToken`).once("value");

        return Promise.all([userQuery, deviceToken]).then((result) => {
            const userName = result[0].val();
            const tokenId = result[1].val();

            const payload = {
                notification: {
                    title: "Friend Request",
                    body: `${userName} has sent you a request.`,
                    icon: "default",
                    click_action: "com.example.firebasechat_TARGET_NOTIFICATION"
                },
                data: {
                    user_id: fromUserId
                }
            };
        
            return admin.messaging().sendToDevice(tokenId, payload).then((response) => {
                return console.log("This was the notification feature.");
            });
        });
    });
});
