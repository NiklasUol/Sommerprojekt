#include <WiFiManager.h>
#include <ArduinoMqttClient.h>

#include <WiFiUdp.h>
#include <NTPClient.h>

#include <Wire.h> // Enable this line if using Arduino Uno, Mega, etc.
#include <Adafruit_GFX.h>
#include "Adafruit_LEDBackpack.h"

WiFiManager wifiManager;
WiFiClient client;

//Fuer Zeitabruf
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);

//Mqtt Settings
MqttClient mqttClient(client);
const char broker[] = "broker.hivemq.com";
//const char broker[] = "public.mqtthq.com";

int port = 1883;
const char topicWeckzeit[] = "wecker/weckzeit";
const char topicStopAlarm[] = "wecker/stopAlarm";
const char topicSettings[] = "wecker/settings";

//Wecker-Attribute
int weckminute = 0;
int weckstunde = 0;
bool alarm = false;

bool dimDisplay = true;
bool displayIsDimmed = false;
int dimTime = 22; //22Uhr
int helligkeit = 7;

//Pins (GPIO Nummern entsprechen nicht Anschluessen)
const int buzzer = 14; //D5
const int CLK = D1; //Set the CLK pin connection to the display
const int DIO = D2; //Set the DIO pin connection to the display

Adafruit_7segment display = Adafruit_7segment();

void updateTimeClient();

void setup() {
  Serial.begin(115200);
  display.begin(0x70);
  display.print("STAR");
  display.setBrightness(helligkeit); //set the diplay to maximum brightness
  display.writeDisplay();
  wifiManager.autoConnect("Wecker");

  mqttSetup();
  mqttSubscribe(topicWeckzeit);
  mqttSubscribe(topicStopAlarm);
  mqttSubscribe(topicSettings);

  timeClient.begin();
  timeClient.setTimeOffset(7200);
  timeClient.setUpdateInterval(86400000);  //Zeit wird alle 24h neu geladen
  updateTimeClient();

  pinMode(buzzer, OUTPUT);
  digitalWrite(buzzer,LOW);

  tone(buzzer, 700, 500);
}



void loop() {
  mqttClient.poll();   //Haelt die MQTT Verbindung aufrecht
  updateTimeClient();  //Laedt die Zeit regelmaeßig vom Server
  startAlarm();
  setDisplay(timeClient.getHours(), timeClient.getMinutes());
}



void mqttSetup() {
  Serial.print("Versuche mit folgendem MQTT-Broker zu verbinden: ");
  Serial.println(broker);

  while (!mqttClient.connect(broker, port)) {
    Serial.print("MQTT Verbindung fehlgeschlagen! Error: ");
    Serial.println(mqttClient.connectError());
    tone(buzzer, 100, 500);
    delay(2000);
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

  if (receivedTopic.equals(topicWeckzeit)) {
    Serial.println("Weckzeit " + nachricht + " gespeichert!");
    int deviderIndex = nachricht.indexOf(':');
    weckstunde = nachricht.substring(0, deviderIndex).toInt();
    weckminute = nachricht.substring(deviderIndex + 1).toInt();

    //Sendet Empfangsbestaetigung an Smartphone zurueck
    const char sendTopic[] = "wecker/response";
    sendMqttMessage(sendTopic, "Daten erhalten!");
    
    //Signalton zur Bestätigung
    for(int i = 0; i < 30; i++){
      tone(buzzer, 300 + (20 * i), 100);
    }
    
    //Wecker zeigt empfangende Zeit an
    for(int i = 0; i < 5; i++){
      setDisplay(weckstunde, weckminute);
      delay(1000);
      display.clear();
      display.writeDisplay();
      delay(1000);
    }
  }

  if (receivedTopic.equals(topicStopAlarm)) {
    alarm = false;
    Serial.println("Alarm gestopp!");
  }

  if(receivedTopic.equals(topicSettings)){
    helligkeit = 15* (1 / nachricht.substring(0).toInt());
    display.setBrightness(helligkeit);
    display.writeDisplay();
    sendMqttMessage("wecker/response", "Settings erhalten");
    tone(buzzer, 700, 500);

    if(nachricht.length() > 1){
      dimTime = nachricht.substring(2, nachricht.length()).toInt();
      dimDisplay = true;
    }
    else {
      dimDisplay = false;
      displayIsDimmed = false;
    }
  }
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

bool dimDone = false;

void setDisplay(int hours, int minutes) {
  //TODO: Display Programmierung hinzufügen
  int time = (hours * 100) + minutes;
  if(!displayIsDimmed){
    display.print(time);
    if(!dimDone && dimDisplay && hours == dimTime){
    display.setBrightness(0);
    display.clear();
    displayIsDimmed = true;
    dimDone = true;
    }
    display.writeDisplay();
  }

  if(dimDone && dimTime != hours){
    dimDone = false;
  }
}


bool alarmDone = false;

void startAlarm() {
  if (timeClient.getHours() == weckstunde && timeClient.getMinutes() == weckminute && !alarmDone) {
    alarm = true;
    Serial.println("Alarm gestartet...");
    display.setBrightness(helligkeit);
    display.writeDisplay();
    displayIsDimmed = false;
    while (alarm) {
      Serial.println("Alarm");
      tone(buzzer,500);
      delay(400);
      display.clear();
      display.writeDisplay();
      noTone(buzzer);
      delay(400);

      setDisplay(timeClient.getHours(), timeClient.getMinutes());
      mqttClient.poll();
    }
    alarmDone = true;
  }
  
  //Verhindert erneute Auslösung des Alarms
  else if (!(timeClient.getHours() == weckstunde && timeClient.getMinutes() == weckminute) && alarmDone) {
    alarmDone = false;
  }
}