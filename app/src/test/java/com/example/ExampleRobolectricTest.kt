package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.TrendTimeframe
import com.example.data.repository.ReferenceWaterData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read H2Know app_name from context and verify longitudinal trend synthesis`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("H2Know Beta", appName)

        val austinProviders = ReferenceWaterData.presetSystemsForZip("78701")
        assertNotNull(austinProviders)
        assertTrue(austinProviders!!.isNotEmpty())
        assertEquals("TX2270001", austinProviders.first().pwsid)

        val (seriesMap, alerts, _) = ReferenceWaterData.buildLongitudinalIntelligence(
            pwsid = "MI0002310",
            systemName = "FLINT, CITY OF",
            zip = "48502",
            sourceType = "Purchased surface water",
            currentScore = 76,
            droughtLevel = "none",
            contaminants = emptyList(),
            violations = emptyList()
        )
        assertEquals(4, seriesMap[TrendTimeframe.ONE_YEAR]?.points?.size)
        assertEquals(5, seriesMap[TrendTimeframe.FIVE_YEAR]?.points?.size)
        assertEquals(10, seriesMap[TrendTimeframe.TEN_YEAR]?.points?.size)
        assertTrue(alerts.isNotEmpty())
    }
}
