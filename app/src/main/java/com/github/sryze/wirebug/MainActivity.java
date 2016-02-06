package com.github.sryze.wirebug;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String ADB_TCP_PORT_PROPERTY = "service.adb.tcp.port";
    private static final String ADB_TCP_PORT_DISABLED = "-1";
    private static final String ADB_TCP_PORT_DEFAULT = "5555";

    private Switch wifiDebuggingSwitch;
    private TextView adbConnectTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adbConnectTextView = (TextView) findViewById(R.id.text_adb_connect);
        adbConnectTextView.setText(null);

        wifiDebuggingSwitch = (Switch) findViewById(R.id.switch_wifi_debugging);
        wifiDebuggingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setWifiDebuggingEnabled(isChecked);
                if (isChecked) {
                    adbConnectTextView.setText(
                            String.format(getString(R.string.adb_connect), getWifiIpAddress()));
                } else {
                    adbConnectTextView.setText(null);
                }
            }
        });
        wifiDebuggingSwitch.setChecked(isWifiDebuggingEnabled());
    }

    private static boolean isWifiDebuggingEnabled() {
        try {
            String command = "getprop " + ADB_TCP_PORT_PROPERTY;
            InputStream inputStream = Runtime.getRuntime().exec(command).getInputStream();
            String port = new BufferedReader(new InputStreamReader(inputStream)).readLine();
            return !port.equals(ADB_TCP_PORT_DISABLED);
        } catch (IOException e) {
            Log.e(TAG, "Error executing getprop: " + e.getMessage());
            return false;
        }
    }

    private static void setWifiDebuggingEnabled(boolean isEnabled) {
        String port = isEnabled ? ADB_TCP_PORT_DEFAULT : ADB_TCP_PORT_DISABLED;
        try {
            Runtime.getRuntime().exec("setprop " + ADB_TCP_PORT_PROPERTY + " " + port);
            Log.i(TAG, "Debugging over TCP is enabled: " + (isEnabled ? "YES" : "NO"));
        } catch (IOException e) {
            Log.e(TAG, "Error executing setprop: " + e.getMessage());
        }
    }

    private static String getStringFromIpAddress(int ipAddress) {
        return String.format("%d.%d.%d.%d", ipAddress & 0xFF, (ipAddress >> 8) & 0xFF,
                (ipAddress >> 16) & 0xFF, (ipAddress >> 24) & 0xFF);
    }

    private String getWifiIpAddress() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return getStringFromIpAddress(wifiInfo.getIpAddress());
    }
}
