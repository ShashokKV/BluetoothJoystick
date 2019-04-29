#include <Servo.h>
#include <NewPing.h>
#include <Wire.h>
#include <Adafruit_MotorShield.h>
#include "utility/Adafruit_MS_PWMServoDriver.h"

#define SONAR_NUM 2
#define F_LED_PIN 2
#define F_TRIG_PIN 3
#define F_ECHO_PIN 4
#define F_SONAR_PIN_PWR 5
#define F_SENSOR_PIN 6
#define F_SENS_PIN_PWR 7
#define B_ECHO_PIN 8
#define B_TRIG_PIN 9
#define SERVO_PIN 10
#define B_SENSOR_PIN 12
#define B_LED_PIN 13
#define F_SONAR_PIN_PWR A1
#define B_SENS_PIN_PWR A2
#define B_SONAR_PIN_PWR A3
#define MAX_DISTANCE 100
Adafruit_MotorShield AFMS = Adafruit_MotorShield();
Adafruit_DCMotor *myMotor = AFMS.getMotor(1);
NewPing sonar[SONAR_NUM] = {
  NewPing(F_TRIG_PIN, F_ECHO_PIN, MAX_DISTANCE),
  NewPing(B_TRIG_PIN, B_ECHO_PIN, MAX_DISTANCE)
};

int angleGlobal;
String val = "";
boolean x, y, b, l;
boolean autoStop;
Servo servo;

void setup() {
  Serial.begin(9600);
  pinMode(F_LED_PIN, OUTPUT);
  pinMode(F_LED_PIN, OUTPUT);
  pinMode(F_SENSOR_PIN, INPUT);
  pinMode(B_SENSOR_PIN, INPUT);
  pinMode(F_SENS_PIN_PWR, OUTPUT);
  pinMode(F_SONAR_PIN_PWR, OUTPUT);
  pinMode(B_SENS_PIN_PWR, OUTPUT);
  pinMode(B_SONAR_PIN_PWR, OUTPUT);

  AFMS.begin();
  
  angleGlobal = 90;
  autoStop = false;
  servo.attach(SERVO_PIN);
}

void turnServo(int angle) {
  angle = constrain(angle, 78, 98);
  if (angle != angleGlobal) {
    servo.write(angle);
    angleGlobal = angle;
  }
}

void goForward() {
  myMotor->run(FORWARD);
}

void goBackward() {
  myMotor->run(BACKWARD);
}

void throttle(int speedVal) {
  speedVal = constrain(checkSensors(speedVal), -255, 255);
  if (speedVal >= 0) {
    goForward();
  } else {
    speedVal = -speedVal;
    goBackward();
  }

  myMotor->setSpeed(speedVal);
}

int checkSensors(int speedVal) {
  if (!autoStop) return speedVal;
  int cm;
  int sensor_pin;
  if (speedVal > 0) {
    cm = sonar[0].ping_cm();
    sensor_pin = F_SENSOR_PIN;
  } else if (speedVal < 0) {
    cm = sonar[1].ping_cm();
    sensor_pin = B_SENSOR_PIN;
  } else {
    return 0;
  }
  if (autoStop) {
    if (digitalRead(sensor_pin) == HIGH) {
      speedVal = 0;
    } else if ((cm > 20) && (cm < 50)) {
      speedVal = (int)speedVal / 2;
    } else if ((cm > 10) && (cm <= 20)) {
      speedVal = (int) speedVal / 4;
    } else if ((cm > 0) && (cm <= 10)) {
      speedVal = 0;
    }
  }
  return speedVal;
}

void turnLights(int val) {
  if (val == 0) {
    digitalWrite(F_LED_PIN, LOW);
    digitalWrite(B_LED_PIN, LOW);
  } else {
    digitalWrite(F_LED_PIN, HIGH);
    digitalWrite(B_LED_PIN, HIGH);
  }
}

void turnAutoBrakes(int val) {
  if (val == 0) {
    autoStop = false;
    digitalWrite(F_SENS_PIN_PWR, LOW);
    digitalWrite(F_SONAR_PIN_PWR, LOW);
    digitalWrite(B_SENS_PIN_PWR, LOW);
    digitalWrite(B_SONAR_PIN_PWR, LOW);
  } else {
    autoStop = true;
    digitalWrite(F_SENS_PIN_PWR, HIGH);
    digitalWrite(F_SONAR_PIN_PWR, HIGH);
    digitalWrite(B_SENS_PIN_PWR, HIGH);
    digitalWrite(B_SONAR_PIN_PWR, HIGH);
  }
}

void loop() {
  if (Serial.available()) {
    char c = Serial.read();
    if (c == '#') {
      if (x) {
        turnServo(val.toInt());
      } else if (y) {
        throttle(val.toInt());
      } else if (l) {
        turnLights(val.toInt());
      } else if (b) {
        turnAutoBrakes(val.toInt());
      }
      val = "";
    } else if (c == 'X') {
      x = true;
      y = false;
      b = false;
      l = false;
      val = "";
    } else if (c == 'Y') {
      x = false;
      y = true;
      b = false;
      l = false;
      val = "";
    } else if (c == 'B') {
      x = false;
      y = false;
      b = true;
      l = false;
      val = "";
    } else if (c == 'L') {
      x = false;
      y = false;
      b = false;
      l = true;
      val = "";
    } else if (c == 'S') {
      throttle(0);
      x = false;
      y = false;
      b = false;
      l = false;
      val = "";
    } else if (c == 'T') {
      x = false;
      y = false;
      b = false;
      l = false;
      val = "";
    } else {
      val += c;
    }
  }
}
