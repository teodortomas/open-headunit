package com.andrerinas.openheadunit.aap.protocol.messages

import com.andrerinas.openheadunit.aap.protocol.proto.Sensors
import com.google.protobuf.Message

/**
 * The selected gear, announced so the parked claim is not carried by the parking brake alone.
 *
 * Android Auto cross-checks the two: a brake that says engaged while the gear says drive is an
 * inconsistent car, and the phone is free to believe neither. Sent in reply to the phone's sensor
 * start request, only when Settings asked for the parked claim.
 */
class GearEvent(selected: Sensors.SensorBatch.GearData.GEAR)
    : SensorEvent(Sensors.SensorType.GEAR_VALUE, makeProto(selected)) {

    companion object {
        private fun makeProto(selected: Sensors.SensorBatch.GearData.GEAR): Message {
            return Sensors.SensorBatch.newBuilder()
                    .addGearData(
                            Sensors.SensorBatch.GearData.newBuilder().apply {
                                gear = selected
                            }
                    )
                    .build()
        }
    }
}
