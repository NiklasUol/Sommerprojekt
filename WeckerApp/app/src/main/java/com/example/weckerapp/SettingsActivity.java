package com.example.weckerapp;

import static com.example.weckerapp.MainActivity.client;
import static com.example.weckerapp.MainActivity.newMessage;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class SettingsActivity extends AppCompatActivity {

    private Context appContext;
    private ResponseThread responseThread;
    private SharedPreferences.Editor editor;
    private SeekBar helligkeitBar;
    private Switch dimmungSwitch;
    private SeekBar dimmungBar;

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

        TextView helligkeitText = (TextView) findViewById(R.id.helligkeitText);
        helligkeitBar = (SeekBar) findViewById(R.id.helligkeitBar);
        helligkeitBar.setProgress(preferences.getInt("helligkeit", 7));
        helligkeitText.setText("Helligkeit: " + helligkeitBar.getProgress());
        helligkeitBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                helligkeitText.setText("Helligkeit: " + seekBar.getProgress());
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        TextView dimmungText = (TextView) findViewById(R.id.schalfzeitText);
        dimmungSwitch = (Switch) findViewById(R.id.dimmungSwitch);
        dimmungSwitch.setChecked(preferences.getBoolean("dimmung", true));
        dimmungSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                dimmungBar.setEnabled(b);
                dimmungText.setEnabled(b);
            }
        });

        dimmungBar = (SeekBar) findViewById(R.id.schlafzeitBar);
        dimmungBar.setProgress(preferences.getInt("schlafzeit", 22));
        if(!dimmungSwitch.isChecked()){
            dimmungBar.setEnabled(false);
        }

        dimmungText.setText("Ausschalten um " + dimmungBar.getProgress() + "Uhr");
        dimmungBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                dimmungText.setText("Ausschalten um " + seekBar.getProgress() + " Uhr");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor = preferences.edit();
                String message = String.valueOf(helligkeitBar.getProgress());
                if(dimmungSwitch.isChecked()){
                    message += ";" + dimmungBar.getProgress();
                }
                sendSettings("wecker/settings", message);

                String broker = brokerTextfeld.getText().toString();
                String port = portTextfeld.getText().toString();

                editor.putString("broker", "tcp://" + broker + ":" + port);
                editor.commit();
                MainActivity.brokerURL = "tcp://" + broker + ":" + port;

                //Toast.makeText(appContext, "Erfolgreich gespeichert!", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void sendSettings(String topic, String message){
        if(!client.isConnected()){
            Toast.makeText(appContext, "Keine Verbindung zum MQTT-Broker", Toast.LENGTH_LONG).show();
            return;
        }

        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
        try {
            client.publish(topic, mqttMessage);
            if(responseThread != null){
                responseThread.interrupt();
            }
            responseThread = new ResponseThread();
            responseThread.start();
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }


    private class ResponseThread extends Thread{
        public void run()
        {
            Looper.prepare();
            Log.e("Thread", "Thread working...");
            for(int i = 0; i < 30; i++){
                try {
                    Thread.sleep(100);
                    if(newMessage){
                        editor.putInt("helligkeit", helligkeitBar.getProgress());
                        editor.putInt("schlafzeit", dimmungBar.getProgress());
                        editor.putBoolean("dimmung", dimmungSwitch.isChecked());
                        Toast.makeText(appContext, "Einstellungen geändert!", Toast.LENGTH_SHORT).show();
                        newMessage = false;
                        return;
                    }
                } catch (InterruptedException e) {
                    Log.e("Thread", "Thread stopped");
                    return;
                }
            }
            Toast.makeText(appContext, "Wecker ist nicht erreichbar!", Toast.LENGTH_SHORT).show();
            Log.e("Thread", "Thread stopped");
        }
    }
}