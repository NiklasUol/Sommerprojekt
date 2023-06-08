#include <WiFiManager.h>
#include <ArduinoMqttClient.h>

WiFiManager wifiManager;
WiFiClient client;


//MQTT Settings
MqttClient mqttClient(client);
const char broker[] = "broker.hivemq.com";
int        port     = 1883;
const char topic[]  = "wecker/stopalarm";

void setup() {
  mqttSetup();
  Serial.begin(115200);
}

void mqttSetup(){
  Serial.print("Versuche mit folgendem MQTT-Broker zu verbinden: ");
  Serial.println(broker);

  if (!mqttClient.connect(broker, port)) {
    Serial.print("MQTT Verbindung fehlgeschlagen! Error: ");
    Serial.println(mqttClient.connectError());

    while (1);
  }

  Serial.println("Erfolgreich verbunden!");
  Serial.println();

}
void loop() {
  mqttClient.poll();
  if(Serial.readString().equals("Stop")){
    sendSignal();
  }
}

void sendSignal(){
  mqttClient.beginMessage(topic);
  mqttClient.print("Stop Alarm!");
  mqttClient.endMessage();
  }