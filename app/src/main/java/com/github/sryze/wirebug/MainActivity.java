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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String ADB_TCP_PORT_PROPERTY = "service.adb.tcp.port";
    private static final int ADB_TCP_PORT_DEFAULT = 5555;

    private Switch wifiDebuggingSwitch;
    private View instructionsView;
    private TextView commandTextView;
    private View notConnectedView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean isEnabled = isWifiDebuggingEnabled();
        instructionsView = findViewById(R.id.view_instructions);
        commandTextView = (TextView) findViewById(R.id.text_command);
        updateInstructions(isEnabled);

        notConnectedView = findViewById(R.id.view_not_connected);
        notConnectedView.setVisibility(View.INVISIBLE);

        wifiDebuggingSwitch = (Switch) findViewById(R.id.switch_wifi_debugging);
        wifiDebuggingSwitch.setChecked(isEnabled);
        wifiDebuggingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setWifiDebuggingEnabled(isChecked);
                updateInstructions(isChecked);
            }
        });

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                switch (networkInfo.getState()) {
                    case CONNECTED:
                        instructionsView.setVisibility(View.VISIBLE);
                        notConnectedView.setVisibility(View.INVISIBLE);
                        break;
                    case DISCONNECTED:
                        instructionsView.setVisibility(View.GONE);
                        notConnectedView.setVisibility(View.VISIBLE);
                        break;
                }
                updateIpAddress();
            }
        }, intentFilter);
    }

    private void updateIpAddress() {
        String command = String.format("adb connect %s", getWifiIpAddress());
        commandTextView.setText(command);
    }

    private void updateInstructions(boolean isVisible) {
        updateIpAddress();
        instructionsView.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
    }

    private String getWifiIpAddress() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return getStringFromIpAddress(wifiInfo.getIpAddress());
    }

    private static int getAdbTcpPort() {
        try {
            String[] command = new String[] {"getprop", ADB_TCP_PORT_PROPERTY};
            Process process = Runtime.getRuntime().exec(command);
            String output = new BufferedReader(
                    new InputStreamReader(process.getInputStream())).readLine();
            Log.d(TAG, "getprop output: " + output);
            return Integer.parseInt(output);
        } catch (IOException e) {
            Log.e(TAG, "Error executing getprop: " + e.getMessage());
        } catch (NumberFormatException e) {
            // OK
        }
        return 0;
    }

    private static void setAdbTcpPort(int portNumber) {
        try {
            String[] command = new String[] {
                "setprop",
                ADB_TCP_PORT_PROPERTY,
                portNumber > 0 ? String.format("%d", portNumber) : ""
            };
            Process process = Runtime.getRuntime().exec(command);
            String output = new BufferedReader(
                    new InputStreamReader(process.getInputStream())).readLine();
            Log.d(TAG, "setprop output: " + output);
        } catch (IOException e) {
            Log.e(TAG, "Error executing setprop: " + e.getMessage());
        }
    }

    private static void restartAdbDaemon() {
        try {
            Runtime.getRuntime().exec("stop adbd");
            Runtime.getRuntime().exec("start adbd");
        } catch (IOException e) {
            Log.e(TAG, "Error restarting ADB daemon: " + e.getMessage());
        }
    }

    private static boolean isWifiDebuggingEnabled() {
        return getAdbTcpPort() > 0;
    }

    private static void setWifiDebuggingEnabled(boolean isEnabled) {
        Log.i(TAG, "Debugging over TCP is enabled: " + (isEnabled ? "YES" : "NO"));
        setAdbTcpPort(isEnabled ? ADB_TCP_PORT_DEFAULT : 0);
        Log.i(TAG, "Restarting ADB daemon (this will kill your debugging session)");
        restartAdbDaemon();
    }

    private static String getStringFromIpAddress(int ipAddress) {
        return String.format("%d.%d.%d.%d", ipAddress & 0xFF, (ipAddress >> 8) & 0xFF,
                (ipAddress >> 16) & 0xFF, (ipAddress >> 24) & 0xFF);
    }
}
