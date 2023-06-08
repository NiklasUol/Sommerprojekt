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
  Serial.begin(115200);
  Serial.setTimeout(10);
  wifiManager.autoConnect("Wemos_D1");
  mqttSetup();
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
  if(Serial.available() > 0 ){
    sendSignal();
    Serial.print("Signal gesendet.");
    Serial.print(Serial.readString());
  }
}

void sendSignal(){
  mqttClient.beginMessage(topic);
  mqttClient.print("Stop Alarm!");
  mqttClient.endMessage();
  }