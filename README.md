## Hue motion sensor (for SmartThings integration, no Hue bridge)

**Motion:**
Very snappy, works better than original smartthings motion sensor (even if the DTH runs in the cloud) having no delay or sleep time for reporting the motion events.

**Temperature:**
It's accurate and reported in time.

**Illuminance:**
Since the sensor reads a faulty value right after the motion is reported, any illuminance measurements right after a motion event (less than 1.5 sec) will be ignored.

**Battery:**
Used the same logic as the smartthings motion sensor. Seems accurate.

# Installation
* Create a new custom DTH from code or by integrating directly with the Github repo.
* Reset your Hue motion sensor and put it in the pairing mode.
* Open your Smartthings app and search for a new device.
