package org.copdai.android.sensors.agent;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class GpsAgent extends Service implements LocationListener {

    private static final String NOTIFICATION_CHANNEL_ID_SERVICE = "pl.***.***";
    private static final String NOTIFICATION_CHANNEL_ID_INFO = "pl.***.***";
    final String TAG = "GpsAgent";

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 5;
    private static final long MIN_TIME_BW_UPDATES = 500;
    LocationManager m_locationManager;
    Context context;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID_SERVICE, "*****", NotificationManager.IMPORTANCE_DEFAULT));
            nm.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID_INFO, "********", NotificationManager.IMPORTANCE_DEFAULT));

        } else {

        }
        startServiceOreoCondition();*/
        // startForeground(1,new Notification());
    }

    /*
    private void startServiceOreoCondition() {
        if (Build.VERSION.SDK_INT >= 26) {

            String NOTIFICATION_CHANNEL_ID = "pl.***.***";
            String channelName = "Communication Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();
            startForeground(4, notification);
        }else{
            startForeground(3, new Notification());
        }
    }*/

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context c = this.getApplicationContext();
        super.onStartCommand(intent, flags, startId);
        Log.i(TAG, "GPS Agent Start Command called");
        this.m_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Gps Agent does not have access to location service");
        }
        this.m_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
        this.m_locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
        return START_STICKY;

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.i(TAG, "lat : " +location.getLatitude() + " long : " + location.getLongitude() + " altitude: "
                + location.getAltitude() + " speed : " + location.getSpeed() + " accuracy: " + location.getAccuracy()
                + " time " + location.getTime());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.i(TAG, "Status changed");
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        Log.i(TAG, "Provider Enabled");
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        Log.i(TAG, "Provider disabled");
    }
}
