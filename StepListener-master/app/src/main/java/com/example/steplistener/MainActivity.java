package com.example.steplistener;


import android.content.Intent;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements SensorEventListener, StepListener {
    private StepDetector simpleStepDetector;//instance of step detector
    private SensorManager sensorManager;//instance of sensormanager
    private Sensor accel;//instance of sensor
    private static int numberSteps;//will hold number of steps
    private static double numberKilo;//will hold number of kilometers
    private TextView TvSteps;//Text view where number of steps will be displayed
    private TextView Kilometer;//Text view where number of kilometers will be displayed
    private boolean firstStart;//helper used in on start to display empty string on Steps and Kilometers
    private boolean sensorOn=false;//helper to track sensor activity
    private String beginDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());//will store the date aplication is first started



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firstStart=true;

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);
        //sensorManager.registerListener((SensorEventListener) MainActivity.this, accel, SensorManager.SENSOR_DELAY_FASTEST);

        TvSteps = (TextView) findViewById(R.id.stepsValue);//Text view that displays number of steps
        Kilometer = (TextView) findViewById(R.id.kilometer);//Text view that displays number of kilometers

        Button BtnStart = (Button) findViewById(R.id.btn_start);//start button
        Button BtnStop = (Button) findViewById(R.id.btn_stop);//stop button
        //if start button is clicked
        BtnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if(!sensorOn) {
                    sensorManager.registerListener((SensorEventListener) MainActivity.this, accel, SensorManager.SENSOR_DELAY_FASTEST);
                    startService(new Intent(MainActivity.this,MyService.class));
                    displayStepsKilometers(0,0.0);
                    sensorOn=true;
                }
                //displayStepsKilometers(0,0.0);
            }
        });

        //if stop button is clicked
        BtnStop.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if(sensorOn){
                    sensorManager.unregisterListener(MainActivity.this);
                    stopService(new Intent(MainActivity.this,MyService.class));
                    sensorOn=false;
                }
                TvSteps.setText("");
                Kilometer.setText("");
            }
        });
    }
    //overriding on start in order to turn sensor on if user navigated to another app etc.
    //overriding on start in order to get number of steps user took while app wasn't in focus
    @Override
    protected void onStart() {
        super.onStart();
        if(firstStart){
            TvSteps.setText("");
            Kilometer.setText("");
            firstStart=false;
            return;
        }

        if(!sensorOn) {
            sensorManager.registerListener((SensorEventListener) MainActivity.this, accel, SensorManager.SENSOR_DELAY_FASTEST);
            sensorOn=true;
        }

        if(!firstStart) {
            numberSteps = MyService.getNumberSteps();
            numberKilo = MyService.getNumKilo();
            displayStepsKilometers(numberSteps, numberKilo);
        }
        checkCalendar();

    }
    //checking to see if the date of the last calendar update is the same as current
    private void checkCalendar() {
        String endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if(!beginDate.equals(endDate)){
            launchCalendar();
        }
    }
    //launching new calendar intent and reseting the date
    public void launchCalendar(){
        //reseting begin date
        beginDate=new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        //new instance of Calendar
        Calendar cal = new GregorianCalendar();
        cal.setTime(new Date());//current system time
        cal.add(Calendar.DATE, -1);//day -1 since we are updating at 00:00 next day
        String numSteps=""+numberSteps;
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);
        intent.putExtra(CalendarContract.Events.TITLE, "Step Listener, Todays update");
        intent.putExtra(CalendarContract.Events.DESCRIPTION, "Today you took "+numSteps+" steps!");
        intent.putExtra(CalendarContract.Events.ALL_DAY, false);
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,cal.getTime().getTime());
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME,cal.getTime().getTime() + 600000);

        startActivity(intent);
    }

    //overriding on stop in order to unregister sensor while app is not being used
    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(MainActivity.this);
        sensorOn=false;
    }
    //method displays Current number of steps and kilometers user has walked
    public void displayStepsKilometers(int nmSteps, double nmKilo){
        TvSteps = (TextView) findViewById(R.id.stepsValue);//Text view that displays number of steps
        Kilometer = (TextView) findViewById(R.id.kilometer);//Text view that displays number of kilometers
        numberSteps = nmSteps;
        numberKilo = nmKilo;
        TvSteps.setText("" + String.format("%d", numberSteps));
        Kilometer.setText("" + new DecimalFormat("##.##").format(numberKilo) + " km");
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
    //every step user takes update number of steps and kilometers on screen
    @Override
    public void step(long timeNs) {
        numberSteps=MyService.getNumberSteps();
        numberKilo=MyService.getNumKilo();
        displayStepsKilometers(numberSteps,numberKilo);
    }


}
