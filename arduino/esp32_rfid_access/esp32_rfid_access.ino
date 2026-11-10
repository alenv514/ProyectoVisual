#include <SPI.h>
#include <MFRC522.h>

// Mapeo explícito para ESP32 DevKit
#define SCK_PIN  18
#define MISO_PIN 19
#define MOSI_PIN 23
#define SS_PIN   21
#define RST_PIN  22

MFRC522 mfrc522(SS_PIN, RST_PIN);

void setup() {
  Serial.begin(115200);
  SPI.begin(SCK_PIN, MISO_PIN, MOSI_PIN, SS_PIN);
  mfrc522.PCD_Init();
}

void loop() {
  // Lógica pendiente de implementar...
}
