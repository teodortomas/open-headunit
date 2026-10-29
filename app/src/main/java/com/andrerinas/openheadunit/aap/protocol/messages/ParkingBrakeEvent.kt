package com.andrerinas.openheadunit.aap.protocol.messages

import com.andrerinas.openheadunit.aap.protocol.proto.Sensors
import com.google.protobuf.Message

/**
 * The parking brake as Android Auto reads it.
 *
 * An engaged brake is what makes the phone treat the session as parked instead of moving, which is
 * the state its tap limit ("Safety pause") and keyboard lockout hang off. A real head unit answers
 * this sensor from its handbrake wire; this one answers it only when Settings says to, because it
 * has no wire to read. Sent in reply to the phone's sensor start request, the same way a car sends
 * it when the brake is pulled.
 */
class ParkingBrakeEvent(engaged: Boolean)
    : SensorEvent(Sensors.SensorType.PARKING_BRAKE_VALUE, makeProto(engaged)) {

    companion object {
        private fun makeProto(engaged: Boolean): Message {
            return Sensors.SensorBatch.newBuilder()
                    .addParkingbrakeData(
                            Sensors.SensorBatch.ParkingBrakeData.newBuilder().apply {
                                isEngaged = engaged
                            }
                    )
                    .build()
        }
    }
}
