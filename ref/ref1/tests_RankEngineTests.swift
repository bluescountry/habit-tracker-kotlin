import XCTest
@testable import Dayquest

/// Assumes AppStore exposes mutable `level`, `xp`, `cap`, `claimed: Set<String>` and `rankStyle`,
/// with `rating` and `rankInfo` from the extension in the guide, section 7.
/// If AppStore is `@MainActor`, keep the annotation below; otherwise remove it.
/// Seed state: level 14, 1,648 / 2,480 XP, nothing claimed today -> rating 1,913.
@MainActor
final class RankEngineTests: XCTestCase {

    private func store(_ style: RankStyle = .level, level: Int = 14, xp: Int = 1648,
                       cap: Int = 2480, claimed: Int = 0) -> AppStore {
        let s = AppStore()
        s.level = level
        s.xp = xp
        s.cap = cap
        s.claimed = Set((0..<claimed).map { "q\($0)" })
        s.rankStyle = style
        return s
    }

    private func assertRank(_ info: RankInfo?, label: String, tier: String, numeral: String,
                            figure: String, unit: String, progress: Double, foot: String, tierIndex: Int,
                            file: StaticString = #filePath, line: UInt = #line) {
        guard let info else { return XCTFail("expected a rank", file: file, line: line) }
        XCTAssertEqual(info.label, label, file: file, line: line)
        XCTAssertEqual(info.tier, tier, file: file, line: line)
        XCTAssertEqual(info.numeral, numeral, file: file, line: line)
        XCTAssertEqual(info.figure, figure, file: file, line: line)
        XCTAssertEqual(info.unit, unit, file: file, line: line)
        XCTAssertEqual(info.progress, progress, accuracy: 0.001, file: file, line: line)
        XCTAssertEqual(info.foot, foot, file: file, line: line)
        XCTAssertEqual(info.tierIndex, tierIndex, file: file, line: line)
    }

    // MARK: rating

    func testSeedRatingIs1913() {
        XCTAssertEqual(store().rating, 1913)
    }

    func testEachClaimedQuestAdds35Rating() {
        // xp stays the same here so only the quest bonus changes
        XCTAssertEqual(store(claimed: 1).rating, 1913 + 35)
        XCTAssertEqual(store(claimed: 2).rating, 1913 + 70)
    }

    func testLevelStyleHasNoRank() {
        XCTAssertNil(store(.level).rankInfo)
    }

    // MARK: the four seed values

    func testSeedDota2IsCrusaderStar3() {
        assertRank(store(.dota).rankInfo,
                   label: "Crusader · star 3", tier: "Crusader", numeral: "3",
                   figure: "1,913", unit: "MMR", progress: 0.4221,
                   foot: "89 MMR to Crusader · star 4", tierIndex: 2)
    }

    func testSeedMobileLegendsIsGrandmasterIII() {
        assertRank(store(.mlbb).rankInfo,
                   label: "Grandmaster III", tier: "Grandmaster", numeral: "III",
                   figure: "2 of 3", unit: "stars", progress: 0.5813,
                   foot: "67 rating to Grandmaster II", tierIndex: 3)
    }

    func testSeedLeagueIsGoldIV() {
        assertRank(store(.lol).rankInfo,
                   label: "Gold IV", tier: "Gold", numeral: "IV",
                   figure: "7", unit: "LP", progress: 0.0743,
                   foot: "162 rating to Gold III", tierIndex: 3)
    }

    func testSeedValorantIsPlatinum1() {
        assertRank(store(.valorant).rankInfo,
                   label: "Platinum 1", tier: "Platinum", numeral: "1",
                   figure: "6", unit: "RR", progress: 0.065,
                   foot: "187 rating to Platinum 2", tierIndex: 4)
    }

    // MARK: rank changes

    func testDotaStaysStar3AfterTwoClaimsAndReachesStar4OnTheThird() {
        // xp after Body (+90), Read (+70): 1,808; after Listen (+50): 1,858
        let two = store(.dota, xp: 1808, claimed: 2)
        XCTAssertEqual(two.rating, 1992)
        XCTAssertEqual(two.rankInfo?.label, "Crusader · star 3")

        let three = store(.dota, xp: 1858, claimed: 3)
        XCTAssertEqual(three.rating, 2030)
        assertRank(three.rankInfo,
                   label: "Crusader · star 4", tier: "Crusader", numeral: "4",
                   figure: "2,030", unit: "MMR", progress: 0.1818,
                   foot: "126 MMR to Crusader · star 5", tierIndex: 2)
        // this label change is what triggers the rank-up screen
        XCTAssertNotEqual(two.rankInfo?.label, three.rankInfo?.label)
    }

    // MARK: boundaries

    func testExactTierBoundaryLandsOnTheFirstDivisionOfTheNewTier() {
        // level 12, 0 xp -> 11 * 140 = 1,540 = Crusader floor
        let s = store(.dota, level: 12, xp: 0)
        XCTAssertEqual(s.rating, 1540)
        XCTAssertEqual(s.rankInfo?.label, "Crusader · star 1")
        XCTAssertEqual(s.rankInfo?.progress ?? -1, 0, accuracy: 0.001)
    }

    func testOneBelowTheBoundaryIsTheTopDivisionAndPointsAtTheNextTier() {
        let s = store(.dota, level: 11, xp: 2470)
        XCTAssertEqual(s.rating, 1539)
        XCTAssertEqual(s.rankInfo?.label, "Guardian · star 5")
        XCTAssertEqual(s.rankInfo?.foot, "1 MMR to Crusader · star 1")
    }

    // MARK: top tiers (no divisions)

    func testTopTiersShowNameFullBarAndNoDivision() {
        let cases: [(AppStore, String, String)] = [
            (store(.dota, level: 40, xp: 0), "Immortal", "40"),     // rating 5,460
            (store(.lol, level: 36, xp: 0), "Master", "100"),       // rating 4,900
            (store(.valorant, level: 38, xp: 0), "Radiant", "180"), // rating 5,180
        ]
        for (s, name, figure) in cases {
            let info = s.rankInfo
            XCTAssertEqual(info?.label, name)
            XCTAssertEqual(info?.tier, name)
            XCTAssertEqual(info?.numeral, "I")
            XCTAssertEqual(info?.figure, figure)
            XCTAssertEqual(info?.progress ?? -1, 1, accuracy: 0.001)
            XCTAssertEqual(info?.foot, "Top tier. Keep climbing.")
        }
    }

    // MARK: invariants over the whole range

    func testRatingAndTierNeverGoDownAsXpGrows() {
        for style in RankStyle.allCases where style != .level {
            var lastRating = -1
            var lastTier = -1
            for level in 1...60 {
                for fraction in [0.0, 0.5, 0.99] {
                    let s = store(style, level: level, xp: Int(2480 * fraction))
                    guard let info = s.rankInfo else { return XCTFail("no rank for \(style)") }
                    XCTAssertGreaterThanOrEqual(s.rating, lastRating, "\(style) rating dropped at level \(level)")
                    XCTAssertGreaterThanOrEqual(info.tierIndex, lastTier, "\(style) tier dropped at level \(level)")
                    XCTAssertTrue((0.0...1.0).contains(info.progress), "\(style) progress out of range at level \(level)")
                    XCTAssertFalse(info.label.isEmpty)
                    lastRating = s.rating
                    lastTier = info.tierIndex
                }
            }
        }
    }
}
