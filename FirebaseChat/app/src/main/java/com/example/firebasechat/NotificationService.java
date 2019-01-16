package com.example.firebasechat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NotificationService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String notificationTitle = remoteMessage.getNotification().getTitle();
        String notificationMessage = remoteMessage.getNotification().getBody();
        String clickAction = remoteMessage.getNotification().getClickAction();
        String fromUserId = remoteMessage.getData().get(getString(R.string.user_id));

        createNotificationChannel(getString(R.string.friend_request_channel), "Friend Requests", "Notifications of your friend requests.");

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, getString(R.string.friend_request_channel))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(notificationTitle)
                .setContentText(notificationMessage);

        Intent profileIntent = new Intent(clickAction);
        profileIntent.putExtra(getString(R.string.user_id), fromUserId);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, profileIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setContentIntent(pendingIntent);

        int notificationId = (int) System.currentTimeMillis();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(notificationId, notificationBuilder.build());
    }

    private void createNotificationChannel(String channelId, String channelName, String channelDescription) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
            channel.setDescription(channelDescription);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
