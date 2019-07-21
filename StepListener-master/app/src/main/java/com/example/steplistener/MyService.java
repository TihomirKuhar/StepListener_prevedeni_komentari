package com.example.steplistener;

import android.app.Activity;
import android.app.AppComponentFactory;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import static com.example.steplistener.Notifications.CHANNEL_1_ID;


public class MyService extends Service implements SensorEventListener, StepListener{
    private static final int NOTIF_ID = 2;
    private StepDetector simpleStepDetector;//instance of step detector
    private SensorManager sensorManager;//instance of sensormanager
    private Sensor accel;//instance of sensor
    private boolean first = true;//first start
    private static int numSteps = 0;//will hold number of steps
    private static double numKilo = 0.0;//will hold number of kilometers
    private int numStepsInKilometer = 1250;//help counter to generate notification ecery 1km
    private int numStepsInOneTenthOfKilometer = 125;//help counter to update the value of kilometers every 100m
    private NotificationManagerCompat notificationManager;//notification instance
    private boolean sensorOn = false;//will help with sensor off on logic
    private String beginDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());//will store the date aplication is first started


    public static int getNumberSteps(){
        return numSteps;
    }//returns number of steps taken
    public static double getNumKilo(){
        return  numKilo;
    }//returns number of km done
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationManager=NotificationManagerCompat.from(this);
        //if(first)resetCounters();//check if this is the first time on start command is called if yes set all values to default

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //create a stepdetector and make it listen for changes in the sensor
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);

        if(!sensorOn) {
            sensorManager.registerListener((SensorEventListener) MyService.this, accel, SensorManager.SENSOR_DELAY_FASTEST);
            sensorOn=true;
        }
        startForeground();
        return super.onStartCommand(intent, flags, startId);
        //return START_STICKY;

    }
    //creating a foreground notification, makes the app run in background on api s 26 and higher
    private void startForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
                CHANNEL_1_ID) // don't forget create a notification channel first
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_accessibility_black_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Running in background")
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build());

    }
    //check if needed

    @Override
    public void onDestroy() {
        super.onDestroy();
        generateNotification("Service Stoped","Sensor off");
        sensorManager.unregisterListener(MyService.this);
        resetCounters();
        stopSelf();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override
    public void step(long timeNs) {
        numSteps++;//increasing number od steps each step
        numStepsInKilometer--;//decreasing number of steps in a kilometer each step
        numStepsInOneTenthOfKilometer--;//decreasing number of steps in a tenth of a kilometer each step

        checkTimeForReset();
        //updating number of kilometers by 0.1 every 125 steps
        if(numStepsInOneTenthOfKilometer == 0){
            numStepsInOneTenthOfKilometer = 125;
            numKilo = numKilo + 0.1;

        }
        //generating notification to be called every kilometer user has walked
        if (numStepsInKilometer == 0){
            //kilometar +1
            numStepsInKilometer = 1250;

            generateNotification("Steps: "+numSteps,new DecimalFormat("##.##").format(numKilo)+" km");
        }
    }

    //method resets all coumters and text views to 0,"",current date
    public void resetCounters(){
        first = false;
        numSteps = 0;
        numKilo = 0.0;
        numStepsInKilometer = 1250;
        numStepsInOneTenthOfKilometer = 125;
        //we are storing a date on which aplication was started
        beginDate=new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

    }

    //method for creating notification and notifying the user with current number of steps and kilometers traveled
    public void generateNotification(String textTitle, String textMessage){
        String title=textTitle;//holds title of the notification, will appear on top
        String message=textMessage;//holds content of the notification, will appear in a window

        Notification notification=new NotificationCompat.Builder(this,CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_accessibility_black_24dp)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();

        notificationManager.notify(1,notification); //notifying the system with costum made notification
    }

    //method checks if the current date is the same value as the date the aplication was started if  not notify the user with the current values and reset
    public void checkTimeForReset(){
        String endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if(!beginDate.equals(endDate)){

            generateNotification("In Last 24hrs you took: "+numSteps+" steps!",numKilo+" km covered!");

            resetCounters();

        }
    }

}
