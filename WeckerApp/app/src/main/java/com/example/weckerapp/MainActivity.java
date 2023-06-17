package com.example.weckerapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MainActivity extends AppCompatActivity {

    private Context appContext;
    private MqttClient client;
    public static String brokerURL = "tcp://broker.hivemq.com:1883";
    public static String clientId = "Smartphone";

    public static TextView statusfeld;
    private TimePicker timePicker;

    private SharedPreferences preferences;

    private Thread responseThread;
    private Thread mqttConnectThread;
    public static Boolean newMessage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appContext = getApplicationContext();

        preferences  = PreferenceManager.getDefaultSharedPreferences(appContext);
        brokerURL = preferences.getString("broker", brokerURL);

        timePicker = (TimePicker) findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        timePicker.setSaveEnabled(true);
        //Lade gespeicherte Einstellungen der Weckzeit
        if(preferences.contains("hour")){
            timePicker.setHour(preferences.getInt("hour", 8));
            timePicker.setMinute(preferences.getInt("minute", 0));
        }

        statusfeld = (TextView) findViewById(R.id.statusfeld);
        statusfeld.setText("Warte auf Eingabe");
        statusfeld.setTextColor(Color.DKGRAY);

        connectMqtt("wecker/weckzeitresponse");

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int stunde = timePicker.getHour();
                int minute = timePicker.getMinute();
                sendSignal("wecker/weckzeit", stunde + ":" + minute);
            }
        });

        ImageButton settingsButton = (ImageButton) findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(appContext, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onDestroy() {
        disconnectMqtt();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        connectMqtt("wecker/weckzeitresponse");
        super.onResume();
    }

    private void connectMqtt(String topic){
        statusfeld.setTextColor(Color.DKGRAY);
        statusfeld.setText("Wird verbunden...");

        MemoryPersistence persistence = new MemoryPersistence();
        try {
            if(mqttConnectThread == null){
                client = new MqttClient(brokerURL, clientId, persistence);
                mqttConnectThread = new MqttConnectThread(client, topic);
            }
            if(!mqttConnectThread.isAlive()){
                client = new MqttClient(brokerURL, clientId, persistence);
                mqttConnectThread = new MqttConnectThread(client, topic);
                mqttConnectThread.start();
            }
        } catch (MqttException e) {
            System.err.println(e.getMessage());
        }
    }

    private void disconnectMqtt(){
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void publish(String topic, String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            if(client.isConnected()){
                client.publish(topic, mqttMessage);
            }
            else {
                statusfeld.setTextColor(Color.RED);
                statusfeld.setText("Keine Verbindung zum MQTT-Broker");
                Toast.makeText(appContext, "Keine Verbindung zum MQTT-Broker", Toast.LENGTH_LONG).show();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    private void sendSignal(String topic, String message){
        if(!client.isConnected()){
            Toast.makeText(appContext, "Keine Verbindung zum MQTT-Broker", Toast.LENGTH_LONG).show();
            return;
        }
        statusfeld.setText("Wird gesendet...");
        statusfeld.setTextColor(Color.DKGRAY);
        publish(topic, message);
        //subscribe("wecker/weckzeitresponse");
        if(responseThread != null){
            responseThread.interrupt();
        }
        responseThread = new ResponseThread();
        responseThread.start();
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
                        Toast.makeText(appContext, "Weckzeit erfolgreich gesendet!", Toast.LENGTH_SHORT).show();
                        newMessage = false;
                        statusfeld.setText("Weckzeit erfolgreich übertragen!");
                        statusfeld.setTextColor(Color.GREEN);

                        //Speichert die eingestellte Zeit in der App
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putInt("hour", timePicker.getHour());
                        editor.putInt("minute", timePicker.getMinute());
                        editor.commit();
                        Log.e("Thread", "Thread stopped");
                        return;
                    }
                } catch (InterruptedException e) {
                    Log.e("Thread", "Thread stopped");
                    return;
                }
            }
            Toast.makeText(appContext, "Weckzeit konnte nicht gesendet werden!", Toast.LENGTH_SHORT).show();
            statusfeld.setText("Wecker ist nicht erreichbar");
            Log.e("Thread", "Thread stopped");
        }
    }
}