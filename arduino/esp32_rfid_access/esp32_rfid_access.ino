/*
 * =====================================================================
 * SISTEMA DE CONTROL DE ACCESO Y ASISTENCIA CON RFID (ESP32 + RC522)
 * =====================================================================
 * Conexión ESP32 DevKit <-> MFRC522:
 *   3.3V  -> 3V3
 *   RST   -> GPIO 22
 *   GND   -> GND
 *   MISO  -> GPIO 19
 *   MOSI  -> GPIO 23
 *   SCK   -> GPIO 18
 *   SDA   -> GPIO 21 (SS)
 * =====================================================================
 */

#include <SPI.h>
#include <MFRC522.h>

#define RST_PIN         22
#define SS_PIN          21
#define SCK_PIN         18
#define MISO_PIN        19
#define MOSI_PIN        23

#define PIN_LED_VERDE    2
#define PIN_LED_ROJO     4
#define PIN_BUZZER      15

MFRC522 mfrc522(SS_PIN, RST_PIN);

void setup() {
  Serial.begin(115200);
  delay(300);

  pinMode(PIN_LED_VERDE, OUTPUT);
  pinMode(PIN_LED_ROJO, OUTPUT);
  pinMode(PIN_BUZZER, OUTPUT);

  digitalWrite(PIN_LED_VERDE, LOW);
  digitalWrite(PIN_LED_ROJO, LOW);
  digitalWrite(PIN_BUZZER, LOW);

  // Inicializar bus SPI
  SPI.begin(SCK_PIN, MISO_PIN, MOSI_PIN, SS_PIN);
  delay(100);

  mfrc522.PCD_Init();
  delay(100);

  // Asegurar antena activa
  mfrc522.PCD_AntennaOn();
  delay(50);

  // Pitido inicial
  digitalWrite(PIN_BUZZER, HIGH);
  delay(80);
  digitalWrite(PIN_BUZZER, LOW);

  Serial.println("ESP32_LISTO");
  Serial.print("TEST_RC522: ");
  mfrc522.PCD_DumpVersionToSerial();
}

void loop() {
  // 1. Escuchar respuestas de Java
  if (Serial.available() > 0) {
    String respuesta = Serial.readStringUntil('\n');
    respuesta.trim();
    procesarRespuestaJava(respuesta);
  }

  // 2. Detección robusta de tarjetas y llaveros (WakeupA)
  byte bufferATQA[2];
  byte bufferSize = sizeof(bufferATQA);
  MFRC522::StatusCode status = mfrc522.PICC_WakeupA(bufferATQA, &bufferSize);

  if (status != MFRC522::STATUS_OK && status != MFRC522::STATUS_COLLISION) {
    // Si WakeupA no respondió, probar con IsNewCardPresent
    if (!mfrc522.PICC_IsNewCardPresent()) {
      return;
    }
  }

  // Leer número de serie de la tarjeta
  if (!mfrc522.PICC_ReadCardSerial()) {
    return;
  }

  // Construir UID
  String uidStr = "";
  for (byte i = 0; i < mfrc522.uid.size; i++) {
    if (mfrc522.uid.uidByte[i] < 0x10) {
      uidStr += "0";
    }
    uidStr += String(mfrc522.uid.uidByte[i], HEX);
  }
  uidStr.toUpperCase();

  // Enviar a Java: UID:XXXXXX
  Serial.println("UID:" + uidStr);

  // Pitido de detección
  digitalWrite(PIN_BUZZER, HIGH);
  delay(60);
  digitalWrite(PIN_BUZZER, LOW);

  mfrc522.PICC_HaltA();
  mfrc522.PCD_StopCrypto1();

  delay(600);
}

void procesarRespuestaJava(String resp) {
  if (resp.startsWith("OK:")) {
    digitalWrite(PIN_LED_VERDE, HIGH);
    digitalWrite(PIN_BUZZER, HIGH);
    delay(150);
    digitalWrite(PIN_BUZZER, LOW);
    delay(1000);
    digitalWrite(PIN_LED_VERDE, LOW);
  } else if (resp.startsWith("ERR:")) {
    for (int i = 0; i < 2; i++) {
      digitalWrite(PIN_LED_ROJO, HIGH);
      digitalWrite(PIN_BUZZER, HIGH);
      delay(100);
      digitalWrite(PIN_LED_ROJO, LOW);
      digitalWrite(PIN_BUZZER, LOW);
      delay(100);
    }
  }
}
