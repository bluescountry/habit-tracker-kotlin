package com.dayquest.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Expected values come from running the prototype's own claim(), removeQuest() and checkGoals() code
 * (dayquest-mobile-app.html) on the same starting states.
 * Seed: level 14, 1,648 / 2,480 XP, streak 13 (not yet secured today), Level style, sample rewards.
 */
class ReducerTest {
    private val now = "2026-10-26T09:00:00Z"

    /** Seed state with every step of [id] ticked, so it can be claimed. */
    private fun ready(id: String, tweak: (AppState) -> AppState = { it }): AppState {
        val s = tweak(AppState.seed())
        return s.copy(stepsDone = s.stepsDone + (id to s.stepsDone.getValue(id).map { true }), selectedQuestId = id)
    }

    // ---- claim -----------------------------------------------------------------------------

    @Test fun claimingBodyAtTheSeedEarnsTheSmallAndTheStreakReward() {
        val r = assertNotNull(claim(ready("body"), "body", now))
        assertEquals(1738, r.state.xp)
        assertEquals(14, r.state.level)
        assertEquals(2480, r.state.cap)
        assertTrue(r.state.securedToday)
        assertEquals(14, r.state.streak)
        assertEquals(setOf("body"), r.state.claimed)
        assertEquals(
            "+90 XP — Body quest complete. Reward ready: An iced latte. " +
                "Streak extended to 14 days. Reward earned: A nice dinner out.",
            r.message,
        )
        assertEquals(setOf("body", "streak:14"), r.state.earned.keys)
        assertEquals(RewardKind.Small, r.state.earned.getValue("body").kind)
        assertEquals(RewardKind.Medium, r.state.earned.getValue("streak:14").kind)
        assertEquals("14-day streak", r.state.earned.getValue("streak:14").from)
        assertEquals(1953, rating(r.state))
        assertEquals(listOf("streak:14"), r.events.filterIsInstance<Event.RewardEarned>().map { it.key })
        assertTrue(r.events.none { it is Event.RankUp })       // Level style, same level
    }

    @Test fun aQuestNeedsAllStepsAndCanOnlyBeClaimedOnce() {
        assertNull(claim(AppState.seed(), "body", now))        // only 1 of 4 steps ticked
        val first = assertNotNull(claim(ready("body"), "body", now))
        assertNull(claim(first.state, "body", now))
        assertNull(claim(ready("body"), "nope", now))
    }

    @Test fun aSecondClaimTheSameDayEarnsNoStreakReward() {
        val s = ready("body") { it.copy(securedToday = true, claimed = setOf("read")) }
        val r = assertNotNull(claim(s, "body", now))
        assertEquals("+90 XP — Body quest complete. Reward ready: An iced latte.", r.message)
        assertEquals(setOf("body"), r.state.earned.keys)
        assertEquals(1738, r.state.xp)
        assertEquals(1988, rating(r.state))
    }

    @Test fun levelUpPaysTheBigRewardOnceAndClearsTheSlot() {
        val r = assertNotNull(claim(ready("body") { it.copy(xp = 2450) }, "body", now))
        assertEquals(15, r.state.level)
        assertEquals(60, r.state.xp)
        assertEquals(2780, r.state.cap)                         // round(2480 * 1.12 / 10) * 10
        assertNull(r.state.goals.level)                         // slot cleared, the user writes the next one
        assertEquals(RewardKind.Big, r.state.earned.getValue("level:15").kind)
        assertEquals("Reached Level 15", r.state.earned.getValue("level:15").from)
        assertEquals(
            "Level up: Level 15. Level 15 reached! +90 XP — Body quest complete. Reward ready: An iced latte. " +
                "Streak extended to 14 days. Reward earned: A nice dinner out, New running shoes.",
            r.message,
        )
        // order of the celebration screens: rank-up first, then each earned reward
        assertTrue(r.events[0] is Event.RankUp)
        assertEquals(listOf("streak:14", "level:15"), r.events.drop(1).map { (it as Event.RewardEarned).key })
        assertEquals(1998, rating(r.state))
    }

    @Test fun mobileLegendsEpicRewardFiresWhenTheRatingFirstReachesTheTier() {
        val r = assertNotNull(claim(ready("body") { it.copy(level = 17, xp = 1000, rankStyle = RankStyle.Mlbb) }, "body", now))
        assertEquals(2337, rating(r.state))
        assertEquals(
            "Rank up: Epic V. +90 XP — Body quest complete. Reward ready: An iced latte. " +
                "Streak extended to 14 days. Reward earned: A nice dinner out, Pizza night with friends.",
            r.message,
        )
        val epic = r.state.earned.getValue("rank:mlbb:Epic")
        assertEquals(RewardKind.Big, epic.kind)
        assertEquals("Reached Mobile Legends Epic", epic.from)
        val events = r.events
        assertTrue(events[0] is Event.RankUp)
        val tierEvent = events.last() as Event.RewardEarned
        assertTrue(tierEvent.isWord)
        assertEquals("Epic", tierEvent.numeral)
        assertFalse("rank:mlbb:Legend" in r.state.earned)       // Legend is still ahead
    }

    @Test fun rankRewardsOnlyFireForTheSelectedGame() {
        // Level 17 puts the rating past Dota's Archon and Mobile Legends' Epic, but only the selected game pays
        val levelStyle = assertNotNull(claim(ready("body") { it.copy(level = 17, xp = 1000) }, "body", now))
        assertTrue(levelStyle.state.earned.keys.none { it.startsWith("rank:") })
    }

    // ---- enjoy -----------------------------------------------------------------------------

    @Test fun enjoyingAnEarnedRewardIsOneWay() {
        val earned = assertNotNull(claim(ready("body"), "body", now)).state
        val later = "2026-10-26T18:00:00Z"
        val once = enjoy(earned, "body", later)
        assertEquals(later, once.earned.getValue("body").enjoyedAt)
        assertEquals(once, enjoy(once, "body", "2026-10-27T00:00:00Z"))   // second call changes nothing
        assertEquals(earned, enjoy(earned, "missing", later))
    }

    // ---- remove / undo ---------------------------------------------------------------------

    @Test fun removingAQuestHidesItAndSelectsTheNextOneAndUndoRestoresIt() {
        val s = AppState.seed().copy(selectedQuestId = "read")
        val removed = assertNotNull(removeQuest(s, "read"))
        assertEquals(setOf("read"), removed.archived)
        assertEquals(5, removed.visibleQuests.size)
        assertEquals("body", removed.selectedQuestId)            // first remaining unclaimed quest
        val back = undoRemove(removed, "read")
        assertTrue(back.archived.isEmpty())
        assertEquals("read", back.selectedQuestId)
        assertEquals(6, back.visibleQuests.size)
    }

    @Test fun aClaimedQuestCannotBeRemoved() {
        val claimed = assertNotNull(claim(ready("body"), "body", now)).state
        assertNull(removeQuest(claimed, "body"))
        assertNull(removeQuest(AppState.seed(), "missing"))
    }

    // ---- setting rewards -------------------------------------------------------------------

    @Test fun questRewardsAreTrimmedCappedAndLockedOnceClaimed() {
        val s = AppState.seed()
        assertEquals("Tea", setQuestReward(s, "read", "  Tea  ").rewards["read"])
        assertEquals(40, setQuestReward(s, "read", "x".repeat(60)).rewards.getValue("read").length)
        assertFalse("body" in setQuestReward(s, "body", "   ").rewards)          // blank removes
        val claimed = assertNotNull(claim(ready("body"), "body", now)).state
        assertEquals("An iced latte", setQuestReward(claimed, "body", "Changed").rewards["body"])
        assertEquals("An iced latte", claimed.earned.getValue("body").text)      // the earned copy never changes
    }

    @Test fun milestoneSlotsRejectMilestonesThatAreAlreadyReached() {
        val s = AppState.seed()
        assertEquals(s, setGoal(s, GoalSlot.Streak(7), "Anything"))              // streak is 13, 7 is behind us
        assertEquals("A hike", setGoal(s, GoalSlot.Streak(14), "A hike").goals.streak[14])
        assertEquals(s, setGoal(s, GoalSlot.Streak(10), "Not a milestone"))
        assertEquals("Pizza", setGoal(s, GoalSlot.Rank("mlbb:Epic"), "Pizza").goals.rank["mlbb:Epic"])
        assertEquals(s, setGoal(s, GoalSlot.Rank("mlbb:Grandmaster"), "Not a reward tier"))
        val past = s.copy(level = 20)                                            // rating well past Epic
        assertEquals(past, setGoal(past, GoalSlot.Rank("mlbb:Epic"), "Too late"))
        assertNull(setGoal(s, GoalSlot.Level, " ").goals.level)                  // blank clears the level slot
    }

    // ---- onboarding ------------------------------------------------------------------------

    @Test fun onboardingNeedsATrimmedNameOfOneToTwentyFourCharacters() {
        val s = AppState()
        assertNull(completeOnboarding(s, "   "))
        assertNull(completeOnboarding(s, "x".repeat(25)))
        assertEquals("Sam", completeOnboarding(s, "  Sam ")?.name)
        assertEquals(24, completeOnboarding(s, "x".repeat(24))?.name?.length)
    }
}
