package dev.shiqan.materialwidgets.widget.toggles

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickToggleStyleTest {

    @Test
    fun defaultStyleIsRoundCircle() {
        assertEquals(QuickButtonStyle.ROUND, QuickButtonStyle.DEFAULT)
        assertEquals(QuickButtonShape.CIRCLE, QuickButtonStyle.ROUND.toShape())
    }

    @Test
    fun curvyStyleMapsToScallopShape() {
        assertEquals(QuickButtonShape.SCALLOP_8, QuickButtonStyle.CURVY.toShape())
    }

    @Test
    fun fromIdResolvesKnownIdsAndFallsBackToDefault() {
        assertEquals(QuickButtonStyle.ROUND, QuickButtonStyle.fromId("round"))
        assertEquals(QuickButtonStyle.CURVY, QuickButtonStyle.fromId("curvy"))
        assertEquals(QuickButtonStyle.DEFAULT, QuickButtonStyle.fromId(null))
        assertEquals(QuickButtonStyle.DEFAULT, QuickButtonStyle.fromId("unknown"))
    }

    @Test
    fun allQuickToggleTypesHaveUniquePreferenceKeys() {
        val keys = QuickToggleType.entries.map { it.prefKey }
        assertEquals(9, keys.size)
        assertEquals(keys.size, keys.toSet().size)
    }
}
