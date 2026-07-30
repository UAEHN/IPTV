package com.uaehn.shasha.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Source(
    val url: String,
    val label: String = "",
    /**
     * Some broadcaster CDNs only answer when the request looks like it came
     * from their own player page. When set, the player sends a Referer and
     * Origin built from the channel's [Channel.site].
     */
    val referer: Boolean = false,
)

@Serializable
data class Channel(
    val id: String,
    val ar: String,
    val en: String,
    val cat: String,
    val country: String = "",
    @SerialName("countryAr") val countryAr: String = "",
    val site: String = "",
    val sources: List<Source> = emptyList(),
    /**
     * Optional absolute URL. Normally empty: every channel ships a bundled
     * tile under assets/logos, so the grid renders with no network at all.
     * A catalog pulled from the network can point at a remote image for a
     * channel added after the APK was built.
     */
    val logo: String = "",
    /**
     * False when the catalog shipped a source that could not be confirmed live
     * at build time. Kept so a future build can surface it rather than
     * pretending every channel is equally reliable.
     */
    val verified: Boolean = true,
) {
    val assetLogo: String get() = "file:///android_asset/logos/$id.webp"

    /** Primary name in the reading language, with the other as the subtitle. */
    fun name(arabic: Boolean): String = if (arabic) ar else en.ifBlank { ar }

    fun subtitle(arabic: Boolean): String = if (arabic) en else ar

    /** What the logo loader should try, in order. */
    val logoModel: String get() = logo.ifEmpty { assetLogo }
}

@Serializable
data class Category(val id: String, val ar: String, val en: String) {
    fun label(arabic: Boolean): String = if (arabic) ar else en.ifBlank { ar }
}

@Serializable
data class Catalog(
    val schema: Int = 1,
    val updated: String = "",
    val note: String = "",
    val categories: List<Category> = emptyList(),
    val channels: List<Channel> = emptyList(),
) {
    fun category(id: String): Category? = categories.firstOrNull { it.id == id }
    fun channel(id: String): Channel? = channels.firstOrNull { it.id == id }
    fun inCategory(id: String): List<Channel> = channels.filter { it.cat == id }

    companion object {
        val Empty = Catalog()
    }
}

/**
 * Arabic search has to be forgiving or it is useless: users type without
 * diacritics, and the alef and yaa variants are routinely interchanged. This
 * folds a string to a comparable form — strip tashkeel and tatweel, unify the
 * alef family, taa marbuta, and the dotless yaa — so "الاخبارية" matches
 * "الإخبارية".
 */
fun String.foldArabic(): String {
    val sb = StringBuilder(length)
    for (ch in this) {
        when (ch) {
            // Tashkeel (fathatan..sukun), superscript alef, and tatweel.
            in 'ً'..'ْ', 'ٰ', 'ـ' -> Unit
            'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
            'ى' -> sb.append('ي')
            'ة' -> sb.append('ه')
            'ؤ' -> sb.append('و')
            'ئ' -> sb.append('ي')
            else -> sb.append(ch.lowercaseChar())
        }
    }
    return sb.toString()
}

/** True when [query] matches the channel's Arabic name, Latin name, or country. */
fun Channel.matches(query: String): Boolean {
    val q = query.trim().foldArabic()
    if (q.isEmpty()) return true
    return ar.foldArabic().contains(q) ||
        en.foldArabic().contains(q) ||
        countryAr.foldArabic().contains(q) ||
        country.lowercase().contains(q)
}
