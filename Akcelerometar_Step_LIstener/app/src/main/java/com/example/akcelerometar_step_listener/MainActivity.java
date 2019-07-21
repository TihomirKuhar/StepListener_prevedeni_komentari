package com.example.akcelerometar_step_listener;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity implements SensorEventListener {

        private SensorManager mSensorManager;
        private Sensor mSensor;
        private boolean isSensorPresent = false;
        private TextView mStepsSinceReboot;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.MainActivity);

            mStepsSinceReboot =
                    (TextView)findViewById(R.id.stepssincereboot);

            mSensorManager = (SensorManager)
                    this.getSystemService(Context.SENSOR_SERVICE);
            if(mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
                    != null)
            {
                mSensor =
                        mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                isSensorPresent = true;
            }
            else
            {
                isSensorPresent = false;
            }

        }

        @Override
        protected void onResume() {
            super.onResume();
            if(isSensorPresent)
            {
                mSensorManager.registerListener(this, mSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
        }

        @Override
        protected void onPause() {
            super.onPause();
            if(isSensorPresent)
            {
                mSensorManager.unregisterListener(this);
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            mStepsSinceReboot.setText(String.valueOf(event.values[0]));

        }
