# ChaffChief
Open source fluid bed coffee roaster firmware &amp; apps

## Introduction

ChaffChief is designed to be a full-featured firmware & apps package that provides the functionalities of a small fluid bed coffee roaster that executes users defined profiles with precision.

To use ChaffChief, you need a machine setup and an app.

It enables the machine to:
- Receive and run roasting profiles
- Remember the last used roasting profile even after power off

It has one app that enabled you to:
- Create, edit and store roasting profiles
- Send profiles to the roaster machine
- Follow the execution of a roast
- Keep the logs of the roast executions

ChaffChief is comprised of two solutions: a machine firmware and an app that communicates with the machine.

Currently the hardware needed is:
- Firmware
  - Arduino Mega 2560 rev3
  - Air popcorn popper
  - 20a+ Solid state relay with an heat dissipator
  - Mosfet with an heat dissipator compatible with the popcorn popper or an compatible DC motor driver
  - Arduino rated power supply
  - DC power supply rated at 12V (or greater, defined by your choice of popcorn popper)
  - HC-06 bluetooth arduino module
  - MAX31856 arduino module
  - Optional push button module
  - Optional rgb led module
  - Arduino jumper cables to make the arduino logic connections
  - Some robust electric copper wires to make the AC connections, some for the DC connections and a male power plug
  - An arduino usb cable to program the Arduino and debug the firmware if needed
  - A computer to program the Arduino
- App
  - A device running Android 6.0+

*As of 01 december 2020, the app isn't yet provided, only the firmware. The author is working on making it available as soon as possible.*

## Donating

Please consider donating to the author if you find this sofware or portions of it useful. A huge amount of time and resources were spent on the making of this software.

### PAYPAL
<form action="https://www.paypal.com/donate" method="post" target="_top">
<input type="hidden" name="cmd" value="_donations" />
<input type="hidden" name="business" value="NUHKNZCBCPCLQ" />
<input type="hidden" name="item_name" value="Doações para projetos de programação de código aberto" />
<input type="hidden" name="currency_code" value="BRL" />
<input type="image" src="https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif" border="0" name="submit" title="PayPal - The safer, easier way to pay online!" alt="Donate with PayPal button" />
<img alt="" border="0" src="https://www.paypal.com/en_BR/i/scr/pixel.gif" width="1" height="1" />
</form>

<img src="site/images/donation_paypal_qrcode.png" width="100">

![Donate](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=NUHKNZCBCPCLQ&item_name=Doa%C3%A7%C3%B5es+para+projetos+de+programa%C3%A7%C3%A3o+de+c%C3%B3digo+aberto&currency_code=BRL)

### PIX
<img src="site/images/donation_pix_qrcode.png" width="100">

## Hardware needed

## Profile exchange format

### Format

Profile contents changing parts are defined with `<field>` in thhe following model. All the communication is done with a ascii text-based protocol over the arduino serial interface. When using bluetooth, you should use the bluetooth serial model.

The lines on the communication are newline-terminated, meaning they end with an newline character (ascii 13).

A profile may be sent to the machine at any moment when it is on its stopped state. It is comprised of:

```
start_receive_profile
uuid<profile_uuid>
<line>
...
<line>
end_receive_profile
```

Currently a profile must have no more than 60 lines (or profile points, as a line represents a profile point) and at least 2 lines.

A `<line>` is comprised of:

```
<time>,<temperature>,<fan>
```

Where:
- `<time>` is the time on the roasting process where the roaster should be at this lines' desired temperature. The times are expressed on milliseconds: 3000 means at the second 3 since the beggining of the roast, 120000 means two minutes. The first line of the profile must use time 0, indicating it starts from this line. It should be an integer.
- `<temperature>` is the temperature desired on this lines' time on the roast, in Celsius. It is a good practice to start a profile on a temperature close to ambient temperature. It should be an integer.
- `<fan>` is the ventilation percentage (without the %, only the number) desired on this lines' time of the roast. It should be an integer. You should not use 0 for this value at any time, since it would stop the air flow and thus interrupting the roast. It's also not supported on the firmware. If a profile has a line with fan value 0, the roaster will refuse the profile.

### Examples

```
start_receive_profile
uuid0d4b28e2-845c-415a-8b87-888fd48eabe8
0,0,70
3000,28,70
120000,100,70
239000,167,70
299000,195,70
335000,204,70
370000,209,70
418000,212,70
end_receive_profile
```

On this example, the roast starts with temperature 0 and fan 70%. Quickly it catches to temperature 28 after 3 seconds. The profile finishing temperature is 212 degrees Celsius, at 6:58 (418 seconds means 6 minutes and 58 seconds).

## Firmware

## App

## Contributing

Feel free to contribute to this software. Any help on this regard will be really appreciated.

To contribute, you should clone this repository, make the desired changes and open a pull request to this repository.

As soon as possible, if needed, the author will contact you to review your pull request and discuss it.

As examples of how you can contribute, you may:

- Create a new app, for example, for a new architecture. The basic functionalities the app must have are: store profiles, edit them (the simples form may be text-based) and connect with the roaster via bluetooth and send any profile to the roaster. You may increase the functionalities later;
- Contribute on an existing app;
- Contribute on the firmware (for this some experience with running the project and Arduino is desired): adding functionalities or fixing bugs you may find;
- Example of a new functionality to the firmware: the possibility to have more than one profile on the machine at any time. Adding optional display modules. Adding new optional push buttons;
## Author & Licensing

This software is made originally by Roberto Buaiz.

Copyright 2020, Roberto Buaiz.

License: GPL v3 (see LICENSING.TXT, https://www.gnu.org/licenses/gpl-3.0.txt)