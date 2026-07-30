package com.uaehn.shasha.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Arabic search is the one piece of logic here with real edge cases, and all of
 * them come from how people actually type: without diacritics, and without
 * caring which alef or yaa variant is correct.
 */
class ArabicSearchTest {

    private fun channel(ar: String, en: String = "", countryAr: String = "", country: String = "") =
        Channel(
            id = "test", ar = ar, en = en, cat = "news",
            country = country, countryAr = countryAr,
            sources = listOf(Source("https://example.com/index.m3u8")),
        )

    @Test
    fun `hamza forms on alef all fold together`() {
        assertEquals("ا", "أ".foldArabic())
        assertEquals("ا", "إ".foldArabic())
        assertEquals("ا", "آ".foldArabic())
        assertEquals("ا", "ٱ".foldArabic())
    }

    @Test
    fun `dotless yaa taa marbuta and waw hamza fold`() {
        assertEquals("علي", "على".foldArabic())
        assertEquals("قناه", "قناة".foldArabic())
        assertEquals("مسوول", "مسؤول".foldArabic())
        assertEquals("شيي", "شيئ".foldArabic())
    }

    @Test
    fun `tashkeel and tatweel are stripped`() {
        assertEquals("القران", "الْقُرْآن".foldArabic())
        assertEquals("مباشر", "مـــباشر".foldArabic())
    }

    @Test
    fun `typing without hamza still finds the channel`() {
        val ekhbariya = channel("الإخبارية", "Al Ekhbariya")
        assertTrue(ekhbariya.matches("الاخبارية"))
        assertTrue(ekhbariya.matches("الإخبارية"))
        assertTrue(ekhbariya.matches("اخبار"))
    }

    @Test
    fun `taa marbuta spelling variants both match`() {
        val arabiya = channel("العربية", "Al Arabiya")
        assertTrue(arabiya.matches("العربيه"))
        assertTrue(arabiya.matches("العربية"))
    }

    @Test
    fun `latin name matches case insensitively`() {
        val jazeera = channel("الجزيرة", "Al Jazeera")
        assertTrue(jazeera.matches("jazeera"))
        assertTrue(jazeera.matches("AL JAZ"))
    }

    @Test
    fun `country is searchable in both scripts`() {
        val qatari = channel("الجزيرة", "Al Jazeera", countryAr = "قطر", country = "QA")
        assertTrue(qatari.matches("قطر"))
        assertTrue(qatari.matches("qa"))
    }

    @Test
    fun `an empty query matches everything and a miss matches nothing`() {
        val channel = channel("الجزيرة", "Al Jazeera")
        assertTrue(channel.matches(""))
        assertTrue(channel.matches("   "))
        assertFalse(channel.matches("bbc"))
    }

    @Test
    fun `surrounding whitespace is ignored`() {
        assertTrue(channel("الحدث", "Al Hadath").matches("  الحدث  "))
    }
}
