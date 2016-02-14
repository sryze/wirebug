package com.github.sryze.wirebug;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        final SharedPreferences preferences = getSharedPreferences("Settings", MODE_PRIVATE);

        final Switch disableOnLockSwitch = (Switch) findViewById(R.id.switch_disable_on_lock);
        disableOnLockSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("disable_on_lock", isChecked);
                editor.commit();
            }
        });
        disableOnLockSwitch.setChecked(preferences.getBoolean("disable_on_lock", false));
    }
}
