#include <WiFiManager.h>
#include <ArduinoMqttClient.h>

WiFiManager wifiManager;
WiFiClient client;

//Mqtt Settings
MqttClient mqttClient(client);
const char broker[] = "broker.hivemq.com";
int        port     = 1883;
const char topic[]  = "wecker/weckzeit";

//Wecker-Attribute
String weckzeit;


void setup() {
  Serial.begin(115200);  
   wifiManager.autoConnect("Wemos_D1");

   mqttSetup();
  mqttSubscribe(topic);
}

void loop() {
  mqttClient.poll();
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

  mqttClient.onMessage(onMqttMessage);
}

void mqttSubscribe(const char topic[]){
    mqttClient.subscribe(topic);
  Serial.print("Mit folgendem Mqtt-Topic verbunden: ");
  Serial.println(topic);
}

//Ausgabe der empfangenden Nachricht und Speicherung der Weckzeit
void onMqttMessage(int messageSize) {
  Serial.println("Nachricht mit folgendem Topic erhalten: '");
  String receivedTopic = mqttClient.messageTopic();
  Serial.print(receivedTopic);
  Serial.print("', laenge ");
  Serial.print(messageSize);
  Serial.println(" bytes:");

  // Nachricht lesen und ausgeben
  String nachricht;
  while (mqttClient.available()) {
    nachricht = nachricht + (char)mqttClient.read();
  }
  Serial.println(nachricht);
  Serial.println();

  if(receivedTopic.equals("wecker/weckzeit")){
    Serial.println("Weckzeit " + nachricht + " gespeichert!");
    weckzeit = nachricht;
  }

  //Sendet Empfangsbestaetigung zurueck
  const char sendTopic[]  = "wecker/weckzeitresponse";
  sendMqttMessage(sendTopic, "Daten erhalten!");
}

void sendMqttMessage(const char sendTopic[], String message){
  mqttClient.beginMessage(sendTopic);
  mqttClient.print(message);
  mqttClient.endMessage();
}
