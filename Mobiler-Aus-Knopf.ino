#include <WiFiManager.h>
#include <ArduinoMqttClient.h>

WiFiManager wifiManager;
WiFiClient client;


void sendSignal(String signal){
    if(mqttClient.poll() == 1){//Wenn der Knopf gedrück wurde

    }
}
