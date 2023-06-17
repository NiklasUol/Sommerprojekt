package com.example.weckerapp;

import android.graphics.Color;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttConnectThread extends Thread{
    private MqttClient client;
    private String topic;

    public MqttConnectThread(MqttClient client, String topic){
        this.client = client;
        this.topic = topic;
    }

    @Override
    public void run() {
        System.out.println("THREAD START");
        try {
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            //Connect to the broker
            client.connect(connectOptions);
            if(!client.isConnected()){
                return;
            }
            MainActivity.statusfeld.setText("Warte auf Eingabe...");
            MainActivity.statusfeld.setTextColor(Color.DKGRAY);
        }
        catch (MqttException e) {
            System.err.println(e.getMessage());
            System.err.println(e.getCause());
            MainActivity.statusfeld.setText("Keine Verbindung zum MQTT-Broker");
            MainActivity.statusfeld.setTextColor(Color.RED);
            return;
        }


        try {
            client.subscribe(topic, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    byte[] byteMessage = message.getPayload();
                    String stringMessage = new String(byteMessage);

                    Log.e("Mqtt message", stringMessage);
                    MainActivity.newMessage = true;

                    client.messageArrivedComplete(message.getId(), message.getQos());
                    Log.e("Client connected:", String.valueOf(client.isConnected()));
                }
            });
        } catch (MqttException e) {
            System.err.println(e.getMessage());
        }

        System.out.println("THREAD STOPP");
    }
}
