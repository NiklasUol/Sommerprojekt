#include <WiFiManager.h>
#include <ArduinoMqttClient.h>

WiFiManager wifiManager;
WiFiClient client;


//MQTT Settings
MqttClient mqttClient(client);
const char broker[] = "broker.hivemq.com";
int        port     = 1883;
const char topic[]  = "wecker/stopAlarm";

const int knopf = 14; //D5

void setup() {
  Serial.begin(115200);
  Serial.setTimeout(10);
  wifiManager.autoConnect("Aus-Knopf");
  mqttSetup();

  pinMode(knopf, INPUT);
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
  
  //if(Serial.available() > 0 ){   //Zum Testen in der Konsole
  if(digitalRead(knopf) == HIGH){
    sendSignal();
    Serial.print("Signal gesendet.");
    Serial.print(Serial.readString());
    delay(1500);
  }
}

void sendSignal(){
  mqttClient.beginMessage(topic);
  mqttClient.print("Stop Alarm!");
  mqttClient.endMessage();
  }