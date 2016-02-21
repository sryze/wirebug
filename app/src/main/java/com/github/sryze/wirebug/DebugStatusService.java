/*
 * This file is part of Wirebug.
 *
 * Wirebug is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Wirebug is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Wirebug.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.sryze.wirebug;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class DebugStatusService extends Service {
    private static final String TAG = "DebugStatusService";
    private static final int STATUS_NOTIFICATION = 0;
    private static final long STATUS_UPDATE_INTERVAL = 5000;

    private Handler autoUpdateHandler = new Handler();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                KeyguardManager keyguardManager =
                        (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                if (keyguardManager.inKeyguardRestrictedInputMode()) {
                    Log.d(TAG, "Screen was locked");
                    SharedPreferences preferences =
                            context.getSharedPreferences("Settings", Context.MODE_PRIVATE);
                    if (preferences.getBoolean("disable_on_lock", false)) {
                        Log.i(TAG, "Disabling debugging because disable_on_lock is true");
                        DebugManager.setWifiDebuggingEnabled(false);
                        updateStatus();
                    }
                }
            }
        }, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Performing automatic status update");
                updateStatus();
                autoUpdateHandler.removeCallbacksAndMessages(null);
                autoUpdateHandler.postDelayed(this, STATUS_UPDATE_INTERVAL);
            }
        }.run();

        return START_STICKY;
    }

    private void updateStatus() {
        boolean isEnabled = DebugManager.isWifiDebuggingEnabled();
        Log.i(TAG, String.format("Updating status to %s", isEnabled ? "enabled" : "disabled"));

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(STATUS_NOTIFICATION);

        if (isEnabled) {
            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            Notification notification = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.status_enabled))
                    .setContentIntent(pendingIntent)
                    .setCategory(Notification.CATEGORY_STATUS)
                    .build();
            notification.flags |= Notification.FLAG_NO_CLEAR;
            notificationManager.notify(STATUS_NOTIFICATION, notification);
        }
    }
}
