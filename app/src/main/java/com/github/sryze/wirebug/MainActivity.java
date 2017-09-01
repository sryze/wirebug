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

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static final String WARNED_ABOUT_ROOT_KEY = "warned_about_root";

    private ToggleButton toggleDebuggingButton;
    private View connectedView;
    private View instructionsView;
    private TextView connectCommandTextView;
    private TextView wifiNetworkTextView;
    private View notConnectedView;

    private CompoundButton.OnCheckedChangeListener enableSwitchChangeListener;
    private BroadcastReceiver networkStateChangedReceiver;
    private BroadcastReceiver debugStatusChangedReceiver;

    private SharedPreferences preferences;
    private ConnectivityManager connectivityManager;
    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            ActivityManager.TaskDescription taskDescription = new ActivityManager.TaskDescription(
                getString(R.string.app_name),
                BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher),
                ContextCompat.getColor(this, R.color.colorTaskDescription));
            setTaskDescription(taskDescription);
        }

        toggleDebuggingButton = findViewById(R.id.switch_enable_debugging);
        connectedView = findViewById(R.id.view_connected);
        instructionsView = findViewById(R.id.view_instructions);
        connectCommandTextView = findViewById(R.id.text_connect_command);
        wifiNetworkTextView = findViewById(R.id.text_wifi_network);
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
                    Timber.i("Could NOT %s debugging", isChecked ? "enable" : "disable");
                    String toastText = isChecked
                        ? getString(R.string.could_not_enable)
                        : getString(R.string.could_not_disable);
                    Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();
                    toggleDebuggingButton.setChecked(isActuallyEnabled);
                }
            }
        };

        Shell.getShell().setLoggingEnabled(true);
        Shell.getShell().setLogPriority(Log.DEBUG);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!preferences.getBoolean(WARNED_ABOUT_ROOT_KEY, false)
            && !Shell.getShell().canExecAsRoot()) {
            new AlertDialog.Builder(this)
                .setTitle(R.string.warning)
                .setMessage(R.string.not_rooted)
                .setPositiveButton(R.string.ok, null)
                .show();
            preferences.edit().putBoolean(WARNED_ABOUT_ROOT_KEY, true).apply();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        boolean isConnected = NetworkUtils.isConnectedToWifi(connectivityManager);
        connectedView.setVisibility(isConnected ? View.VISIBLE : View.GONE);
        notConnectedView.setVisibility(isConnected ? View.GONE : View.VISIBLE);

        boolean isEnabled = DebugManager.isTcpDebuggingEnabled();
        updateInstructions(isEnabled);
        updateStatus();

        toggleDebuggingButton.setOnCheckedChangeListener(null);
        toggleDebuggingButton.setChecked(isEnabled);
        toggleDebuggingButton.setOnCheckedChangeListener(enableSwitchChangeListener);

        networkStateChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Timber.d("Received network state changed broadcast");
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
                updateConnectionInfo();
            }
        };
        registerReceiver(networkStateChangedReceiver,
            new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));

        debugStatusChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Timber.d("Received debug state change broadcast");
                boolean isEnabled =
                    intent.getBooleanExtra(DebugStatusService.EXTRA_IS_ENABLED, false);
                updateInstructions(isEnabled);
                toggleDebuggingButton.setOnCheckedChangeListener(null);
                toggleDebuggingButton.setChecked(isEnabled);
                toggleDebuggingButton.setOnCheckedChangeListener(enableSwitchChangeListener);
            }
        };
        registerReceiver(debugStatusChangedReceiver,
            new IntentFilter(DebugStatusService.ACTION_STATUS_CHANGED));
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

    private void updateConnectionInfo() {
        connectCommandTextView.setText(String.format(
            "adb connect %s",
            NetworkUtils.getWifiIpAddressString(wifiManager)));
        wifiNetworkTextView.setText(String.format(
            getString(R.string.wifi_network),
            NetworkUtils.getWifiNetworkName(wifiManager)));
    }

    private void updateInstructions(boolean isVisible) {
        updateConnectionInfo();
        instructionsView.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
    }

    private void updateStatus() {
        Intent intent = new Intent(this, DebugStatusService.class);
        intent.setAction(DebugStatusService.ACTION_UPDATE_STATUS);
        startService(intent);
    }
}
