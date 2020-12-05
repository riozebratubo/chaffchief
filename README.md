# ChaffChief
Open source fluid bed coffee roaster firmware &amp; apps

![](https://img.shields.io/badge/License-GPLv3-green?style=flat-square)
![](https://img.shields.io/badge/Version-1.0-blue?style=flat-square)
![](https://img.shields.io/badge/Platforms-Arduino,%20Android-orange?style=flat-square)
## Introduction

ChaffChief is designed to be a full-featured firmware & apps package that provides the functionalities of a small fluid bed coffee roaster using an air popcorn popper, executing user-defined profiles with precision.

To use ChaffChief, you need a machine setup and an app.

It enables the machine to:
- Receive and run roasting profiles
- Remember the last used roasting profile even after power off

It has one app that enables you to:
- Create, edit and store roasting profiles
- Send profiles to the roaster machine
- Follow the execution of a roast
- Keep the logs of roast executions

ChaffChief is comprised of two solutions: a machine firmware and an app that communicates with the machine.

Currently the hardware needed is:
- Firmware: Arduino Mega + Components
- App: a device running Android 6.0+

## Hardware needed

Please check [Hardware needed](https://github.com/riozebratubo/chaffchief/wiki/Hardware-needed).

## Donating

Please consider donating to the author if you find this software or portions of it useful. A huge amount of time and resources were spent on the making of this software.

### PAYPAL

<img src="site/images/donation_paypal_qrcode.png" width="100">

[Donate link](https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=NUHKNZCBCPCLQ&item_name=Doa%C3%A7%C3%B5es+para+projetos+de+programa%C3%A7%C3%A3o+de+c%C3%B3digo+aberto&currency_code=BRL)

### PIX
<img src="site/images/donation_pix_qrcode.png" width="100">

## Profile type

The profile type ChaffChief currently uses is a set of profile points in time determining two things: at which temperature should the roaster be at the given time and what percentage of the ventilation it should use at this time.

Between two profile points, the roaster will use the intermediary values as if we're making an straight line on a graph of the temperatures in time. This is called linear interpolation. For example, if at time 0 we set a temperature of 20 degrees Celsius and at time 60 we set a temperature of 100 degrees, we only should have to define those two points, and the roaster will know it should start at 20 and be at 100 at 60s, meaning it will be at 60 at 30s and so on.

For more details, see the [profile exchange format](https://github.com/riozebratubo/chaffchief/wiki/Profile-exchange-format).

## Firmware

*(Still in writing)*
## App

*(Still in writing)*
## Contributing

Feel free to contribute to this software. Any help on this regard will be really appreciated.

To contribute, you should clone this repository, make the desired changes and open a pull request to this repository.

As soon as possible, if needed, the author will contact you to review your pull request and discuss it.

As examples of how you can contribute, you may:

- Create a new app, for example, for a new architecture. The basic functionalities the app must have are: store profiles, edit them (the simplest form may be text-based) and connect with the roaster via bluetooth and send any profile to the roaster. You may increase the functionalities later;
- Contribute on an existing app;
- Contribute on the firmware (for this some experience with running the project and Arduino is desired): adding functionalities or fixing bugs you may find;
- Example of a new functionality to the firmware: the possibility to have more than one profile on the machine at any time. Adding optional display modules. Adding new optional push buttons;
## Author & Licensing

This software is made originally by Roberto Buaiz.

Copyright 2020, Roberto Buaiz.

License: GPL v3 (see LICENSING.TXT, https://www.gnu.org/licenses/gpl-3.0.txt)