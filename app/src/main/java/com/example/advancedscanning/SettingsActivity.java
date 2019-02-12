package com.example.advancedscanning;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {

    protected static final String KEY_PREF_FMD_SERVER = "FMDServer";

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().
                replace(android.R.id.content, new SettingsFragment()).commit();
    }
}
