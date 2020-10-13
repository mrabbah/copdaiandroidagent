package org.copdai.android.sensors.agent;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class HealthCheckAgent extends BroadcastReceiver {
    final String TAG = "HealthCheckAgent";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Checking running services");

        boolean isGpsAgentRunning = false;
        boolean isAccelerometerAgentRunning = false;

       ActivityManager manager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (GpsAgent.class.getName().equals(service.service.getClassName())) {
                Log.i(TAG, "Gps Agent is running no action will be take");
                isGpsAgentRunning = true;
            } else if (AccelerometerAgent.class.getName().equals(service.service.getClassName())) {
                Log.i(TAG, "Accelerometer Agent is running no action will be take");
                isAccelerometerAgentRunning = true;
            }
        }

        try {
            if(!isGpsAgentRunning) {
                Log.i(TAG, "Starting GPS Agent");
                Intent gpsAgent = new Intent(context, GpsAgent.class);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // ContextCompat.startForegroundService(context, gpsAgent);
                    // context.startForegroundService(gpsAgent);
                    context.startService(gpsAgent);
                } else {
                    context.startService(gpsAgent);
                }

            }
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
        try {
            if(!isAccelerometerAgentRunning) {
                Log.i(TAG, "Starting Accelerometer Agent");
                Intent accelerometerAgent = new Intent(context, AccelerometerAgent.class);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // ContextCompat.startForegroundService(context, accelerometerAgent);
                    // context.startForegroundService(accelerometerAgent);
                    context.startService(accelerometerAgent);
                } else {
                    context.startService(accelerometerAgent);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }

    }
}
