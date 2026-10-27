package com.dayquest.domain

import java.util.Locale

private val ROMAN = listOf("", "I", "II", "III", "IV", "V")

fun rating(s: AppState): Int =
    Math.round((s.level - 1) * 140f + s.xp.toFloat() / s.cap * 140f + s.claimed.size * 35f)

private fun divLabel(style: RankStyle, ti: Int, di: Int): String {
    val t = style.tiers[ti]
    val n = t.divisions
    return when {
        n == 0 -> t.name
        style == RankStyle.Dota -> "${t.name} · star ${di + 1}"
        style == RankStyle.Valorant -> "${t.name} ${di + 1}"
        else -> "${t.name} ${ROMAN[n - di]}"
    }
}

fun rankInfo(s: AppState): RankInfo? {
    val style = s.rankStyle
    if (style == RankStyle.Level) return null
    val r = rating(s)
    val tiers = style.tiers
    var ti = 0
    while (ti + 1 < tiers.size && r >= tiers[ti + 1].low) ti++
    val t = tiers[ti]
    if (t.divisions == 0) {
        return RankInfo(
            t.name,
            t.name,
            "I",
            String.format(Locale.US, "%,d", r - t.low),
            "${if (style == RankStyle.Dota) "MMR" else "rating"} over ${String.format(Locale.US, "%,d", t.low)}",
            1f,
            "Top tier. Keep climbing.",
            ti
        )
    }
    val w = (tiers[ti + 1].low - t.low).toFloat() / t.divisions
    val di = minOf(t.divisions - 1, ((r - t.low) / w).toInt())
    val dLow = t.low + di * w
    val p = (r - dLow) / w
    val toNext = maxOf(1, Math.ceil((dLow + w - r).toDouble()).toInt())
    val (nti, ndi) = if (di + 1 < t.divisions) ti to di + 1 else ti + 1 to 0
    val unitWord = if (style == RankStyle.Dota) "MMR" else "rating"
    val figure = when (style) {
        RankStyle.Dota -> String.format(Locale.US, "%,d", r)
        RankStyle.Mlbb -> "${minOf(2, (p * 3).toInt()) + 1} of 3"
        else -> minOf(99, (p * 100).toInt()).toString()
    }
    val numeral = if (style == RankStyle.Dota || style == RankStyle.Valorant) "${di + 1}" else ROMAN[t.divisions - di]
    return RankInfo(
        divLabel(style, ti, di),
        t.name,
        numeral,
        figure,
        style.unit,
        p,
        String.format(Locale.US, "%,d %s to %s", toNext, unitWord, divLabel(style, nti, ndi)),
        ti
    )
}

fun displayInfo(s: AppState): RankDisplay {
    val rk = rankInfo(s)
    return if (rk != null) {
        RankDisplay(label = rk.label, tier = rk.tier, numeral = rk.numeral)
    } else {
        RankDisplay(label = "Level ${s.level}", tier = "Level", numeral = s.level.toString())
    }
}
