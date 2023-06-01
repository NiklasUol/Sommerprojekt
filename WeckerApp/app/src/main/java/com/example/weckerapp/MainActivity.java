package com.example.weckerapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
    private static final String BROKER_URL = "tcp://broker.hivemq.com:1883";
    private static final String CLIENT_ID = "Smartphone";

    private TextView statusfeld;
    private TimePicker timePicker;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appContext = getApplicationContext();
        preferences  = PreferenceManager.getDefaultSharedPreferences(appContext);

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

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int stunde = timePicker.getHour();
                int minute = timePicker.getMinute();
                sendSignal("wecker/weckzeit", stunde + ":" + minute);
            }
        });
        subscribe("wecker/weckzeitresponse");
    }

    @Override
    protected void onDestroy() {
        disconnectMqtt();
        super.onDestroy();
    }

    private void connectMqtt(){
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            client = new MqttClient(BROKER_URL, CLIENT_ID, persistence);

            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);

            //Connect to the broker
            client.connect(connectOptions);
        } catch (MqttException e) {
            System.err.println(e.getMessage());
            System.err.println(e.getCause());
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
            client.publish(topic, mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void subscribe(String topic) {
        if(client == null){
            connectMqtt();
        }
        try {
            client.subscribe(topic, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    byte[] byteMessage = message.getPayload();
                    String stringMessage = new String(byteMessage);

                    Log.e("Mqtt message", stringMessage);
                    newMessage = true;

                    client.messageArrivedComplete(message.getId(), message.getQos());
                    Log.e("Client connected:", String.valueOf(client.isConnected()));
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private boolean newMessage = false;
    private void sendSignal(String topic, String message){
        statusfeld.setText("Wird gesendet...");
        statusfeld.setTextColor(Color.DKGRAY);
        connectMqtt();
        publish(topic, message);
        subscribe("wecker/weckzeitresponse");
        new ResponseThread(waitForResponse).start();
        //Log.e("Client connected: ", String.valueOf(client.isConnected()));
    }

    Runnable waitForResponse = new Runnable() {
        @Override
        public void run() {
            Looper.prepare();
            Log.e("Thread", "Thread working...");
            for(int i = 0; i < 3000; i++){
                try {
                    Thread.currentThread().sleep(1);
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
                        break;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(i == 2999){
                    Toast.makeText(appContext, "Weckzeit konnte nicht gesendet werden!", Toast.LENGTH_SHORT).show();
                    statusfeld.setText("Wecker ist nicht erreichbar");
                    statusfeld.setTextColor(Color.RED);
                }
            }
            Log.e("Thread", "Thread stopped");
        }
    };
}