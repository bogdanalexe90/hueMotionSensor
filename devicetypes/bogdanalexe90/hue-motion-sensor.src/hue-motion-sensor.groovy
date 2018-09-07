/**
 *  Hue Motion Sensor
 *
 *  Copyright 2018 Bogdan Alexe
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
import physicalgraph.zigbee.zcl.DataType
 
 
metadata {
    definition (name: "Hue Motion Sensor", namespace: "bogdanalexe90", author: "Bogdan Alexe") {
		capability "Motion Sensor"
		capability "Configuration"
		capability "Battery"
		capability "Temperature Measurement"
        capability "Illuminance Measurement"
		capability "Refresh"
		capability "Health Check"
		capability "Sensor"
        
        command "enrollResponse"

		fingerprint profileId: "0104", inClusters: "0000,0001,0003,0406,0400,0402", outClusters: "0019", manufacturer: "Philips", model: "SML001", deviceJoinName: "Hue Motion Sensor"
	}
    
    preferences {
		section {
			input title: "Temperature Offset", description: "This feature allows you to correct any temperature variations by selecting an offset. Ex: If your sensor consistently reports a temp that's 5 degrees too warm, you'd enter '-5'. If 3 degrees too cold, enter '+3'.", displayDuringSetup: false, type: "paragraph", element: "paragraph"
			input "tempOffset", "decimal", title: "Degrees", description: "Adjust temperature by this many degrees", range: "*..*", displayDuringSetup: false
		}
        
        section {
			input title: "illuminance Offset", description: "This feature allows you to correct any illuminance variations by selecting an offset. Ex: If your sensor consistently reports a illuminance that's 5lux too high, you'd enter '-5'. If 3lux too low, enter '+3'.", displayDuringSetup: false, type: "paragraph", element: "paragraph"
			input "luxOffset", "number", title: "Lux", description: "Adjust illuminance by this many lux", range: "*..*", displayDuringSetup: false
		}
	}

	tiles(scale: 2) {
		multiAttributeTile(name: "motion", type: "generic", width: 6, height: 4) {
			tileAttribute("device.motion", key: "PRIMARY_CONTROL") {
				attributeState "active", label: 'motion', icon: "st.motion.motion.active", backgroundColor: "#00A0DC"
				attributeState "inactive", label: 'no motion', icon: "st.motion.motion.inactive", backgroundColor: "#cccccc"
			}
		}
        
        valueTile("temperature", "device.temperature", width: 2, height: 2) {
			state("temperature", label: '${currentValue}°', unit: 'dF',
					backgroundColors: [
							[value: 31, color: "#153591"],
							[value: 44, color: "#1e9cbb"],
							[value: 59, color: "#90d2a7"],
							[value: 74, color: "#44b621"],
							[value: 84, color: "#f1d801"],
							[value: 95, color: "#d04e00"],
							[value: 96, color: "#bc2323"]
					]
			)
		}
       
        valueTile("illuminance", "device.illuminance", inactiveLabel: false, width: 2, height: 2) {
			state "luminosity", label:'${currentValue} ${unit}', unit:"lux"
		}
        
        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
			state "battery", label: '${currentValue}% battery', unit: ""
		}
        
		standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", action: "refresh.refresh", icon: "st.secondary.refresh"
		}

		main(["motion", "temperature", "illuminance"])
        details(["motion", "temperature", "illuminance", "battery", "refresh"])
	}
}


private Map getBatteryResultEvent(BigDecimal newVolts) {
	if (newVolts == 0 || newVolts == 255) {
    	return [:]
    }
    
    BigDecimal minVolts = 2.1
    BigDecimal maxVolts = 3.0
    String currDeviceBatteryPercent = device.currentState("battery")?.value
    BigDecimal currBatteryPercent = new BigDecimal(currDeviceBatteryPercent ?: "100")
    
    Map batteryEventResult = [
    	name: "battery",
        value: currBatteryPercent,
        descriptionText: "{{ device.displayName }} battery was {{ value }}",
        unit: "%",
        translatable: true
    ]

	// Find the corresponding voltage from our range
    BigDecimal currVolts = (currBatteryPercent  / 100) * (maxVolts - minVolts) + minVolts
    currVolts = currVolts.setScale(1, BigDecimal.ROUND_HALF_UP)
    
    // Only update the battery reading if we don't have a last reading,
    // OR we have received the same reading twice in a row
    // OR we don't currently have a battery reading
    // OR the value we just received is at least 2 steps off from the last reported value
    if (state?.lastVolts == null || state?.lastVolts == newVolts || currDeviceBatteryPercent == null || Math.abs(currVolts - newVolts) > 0.1) {
        def newBatteryPercent = ((newVolts - minVolts) / (maxVolts - minVolts)) * 100
        newBatteryPercent = newBatteryPercent > 0 ? newBatteryPercent.setScale(1, BigDecimal.ROUND_HALF_UP) : 1
        batteryEventResult.value = newBatteryPercent.min(100)
        
        log.debug "Updating battery value: $batteryEventResult.value"
    }

    state.lastVolts = newVolts

	return createEvent(batteryEventResult)
}

private Map getTemperatureResultEvent(BigDecimal newTemp) {
    // Convert C to F if needed
	newTemp = getTemperatureScale() == 'C' ? newTemp : (newTemp * 1.8 + 32)

	// Apply configured offset
	if (tempOffset) {
    	newTemp = newTemp + (tempOffset as BigDecimal)
    }
    
    newTemp = newTemp.setScale(1, BigDecimal.ROUND_HALF_UP)
	log.debug "Updating temperature value: $newTemp"
	
    return createEvent([
        name: "temperature",
        value: newTemp,
        descriptionText: "{{ device.displayName }} was {{ value} }°",
        unit: getTemperatureScale(),
        translatable: true
	])
}

private Map getLuminanceResultEvent(Integer newIlluminance) {
	newIlluminance = zigbee.lux(newIlluminance)
    
    // Apply configured offset
    if (luxOffset) {
    	newIlluminance = newIlluminance + (luxOffset as Integer)
    }

	log.debug "Updating illuminance value: $newIlluminance"
    
	return createEvent([
        name: "illuminance",
        value: newIlluminance,
        descriptionText: "{{ device.displayName }} was {{ value }}",
        unit: "lux",
        translatable: true
	])
}

private Map getMotionResultEvent(String newMotionAction) {
    log.debug "Updating motion value: $newMotionAction"

	return createEvent([
        name: "motion",
        value: newMotionAction,
        descriptionText: newMotionAction == "active" ? "{{ device.displayName }} detected motion" : "{{ device.displayName }} motion has stopped",
        translatable: true
	])
}

private sendCheckIntervalEvent(Long value) {
	sendEvent(
    	name: "checkInterval",
        value: value, 
        displayed: false,
        data: [
        	protocol: "zigbee",
        	hubHardwareId: device.hub.hardwareID, 
        	offlinePingable: "1"
        ]
    )
}


def parse(String description) {
    log.info "Parse description: $description"
    
	if (description?.startsWith("temperature")) {
    	return getTemperatureResultEvent((description - "temperature: ") as BigDecimal)
    }
    
    if (description?.startsWith("illuminance")) {
    	return getLuminanceResultEvent((description - "illuminance: ") as Integer)
    }
  
    Map descMap = zigbee.parseDescriptionAsMap(description)
  
    switch (descMap?.clusterInt){
    	// Battery event
    	case zigbee.POWER_CONFIGURATION_CLUSTER:
        	if (descMap.commandInt != 0x07 && descMap.value) {
        		return getBatteryResultEvent(zigbee.convertHexToInt(descMap.value) / 10)
            }
        break
        
        // Temperature event
        case zigbee.TEMPERATURE_MEASUREMENT_CLUSTER:
        	if (descMap.commandInt == 0x07) {
            	if (descMap.data[0] == "00") {
					log.debug "Temperature reporting config response: $descMap"
                    sendCheckIntervalEvent(60 * 12)
				} else {
					log.warn "Temperature reporting config failed - error code: ${descMap.data[0]}"
				}
            } else if (descMap.value) { 
            	return getTemperatureResultEvent(zigbee.convertHexToInt(descMap.value) / 100)
            }
        break
        
        // Illuminance event
        case 0x0400:
        	if (descMap.commandInt == 0x07) {
            	if (descMap.data[0] == "00") {
					log.debug "Illuminance reporting config response: $descMap"
					sendCheckIntervalEvent(60 * 12)
				} else {
					log.warn "Illuminance reporting config failed - error code: ${descMap.data[0]}"
				}
            } else {
            	return getLuminanceResultEvent(descMap.value ? zigbee.convertHexToInt(descMap.value) : 0)
            }         
        break;
        
        // Motion event
        case 0x0406:
        	if (descMap.attrInt == 0x0000) {
				return getMotionResultEvent(descMap.value?.endsWith("01") ? "active" : "inactive")
            }       
        break;
    }
    
    return [:]
}

def refresh() {
	log.info "### Refresh"
	def refreshCmds = []

	refreshCmds += zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0020) +
    	zigbee.readAttribute(zigbee.TEMPERATURE_MEASUREMENT_CLUSTER, 0x0000) +
        zigbee.readAttribute(0x0400, 0x0000) +
        zigbee.readAttribute(0x0406, 0x0000) +
		zigbee.enrollResponse()

	return refreshCmds
}

def configure() {
	log.info "### Configure"
	// Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
	sendCheckIntervalEvent(2 * 60 * 60 + 1 * 60)

	def configCmds = []

	// reporting interval if no activity
	// temperature - minReportTime 10 seconds, maxReportTime 5 min
    // illuminance - minReportTime 5 seconds, maxReportTime 5 min
	// battery - minReport 30 seconds, maxReportTime 6 hrs by default    
	configCmds += zigbee.temperatureConfig(30, 300)
    configCmds += zigbee.configureReporting(0x0400, 0x0000, DataType.UINT16, 5, 300, 100)
    configCmds += zigbee.batteryConfig()

	return refresh() + configCmds + refresh() // send refresh cmd as part of config
}

def ping() {
	log.info "### Ping"
    return zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, 0x0020)
}
