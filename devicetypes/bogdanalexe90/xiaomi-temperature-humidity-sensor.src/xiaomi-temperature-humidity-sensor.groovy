/**
 *  Xiaomi Temperature Humidity Sensor
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
	definition (name: "Xiaomi Temperature Humidity Sensor", namespace: "bogdanalexe90", author: "Bogdan Alexe") {
		capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Sensor"
		capability "Battery"
		capability "Health Check"
        
        attribute "pressure", "number"
        
        fingerprint profileId: "0104", deviceId: "5F01", inClusters: "0000,0003,FFFF,0402,0403,0405", outClusters: "0000,0004,FFFF", manufacturer: "LUMI", model: "lumi.weather", deviceJoinName: "Xiaomi Aqara Temperature Humidity Sensor"
	}

	tiles(scale: 2) {
		multiAttributeTile(name: "temperature", type: "generic", width: 6, height: 4) {
			tileAttribute("device.temperature", key: "PRIMARY_CONTROL") {
				attributeState "temperature", label: '${currentValue}°' , unit: "dF", icon: "st.Weather.weather2",
                    backgroundColors: [
                        // Celsius
                        [value: 0, color: "#153591"],
                        [value: 7, color: "#1e9cbb"],
                        [value: 15, color: "#90d2a7"],
                        [value: 23, color: "#44b621"],
                        [value: 28, color: "#f1d801"],
                        [value: 35, color: "#d04e00"],
                        [value: 37, color: "#bc2323"],
                        // Fahrenheit
                        [value: 40, color: "#153591"],
                        [value: 44, color: "#1e9cbb"],
                        [value: 59, color: "#90d2a7"],
                        [value: 74, color: "#44b621"],
                        [value: 84, color: "#f1d801"],
                        [value: 95, color: "#d04e00"],
                        [value: 96, color: "#bc2323"]
					]
			}
            tileAttribute("device.humidity", key: "SECONDARY_CONTROL") {
                attributeState "humidity", label:'${currentValue}%', unit:"%", icon: "st.Weather.weather12"
    		}
		}
        
        valueTile("humidity", "device.humidity", width: 2, height: 2, decoration:"flat") {
			state "humidity", label: '${currentValue}% humidity', unit: "%"
		}
		valueTile("pressure", "device.pressure", width: 2, height: 2, decoration:"flat") {
			state "pressure", label: '${currentValue} mbar pressure', unit: "mbar"
		}
        valueTile("battery", "device.battery", width: 2, height: 2, decoration:"flat") {
			state "battery", label: '${currentValue}% battery', unit: "%"
		}

		main("temperature")
		details(["temperature","humidity","pressure","battery"])
	}
}

private getPRESSURE_MEASUREMENT_CLUSTER() { 0x0403 }
private getPRESSURE_MEASURE_VALUE() { 0x0000 }
private getBASIC_CLUSTER() { 0x0000 }
private getCUSTOM_MEASURE_VALUE() { 0xFF01 }

private Map getTemperatureResultEvent(BigDecimal newTemp) {
    // Convert C to F if needed
	newTemp = getTemperatureScale() == 'C' ? newTemp : (newTemp * 1.8 + 32)
    newTemp = newTemp.setScale(1, BigDecimal.ROUND_HALF_UP)
	log.debug "Updating temperature value: $newTemp"
	
    return createEvent([
        name: "temperature",
        value: newTemp,
        descriptionText: "{{ device.displayName }} was {{ value }}°",
        unit: getTemperatureScale()
	])
}

private Map getHumidityResultEvent(BigDecimal newHumidity) {
	newHumidity = newHumidity.setScale(1, BigDecimal.ROUND_HALF_UP)
	log.debug "Updating humidity value: $newHumidity"
    
	return createEvent([
        name: "humidity",
        value: newHumidity,
        descriptionText: "{{ device.displayName }} was {{ value }}%",
        unit: "%"
	])
}

private Map getPressureResultEvent(Integer newPressure) {
	log.debug "Updating pressure value: $newPressure"
    
	return createEvent([
        name: "pressure",
        value: newPressure,
        descriptionText: "{{ device.displayName }} was {{ value }} mbar",
        unit: "mbar"
	])
}

private Map getBatteryResultEvent(BigDecimal newVolts) {
	if (newVolts == 0 || newVolts == 255) {
    	return [:]
    }
    
    BigDecimal minVolts = 2.7
    BigDecimal maxVolts = 3.1
    
    BigDecimal newBatteryPercent = ((newVolts - minVolts) / (maxVolts - minVolts)) * 100
    newBatteryPercent = (newBatteryPercent.min(100)).max(1)
    newBatteryPercent = newBatteryPercent.setScale(0, BigDecimal.ROUND_HALF_UP)
    
    log.debug "Updating battery value: $newBatteryPercent"
    
    return createEvent([
    	name: "battery",
        value: newBatteryPercent,
        descriptionText: "{{ device.displayName }} battery was {{ value }}%",
        unit: "%"
    ])
}

private List<Map> getCustomEventList(List<String> data) {
    // https://github.com/dresden-elektronik/deconz-rest-plugin/issues/42#issuecomment-367801988
	List<String> reverseData = data.reverse()    
    String pressureReading = [reverseData.get(4), reverseData.get(5), reverseData.get(6), reverseData.get(7)].join("")
    String humidityReading = [reverseData.get(10), reverseData.get(11)].join("")
    String temparatureReading = [reverseData.get(14), reverseData.get(15)].join("")
    String batteryReading = [reverseData.get(33), reverseData.get(34)].join("")
   
    return [
    	getPressureResultEvent((zigbee.convertHexToInt(pressureReading) / 100) as Integer),
        getHumidityResultEvent(zigbee.convertHexToInt(humidityReading) / 100),
        getTemperatureResultEvent(zigbee.convertHexToInt(temparatureReading) / 100),
        getBatteryResultEvent(zigbee.convertHexToInt(batteryReading) / 1000)
    ]
}


def parse(String description) {
	log.debug "description: $description"
	
    if (description?.startsWith("temperature")) {
    	return getTemperatureResultEvent((description - "temperature: ") as BigDecimal)
    }
    
    if (description?.startsWith("humidity")) {
    	return getHumidityResultEvent((description - "humidity: " - "%") as BigDecimal)
    }
    
    Map descMap = zigbee.parseDescriptionAsMap(description)
    
    switch (descMap?.clusterInt) {               
        // Presure event
        case PRESSURE_MEASUREMENT_CLUSTER:
        	if (descMap.attrInt == PRESSURE_MEASURE_VALUE && descMap.value) {
                log.info "Parsing pressure: $descMap"
            	return getPressureResultEvent(zigbee.convertHexToInt(descMap.value))
            }
        break
        
        // Custom event - battery, temperature, humidity, pressure
        case BASIC_CLUSTER:
        	if (descMap.attrInt == CUSTOM_MEASURE_VALUE && descMap.data){
                log.info "Parsing custom attribute: $descMap"
                return getCustomEventList(descMap.data)
            }
        break
    }
}

def installed() {
	// Device wakes up every 1 hour, this interval allows us to miss one wakeup notification before marking offline
	log.info "### Installed"
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}

def updated() {
	// Device wakes up every 1 hours, this interval allows us to miss one wakeup notification before marking offline
	log.info "### Updated"
	sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}
