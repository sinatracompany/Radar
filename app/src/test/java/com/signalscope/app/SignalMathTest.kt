package com.signalscope.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalMathTest {
    @Test fun rejectsInvalidRssi() {
        assertTrue(SignalMath.isReasonableRssi(-70))
        assertFalse(SignalMath.isReasonableRssi(42))
        assertFalse(SignalMath.isReasonableRssi(-999))
    }

    @Test fun qualityIsBounded() {
        assertEquals(0f, SignalMath.signalQuality(-120), 0.001f)
        assertEquals(1f, SignalMath.signalQuality(-20), 0.001f)
    }
}
