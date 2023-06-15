#include <WiFiManager.h>
#include <ArduinoMqttClient.h>

#include <WiFiUdp.h>
#include <NTPClient.h>

WiFiManager wifiManager;
WiFiClient client;

//Fuer Zeitabruf
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);

//Mqtt Settings
MqttClient mqttClient(client);
const char broker[] = "broker.hivemq.com";
int port = 1883;
const char topic[] = "wecker/weckzeit";

//Wecker-Attribute
int weckminute = 0;
int weckstunde = 0;
bool alarm = false;

//Pins (GPIO Nummern entsprechen nicht Anschluessen)
const int buzzer = 12; //D6

void updateTimeClient();

void setup() {
  Serial.begin(115200);
  wifiManager.autoConnect("Wecker");

  mqttSetup();
  mqttSubscribe(topic);

  timeClient.begin();
  timeClient.setTimeOffset(7200);
  timeClient.setUpdateInterval(86400000);  //Zeit wird alle 24h neu geladen
  updateTimeClient();

  pinMode(buzzer, OUTPUT);
}



void loop() {
  mqttClient.poll();   //Haelt die MQTT Verbindung aufrecht
  updateTimeClient();  //Laedt die Zeit regelmaeßig vom Server
  startAlarm();
}



void mqttSetup() {
  Serial.print("Versuche mit folgendem MQTT-Broker zu verbinden: ");
  Serial.println(broker);

  if (!mqttClient.connect(broker, port)) {
    Serial.print("MQTT Verbindung fehlgeschlagen! Error: ");
    Serial.println(mqttClient.connectError());

    while (1)
      ;
  }

  Serial.println("Erfolgreich verbunden!");
  Serial.println();

  mqttClient.onMessage(onMqttMessage);
}

void mqttSubscribe(const char topic[]) {
  mqttClient.subscribe(topic);
  Serial.print("Mit folgendem Mqtt-Topic verbunden: ");
  Serial.println(topic);
}

//Ausgabe der empfangenden Nachricht und Speicherung der Weckzeit
void onMqttMessage(int messageSize) {
  Serial.print("Uhrzeit: ");
  Serial.println(timeClient.getFormattedTime());
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

  if (receivedTopic.equals("wecker/weckzeit")) {
    Serial.println("Weckzeit " + nachricht + " gespeichert!");
    int deviderIndex = nachricht.indexOf(':');
    weckstunde = nachricht.substring(0, deviderIndex).toInt();
    weckminute = nachricht.substring(deviderIndex + 1).toInt();

    //Sendet Empfangsbestaetigung an Smartphone zurueck
    const char sendTopic[] = "wecker/weckzeitresponse";
    sendMqttMessage(sendTopic, "Daten erhalten!");
  }
  
  //TODO: Hier alarm auf false setzten, wenn das Topic der Nachricht "wecker/stopAlarm" ist
  /*
  .....
  */


}

void sendMqttMessage(const char sendTopic[], String message) {
  mqttClient.beginMessage(sendTopic);
  mqttClient.print(message);
  mqttClient.endMessage();
}

//Laedt die Zeit von einem Zeitserver herunter
//Muss nicht bei jedem Zeitabruf verwendet werden, da die Zeit auch so weiterlaeuft
//(Stattdessen z.B. timeClient.getHours())
void updateTimeClient() {
  if (timeClient.update()) {
    Serial.println("Zeit geladen!");
    Serial.print("Stunde: ");
    Serial.print(timeClient.getHours());
    Serial.print(", Minute: ");
    Serial.println(timeClient.getMinutes());
    Serial.println();
  }
}

void display(int hours, int minutes) {
  //TODO: Display Programmierung hinzufügen
  //...
}


void startAlarm() {
  if (timeClient.getHours() == weckstunde && timeClient.getMinutes() == weckminute) {
    alarm = true;
    Serial.println("Alarm gestartet...");
    while (alarm) {
      tone(buzzer, 300);
      delay(200);
      noTone(buzzer);
      delay(200);
      display(timeClient.getHours(), timeClient.getMinutes());
    }
  }
}
