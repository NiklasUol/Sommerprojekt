package com.example.weckerapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    private Context appContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        appContext = getApplicationContext();

        SharedPreferences preferences  = PreferenceManager.getDefaultSharedPreferences(appContext);

        TextInputEditText brokerTextfeld = (TextInputEditText) findViewById(R.id.brokerInput);
        TextInputEditText portTextfeld = (TextInputEditText) findViewById(R.id.portInput);
        brokerTextfeld.setText(preferences.getString("broker", MainActivity.brokerURL).substring(6).split(":")[0]);
        portTextfeld.setText(preferences.getString("broker", MainActivity.brokerURL).substring(6).split(":")[1]);

        Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = preferences.edit();
                String broker = brokerTextfeld.getText().toString();
                String port = portTextfeld.getText().toString();

                editor.putString("broker", "tcp://" + broker + ":" + port);
                editor.commit();
                MainActivity.brokerURL = "tcp://" + broker + ":" + port;

                Toast.makeText(appContext, "Erfolgreich gespeichert!", Toast.LENGTH_LONG).show();

            }
        });
    }
}