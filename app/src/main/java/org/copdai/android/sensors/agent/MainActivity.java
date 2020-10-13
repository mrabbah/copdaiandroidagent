package org.copdai.android.sensors.agent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.view.Surface;

public class MainActivity extends AppCompatActivity implements SensorEventListener, CompoundButton.OnCheckedChangeListener {

    private SensorManager sensorManager;
    private Sensor mSensorAccelerometer, mSensorGyroscope, mSensorMagnetometer/*, linearAccelerometer*/;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private BootReceiver bootReceiver;
    private float[] mMagnetometerData = new float[3];
    private float[] mAccelerometerData = new float[3];
    private float[] mGyroscopeData = new float[3];
    private int delay;
    private int refreshInterval;
    private long lastUpdate;
    private boolean convert = false;
    final String TAG = "MainActivity";

    // System display. Need this for determining rotation.
    private Display mDisplay;


    TextView xValue, yValue, zValue, xGyroValue, yGyroValue, zGyroValue, xRotationValue, yRotationValue, zRotationValue,latitude,longitude;
    Switch switchConversion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        delay = SensorManager.SENSOR_DELAY_NORMAL;
        refreshInterval = 100; //100ms

        switchConversion = (Switch) findViewById(R.id.switchConversion);
        switchConversion.setOnCheckedChangeListener(this);
        convert = switchConversion.isChecked();


        xValue = (TextView) findViewById(R.id.xValue);
        yValue = (TextView) findViewById(R.id.yValue);
        zValue = (TextView) findViewById(R.id.zValue);

        xGyroValue = (TextView) findViewById(R.id.xGyroValue);
        yGyroValue = (TextView) findViewById(R.id.yGyroValue);
        zGyroValue = (TextView) findViewById(R.id.zGyroValue);

        xRotationValue = (TextView) findViewById(R.id.xRotationValue);
        yRotationValue = (TextView) findViewById(R.id.yRotationValue);
        zRotationValue = (TextView) findViewById(R.id.zRotationValue);

        latitude=(TextView) findViewById(R.id.latitude);
        longitude=(TextView) findViewById(R.id.longitude);


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mSensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorMagnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean isGPS_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);



        if(mSensorAccelerometer !=null){
            sensorManager.registerListener(MainActivity.this, mSensorAccelerometer,delay);
        }
        else{
            xValue.setText("Accelerometer not supported");
            yValue.setText("Accelerometer not supported");
            zValue.setText("Accelerometer not supported");
        }

        /*if(linearAccelerometer!=null){
            sensorManager.registerListener(MainActivity.this,linearAccelerometer, delay);
        }
        else{
            xValue.setText("Accelerometer not supported");
            yValue.setText("Accelerometer not supported");
            zValue.setText("Accelerometer not supported");
        }*/

        if(mSensorGyroscope !=null){
            sensorManager.registerListener(MainActivity.this, mSensorGyroscope,delay);
        }
        else{
            xGyroValue.setText("Gyroscope not supported");
            yGyroValue.setText("Gyroscope not supported");
            zGyroValue.setText("Gyroscope not supported");
        }

        if(mSensorGyroscope == null || mSensorAccelerometer == null){
            xRotationValue.setText("Rotation sensor not available");
            yRotationValue.setText("Rotation sensor not available");
            zRotationValue.setText("Rotation sensor not available");
        }
        if(isGPS_enabled){
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    longitude.setText(String.valueOf(location.getLongitude()));
                    latitude.setText(String.valueOf(location.getLatitude()));

                }
            };
        }

        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.ACCESS_FINE_LOCATION},1);
        }
        else{
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,locationListener);
            } catch (Exception ex) {
                longitude.setText("Pb to get GPS location");
                latitude.setText(ex.getMessage());
            }
        }

        // Get the display from the window manager (for rotation).
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();

        lastUpdate = System.currentTimeMillis();
        
        startHealthCheckerAgent();
        // startBootReceiver();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,locationListener);
                longitude.setText("Getting Longitude");
                latitude.setText("Getting latitude");
            }
            else {
                longitude.setText("Acces not garanted");
                latitude.setText("Acces not garanted");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // The sensor type (as defined in the Sensor class).
        int sensorType = sensorEvent.sensor.getType();
        long curTime = System.currentTimeMillis();
        if((curTime - lastUpdate) > refreshInterval) {
            lastUpdate = curTime;
            /*if(sensor.getType()==Sensor.TYPE_LINEAR_ACCELERATION) {
                xValue.setText(String.valueOf(event.values[0]));
                yValue.setText(String.valueOf(event.values[1]));
                zValue.setText(String.valueOf(event.values[2]));
            }
            */

            switch (sensorType) {
                case Sensor.TYPE_ACCELEROMETER:
                    mAccelerometerData = sensorEvent.values.clone();
                    DeviceAcceleration deviceAcceleration = SensorsUtil.getDeviceAcceleration(mAccelerometerData.clone());
                    xValue.setText(String.valueOf(deviceAcceleration.x));
                    yValue.setText(String.valueOf(deviceAcceleration.y));
                    zValue.setText(String.valueOf(deviceAcceleration.z));
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mMagnetometerData = sensorEvent.values.clone();
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    mGyroscopeData = sensorEvent.values.clone();
                    DeviceAngularAcceleration deviceAngularAcceleration = SensorsUtil.getDeviceAngularAcceleration(mGyroscopeData);
                    xGyroValue.setText(String.valueOf(deviceAngularAcceleration.x));
                    yGyroValue.setText(String.valueOf(deviceAngularAcceleration.y));
                    zGyroValue.setText(String.valueOf(deviceAngularAcceleration.z));
                    break;
                default:
                    return;
            }


            DeviceOrientation deviceOrientation =
                    SensorsUtil.getDeviceOrientation(mAccelerometerData,
                            mMagnetometerData,
                            mDisplay);

            xRotationValue.setText(String.valueOf(deviceOrientation.azimuth));
            yRotationValue.setText(String.valueOf(deviceOrientation.pitch));
            zRotationValue.setText(String.valueOf(deviceOrientation.roll));


        }



    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, mSensorAccelerometer, delay);
        // sensorManager.registerListener(this, linearAccelerometer, delay);
        sensorManager.registerListener(this, mSensorGyroscope, delay);
        sensorManager.registerListener(this, mSensorMagnetometer, delay);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        sensorManager.unregisterListener(this);
        // unregisterReceiver(bootReceiver);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        this.convert = isChecked;
        if(isChecked) {
            switchConversion.setText(switchConversion.getTextOn());
        } else {
            switchConversion.setText(switchConversion.getTextOff());
        }
    }



    private void startHealthCheckerAgent() {
        Intent healthCheckerIntent = new Intent(this, HealthCheckAgent.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, healthCheckerIntent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if(pendingIntent == null) {
            Log.e(TAG, "pendingIntent is null");
        }
        if(alarmManager == null) {
            Log.e(TAG, "alarmManager is null");
        }
        if (pendingIntent != null && alarmManager != null) {
            Log.i(TAG, "Starting Health checker agent");
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    100, 1 * 60 * 1000, pendingIntent);
        }
    }

    private void startBootReceiver() {
        bootReceiver = new BootReceiver();
        registerReceiver(bootReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }
}