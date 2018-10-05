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
    definition (name: "Hue Motion Sensor", namespace: "bogdanalexe90", author: "Bogdan Alexe", , mnmn: "SmartThings", vid: "generic-motion", ocfDeviceType: "oic.r.sensor.motion") {
		capability "Motion Sensor"
		capability "Configuration"
		capability "Battery"
		capability "Temperature Measurement"
        capability "Illuminance Measurement"
		capability "Refresh"
		capability "Sensor"
        capability "Health Check"

		fingerprint profileId: "0104", inClusters: "0000,0001,0003,0400,0402,0406", outClusters: "0019", manufacturer: "Philips", model: "SML001", deviceJoinName: "Hue Motion Sensor"
	}
    
    preferences {        
        section {
			input "motionSensitivity", "enum", title: "Motion Sensitivity", options: ["Low", "Medium", "High"], displayDuringSetup: false, defaultValue: "High"
		}
        
		section {
			input "tempOffset", "decimal", title: "Temperature Offset", description: "Offset temperature by this many degrees", range: "*..*", displayDuringSetup: false
		}
        
        section {
			input "luxOffset", "number", title: "Illuminance Offset", description: "Offset illuminance by this many lux", range: "*..*", displayDuringSetup: false
		}
	}

	tiles(scale: 2) {
		multiAttributeTile(name: "motion", type: "generic", width: 6, height: 4) {
			tileAttribute("device.motion", key: "PRIMARY_CONTROL") {
				attributeState "active", label: 'motion', icon: "st.motion.motion.active", backgroundColor: "#00A0DC"
				attributeState "inactive", label: 'no motion', icon: "st.motion.motion.inactive", backgroundColor: "#cccccc"
			}
		}
        
        valueTile("temperature", "device.temperature", inactiveLabel: false, width: 2, height: 2) {
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
			state "luminosity", label:'${currentValue} lux', unit:"lux", 
            		backgroundColors:[
                            [value: 9, color: "#767676"],
                            [value: 315, color: "#ffa81e"],
                            [value: 1000, color: "#fbd41b"]
					]
		}
        
        valueTile("battery", "device.battery", inactiveLabel: false, width: 2, height: 2, decoration: "flat") {
			state "battery", label: '${currentValue}% battery', unit: ""
		}
        
		standardTile("refresh", "device.refresh", inactiveLabel: false, width: 2, height: 2, decoration: "flat") {
			state "default", action: "refresh.refresh", icon: "st.secondary.refresh"
		}

		main("motion")
        details(["motion", "temperature", "illuminance", "battery", "refresh"])
	}
}

private getMOTION_ALERT_CLUSTER() { 0x0406 }
private getMOTION_ALERT_VALUE() { 0x0000 }
private getMOTION_SENSITIVITY_VALUE() { 0x0030 }
private getILLUMINANCE_MEASUREMENT_CLUSTER() { 0x0400 }
private getILLUMINANCE_MEASURE_VALUE() { 0x0000 }
private getTEMPERATURE_MEASURE_VALUE() { 0x0000 }
private getBATTERY_MEASURE_VALUE() { 0x0020 }

private Map getBatteryResultEvent(BigDecimal newVolts) {
	if (newVolts == 0 || newVolts == 255) {
    	return [:]
    }
    
    BigDecimal minVolts = 2.1
    BigDecimal maxVolts = 3.0
    
    BigDecimal newBatteryPercent = ((newVolts - minVolts) / (maxVolts - minVolts)) * 100
    newBatteryPercent = (newBatteryPercent.min(100)).max(1)
    newBatteryPercent = newBatteryPercent.setScale(1, BigDecimal.ROUND_HALF_UP)
    
    log.debug "Updating battery value: $newBatteryPercent"
    
    return createEvent([
    	name: "battery",
        value: newBatteryPercent,
        descriptionText: "{{ device.displayName }} battery was {{ value }}%",
        unit: "%"
    ])
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
        descriptionText: "{{ device.displayName }} was {{ value }}°",
        unit: getTemperatureScale()
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
        descriptionText: "{{ device.displayName }} was {{ value }} lux",
        unit: "lux"
	])
}

private Map getMotionResultEvent(String newMotionAction) {
    log.debug "Updating motion value: $newMotionAction"

	return createEvent([
        name: "motion",
        value: newMotionAction,
        descriptionText: newMotionAction == "active" ? "{{ device.displayName }} detected motion" : "{{ device.displayName }} motion has stopped"
	])
}

private Integer convertMotionSensitivityToInt (String sensitivity) {
    if (sensitivity == "Low") {
        return 0
    }

    if (sensitivity == "Medium") {
        return 1
    }
    
    // High
    return 2
}


Map parse(String description) {
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
        	if (descMap.attrInt == BATTERY_MEASURE_VALUE && descMap.value) {
        		return getBatteryResultEvent(zigbee.convertHexToInt(descMap.value) / 10)
            }
        break
        
        // Temperature event
        case zigbee.TEMPERATURE_MEASUREMENT_CLUSTER:
        	if (descMap.commandInt == 0x07) {
            	if (descMap.data?.first() == "00") {
					log.debug "Temperature reporting config response: $descMap"
                    sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
				} else {
					log.warn "Temperature reporting config failed - error code: ${descMap.data?.first()}"
				}
            } else if (descMap.attrInt == TEMPERATURE_MEASURE_VALUE && descMap.value) {
            	return getTemperatureResultEvent(zigbee.convertHexToInt(descMap.value) / 100)
            }
        break
        
        // Illuminance event
        case ILLUMINANCE_MEASUREMENT_CLUSTER:
        	if (descMap.commandInt == 0x07) {
            	if (descMap.data?.first() == "00") {
					log.debug "Illuminance reporting config response: $descMap"
                    sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
				} else {
					log.warn "Illuminance reporting config failed - error code: ${descMap.data?.first()}"
				}
            } else if (descMap.attrInt == ILLUMINANCE_MEASURE_VALUE && descMap.value) {
            	return getLuminanceResultEvent(zigbee.convertHexToInt(descMap.value))
            }         
        break;
     
        // Motion event
        case MOTION_ALERT_CLUSTER:  
            if (descMap.commandInt == 0x04) {
	            if (descMap.data?.first() == "00") {
            		log.debug "Motion sensor sensitivity config response: $descMap"
                	sendEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
                } else {
                	log.warn "Motion sensor sensitivity config failed - error code: ${descMap.data?.first()}"
                }
            } else if (descMap.attrInt == MOTION_ALERT_VALUE && descMap.value) {
				return getMotionResultEvent(descMap.value == "01" ? "active" : "inactive")
            }
        break;
    }
    
    return [:]
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	log.info "### Ping"
	zigbee.readAttribute(zigbee.IAS_ZONE_CLUSTER, zigbee.ATTRIBUTE_IAS_ZONE_STATUS)
}

def refresh() {
	log.info "### Refresh"

	return [
        zigbee.readAttribute(MOTION_ALERT_CLUSTER, MOTION_ALERT_VALUE),
        zigbee.readAttribute(ILLUMINANCE_MEASUREMENT_CLUSTER, ILLUMINANCE_MEASURE_VALUE),
        zigbee.readAttribute(zigbee.TEMPERATURE_MEASUREMENT_CLUSTER, TEMPERATURE_MEASURE_VALUE),
	    zigbee.readAttribute(zigbee.POWER_CONFIGURATION_CLUSTER, BATTERY_MEASURE_VALUE)
    ]
}

def configure() {
	log.info "### Configure"
	
    // Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	
    // Configure reporting interval if no activity
    //    - motion - minReportTime 0 seconds, maxReportTime 2 min
	//    - illuminance - minReportTime 0 seconds, maxReportTime 10 min
    //    - temperature - minReportTime 0 seconds, maxReportTime 10 min
	//    - battery - minReport 30 seconds, maxReportTime 6 hrs by default  
    def configCmds = [
	    zigbee.configureReporting(MOTION_ALERT_CLUSTER, MOTION_ALERT_VALUE, DataType.BITMAP8, 0, 120, null),
	    zigbee.configureReporting(ILLUMINANCE_MEASUREMENT_CLUSTER, ILLUMINANCE_MEASURE_VALUE, DataType.UINT16, 0, 600, 100),
        zigbee.temperatureConfig(0, 600, 1),
        zigbee.batteryConfig(),
    ]
    
    // Remember default motion sensitivity
    state.lastMotionSensitivity = "High"
    
	return configCmds + refresh()
}

def updated () {
	log.info "### Updated"
    def motionSensitivityValue = motionSensitivity ?: "High"
    
    // Configure motion sensitivity
    if (state.lastMotionSensitivity != motionSensitivityValue) {
    	log.debug "Updating motion sensitivity - $motionSensitivityValue"
	    sendHubCommand(zigbee.writeAttribute(MOTION_ALERT_CLUSTER, MOTION_SENSITIVITY_VALUE, DataType.UINT8, convertMotionSensitivityToInt(motionSensitivityValue), [mfgCode: 0x100b]).collect{new physicalgraph.device.HubAction(it)}) 
    }
    
    // Update temperature
    if (state.lastTempOffset != lastTempOffset) {
    	log.debug "Updating temperature"
    	sendHubCommand(zigbee.readAttribute(zigbee.TEMPERATURE_MEASUREMENT_CLUSTER, TEMPERATURE_MEASURE_VALUE).collect{new physicalgraph.device.HubAction(it)})
	}

	// Update illuminance
    if (state.lastLuxOffset != luxOffset) {
    	log.debug "Updating illuminance"
    	sendHubCommand(zigbee.readAttribute(ILLUMINANCE_MEASUREMENT_CLUSTER, ILLUMINANCE_MEASURE_VALUE).collect{new physicalgraph.device.HubAction(it)}, 0)
    }
    
    state.lastMotionSensitivity = motionSensitivityValue
    state.lastTempOffset = tempOffset
    state.lastLuxOffset = luxOffset
}