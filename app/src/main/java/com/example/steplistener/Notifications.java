package com.example.steplistener;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class Notifications extends Application {
    public static final String CHANNEL_1_ID="channel_1";//Inicijaliziranje kanala
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }
    //Kreiranje notifikacijskog kanala s imenom channel_1
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BASE){
            NotificationChannel channel1 = new NotificationChannel(
                    CHANNEL_1_ID,
                    "Channel 1",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel1.setDescription("Channel1 displays numbers of steps user has made alongside distance covered");
            NotificationManager manager=getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel1);
        }
    }
}
