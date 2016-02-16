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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String ADB_TCP_PORT_PROPERTY = "service.adb.tcp.port";
    private static final int ADB_TCP_PORT_DEFAULT = 5555;

    private static final int STATUS_NOTIFICATION = 0;

    private Switch wifiDebuggingSwitch;
    private View connectedView;
    private View instructionsView;;
    private TextView connectCommandTextView;
    private TextView wifiNetworkTextView;
    private View notConnectedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Shell.getShell().setLoggingEnabled(true);
        Shell.getShell().setLogPriority(Log.DEBUG);

        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = networkInfo != null ? networkInfo.isConnected() : false;

        connectedView = findViewById(R.id.view_connected);
        connectedView.setVisibility(isConnected ? View.VISIBLE : View.GONE);

        instructionsView = findViewById(R.id.view_instructions);
        instructionsView.setVisibility(View.INVISIBLE);

        connectCommandTextView = (TextView) findViewById(R.id.text_connect_command);
        wifiNetworkTextView = (TextView) findViewById(R.id.text_wifi_network);

        notConnectedView = findViewById(R.id.view_not_connected);
        notConnectedView.setVisibility(View.INVISIBLE);

        boolean isEnabled = isWifiDebuggingEnabled();
        updateInstructions(isEnabled);
        updateStatusNotification(isEnabled);

        wifiDebuggingSwitch = (Switch) findViewById(R.id.switch_wifi_debugging);
        wifiDebuggingSwitch.setChecked(isEnabled);
        wifiDebuggingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setWifiDebuggingEnabled(isChecked);
                boolean isActuallyEnabled = isWifiDebuggingEnabled();
                if (isChecked == isActuallyEnabled) {
                    updateInstructions(isChecked);
                    updateStatusNotification(isChecked);
                } else {
                    String toastText = isChecked
                            ? getString(R.string.could_not_enable)
                            : getString(R.string.could_not_disable);
                    Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    wifiDebuggingSwitch.setChecked(isActuallyEnabled);
                }
            }
        });

        IntentFilter wifiIntentFilter = new IntentFilter();
        wifiIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                switch (networkInfo.getState()) {
                    case CONNECTED:
                        connectedView.setVisibility(View.VISIBLE);
                        notConnectedView.setVisibility(View.INVISIBLE);
                        break;
                    case DISCONNECTED:
                        connectedView.setVisibility(View.GONE);
                        notConnectedView.setVisibility(View.VISIBLE);
                        break;
                }
                updateWifiInfo();
            }
        }, wifiIntentFilter);

        IntentFilter screenIntentFilter = new IntentFilter();
        screenIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                KeyguardManager keyguardManager =
                        (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
                if (keyguardManager.inKeyguardRestrictedInputMode()) {
                    SharedPreferences preferences =
                            getSharedPreferences("Settings", MODE_PRIVATE);
                    if (preferences.getBoolean("disable_on_lock", false)) {
                        wifiDebuggingSwitch.setChecked(false);
                    }
                }
            }
        }, screenIntentFilter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = new MenuInflater(this);
        menuInflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWifiInfo() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        int ipAddress = wifiInfo.getIpAddress();
        connectCommandTextView.setText(
                String.format("adb connect %s", getStringFromIpAddress(ipAddress)));

        String ssid = wifiInfo.getSSID();
        wifiNetworkTextView.setText(
                String.format(getString(R.string.wifi_network), ssid));
    }

    private void updateInstructions(boolean isVisible) {
        updateWifiInfo();
        instructionsView.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateStatusNotification(boolean isEnabled) {
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
                    .build();
            notification.flags |= Notification.FLAG_NO_CLEAR;
            notificationManager.notify(STATUS_NOTIFICATION, notification);
        }
    }

    private static String getStringFromIpAddress(int ipAddress) {
        return String.format("%d.%d.%d.%d",
                ipAddress & 0xFF,
                (ipAddress >> 8) & 0xFF,
                (ipAddress >> 16) & 0xFF,
                (ipAddress >> 24) & 0xFF);
    }

    private static boolean isWifiDebuggingEnabled() {
        return getAdbTcpPort() > 0;
    }

    private static void setWifiDebuggingEnabled(boolean isEnabled) {
        if (setAdbTcpPort(isEnabled ? ADB_TCP_PORT_DEFAULT : 0)) {
            Log.i(TAG, "Debugging over TCP is enabled: " + (isEnabled ? "YES" : "NO"));
            Log.i(TAG, "Restarting ADB daemon (this will kill your debugging session)");
            restartAdbDaemon();
        }
    }

    private static int getAdbTcpPort() {
        try {
            String output = Shell.getShell().exec("getprop " + ADB_TCP_PORT_PROPERTY);
            return Integer.parseInt(output.trim());
        } catch (ShellException e) {
            Log.e(TAG, String.format("Error getting current TCP port: %s", e.getMessage()));
        } catch (NumberFormatException e) {
            // OK
        }
        return 0;
    }

    private static boolean setAdbTcpPort(int port) {
        try {
            String portArg = port > 0 ? String.format("%d", port) : "\"\"";
            String command = String.format("setprop %s %s", ADB_TCP_PORT_PROPERTY, portArg);
            Shell.getShell().execAsRoot(command);
            return true;
        } catch (ShellException e) {
            Log.e(TAG, String.format(
                    "Error setting TCP port (%s): %s", port, e.getMessage()));
            return false;
        }
    }

    private static void restartAdbDaemon() {
        try {
            Shell.getShell().execAsRoot("stop adbd; start adbd");
        } catch (ShellException e) {
            Log.e(TAG, String.format("Error restarting ADB daemon: %s", e.getMessage()));
        }
    }
}
