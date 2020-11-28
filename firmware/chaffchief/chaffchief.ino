/*

ChaffChief - Open Source Fluid Bed Coffee Roaster Firmware

Original author: Roberto Buaiz (riozebratubo[-at-]yahoo.com.br)
Copyright 2020, Roberto Buaiz
Contributors: -
First version on 01/12/2020
License: GPL v3 (see LICENSING.TXT, https://www.gnu.org/licenses/gpl-3.0.txt)

Please consider donating to the author if you find this sofware or portions of it useful. A huge amount of time and resources were spent on the making of this software.

Info
  - This firmware is built to be commercial-grade, meaning it should be robust and work out-of-the-box. Nevertheless, this firmware is provided as-is. There is no support and no warranties, you may use it at your own risk.
  - This firmware is intented to be built for an Arduino Mega 2560
  - Libraries included: Adafruit MAX31856, Pid (they were customized, licenses on the libraries_licenses folder)
  - Libraries needed: Eeprom, TimerThree (you may have to install them on Arduino IDE)
  - Arduino Mega serial interfaces usage: Serial  -> logging, Serial2 -> bluetooth, Serial3 -> artisan usb
  - The customizations made on Adafruit MAX31856 are: using the chip automatic reading and letting us select the number of samples on each measurement
  - Currently no customizations on PID library, but freezing its version
  - The decision to use GPL v3 for licensing is based on: if someone uses this software commercially, the open source community should have access to its source code

Code options (define to enable):
  - IMPORTANT_LOG_SERIAL: only important events are logged to the Serial
  - DEBUG_LOG_SERIAL: more debug-specific information is logget to the Serial also
  - CAN_UPDATE_PID_PARAMETERS_ON_THE_FLY: the firmware accepts input on the Serial (not Serial2) on the format "X.XX,X.XX,X.XX" corresponding to the pid parameters Kp, Ki, Kd to be ajusted on-the-fly during a roasting session. After finishing a roasting session, the parameters are reseted back to the firmware's default values.
  - ENABLE_LED_INDICATOR: enables switching the color of a RGB led module to indicate the roaster's state
  - ENABLE_PHYSICAL_START_STOP_BUTTON: enables the use of a physical push button to control the roaster's state

Hardware:
  - Please see the project's home page.

Coffee roasting considerations:
  - This firmware is made to receive roasting profiles that are linear interpolated via bluetooth on Serial2. Refer to the project's home page to see the profile format specification and examples.
  - This firmware remembers the last profile received, even on resets or power losses (it uses the arduino EEPROM to do that).
  - This firmware has 3 states of operation: stopped (= manual), roasting, cooling. Pressing the hardware button, if available, will cycle between them, respecting the smooth fan going up or down times. If there's no hardware button, you have to use a bluetooth command for cycling the states.
  - This firmware operates on Celsius temperatures only.
  - This firmware supports Artisan to control the roaster. It works on stopped (manual) mode, and it accepts power and fan commands and responds the current temperature. You can even use Artisan's PID features. Be aware that if you intend to use only this mode, you don't need 95% of this software. Consider writing a smaller firmware that only controls fan and power and answers Artisan. There are a lot of those firmwares available online.
*/

#define IMPORTANT_LOG_SERIAL
//#define DEBUG_LOG_SERIAL
#define CAN_UPDATE_PID_PARAMETERS_ON_THE_FLY
//#define ENABLE_LED_INDICATOR
#define ENABLE_PHYSICAL_START_STOP_BUTTON
















#include <EEPROM.h>
#include <TimerThree.h>
#include "Adafruit_MAX31856.h"
#include "PID_v1.h"

#define FIRMWARE_VERSION "Popcorn, v1.0"

// roaster finite state machine states
#define STATE_STOPPED                           1
#define STATE_PREHEATING                        2 // not being used
#define STATE_ROASTING                          3 // not being used
#define STATE_COOLING                           4 // not being used
#define STATE_ABORTED_WAITING_DUMP              5 // not being used
#define STATE_MENU_MANUAL_AUTO                  6
#define STATE_MENU_SELECT_PROFILE               7
#define STATE_AUTO_ROASTING_PRE                 8 
#define STATE_AUTO_ROASTING                     9
#define STATE_AUTO_COOLING                      10
#define STATE_AUTO_COOLING_AFTER                11
#define STATE_AUTO_END                          12
#define STATE_AUTO_COMPUTER_CONTROLLED_PRE      13 // not being used
#define STATE_AUTO_COMPUTER_CONTROLLED          14 // not being used
#define STATE_BLUETOOTH_PROFILE_RECEIVE         15
#define LAST_STATE_NUMBER                       16

// update this if adding new states
#define IS_NOT_A_ROASTING_STATE(st) ((st != STATE_ROASTING) && (st != STATE_COOLING) && (st != STATE_AUTO_ROASTING_PRE) && (st != STATE_AUTO_ROASTING) && (st != STATE_AUTO_COOLING) && (st != STATE_AUTO_COOLING_AFTER))
#define IS_A_ROASTING_STATE(st) ((st == STATE_ROASTING) || (st == STATE_COOLING) || (st == STATE_AUTO_ROASTING_PRE) || (st == STATE_AUTO_ROASTING) || (st == STATE_AUTO_COOLING) || (st == STATE_AUTO_COOLING_AFTER))

// roaster finite state machine inputs
#define ACTION_PROCCEED            1
#define ACTION_ABORT               2

// roaster finite state machine state transitions table for the action proceed
const int state_transitions_procceed[] = {
  /* --- */ 0,
  /* STATE_STOPPED -> */ STATE_ROASTING,
  /* STATE_PREHEATING -> */ STATE_ROASTING,
  /* STATE_ROASTING -> */ STATE_COOLING,
  /* STATE_COOLING -> */ STATE_STOPPED,
  /* STATE_ABORTED_WAITING_DUMP -> */ STATE_COOLING,
  /* STATE_MENU_MANUAL_AUTO -> */ STATE_MENU_SELECT_PROFILE,
  /* STATE_MENU_SELECT_PROFILE -> */ STATE_AUTO_ROASTING_PRE,
  /* STATE_AUTO_ROASTING_PRE -> */ STATE_AUTO_ROASTING,
  /* STATE_AUTO_ROASTING -> */ STATE_AUTO_COOLING,
  /* STATE_AUTO_COOLING -> */ STATE_AUTO_COOLING_AFTER,  
  /* STATE_AUTO_COOLING_AFTER -> */ STATE_AUTO_END,
  /* STATE_AUTO_END -> */ STATE_MENU_MANUAL_AUTO,
  /* STATE_AUTO_COMPUTER_CONTROLLED_PRE -> */ STATE_AUTO_COMPUTER_CONTROLLED,
  /* STATE_AUTO_COMPUTER_CONTROLLED -> */ STATE_AUTO_END,
  /* STATE_BLUETOOTH_PROFILE_RECEIVE -> */ STATE_BLUETOOTH_PROFILE_RECEIVE
};

// roaster finite state machine state transitions table for the action abort (not being used as we only have one hardware button at the moment)
const int state_transitions_abort[] = {0, STATE_STOPPED, STATE_STOPPED, STATE_ABORTED_WAITING_DUMP, STATE_STOPPED, STATE_STOPPED};

int menu_manual_auto_position = 0;

unsigned int state;

unsigned long time_start = 0;
unsigned long time_stop = 0;
unsigned long time_stop_cooling = 0;

volatile int global_power = 0;
volatile int fan = 0;

// bluetooth

int bluetooth_profile_received = 0;
int bluetooth_profile_selected = 0;

// pid

double pidSetpoint, pidInput, pidOutput;
// this initialization parameters are dummy and will be replaced on each roasting session's start
PID heaterPid(&pidInput, &pidOutput, &pidSetpoint, 15.0, 0.1, 22.5, DIRECT);
byte already_initialized_pid = 0;

// profiles

#define PROFILE_TYPE_PID         2

#define PROFILE_PID_TEMP_MAX 250        // used to reject profile points with temperatures greater than this
#define MAX_PROFILE_PID_POINTS 60       // max profile points
#define PROFILE_PID_MAX_TIME 1200000L   // max profile time (1200000 = 1200s = 20min)

struct Profile {
  int profile_selected_file_type;

  char profile_uuid[40];

  long profile_pid_times[MAX_PROFILE_PID_POINTS + 1];
  byte profile_pid_temps[MAX_PROFILE_PID_POINTS + 1];
  byte profile_pid_fans[MAX_PROFILE_PID_POINTS + 1];
  
  byte profile_pid_length = 0;
  byte profile_pid_fan_length = 0;
};

Profile profile;

void zeroProfile() {
  memset(&profile, 0, sizeof profile);
  profile.profile_selected_file_type = PROFILE_TYPE_PID;
  profile.profile_uuid[0] = '\0';
  profile.profile_pid_length = 0;
  profile.profile_pid_fan_length = 0;
}

#define EEPROM_CURRENT_DATA_VERSION 2

int eeprom_data_version = EEPROM_CURRENT_DATA_VERSION;

void eepromSaveProfile() {
  int eeAddress = 0;
  EEPROM.put(eeAddress, eeprom_data_version);
  eeAddress += sizeof(int);
  EEPROM.put(eeAddress, profile);
}

void eepromLoadProfile() {
  int eeAddress = 0;
  #ifdef IMPORTANT_LOG_SERIAL
  Serial.print(F("EEPROM booting data version: "));
  Serial.println(eeprom_data_version);
  #endif
  EEPROM.get(eeAddress, eeprom_data_version);
  #ifdef IMPORTANT_LOG_SERIAL
  Serial.print(F("EEPROM data read version: "));
  Serial.println(eeprom_data_version);
  #endif
  eeAddress += sizeof(int);
  switch (eeprom_data_version) {
    case 1:
      #ifdef IMPORTANT_LOG_SERIAL
      Serial.println(F("Older version EEPROM profile stored, reverting to no profile."));
      #endif
      zeroProfile();
      break;
    case 2:
      EEPROM.get(eeAddress, profile);
      if (profile.profile_selected_file_type != PROFILE_TYPE_PID) {
        #ifdef IMPORTANT_LOG_SERIAL
        Serial.println(F("Wrong EEPROM stored profile type, reverting to no profile."));
        #endif
        zeroProfile();
      }
      else {
        #ifdef IMPORTANT_LOG_SERIAL
        Serial.print(F("EEPROM read profile size: "));
        Serial.println(profile.profile_pid_length);
        #endif
        bluetooth_profile_received = 1;
        menu_manual_auto_position = 2;
      }
      break;
    default:
      #ifdef IMPORTANT_LOG_SERIAL
      Serial.println(F("Wrong EEPROM data version marker, reverting to no profile."));
      #endif
      zeroProfile();
      break;
  }
  eeprom_data_version = EEPROM_CURRENT_DATA_VERSION;
}

// profile following

#define AUTO_COOLING_MIN_TIME 140000 // 2:20
#define AUTO_COOLING_MAX_TIME 240000 // 4:00
#define AUTO_COOLING_STOP_TEMP 38.0

int profile_point_current = 0;

unsigned long temp_and_fan_recalc_last_time = 0;
int temp_and_fan_recalc_interval = 50;

void displayErrorAndHalt(char* error_str) {
  #ifdef DEBUG_LOG_SERIAL
  Serial.print(F("Fatal error: "));
  Serial.println(error_str);
  Serial.println(F("Halting."));
  #endif
  
  // not displaying anything as we currently don't have a display

  // this halts the arduino, the only way out is to reset it
  while (1) {
    delay(1000);
  }
}

Adafruit_MAX31856 thermo3 = Adafruit_MAX31856(53);

#define CALIBRATION_THERMOCOUPLE_1 0.0
#define CALIBRATION_THERMOCOUPLE_2 0.0

long thermocouple_last_read_time = 0;

volatile byte thermocouple_can_be_read_and_pid_can_be_recalc = 0;

int thermocouple_minimum_wait_time = 200;

int thermocouple_minimum_wait_time_idle = 400;
double thermocouple_last_read = 0;
double thermocouple_new_read = 0;

// relays (power)

#define RELAY1 3
#define RELAY1_GND 25

int relay1_on = 0;

void turn_relay1_on() {
  digitalWrite(RELAY1, 1);
  relay1_on = 1;
}

void turn_relay1_off() {
  digitalWrite(RELAY1, 0);
  relay1_on = 0;
}

void relay1_switch() {
  if (relay1_on) turn_relay1_off();
  else turn_relay1_on();
}

int is_relay1_on() {
  return relay1_on;
}

// mosfet (fan)

#define MOSFET1 4

void mosfet1_control(int pot) {
  if (pot == 0) {
    analogWrite(MOSFET1, 0);
  }
  else {
    analogWrite(MOSFET1, 100 + map(pot, 0, 100, 0, 100));
  }
}

byte mosfet1_non_blocking_smooth_increase_to(int target_fan, long interval_msec) {
  static long last_increase_time = 0;

  if (millis() - last_increase_time > interval_msec) {
    if (fan + 5 <= target_fan) {
      last_increase_time = millis();
      fan += 5;
      if (fan > 100) fan = 100;
      mosfet1_control(fan);
      #ifdef DEBUG_LOG_SERIAL
      Serial.print(F("fan @ "));
      Serial.println(fan);
      #endif
    }
    else if (fan < target_fan) {
      last_increase_time = millis();
      fan = target_fan;
      if (fan > 100) fan = 100;
      mosfet1_control(fan);
      #ifdef DEBUG_LOG_SERIAL
      Serial.print(F("fan @ "));
      Serial.println(fan);
      #endif
    }
  }
  if (fan < target_fan) return 0;
  return 1;
}

byte mosfet1_non_blocking_smooth_decrease_to(int target_fan, long interval_msec) {
  static long last_decrease_time = 0;

  if (millis() - last_decrease_time > interval_msec) {
    if (fan - 5 >= target_fan) {
      last_decrease_time = millis();
      fan -= 5;
      if (fan < 0) fan = 0;
      mosfet1_control(fan);
      #ifdef DEBUG_LOG_SERIAL      
      Serial.print(F("fan @ "));
      Serial.println(fan);
      #endif
    }
    else if (fan > target_fan) {
      last_decrease_time = millis();
      fan = target_fan;
      if (fan < 0) fan = 0;
      mosfet1_control(fan);
      #ifdef DEBUG_LOG_SERIAL
      Serial.print(F("fan @ "));
      Serial.println(fan);
      #endif
    }
  }
  if (fan > target_fan) return 0;
  return 1;
}

// operation indication led

#ifdef ENABLE_LED_INDICATOR

#define LED1_R A1
#define LED1_G A2
#define LED1_B A3

void led1_color(int red, int green, int blue) {
  analogWrite(LED1_R, 255 - red);
  analogWrite(LED1_G, 255 - green);
  analogWrite(LED1_B, 255 - blue);
}
#endif

int getDestinationState(int state_from, int action) {
  if (state_from > LAST_STATE_NUMBER || state_from < 0) return STATE_STOPPED;

  if ((state_from == STATE_MENU_MANUAL_AUTO) && (action == ACTION_PROCCEED) && (menu_manual_auto_position == 1)) {
    return STATE_STOPPED;
  }

  if ((state_from == STATE_MENU_MANUAL_AUTO) && (action == ACTION_PROCCEED) && (menu_manual_auto_position == 2)) {
    return STATE_AUTO_ROASTING_PRE;
  }

  if (action == ACTION_PROCCEED) {
    return state_transitions_procceed[state_from];
  }
  else if (action == ACTION_ABORT) {
    return state_transitions_abort[state_from];
  }

  return STATE_STOPPED;
}

void enterState(int new_state) {
  switch (new_state) {
    case STATE_STOPPED:

      if (state == STATE_COOLING) {
        time_stop_cooling = millis();
      }

      global_power = 0;

      mosfet1_control(0);

      #ifdef ENABLE_LED_INDICATOR 
      led1_color(0, 255, 0);
      #endif

      state = STATE_STOPPED;

      break;
    case STATE_PREHEATING:
      
      // not doing anything
      
      break;
    case STATE_ROASTING:
      state = STATE_ROASTING;

      time_start = millis();
      time_stop = 0;
      time_stop_cooling = 0;

      clean_log();

      break;
    case STATE_COOLING:
      state = STATE_COOLING;

      #ifdef DEBUG_LOG_SERIAL
      Serial.println("Started cooling...");
      #endif

      global_power = 0;

      fan = 100;
      mosfet1_control(fan);

      time_stop = millis();

      break;
    case STATE_ABORTED_WAITING_DUMP:
      state = STATE_ABORTED_WAITING_DUMP;
      break;
    case STATE_MENU_MANUAL_AUTO:
      #ifdef ENABLE_LED_INDICATOR 
      led1_color(0, 255, 0);
      #endif

      state = STATE_MENU_MANUAL_AUTO;
      break;
    case STATE_MENU_SELECT_PROFILE:
      state = STATE_MENU_SELECT_PROFILE;
      break;
    case STATE_AUTO_ROASTING_PRE:
      global_power = 0;
      turn_relay1_off();
      
      profile_point_current = 0;
      
      already_initialized_pid = 0;
      
      clean_log();

      if (profile.profile_selected_file_type == PROFILE_TYPE_PID && (profile.profile_pid_temps[profile_point_current] > 0 && profile.profile_pid_fans[profile_point_current] == 0)) {
        global_power = 0;
  
        fan = 0;
        mosfet1_control(fan);
        
        displayErrorAndHalt("Error: if a profile point has temperature > 0 it should have fan > 0.");
      }
      
      fan = 1;
      mosfet1_control(fan);

      #ifdef ENABLE_LED_INDICATOR 
      led1_color(255, 0, 0);
      #endif

      state = STATE_AUTO_ROASTING_PRE;
      break;
    case STATE_AUTO_ROASTING:
      time_start = millis();
      time_stop = 0;
      time_stop_cooling = 0;

      global_power = 0;
      
      send_bluetooth_log_start();

      state = STATE_AUTO_ROASTING;
      break;
    case STATE_AUTO_COOLING:
      time_stop = millis();

      global_power = 0;
      turn_relay1_off();

      send_bluetooth_log_cooling();

      #ifdef ENABLE_LED_INDICATOR 
      led1_color(0, 0, 255);
      #endif

      state = STATE_AUTO_COOLING;
      break;
    case STATE_AUTO_COOLING_AFTER:
      
      global_power = 0;

      turn_relay1_off();

      send_bluetooth_log_end();

      state = STATE_AUTO_COOLING_AFTER;
      break;
    case STATE_AUTO_END:
      global_power = 0;

      turn_relay1_off();
            
      fan = 0;
      mosfet1_control(fan);

      state = STATE_AUTO_END;
      break;
    case STATE_AUTO_COMPUTER_CONTROLLED_PRE:
      state = STATE_AUTO_COMPUTER_CONTROLLED_PRE;
      break;
    case STATE_AUTO_COMPUTER_CONTROLLED:
      state = STATE_AUTO_COMPUTER_CONTROLLED;
      break;
    case STATE_BLUETOOTH_PROFILE_RECEIVE:
      state = STATE_BLUETOOTH_PROFILE_RECEIVE;
      break;
    default:
      break;
  }
}

// logging

// the log is kept in full in memory, but is sent one line at a time during the roasting.

#define MAX_ROAST_LOG_MINUTES 20
byte roast_log_temps[60 * MAX_ROAST_LOG_MINUTES];
byte roast_log_temps_decimal_part[60 * MAX_ROAST_LOG_MINUTES];
unsigned long last_log_position = 0;
byte zero_log_position_processed = 0;

void clean_log() {
  long i;
  for (i = 0; i < (MAX_ROAST_LOG_MINUTES * 60); i++) {
    roast_log_temps[i] = 0;
    roast_log_temps_decimal_part[i] = 0;
  }
  last_log_position = 0;
  zero_log_position_processed = 0;
}

// artisan integration

void check_and_answer_artisan_serial(float t1, float t2 = 0.0) {
  int value_int;

  if (Serial3.available() > 0) {
    String command = Serial3.readStringUntil('\n');
    if (command == "READ") {
      if (t2 > 0.0) Serial3.println("0," + String(t2) + "," + String(t1));
      else Serial3.println("0,0," + String(t1));
      Serial3.flush();
    }
    else if (command == "CHAN;1200") {
      Serial3.println("# CHAN;1200");
      Serial3.flush();
    }
    else if (command == "UNITS,C") {
    }
    else if (command == "UNITS,F") {
    }
    else {
      String cmd = String(command);
      cmd.toLowerCase();
      int indx = cmd.indexOf(',');
      String key = cmd.substring(0, indx);
      key.toLowerCase();
      String value = cmd.substring(indx + 1, cmd.length());
      char buf[value.length() + 1];
      value.toCharArray(buf, value.length() + 1);
      value_int = atoi(buf);

      if (key == "ot1") {
        global_power = value_int;
        if (global_power < 0) global_power = 0;
        if (global_power > 100) global_power = 100;
      }
      else if (key == "ot2") {
        fan = value_int;
        if (fan < 0) fan = 0;
        if (fan > 100) fan = 100;

        mosfet1_control(fan);
      }
    }
    Serial3.flush();

  }
}

// bluetooth integration

char bluetooth_command_string[80];
byte bluetooth_command_position = 0;

int bluetooth_profile_current_line = 0;

int bluetooth_profile_point = 0;
int bluetooth_profile_fan_point = 0;

long bluetooth_read_pid_time = 0;
long bluetooth_read_pid_time_last = 0;
int bluetooth_read_pid_temp = 0;
int bluetooth_read_pid_temp_last = 0;
int bluetooth_read_pid_fan = 0;
int bluetooth_read_pid_fan_last = 0;

void react_to_bluetooth_command() {

  String error;
  String command = String(bluetooth_command_string);

  #ifdef DEBUG_LOG_SERIAL
  Serial.print("Receiving bluetooth command: '");
  Serial.print(command);
  Serial.println("'");
  #endif
  
  if (command.equals("start_receive_profile")) {
    if (IS_NOT_A_ROASTING_STATE(state)) {
      zeroProfile();
      bluetooth_profile_current_line = 0;
      bluetooth_profile_point = 0;
      bluetooth_profile_received = 0;
      
      bluetooth_read_pid_time_last = 0;
      bluetooth_read_pid_temp_last = 0;
      bluetooth_read_pid_fan_last = 0;
      
      profile.profile_selected_file_type = PROFILE_TYPE_PID;
      enterState(STATE_BLUETOOTH_PROFILE_RECEIVE);
      return;
    }
  }

  if (state == STATE_BLUETOOTH_PROFILE_RECEIVE) {

    if (command.startsWith("uuid")) {
      strncpy(profile.profile_uuid, bluetooth_command_string + 4, 36);
    }
    
    else if (command.equals("end_receive_profile")) {
      if (bluetooth_profile_point <= 1) {
        error = String(F("Error: a profile needs more than one point."));
        displayErrorAndHalt(error.c_str());
      }

      profile.profile_pid_times[bluetooth_profile_point] = bluetooth_read_pid_time_last + 1000;
      profile.profile_pid_temps[bluetooth_profile_point] = 0;
      profile.profile_pid_fans[bluetooth_profile_point] = 100;
      profile.profile_pid_length = (byte) bluetooth_profile_point;

      bluetooth_profile_received = 1;
      
      #ifdef DEBUG_LOG_SERIAL
      Serial.print(F("Received profile size: "));
      Serial.println(profile.profile_pid_length);
      Serial.println(F("Saving profile..."));
      #endif
      
      eepromSaveProfile();

      menu_manual_auto_position = 2;
      enterState(STATE_MENU_MANUAL_AUTO);

      return;
    }
    else {
      if (sscanf(command.c_str(), "%ld,%d,%d", &bluetooth_read_pid_time, &bluetooth_read_pid_temp, &bluetooth_read_pid_fan) < 2) {
        error = String(F("Error: cannot parse profile line: "));
        error.concat(bluetooth_profile_point + 1);
        displayErrorAndHalt(error.c_str());
      }

      if (bluetooth_read_pid_time < 0 || bluetooth_read_pid_time > PROFILE_PID_MAX_TIME || bluetooth_read_pid_temp < 0 || bluetooth_read_pid_temp > PROFILE_PID_TEMP_MAX || bluetooth_read_pid_fan < 0 || bluetooth_read_pid_fan > 100) {
        error = String(F("Error: Time, temperature or fan with invalid values on line "));
        error.concat(bluetooth_profile_point + 1);
        error.concat(": ");
        error.concat(String(bluetooth_read_pid_time));
        error.concat(",");
        error.concat(String(bluetooth_read_pid_temp));
        error.concat(",");
        error.concat(String(bluetooth_read_pid_fan));
        displayErrorAndHalt(error.c_str());
      }

      if (bluetooth_read_pid_time < bluetooth_read_pid_time_last) {
        error = String(F("Error: Time decreases on line "));
        error.concat(bluetooth_profile_point + 1);
        displayErrorAndHalt(error.c_str());
      }

      profile.profile_pid_times[bluetooth_profile_point] = bluetooth_read_pid_time;
      profile.profile_pid_temps[bluetooth_profile_point] = bluetooth_read_pid_temp;
      profile.profile_pid_fans[bluetooth_profile_point] = bluetooth_read_pid_fan;

      bluetooth_read_pid_time_last = bluetooth_read_pid_time;
      bluetooth_read_pid_temp_last = bluetooth_read_pid_temp;
      bluetooth_read_pid_fan_last = bluetooth_read_pid_fan;

      bluetooth_profile_point++;

      return;
    }
  }

  if (command.equals("command_start_stop")) {
    if ((state == STATE_MENU_MANUAL_AUTO) && (menu_manual_auto_position == 2)) {
      bluetooth_profile_selected = 1;
    }
    else if (state == STATE_MENU_MANUAL_AUTO) {
      bluetooth_profile_selected = 0;
    }
    else if (state == STATE_BLUETOOTH_PROFILE_RECEIVE) {
      menu_manual_auto_position = 0;
    }
    int new_state = getDestinationState(state, ACTION_PROCCEED);
    enterState(new_state);
  }

  if (command.equals("command_erase_profile")) {
    zeroProfile();
    eepromSaveProfile();
  }
}

void receive_bluetooth_command_and_possibly_react() {
  while (Serial2.available()) {

    byte incomingByte = Serial2.read();

    if (incomingByte == 13) {
      if (bluetooth_command_position > 0) {
        bluetooth_command_string[bluetooth_command_position] = '\0';

        react_to_bluetooth_command();

        Serial2.println("ack");
        Serial2.flush();

        bluetooth_command_position = 0;
      }
    }
    else if (incomingByte == 10) {
      // do nothing
    }
    else if ((incomingByte >= 32) && (incomingByte <= 126)) {
      bluetooth_command_string[bluetooth_command_position++] = incomingByte;
    }

  }
}

int send_bluetooth_log_last_send_index = -1;

unsigned long send_bluetooth_profile_uuid_last_time = 0;
unsigned long send_bluetooth_temperature_last_time = 0;
unsigned long send_bluetooth_cooling_temperature_last_time = 0;

void broadcast_bluetooth_uuid_now() {
  send_bluetooth_profile_uuid();
}

void broadcast_bluetooth_uuid() {
  if (millis() > send_bluetooth_profile_uuid_last_time + 2000) {
    send_bluetooth_profile_uuid();
    send_bluetooth_profile_uuid_last_time = millis();  
  }
}

void send_bluetooth_profile_uuid() {
  Serial2.print("uuid");
  Serial2.println(profile.profile_uuid);
  Serial2.flush();

  #ifdef DEBUG_LOG_SERIAL
  Serial.print("uuid");
  Serial.println(profile.profile_uuid);
  Serial.flush();
  #endif
}

void send_bluetooth_log_start() {
  Serial2.println("log_start");
  Serial2.flush();
  Serial2.print("uuid");
  Serial2.println(profile.profile_uuid);  
  Serial2.flush();  
}

void send_bluetooth_log_cooling() {
  Serial2.println("log_cooling");
  Serial2.flush();
}

void send_bluetooth_log_end() {
  if (Serial2.availableForWrite() >= 8) {
    Serial2.println("log_end");
    Serial2.flush();
  }
}

void send_bluetooth_current_temperature(float current_temperature) {
  Serial2.print("0,");
  Serial2.print(current_temperature);
  Serial2.println(",0,0,0,0");
  Serial2.flush();
}

void send_bluetooth_log_point(int j, float target_temp, float target_fan) {
  long tj = ((long) j) * 1000;

  Serial2.print(tj);
  Serial2.print(",");
  Serial2.print(roast_log_temps[j]);
  Serial2.print(".");
  if (roast_log_temps_decimal_part[j] < 10) Serial2.print("0");
  Serial2.print(roast_log_temps_decimal_part[j]);
  
  Serial2.print(",");
  Serial2.print(global_power);
  Serial2.print(",");
  Serial2.print(fan);
  
  Serial2.print(",");
  Serial2.print(target_temp);
  Serial2.print(",");
  Serial2.println(target_fan);

  Serial2.flush();

  #ifdef DEBUG_LOG_SERIAL
  Serial.print(tj);
  Serial.print(",");
  Serial.print(roast_log_temps[j]);
  Serial.print(".");
  if (roast_log_temps_decimal_part[j] < 10) Serial.print("0");
  Serial.print(roast_log_temps_decimal_part[j]);
  Serial.print(",");
  Serial.print(global_power);
  Serial.print(",");
  Serial.print(fan);
  Serial.print(",");
  Serial.print(target_temp);
  Serial.print(",");
  Serial.println(target_fan);
  Serial.flush();
  #endif
}

void send_bluetooth_cooling_current_temperature(int j, float current_temperature) {
  long tj = ((long) j) * 1000;
  Serial2.print(tj);
  Serial2.print(",");
  Serial2.print(current_temperature);
  Serial2.println(",0,0,0,0");
  Serial2.flush();
}

volatile unsigned long time_relay_cycle_started = 0;

void update_relay1_state() {
  if (state != STATE_ROASTING && state != STATE_AUTO_ROASTING && state != STATE_AUTO_COMPUTER_CONTROLLED) {
    turn_relay1_off();
    return;
  }

  // rounding the edges of power to avoid opening and closing too fast to the SSR
  int local_global_power = global_power;
  if (local_global_power >= 97) local_global_power = 100;
  if (local_global_power >= 1 && local_global_power <= 3) local_global_power = 3;
  if (local_global_power <= 0) local_global_power = 0;

  unsigned long local_millis = millis();

  if ((local_millis - time_relay_cycle_started) >= 200) {
    time_relay_cycle_started = local_millis;
    // this is a global that will signal to the next loop iteration
    thermocouple_can_be_read_and_pid_can_be_recalc = 1;
  }
    
  if (fan > 25) {
    if ((local_global_power == 0) || ((local_millis - time_relay_cycle_started) >= ((200.0 / 100) * local_global_power))) {
      turn_relay1_off();
    }
    else {
      turn_relay1_on();
    }
  }
  else {
    turn_relay1_off();
  }
}

#ifdef ENABLE_PHYSICAL_START_STOP_BUTTON
long int button_reading_interval = 50;
long int last_button_read = millis();
byte last_button_state = 0;

void react_to_button_press() {
  if ((state == STATE_MENU_MANUAL_AUTO) && (menu_manual_auto_position == 2)) {
    bluetooth_profile_selected = 1;
  }
  else if (state == STATE_MENU_MANUAL_AUTO) {
    bluetooth_profile_selected = 0;
  }
  else if (state == STATE_BLUETOOTH_PROFILE_RECEIVE) {
    menu_manual_auto_position = 0;
  }
  int new_state = getDestinationState(state, ACTION_PROCCEED);
  enterState(new_state);  
}

void check_button_press_and_possibly_react() {
  if (millis() - last_button_read > button_reading_interval) {
    int button_read = digitalRead(22);
    last_button_read = millis();

    if (last_button_state == 0 && button_read == LOW) {
      #ifdef DEBUG_LOG_SERIAL
      Serial.println("*** button press!");
      #endif

      react_to_button_press();
    }
    
    last_button_state = (button_read == LOW ? 1 : 0);
  }
}
#endif

void setup()   {
  state = STATE_MENU_MANUAL_AUTO;

  // bluetooth
  pinMode(25, OUTPUT);
  digitalWrite(25, LOW);

  #ifdef ENABLE_PHYSICAL_START_STOP_BUTTON
  pinMode(22, INPUT_PULLUP);
  #endif
    
  // extra vcc, gnds
  pinMode(A10, OUTPUT);
  pinMode(A11, OUTPUT);
  pinMode(A12, OUTPUT);
  pinMode(A13, OUTPUT);
  digitalWrite(A10, HIGH);
  digitalWrite(A11, HIGH);
  digitalWrite(A12, LOW);
  digitalWrite(A13, LOW);
  
  thermo3.begin();
  // wait for the first measurement to settle
  delay(260);

  // arduino serial monitor
  Serial.begin(115200);

  global_power = 0;

  pinMode(RELAY1, OUTPUT);
  pinMode(RELAY1_GND, OUTPUT);
  digitalWrite(RELAY1_GND, LOW);
  digitalWrite(RELAY1, 0);

  Timer3.initialize(2000); // 2000 = 2ms, 100 runs in 200ms
  Timer3.attachInterrupt(update_relay1_state);

  TCCR0B = TCCR0B & B11111000 | B00000011; // changing pwm frequency on pin D4

  #ifdef ENABLE_LED_INDICATOR
  pinMode(LED1_R, OUTPUT);
  pinMode(LED1_G, OUTPUT);
  pinMode(LED1_B, OUTPUT);

  // led starts on green
  led1_color(0, 255, 0);
  #endif

  pinMode(MOSFET1, OUTPUT);

  max31856_thermocoupletype_t type = thermo3.getThermocoupleType();

  #ifdef IMPORTANT_LOG_SERIAL
  Serial.print(F("Thermocouple type: "));
  Serial.println(type);
  Serial.println("Model: ChaffChief");
  Serial.print("Version: ");
  Serial.println(FIRMWARE_VERSION);
  #endif

  thermocouple_last_read = thermo3.readThermocoupleTemperature(false) + (CALIBRATION_THERMOCOUPLE_1);
  thermocouple_last_read_time = millis();

  global_power = 0;

  #ifdef IMPORTANT_LOG_SERIAL
  Serial.println(F("Booting..."));
  #endif

  // bluetooth
  Serial2.begin(9600);

  // artisan
  Serial3.begin(115200);

  zeroProfile();
  eepromLoadProfile();

  #ifdef IMPORTANT_LOG_SERIAL
  int cr1 = thermo3.getCR1();
  Serial.print("MAX CR1 reg: ");
  Serial.println(cr1);
  #endif
}

int pid_command_received = 0;
double pid_command_p = 0;
double pid_command_i = 0;
double pid_command_d = 0;
byte pid_is_dirty = 1;

void loop() {
  double local_temp1;
  double local_temp2;
  double local_fan;
  double m;
  double m_fan;

  receive_bluetooth_command_and_possibly_react();
  
  #ifdef ENABLE_PHYSICAL_START_STOP_BUTTON
  check_button_press_and_possibly_react();
  #endif

  if (state == STATE_MENU_MANUAL_AUTO) {

    broadcast_bluetooth_uuid();

    local_temp1 = thermocouple_last_read;
    if (millis() - thermocouple_last_read_time >= thermocouple_minimum_wait_time) {
      noInterrupts();
      thermocouple_last_read = thermo3.readThermocoupleTemperature(false) + (CALIBRATION_THERMOCOUPLE_1);
      interrupts();
      thermocouple_last_read_time = millis();
      local_temp1 = thermocouple_last_read;

      #ifdef DEBUG_LOG_SERIAL
      Serial.print(thermocouple_last_read_time);
      Serial.print(",");
      Serial.println(thermocouple_last_read);
      #endif
    }
    if (millis() > send_bluetooth_temperature_last_time + 1000) {
      send_bluetooth_current_temperature(local_temp1);
      send_bluetooth_temperature_last_time = millis();
    }    
  }
  else if (state == STATE_MENU_SELECT_PROFILE) {

    enterState(STATE_MENU_MANUAL_AUTO);

  }
  else if (state == STATE_AUTO_ROASTING_PRE) {

    send_bluetooth_log_last_send_index = -1;

    if (already_initialized_pid == 0) {
      if (profile.profile_selected_file_type == PROFILE_TYPE_PID) {
        pidInput = 0;
        pidSetpoint = 0;  
        pid_is_dirty = 1;
        pidOutput = 0;
        // this is another dummy parameters initialization
        heaterPid = PID{&pidInput, &pidOutput, &pidSetpoint, 5.0, 0.0, 0.0, DIRECT};
        heaterPid.SetOutputLimits(0, 100);
        heaterPid.SetMode(AUTOMATIC);        
        heaterPid.SetSampleTime(100);
      }
      already_initialized_pid = 1;
    }

    int target_fan = 0;

    if (profile.profile_selected_file_type == PROFILE_TYPE_PID) {
      target_fan = profile.profile_pid_fans[0];
    }

    mosfet1_non_blocking_smooth_increase_to(target_fan, 300);

    if (fan == target_fan) {
      enterState(STATE_AUTO_ROASTING);
    }
  }
  else if (state == STATE_AUTO_ROASTING) {
    noInterrupts();
    local_temp1 = thermocouple_last_read;
    interrupts();

    byte local_thermocouple_can_be_read_and_pid_can_be_recalc = 0;
    byte should_read_thermocouple_in_this_pass = 0;
    byte did_read_thermocouple_in_this_pass = 0;
    
    noInterrupts();
    local_thermocouple_can_be_read_and_pid_can_be_recalc = thermocouple_can_be_read_and_pid_can_be_recalc;
    interrupts();

    should_read_thermocouple_in_this_pass = (local_thermocouple_can_be_read_and_pid_can_be_recalc && millis() - thermocouple_last_read_time >= thermocouple_minimum_wait_time);

    if (should_read_thermocouple_in_this_pass) {
      if (profile.profile_selected_file_type == PROFILE_TYPE_PID) {
        // the slope between the last profile time point and the current one
        m = ( ((double)profile.profile_pid_temps[profile_point_current + 1] - (double)profile.profile_pid_temps[profile_point_current]) /
              ((double)profile.profile_pid_times[profile_point_current + 1] - (double)profile.profile_pid_times[profile_point_current]) );
  
        if (((double)profile.profile_pid_times[profile_point_current + 1] - (double)profile.profile_pid_times[profile_point_current]) == 0) m = 0.0;
  
        // pid input will be line interpolated, or (m * (x - x1)) + y1 considering points (x1, y1) the last profile time,temp and (x2,y2) the next profile time, temp
        pidSetpoint = (double)((m * (double)((millis() - time_start) - profile.profile_pid_times[profile_point_current])) + (double)profile.profile_pid_temps[profile_point_current]);
      }
    }

    if (should_read_thermocouple_in_this_pass) {
      noInterrupts();
      thermocouple_last_read = thermo3.readThermocoupleTemperature(false) + (CALIBRATION_THERMOCOUPLE_1);
      interrupts();
      thermocouple_last_read_time = millis();
      did_read_thermocouple_in_this_pass = 1;
      noInterrupts();
      local_temp1 = thermocouple_last_read;
      interrupts();
    }
    
    if (profile.profile_selected_file_type == PROFILE_TYPE_PID) {
            
      pidInput = local_temp1;

      double local_P = 10.0;
      double local_I = 10.0;
      double local_D = 5.0;

      #ifdef CAN_UPDATE_PID_PARAMETERS_ON_THE_FLY
      int p1, p2, i1, i2, d1, d2;
      double read_p, read_i, read_d;
      while (Serial.available() > 0) {
        String command = Serial.readStringUntil('\n');        
        if (sscanf(command.c_str(), "%d.%d,%d.%d,%d.%d", &p1, &p2, &i1, &i2, &d1, &d2) >= 3) {
          pid_command_received = 1;
          read_p = (double)p1 + ((int)log10(p2) == 0 ? ((double)p2/10) : ((int)log10(p2) == 1 ? ((double)p2/100) : 0.0));
          read_i = (double)i1 + ((int)log10(i2) == 0 ? ((double)i2/10) : ((int)log10(i2) == 1 ? ((double)i2/100) : 0.0));
          read_d = (double)d1 + ((int)log10(d2) == 0 ? ((double)d2/10) : ((int)log10(d2) == 1 ? ((double)d2/100) : 0.0));
          pid_command_p = read_p;
          pid_command_i = read_i;
          pid_command_d = read_d;

          #ifdef IMPORTANT_LOG_SERIAL
          Serial.print("New pid settings received: ");
          Serial.print(read_p);
          Serial.print(",");
          Serial.print(read_i);
          Serial.print(",");
          Serial.println(read_d);
          #endif

          local_P = pid_command_p;
          local_I = pid_command_i;
          local_D = pid_command_d;

          pid_is_dirty = 1;
        }
        else {
          #ifdef IMPORTANT_LOG_SERIAL
          Serial.println(F("Error: could not parse the input (pid settings update)."));
          #endif
        }
      }
      #endif

      if (pid_is_dirty) {
        heaterPid.SetTunings(local_P, local_I, local_D);

        #ifdef IMPORTANT_LOG_SERIAL        
        Serial.print("Pid settings updated to: ");
        Serial.print(local_P);
        Serial.print(",");
        Serial.print(local_I);
        Serial.print(",");
        Serial.println(local_D);
        #endif
        
        pid_is_dirty = 0;
      }

      if (did_read_thermocouple_in_this_pass) {        
        heaterPid.Compute();
        thermocouple_can_be_read_and_pid_can_be_recalc = 0;

        int tempPower = round(pidOutput);      
        if (tempPower < 0) tempPower = 0;
        if (tempPower > 100) tempPower = 100;
        global_power = tempPower;

        #ifdef IMPORTANT_LOG_SERIAL
        Serial.print("Global power set: ");
        Serial.println(global_power);
        #endif
      }

      if (did_read_thermocouple_in_this_pass) {
        if (profile.profile_selected_file_type == PROFILE_TYPE_PID) {
  
          m_fan = ( ((double)profile.profile_pid_fans[profile_point_current + 1] - (double)profile.profile_pid_fans[profile_point_current]) /
                    ((double)profile.profile_pid_times[profile_point_current + 1] - (double)profile.profile_pid_times[profile_point_current]) );
    
          if (((double)profile.profile_pid_times[profile_point_current + 1] - (double)profile.profile_pid_times[profile_point_current]) == 0) m_fan = 0.0;
    
          local_fan = (double)(
                        (m_fan * (double)((millis() - time_start) - profile.profile_pid_times[profile_point_current]))
                        + (double)profile.profile_pid_fans[profile_point_current]
                      );
                      
        }
        
        int fan_temp = (int) round(local_fan);
  
        if (fan_temp < 0) fan = 0;
        if (fan_temp > 100) fan = 100;
        fan = fan_temp;
  
        mosfet1_control(fan);

        temp_and_fan_recalc_last_time = millis();
      }
    }

    int log_position = (millis() - time_start) / 1000;
    if (log_position == 0 && !zero_log_position_processed) {
      roast_log_temps[log_position] = local_temp1;
      roast_log_temps_decimal_part[log_position] = ((int) (local_temp1 * 100)) % 100;
      zero_log_position_processed = 1;

      send_bluetooth_log_point(log_position, pidSetpoint, local_fan);        
    }
    if (log_position > last_log_position) {
      roast_log_temps[log_position] = local_temp1;
      roast_log_temps_decimal_part[log_position] = ((int) (local_temp1 * 100)) % 100;
      last_log_position = log_position;      

      send_bluetooth_log_point(log_position, pidSetpoint, local_fan);
    }

    static int last_uuid_position = 0;
    int uuid_position = (millis() + 500 - time_start) / 5000;
    // broadcast bluetooth profile id from time to time
    if (uuid_position > last_uuid_position) {
      broadcast_bluetooth_uuid_now();
      last_uuid_position = uuid_position;
    }
    
    // check if we are past a new profile point
    if (profile.profile_selected_file_type == PROFILE_TYPE_PID) {
      if ((millis() - time_start) >= profile.profile_pid_times[profile_point_current + 1]) {
        if (profile_point_current + 3 > profile.profile_pid_length) {
          // last point: stop, broadcast it and go to cooling
          int local_log_position = (millis() - time_start) / 1000;
          if (local_log_position > last_log_position) {
            send_bluetooth_log_point(local_log_position, pidSetpoint, local_fan);
            last_log_position = local_log_position;
          }
          enterState(getDestinationState(state, ACTION_PROCCEED));
          return;
        }
        else {
          profile_point_current++;
        }
      }   
    }
  }
  else if (state == STATE_AUTO_COOLING) {

    mosfet1_non_blocking_smooth_increase_to(100, 100);

    broadcast_bluetooth_uuid();

    local_temp1 = thermocouple_last_read;
    if (millis() - thermocouple_last_read_time >= thermocouple_minimum_wait_time_idle) {
      
      noInterrupts();
      thermocouple_last_read = thermo3.readThermocoupleTemperature(false) + (CALIBRATION_THERMOCOUPLE_1);
      interrupts();
      
      thermocouple_last_read_time = millis();
            
      local_temp1 = thermocouple_last_read;
            
      #ifdef DEBUG_LOG_SERIAL
      Serial.print("Cooling: ");
      Serial.println(local_temp1);
      #endif
    }

    int log_position = (millis() - time_start) / 1000;
    if (log_position > last_log_position) {
      last_log_position = log_position;
      send_bluetooth_cooling_current_temperature(log_position, local_temp1); 
      send_bluetooth_cooling_temperature_last_time = millis();
    }

    if (((millis() - time_stop) > AUTO_COOLING_MAX_TIME) || (((millis() - time_stop) >= AUTO_COOLING_MIN_TIME) && (local_temp1 <= AUTO_COOLING_STOP_TEMP))) {
      enterState(getDestinationState(state, ACTION_PROCCEED));
    }
  }
  else if (state == STATE_AUTO_COOLING_AFTER) {
    mosfet1_non_blocking_smooth_decrease_to(0, 50);

    if (fan == 0) {
      enterState(STATE_AUTO_END);      
    }
  }
  else if (state == STATE_AUTO_END) {

    broadcast_bluetooth_uuid();

    #ifdef IMPORTANT_LOG_SERIAL
    Serial.println("*** Roasting ended.");
    #endif

    enterState(STATE_MENU_MANUAL_AUTO);
  }
  else if (state == STATE_BLUETOOTH_PROFILE_RECEIVE) {
    
    // not doing anything here, bluetooth is managed in the loop first level

  }
  else {
    // here:
    //   STATE_STOPPED (currently only using this one)
    //   STATE_COOLING
    //   STATE_PREHEATING
    //   STATE_ROASTING
    
    mosfet1_control(fan);

    local_temp1 = thermocouple_last_read;
    if (millis() - thermocouple_last_read_time >= thermocouple_minimum_wait_time) {
      noInterrupts();
      thermocouple_last_read = thermo3.readThermocoupleTemperature(false) + (CALIBRATION_THERMOCOUPLE_1);
      interrupts();
      thermocouple_last_read_time = millis();
      local_temp1 = thermocouple_last_read;
      local_temp2 = thermo3.readCJTemperature();      
    }

    check_and_answer_artisan_serial(local_temp1);

    if (millis() > send_bluetooth_temperature_last_time + 1000) {
      send_bluetooth_current_temperature(local_temp1);
      send_bluetooth_temperature_last_time = millis();
    }
  }

}
