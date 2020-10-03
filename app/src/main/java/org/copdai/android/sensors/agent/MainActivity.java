package org.copdai.android.sensors.agent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor acceloremeter, gyroscope, magneto;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private float[] floatGeoMagneto = new float[3];
    private float[] floatAccelerometer = new float[3];
    private float[] floatOrientation = new float[3];
    private float[] floatRotationMatrix = new float[9];
    private int delay;
    TextView xValue, yValue, zValue, xGyroValue, yGyroValue, zGyroValue, xRotationValue, yRotationValue, zRotationValue,latitude,longitude;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        delay = sensorManager.SENSOR_DELAY_UI;

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

        acceloremeter = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magneto = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean isGPS_enabled = locationManager.isProviderEnabled(locationManager.GPS_PROVIDER);



        if(acceloremeter!=null){
            sensorManager.registerListener(MainActivity.this,acceloremeter,delay);
        }
        else{
            xValue.setText("Accelerometer not supported");
            yValue.setText("Accelerometer not supported");
            zValue.setText("Accelerometer not supported");
        }

        if(gyroscope!=null){
            sensorManager.registerListener(MainActivity.this,gyroscope,delay);
        }
        else{
            xGyroValue.setText("Gyroscope not supported");
            yGyroValue.setText("Gyroscope not supported");
            zGyroValue.setText("Gyroscope not supported");
        }

        if(magneto!=null){
            sensorManager.registerListener(MainActivity.this,magneto,delay);
        }
        else{
            xRotationValue.setText("Magneto not supported");
            yRotationValue.setText("Magneto not supported");
            zRotationValue.setText("Magneto not supported");
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
                longitude.setText("Pb to get location");
                latitude.setText(ex.getMessage());
            }
        }
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
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if(sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            xValue.setText(String.valueOf(event.values[0]));
            yValue.setText(String.valueOf(event.values[1]));
            zValue.setText(String.valueOf(event.values[2]));
            floatAccelerometer = event.values;

            SensorManager.getRotationMatrix(floatRotationMatrix, null,floatAccelerometer, floatGeoMagneto);
            SensorManager.getOrientation(floatRotationMatrix, floatOrientation);

            xRotationValue.setText(String.valueOf( floatOrientation[0]));
            yRotationValue.setText(String.valueOf(floatOrientation[1]));
            zRotationValue.setText(String.valueOf(floatOrientation[2]));
        }
        else if(sensor.getType()==Sensor.TYPE_GYROSCOPE){
            xGyroValue.setText(String.valueOf(event.values[0]));
            yGyroValue.setText(String.valueOf(event.values[1]));
            zGyroValue.setText(String.valueOf(event.values[2]));
        }
        else if(sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
            floatGeoMagneto=event.values;

            SensorManager.getRotationMatrix(floatRotationMatrix, null,floatAccelerometer, floatGeoMagneto);
            SensorManager.getOrientation(floatRotationMatrix, floatOrientation);

            xRotationValue.setText(String.valueOf(floatOrientation[0]));
            yRotationValue.setText(String.valueOf(floatOrientation[1]));
            zRotationValue.setText(String.valueOf(floatOrientation[2]));
        }

    }



}