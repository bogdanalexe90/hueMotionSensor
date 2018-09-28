## Hue motion sensor (for SmartThings integration, no Hue bridge)

**Motion:**
Very snappy, works better than original smartthings motion sensor (even if the DTH runs in the cloud) having no delay or sleep time for reporting the motion events.

Also you can configure the following:
* Motion duration in sec (duration between active and inactive events). Default is 10 sec (comming from the sensor)
* Motion sensitivity (Low, Medium, High). Default is High

**Temperature:**
It's accurate and reported in time. You can also correct the readings by configuring an offset.

**Illuminance:**
It's accurate and reported in time. You can also correct the readings by configuring an offset.

**Battery:**
Used the same logic as the smartthings motion sensor. Seems accurate.

# Installation
* Create a new custom DTH from code or by integrating directly with the Github repo.
* Reset your Hue motion sensor and put it in the pairing mode.
* Open your Smartthings app and search for a new device.
