package com.dayquest.domain

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Assumes AppState is a data class with named parameters level, xp, cap, claimed (Set<String>) and rankStyle,
 * and that rating(AppState) and rankInfo(AppState) are the functions from the guide, section 6.
 * Seed state: level 14, 1,648 / 2,480 XP, nothing claimed today -> rating 1,913.
 */
class RankEngineTest {

    private fun state(
        style: RankStyle = RankStyle.Level,
        level: Int = 14,
        xp: Int = 1648,
        cap: Int = 2480,
        claimed: Int = 0,
    ) = AppState(
        level = level, xp = xp, cap = cap,
        claimed = (1..claimed).map { "q$it" }.toSet(),
        rankStyle = style,
    )

    private fun assertClose(expected: Float, actual: Float, tolerance: Float = 0.001f) =
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected but was $actual")

    private fun assertRank(
        info: RankInfo?, label: String, tier: String, numeral: String, figure: String,
        unit: String, progress: Float, foot: String, tierIndex: Int,
    ) {
        assertNotNull(info)
        assertEquals(label, info.label)
        assertEquals(tier, info.tier)
        assertEquals(numeral, info.numeral)
        assertEquals(figure, info.figure)
        assertEquals(unit, info.unit)
        assertClose(progress, info.progress)
        assertEquals(foot, info.foot)
        assertEquals(tierIndex, info.tierIndex)
    }

    // ---- rating ----------------------------------------------------------------------------

    @Test fun seedRatingIs1913() {
        assertEquals(1913, rating(state()))
    }

    @Test fun eachClaimedQuestAdds35Rating() {
        // xp stays the same here so only the quest bonus changes
        assertEquals(1913 + 35, rating(state(claimed = 1)))
        assertEquals(1913 + 70, rating(state(claimed = 2)))
    }

    @Test fun levelStyleHasNoRank() {
        assertNull(rankInfo(state(RankStyle.Level)))
    }

    // ---- the four seed values ---------------------------------------------------------------

    @Test fun seedDota2IsCrusaderStar3() {
        assertRank(
            rankInfo(state(RankStyle.Dota)),
            label = "Crusader · star 3", tier = "Crusader", numeral = "3",
            figure = "1,913", unit = "MMR", progress = 0.4221f,
            foot = "89 MMR to Crusader · star 4", tierIndex = 2,
        )
    }

    @Test fun seedMobileLegendsIsGrandmasterIII() {
        assertRank(
            rankInfo(state(RankStyle.Mlbb)),
            label = "Grandmaster III", tier = "Grandmaster", numeral = "III",
            figure = "2 of 3", unit = "stars", progress = 0.5813f,
            foot = "67 rating to Grandmaster II", tierIndex = 3,
        )
    }

    @Test fun seedLeagueIsGoldIV() {
        assertRank(
            rankInfo(state(RankStyle.Lol)),
            label = "Gold IV", tier = "Gold", numeral = "IV",
            figure = "7", unit = "LP", progress = 0.0743f,
            foot = "162 rating to Gold III", tierIndex = 3,
        )
    }

    @Test fun seedValorantIsPlatinum1() {
        assertRank(
            rankInfo(state(RankStyle.Valorant)),
            label = "Platinum 1", tier = "Platinum", numeral = "1",
            figure = "6", unit = "RR", progress = 0.065f,
            foot = "187 rating to Platinum 2", tierIndex = 4,
        )
    }

    // ---- rank changes ----------------------------------------------------------------------

    @Test fun dotaStaysStar3AfterTwoClaimsAndReachesStar4OnTheThird() {
        // xp after Body (+90), Read (+70): 1,808; after Listen (+50): 1,858
        val two = rankInfo(state(RankStyle.Dota, xp = 1808, claimed = 2))
        assertEquals(1992, rating(state(RankStyle.Dota, xp = 1808, claimed = 2)))
        assertEquals("Crusader · star 3", two?.label)

        val three = rankInfo(state(RankStyle.Dota, xp = 1858, claimed = 3))
        assertEquals(2030, rating(state(RankStyle.Dota, xp = 1858, claimed = 3)))
        assertRank(
            three,
            label = "Crusader · star 4", tier = "Crusader", numeral = "4",
            figure = "2,030", unit = "MMR", progress = 0.1818f,
            foot = "126 MMR to Crusader · star 5", tierIndex = 2,
        )
        // this label change is what triggers the rank-up screen
        assertTrue(two?.label != three?.label)
    }

    // ---- boundaries ------------------------------------------------------------------------

    @Test fun exactTierBoundaryLandsOnTheFirstDivisionOfTheNewTier() {
        // level 12, 0 xp -> 11 * 140 = 1,540 = Crusader floor
        val s = state(RankStyle.Dota, level = 12, xp = 0)
        assertEquals(1540, rating(s))
        assertEquals("Crusader · star 1", rankInfo(s)?.label)
        assertClose(0f, rankInfo(s)!!.progress)
    }

    @Test fun oneBelowTheBoundaryIsTheTopDivisionAndPointsAtTheNextTier() {
        val s = state(RankStyle.Dota, level = 11, xp = 2470)
        assertEquals(1539, rating(s))
        val info = rankInfo(s)
        assertEquals("Guardian · star 5", info?.label)
        assertEquals("1 MMR to Crusader · star 1", info?.foot)
    }

    // ---- top tiers (no divisions) ----------------------------------------------------------

    @Test fun topTiersShowNameFullBarAndNoDivision() {
        val cases = listOf(
            Triple(state(RankStyle.Dota, level = 40, xp = 0), "Immortal", "40"),     // rating 5,460
            Triple(state(RankStyle.Lol, level = 36, xp = 0), "Master", "100"),        // rating 4,900
            Triple(state(RankStyle.Valorant, level = 38, xp = 0), "Radiant", "180"),  // rating 5,180
        )
        for ((s, name, figure) in cases) {
            val info = rankInfo(s)
            assertNotNull(info)
            assertEquals(name, info.label)
            assertEquals(name, info.tier)
            assertEquals("I", info.numeral)
            assertEquals(figure, info.figure)
            assertClose(1f, info.progress)
            assertEquals("Top tier. Keep climbing.", info.foot)
        }
    }

    // ---- invariants over the whole range ---------------------------------------------------

    @Test fun ratingAndTierNeverGoDownAsXpGrows() {
        for (style in RankStyle.values().filter { it != RankStyle.Level }) {
            var lastRating = -1
            var lastTier = -1
            for (level in 1..60) for (fraction in listOf(0.0, 0.5, 0.99)) {
                val s = state(style, level = level, xp = (2480 * fraction).toInt())
                val info = rankInfo(s)
                assertNotNull(info)
                assertTrue(rating(s) >= lastRating, "$style rating dropped at level $level")
                assertTrue(info.tierIndex >= lastTier, "$style tier dropped at level $level")
                assertTrue(info.progress in 0f..1f, "$style progress out of range at level $level")
                assertTrue(info.label.isNotBlank())
                lastRating = rating(s)
                lastTier = info.tierIndex
            }
        }
    }
}
