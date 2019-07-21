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
    private StepDetector simpleStepDetector;//Instanca StepDetector-a
    private SensorManager sensorManager;//Instanca sensorManager-a
    private Sensor accel;//Instanca senzora
    private boolean first = true;//Prvi start
    private static int numSteps = 0;//Sadržavat će broj koraka
    private static double numKilo = 0.0;//Sadržavat će broj kilometara
    private int numStepsInKilometer = 1250;//Pomoćni brojač kilometara, vrijednosti će se oduzimati
    private int numStepsInOneTenthOfKilometer = 125;//Pomoćni brojač svake desetine kilometara
    private NotificationManagerCompat notificationManager;//Instanca notifikacije
    private boolean sensorOn = false;//Pomagat će oko senzor upaljen/ugašen logike
    //Sadržavat će datum prilikom prvog pokretanja aplikacije, radi notifikacije u slučaju promijene tekućeg dana
    private String beginDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());


    public static int getNumberSteps(){
        return numSteps;
    }//Vraća broj koraka
    public static double getNumKilo(){
        return  numKilo;
    }//Vraća broj kilometara
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        notificationManager=NotificationManagerCompat.from(this);
        //Dohvati instancu sensorManagera
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //Kreiraj stepDetector koji će osluškivati promijene na senzoru
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
    //Kreiramo notifikaciju koja osigurava pozadinsko pokretanje na api-jima 26 i više
    private void startForeground() {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        startForeground(NOTIF_ID, new NotificationCompat.Builder(this,
                CHANNEL_1_ID) // mora postojati notifikacijski kanal
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_accessibility_black_24dp)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Running in background")
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build());
    }
    //Kod prestanka rada, notifikacija o prestanku, gašenje servisa, resetiranje vrijednosti, zaustavljanje servisa
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
        numSteps++;//Broj koraka +1, svaki korak
        numStepsInKilometer--;//Smanjenje broja koraka u jednom kilometru za 1
        numStepsInOneTenthOfKilometer--;//Smanjenje broja koraka u desetini kilometra za 1

        checkTimeForReset();
        //Ažuriranje broja kilometara za 0.1 svakih 125 koraka
        if(numStepsInOneTenthOfKilometer == 0){
            numStepsInOneTenthOfKilometer = 125;
            numKilo = numKilo + 0.1;

        }
        //Generiranje notifikacije za ako korisnik prijeđe udaljenost od 1 kilometra
        if (numStepsInKilometer == 0){
            //kilometar +1
            numStepsInKilometer = 1250;

            generateNotification("Steps: "+numSteps,new DecimalFormat("##.##").format(numKilo)+" km");
        }
    }

    //Metoda resetira sve vrijednosti brojača i tekstualnih polja na 0, „ „ ili trenutni
    public void resetCounters(){
        first = false;
        numSteps = 0;
        numKilo = 0.0;
        numStepsInKilometer = 1250;
        numStepsInOneTenthOfKilometer = 125;
        beginDate=new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

    }

    //Metoda za kreiranje notifikacija i obavještavanje korisnika o trenutnim vrijednostima broja koraka i udaljenosti
    public void generateNotification(String textTitle, String textMessage){
        String title=textTitle;//Naslov notifikacije, dio poruke koji se prikazuje na vrhu
        String message=textMessage;//Sadržaj notifikacije, pokazuje se unutar prozora

        Notification notification=new NotificationCompat.Builder(this,CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_accessibility_black_24dp)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .build();

        notificationManager.notify(1,notification); //Obavještavanje sustava novo kreiranom notifikacijom
    }

    //Metoda provjerava da li je trenutni datum isti kao i onaj kada je aplikacija pokrenuta,
    //ako nije obavještava se korisnika s trenutnim vrijednostima broja koraka i udaljenosti
    public void checkTimeForReset(){
        String endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if(!beginDate.equals(endDate)){

            generateNotification("In Last 24hrs you took: "+numSteps+" steps!",numKilo+" km covered!");

            resetCounters();

        }
    }

}
