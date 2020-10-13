package org.copdai.android.sensors.agent;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

    final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, intent.getAction());
        Log.i(TAG, "Boot Receiver called");
        this.startHealthCheckerAgent(context);

    }

    private void startHealthCheckerAgent(Context context) {
        Intent healthCheckerIntent = new Intent(context, HealthCheckAgent.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, healthCheckerIntent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
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
}
