package org.copdai.android.sensors.agent;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class AccelerometerAgent extends Service implements SensorEventListener  {

    final String TAG = "AccelerationAgent";
    private SensorManager sensorManager;
    private Sensor mSensorAccelerometer, mSensorGyroscope, mSensorMagnetometer;
    private float[] mMagnetometerData = new float[3];
    private float[] mAccelerometerData = new float[3];
    private float[] mGyroscopeData = new float[3];
    private int delay;
    private int refreshInterval;
    private long lastUpdate;

    @Override
    public void onCreate() {
        super.onCreate();
        delay = SensorManager.SENSOR_DELAY_NORMAL;
        refreshInterval = 100; //100ms
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // linearAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorMagnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.i(TAG, "Accelerometer Agent Start Command called");
        if(mSensorAccelerometer !=null){
            sensorManager.registerListener(this, mSensorAccelerometer,delay);
        }
        else{
            Log.e(TAG, "Accelerometer not supported");

        }

        if(mSensorGyroscope !=null){
            sensorManager.registerListener(this, mSensorGyroscope,delay);
        }
        else{
            Log.e(TAG, "Gyroscope not supported");
        }

        if(mSensorGyroscope == null || mSensorAccelerometer == null){
            Log.e(TAG, "Rotation sensor not available");
        }

        lastUpdate = System.currentTimeMillis();

        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // The sensor type (as defined in the Sensor class).
        int sensorType = sensorEvent.sensor.getType();
        long curTime = System.currentTimeMillis();
        if((curTime - lastUpdate) > refreshInterval) {
            lastUpdate = curTime;

            switch (sensorType) {
                case Sensor.TYPE_ACCELEROMETER:
                    mAccelerometerData = sensorEvent.values.clone();
                    DeviceAcceleration deviceAcceleration = SensorsUtil.getDeviceAcceleration(mAccelerometerData.clone());
                    Log.i(TAG, "acceleration m/s^2: x = " + String.valueOf(deviceAcceleration.x)
                            + " y = " + String.valueOf(deviceAcceleration.y) + " z = " + String.valueOf(deviceAcceleration.z));
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mMagnetometerData = sensorEvent.values.clone();
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    mGyroscopeData = sensorEvent.values.clone();
                    DeviceAngularAcceleration deviceAngularAcceleration = SensorsUtil.getDeviceAngularAcceleration(mGyroscopeData);
                    Log.i(TAG, "acc angular radian/s: x = " + String.valueOf(deviceAngularAcceleration.x)
                            + " y = " + String.valueOf(deviceAngularAcceleration.y) + " z = " + String.valueOf(deviceAngularAcceleration.z));
                    break;
                default:
                    return;
            }


            DeviceOrientation deviceOrientation =
                    SensorsUtil.getDeviceOrientation(mAccelerometerData,
                            mMagnetometerData);
            Log.i(TAG, "rotation radian : azimuth = " + String.valueOf(deviceOrientation.azimuth)
                    + " pitch = " + String.valueOf(deviceOrientation.pitch) + " roll = " + String.valueOf(deviceOrientation.roll));

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
