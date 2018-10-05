## Hue motion sensor (for SmartThings integration, no Hue bridge)

**Motion:**
Very snappy, works better than original smartthings motion sensor (even if the DTH runs in the cloud) having no delay or sleep time for reporting the motion events.

Also you can configure the following:
* Motion sensitivity (Low, Medium, High). Default is High

**Temperature:**
It's accurate and reported in time. You can also correct the readings by configuring an offset.

**Illuminance:**
It's accurate and reported in time. You can also correct the readings by configuring an offset.

**Battery:**
Used the same logic as the smartthings motion sensor. Seems accurate.

# Installation
* Create a new custom DTH from code or by integrating directly with the Github repo.
* Reset your Hue motion by pressing the setup button until device led starts to change color (~15 sec).
* Open your Smartthings app and search for a new device.
