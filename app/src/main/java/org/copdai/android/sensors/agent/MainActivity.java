package org.copdai.android.sensors.agent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
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

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements
        SensorEventListener, CompoundButton.OnCheckedChangeListener, DialogInterface.OnClickListener {

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
    private int calibrationInterval;
    private long lastUpdate;
    private boolean convert = false;
    final String TAG = "MainActivity";
    private boolean calibrationPassed = false;

    private float ACC_VALUE_DRIFT_X = 0f;
    private float ACC_VALUE_DRIFT_Y = 0f;
    private float ACC_VALUE_DRIFT_Z = 0f;

    private float ANGULAR_VALUE_DRIFT_X = 0f;
    private float ANGULAR_VALUE_DRIFT_Y = 0f;
    private float ANGULAR_VALUE_DRIFT_Z = 0f;

    private float ACC_ANGULAR_VALUE_DRIFT_PITCH = 0f;
    private float ACC_ANGULAR_VALUE_DRIFT_ROLL = 0f;
    private float ACC_ANGULAR_VALUE_DRIFT_AZIMUTH = 0f;

    private float ORIENTATION_INIT_PITCH = 10f;
    private float ORIENTATION_INIT_ROLL = 10f;
    private float ORIENTATION_INIT_AZIMUTH = 10f;

    private boolean startCalibration = false;
    // System display. Need this for determining rotation.
    private Display mDisplay;

    private final float errorRange = 1.1f;

    TextView xValue, yValue, zValue, xGyroValue, yGyroValue, zGyroValue, xRotationValue, yRotationValue, zRotationValue,latitude,longitude;
    Switch switchConversion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        delay = SensorManager.SENSOR_DELAY_NORMAL;
        refreshInterval = 10; //100ms
        calibrationInterval = 30000; // 10 second

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
        
        // startHealthCheckerAgent();
        // startBootReceiver();
        calibrate();
    }

    public void calibrate() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Demande Calibration");
        builder.setMessage("Veuillez déposer le téléphone sur une surface plate pour calibrage");
        builder.setCancelable(false);
        builder.setPositiveButton("OK",  this);
        // builder.setNeutralButton("OK", this);


        AlertDialog dlg = builder.create();

        dlg.show();

    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Log.i(TAG, "Starting calibration");
        lastUpdate = System.currentTimeMillis();
        startCalibration = true;
        final Timer t = new Timer();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Calibration en cours");
        builder.setMessage("Merci de patienter durant " + calibrationInterval / 1000 + " seconds...");
        builder.setCancelable(false);
        final AlertDialog dlg = builder.create();
        dlg.show();

        t.schedule(new TimerTask() {
            public void run() {
                dlg.dismiss(); // when the task active then close the dialog
                t.cancel(); // also just top the timer thread, otherwise, you may receive a crash report
            }
        }, calibrationInterval); // after 2 second (or 2000 miliseconds), the task will be active.
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
        long curTime = System.currentTimeMillis();
        if(calibrationPassed) {
            // The sensor type (as defined in the Sensor class).
            int sensorType = sensorEvent.sensor.getType();

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
                        DeviceAcceleration deviceAcceleration = SensorsUtil.getDeviceAcceleration(mAccelerometerData.clone(),ACC_VALUE_DRIFT_X,ACC_VALUE_DRIFT_Y,ACC_VALUE_DRIFT_Z);
                        xValue.setText(String.valueOf(deviceAcceleration.x));
                        yValue.setText(String.valueOf(deviceAcceleration.y));
                        zValue.setText(String.valueOf(deviceAcceleration.z));
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD:
                        mMagnetometerData = sensorEvent.values.clone();
                        break;
                    case Sensor.TYPE_GYROSCOPE:
                        mGyroscopeData = sensorEvent.values.clone();
                        DeviceAngularAcceleration deviceAngularAcceleration = SensorsUtil.getDeviceAngularAcceleration(mGyroscopeData,ANGULAR_VALUE_DRIFT_X,ANGULAR_VALUE_DRIFT_Y,ANGULAR_VALUE_DRIFT_Z);
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
                                mDisplay,ACC_ANGULAR_VALUE_DRIFT_AZIMUTH,ACC_ANGULAR_VALUE_DRIFT_PITCH,ACC_ANGULAR_VALUE_DRIFT_ROLL);

                xRotationValue.setText(String.valueOf(deviceOrientation.azimuth));
                yRotationValue.setText(String.valueOf(deviceOrientation.pitch));
                zRotationValue.setText(String.valueOf(deviceOrientation.roll));


            }
        } else if(startCalibration){

            if((curTime - lastUpdate) < calibrationInterval) {
                // The sensor type (as defined in the Sensor class).
                int sensorType = sensorEvent.sensor.getType();
                switch (sensorType) {
                    case Sensor.TYPE_ACCELEROMETER:
                        mAccelerometerData = sensorEvent.values.clone();
                        DeviceAcceleration deviceAcceleration = SensorsUtil.getStandardDeviceAcceleration(mAccelerometerData.clone());
                        if(Math.abs(deviceAcceleration.x) > ACC_VALUE_DRIFT_X) {
                            ACC_VALUE_DRIFT_X = Math.abs(deviceAcceleration.x);
                        }
                        if(Math.abs(deviceAcceleration.y) > ACC_VALUE_DRIFT_Y) {
                            ACC_VALUE_DRIFT_Y = Math.abs(deviceAcceleration.y);
                        }
                        if(Math.abs(deviceAcceleration.z) > ACC_VALUE_DRIFT_Z) {
                            ACC_VALUE_DRIFT_Z = Math.abs(deviceAcceleration.z);
                        }
                        break;
                    case Sensor.TYPE_MAGNETIC_FIELD:
                        mMagnetometerData = sensorEvent.values.clone();
                        break;
                    case Sensor.TYPE_GYROSCOPE:
                        mGyroscopeData = sensorEvent.values.clone();
                        DeviceAngularAcceleration deviceAngularAcceleration = SensorsUtil.getStandardDeviceAngularAcceleration(mGyroscopeData);
                        if(Math.abs(deviceAngularAcceleration.x) > ANGULAR_VALUE_DRIFT_X) {
                            ANGULAR_VALUE_DRIFT_X = Math.abs(deviceAngularAcceleration.x);
                        }
                        if(Math.abs(deviceAngularAcceleration.y) > ANGULAR_VALUE_DRIFT_Y) {
                            ANGULAR_VALUE_DRIFT_Y = Math.abs(deviceAngularAcceleration.y);
                        }
                        if(Math.abs(deviceAngularAcceleration.z) > ANGULAR_VALUE_DRIFT_Z) {
                            ANGULAR_VALUE_DRIFT_Z = Math.abs(deviceAngularAcceleration.z);
                        }
                        break;
                    default:
                        return;
                }


                DeviceOrientation deviceOrientation =
                        SensorsUtil.getStandardDeviceOrientation(mAccelerometerData,
                                mMagnetometerData,
                                mDisplay);

                if(ORIENTATION_INIT_AZIMUTH == 10f || ORIENTATION_INIT_PITCH == 10f ||
                        ORIENTATION_INIT_ROLL == 10f ) {
                    ORIENTATION_INIT_AZIMUTH = deviceOrientation.azimuth;
                    ORIENTATION_INIT_PITCH = deviceOrientation.pitch;
                    ORIENTATION_INIT_ROLL = deviceOrientation.roll;
                } else {
                    float delta_azimuth = Math.abs(ORIENTATION_INIT_AZIMUTH - deviceOrientation.azimuth);
                    float delta_pitch = Math.abs(ORIENTATION_INIT_PITCH - deviceOrientation.pitch);
                    float delta_roll = Math.abs(ORIENTATION_INIT_ROLL - deviceOrientation.roll);
                    if(ACC_ANGULAR_VALUE_DRIFT_AZIMUTH < delta_azimuth) {
                        ACC_ANGULAR_VALUE_DRIFT_AZIMUTH = delta_azimuth;
                    }
                    if(ACC_ANGULAR_VALUE_DRIFT_PITCH < delta_pitch) {
                        ACC_ANGULAR_VALUE_DRIFT_PITCH = delta_pitch;
                    }
                    if(ACC_ANGULAR_VALUE_DRIFT_ROLL < delta_roll) {
                        ACC_ANGULAR_VALUE_DRIFT_ROLL = delta_roll;
                    }
                }
            }

            if((curTime - lastUpdate) >= calibrationInterval) {
                calibrationPassed = true;
                ACC_VALUE_DRIFT_X = ACC_VALUE_DRIFT_X * errorRange;
                ACC_VALUE_DRIFT_Y = ACC_VALUE_DRIFT_Y * errorRange;
                ACC_VALUE_DRIFT_Z = ACC_VALUE_DRIFT_Z * errorRange;

                ANGULAR_VALUE_DRIFT_X = ANGULAR_VALUE_DRIFT_X * errorRange;
                ANGULAR_VALUE_DRIFT_Y = ANGULAR_VALUE_DRIFT_Y * errorRange;
                ANGULAR_VALUE_DRIFT_Z = ANGULAR_VALUE_DRIFT_Z * errorRange;

                ACC_ANGULAR_VALUE_DRIFT_AZIMUTH = ACC_ANGULAR_VALUE_DRIFT_AZIMUTH * errorRange;
                ACC_ANGULAR_VALUE_DRIFT_PITCH = ACC_ANGULAR_VALUE_DRIFT_PITCH * errorRange;
                ACC_ANGULAR_VALUE_DRIFT_ROLL = ACC_ANGULAR_VALUE_DRIFT_ROLL * errorRange;


                Log.i(TAG, "ACC CALIBRATION = [" + ACC_VALUE_DRIFT_X + ", " + ACC_VALUE_DRIFT_Y + ", " +
                        ACC_VALUE_DRIFT_Z + "]");
                Log.i(TAG, "ANG ACC CALIBRATION = [" + ANGULAR_VALUE_DRIFT_X + ", " + ANGULAR_VALUE_DRIFT_Y + ", " +
                        ANGULAR_VALUE_DRIFT_Z + "]");
                Log.i(TAG, "ACC CALIBRATION = [" + ACC_ANGULAR_VALUE_DRIFT_AZIMUTH + ", " + ACC_ANGULAR_VALUE_DRIFT_PITCH + ", " +
                        ACC_ANGULAR_VALUE_DRIFT_ROLL + "]");
            }


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