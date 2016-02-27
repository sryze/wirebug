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

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
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

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Switch enableSwitch;
    private View connectedView;
    private View instructionsView;;
    private TextView connectCommandTextView;
    private TextView wifiNetworkTextView;
    private View notConnectedView;

    private boolean showedRootWarning = false;

    private CompoundButton.OnCheckedChangeListener enableSwitchChangeListener;
    private BroadcastReceiver networkStateChangedReceiver;
    private BroadcastReceiver debugStatusChangedReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enableSwitch = (Switch) findViewById(R.id.switch_enable_debugging);
        connectedView = findViewById(R.id.view_connected);
        instructionsView = findViewById(R.id.view_instructions);
        connectCommandTextView = (TextView) findViewById(R.id.text_connect_command);
        wifiNetworkTextView = (TextView) findViewById(R.id.text_wifi_network);
        notConnectedView = findViewById(R.id.view_not_connected);

        enableSwitchChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DebugManager.setTcpDebuggingEnabled(isChecked);
                boolean isActuallyEnabled = DebugManager.isTcpDebuggingEnabled();
                if (isChecked == isActuallyEnabled) {
                    updateInstructions(isChecked);
                    updateStatus();
                } else {
                    String toastText = isChecked
                            ? getString(R.string.could_not_enable)
                            : getString(R.string.could_not_disable);
                    Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    enableSwitch.setChecked(isActuallyEnabled);
                }
            }
        };

        Log.i(TAG, "Starting status update service");
        startService(new Intent(this, DebugStatusService.class));

        Shell.getShell().setLoggingEnabled(true);
        Shell.getShell().setLogPriority(Log.DEBUG);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!showedRootWarning && !(new File("/system/bin/su")).exists()) {
            new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.not_rooted)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            showedRootWarning = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = networkInfo != null ? networkInfo.isConnected() : false;

        connectedView.setVisibility(isConnected ? View.VISIBLE : View.GONE);
        notConnectedView.setVisibility(isConnected ? View.GONE : View.VISIBLE);

        boolean isEnabled = DebugManager.isTcpDebuggingEnabled();
        updateInstructions(isEnabled);
        updateStatus();

        enableSwitch.setOnCheckedChangeListener(null);
        enableSwitch.setChecked(isEnabled);
        enableSwitch.setOnCheckedChangeListener(enableSwitchChangeListener);

        networkStateChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Received network state changed broadcast");
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
        };
        registerReceiver(networkStateChangedReceiver,
                new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        debugStatusChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "Received debug state change broadcast");
                boolean isEnabled =
                        intent.getBooleanExtra(DebugStatusService.EXTRA_IS_ENABLED, false);
                updateInstructions(isEnabled);
                enableSwitch.setOnCheckedChangeListener(null);
                enableSwitch.setChecked(isEnabled);
                enableSwitch.setOnCheckedChangeListener(enableSwitchChangeListener);
            }
        };
        registerReceiver(debugStatusChangedReceiver,
                new IntentFilter(DebugStatusService.ACTION_DEBUG_STATUS_CHANGED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        
        unregisterReceiver(networkStateChangedReceiver);
        unregisterReceiver(debugStatusChangedReceiver);
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
                String.format("adb connect %s", DebugManager.getStringFromIpAddress(ipAddress)));

        String ssid = wifiInfo.getSSID();
        wifiNetworkTextView.setText(
                String.format(getString(R.string.wifi_network), ssid));
    }

    private void updateInstructions(boolean isVisible) {
        updateWifiInfo();
        instructionsView.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateStatus() {
        startService(new Intent(this, DebugStatusService.class));
    }
}
