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
    private StepDetector simpleStepDetector;//Instanca StepDetector-a
    private SensorManager sensorManager;//Instanca sensorManager-a
    private Sensor accel;//Instanca senzora
    private static int numberSteps;//Sadržavat će ukupni broj koraka
    private static double numberKilo;//Sadržavat će ukupni broj kilometara
    private TextView TvSteps;//Tekstualno polje unutar kojeg će se prikazivati broj prijeđenih koraka
    private TextView Kilometer;//Tekstualno polje unutar kojeg će se prikazivati prijeđena udaljenost u kilometrima
    private boolean firstStart;//Pomoćna varijabla koja se koristi unutar OnStart metode za prikaz praznog Stringa
    private boolean sensorOn=false;//Pomoćna varijabla za uvid u stanje senzora
    //Sadržavat će datum prilikom prvog pokretanja aplikacije, radi notifikacije u slučaju promijene tekućeg dana
    private String beginDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        firstStart=true;
        //Dohvati instancu SensorManagera
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);
        //Povezivanje varijable tekstualnog polja za prikaz koraka sa samim poljem definiram XML-om preko ID-a
        TvSteps = (TextView) findViewById(R.id.stepsValue);
        //Povezivanje varijable tekstualnog polja za prikaz udaljenosti sa samim poljem definiram XML-om preko ID-a
        Kilometer = (TextView) findViewById(R.id.kilometer);
        //Povezivanje varijable Gumba za pokretanje s njegovom XML inačicom preko ID-a
        Button BtnStart = (Button) findViewById(R.id.btn_start);
        // Povezivanje varijable Gumba za zaustavljanje s njegovom XML inačicom preko ID-a
        Button BtnStop = (Button) findViewById(R.id.btn_stop);
        //Pritisak gumba za pokretanje
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

        //Pritisak gumba za zaustavljanje
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
    //Provjera tekućeg datuma i datuma pokretanja aplikacije
    private void checkCalendar() {
        String endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        if(!beginDate.equals(endDate)){
            launchCalendar();
        }
    }
    //Pokretanje kalendara i upis podataka u isti
    public void launchCalendar(){
        //Resetiranje datuma aplikacije
        beginDate=new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        //Nova instanca kalendara
        Calendar cal = new GregorianCalendar();
        cal.setTime(new Date());//Trenutno vrijeme sustava
        cal.add(Calendar.DATE, -1);//Dan -1 buduci da želimo upisati vrijednosti za poslijednja 24 sata
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
    //Metoda prikazuje trenutne vrijednosti broja koraka i udaljenosti koje je korisnik napravio
    public void displayStepsKilometers(int nmSteps, double nmKilo){
        TvSteps = (TextView) findViewById(R.id.stepsValue);//Prikazuje broj koraka
        Kilometer = (TextView) findViewById(R.id.kilometer);//Prikazuje broj kilometara
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
    //Svaki korak koji korisnik napravi dok je ekran aplikacije uključen ažurira ukupni broj koraka
    @Override
    public void step(long timeNs) {
        numberSteps=MyService.getNumberSteps();
        numberKilo=MyService.getNumKilo();
        displayStepsKilometers(numberSteps,numberKilo);
    }
}
