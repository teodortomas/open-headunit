package com.andrerinas.openheadunit.aap.protocol.messages

import com.andrerinas.openheadunit.aap.protocol.proto.Sensors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The parked claim has to reach the wire in the shape Android Auto reads: an engaged brake and a
 * gear in P, each in its own sensor batch under its own sensor type.
 *
 * A field built on the wrong message reads as no data at all, and the phone goes on counting taps
 * as if the car were moving - which is the state this whole claim exists to avoid.
 */
class ReportParkedSensorsTest {

    @Test
    fun `the parking brake goes out as an engaged brake on its own sensor type`() {
        val event = ParkingBrakeEvent(true)

        assertEquals(Sensors.SensorType.PARKING_BRAKE_VALUE, event.sensorType)
        val batch = event.parse(Sensors.SensorBatch.newBuilder()).build()
        assertEquals(1, batch.getParkingbrakeDataCount())
        assertTrue(batch.getParkingbrakeDataList()[0].getIsEngaged())
    }

    @Test
    fun `the gear goes out as park on its own sensor type`() {
        val event = GearEvent(Sensors.SensorBatch.GearData.GEAR.PARK)

        assertEquals(Sensors.SensorType.GEAR_VALUE, event.sensorType)
        val batch = event.parse(Sensors.SensorBatch.newBuilder()).build()
        assertEquals(1, batch.getGearDataCount())
        assertEquals(Sensors.SensorBatch.GearData.GEAR.PARK, batch.getGearDataList()[0].getGear())
    }

    @Test
    fun `neither event carries the other sensor's data`() {
        val brake = ParkingBrakeEvent(true).parse(Sensors.SensorBatch.newBuilder()).build()
        val gear = GearEvent(Sensors.SensorBatch.GearData.GEAR.PARK).parse(Sensors.SensorBatch.newBuilder()).build()

        assertEquals(0, brake.getGearDataCount())
        assertEquals(0, gear.getParkingbrakeDataCount())
    }
}
