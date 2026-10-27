# Dayquest — build guide for Android (Kotlin) and iOS (Swift)

> **How this guide is organised.** Part 1 (sections 1 to 12) is the full specification: tokens, screens, rank engine, icons, tests, database, rewards. Part 2 (at the end of this file) is the build plan for everything that is not built yet, written for Android first. If you have already built the rank engine and its tests, start at Part 2.

Reference UI: `dayquest-mobile-app.html`. Open it in a browser. It is a working prototype with three phones side by side:

1. **Cover** (a poster, not an app screen; skip it or use it as a splash/onboarding page)
2. **Today** (the app: Today, Library, Stats and Profile tabs, plus the New quest sheet)
3. **Quest detail** (steps, claim button, rank-up screen)

Build the app screens (2 and 3) natively. Match the prototype's behaviour and numbers exactly; every value below is taken from it. Use the prototype's functions as the logic reference: `renderToday`, `claim`, `rankInfo`, `rating`, `renderStats`, `showRankUp`.

If this guide and the prototype ever disagree, the prototype wins. The Kotlin and Swift snippets are reference code written without a compiler: expect small fixes when you build them.

Targets: **Android** Kotlin + Jetpack Compose + Material 3 (minSdk 26). **iOS** Swift + SwiftUI (iOS 17+, uses `@Observable`).

---

## 1. Product in one paragraph

Habits become **quests**. Each quest has steps and an XP reward. Finishing all steps and tapping *Claim* adds XP, fills the **level** bar, and secures today's **streak**. Each quest can also carry a **reward the user writes themselves** ("An iced latte"); finishing the quest earns it, and the user marks it enjoyed once they have had it. The user can choose to *see* their progress as plain levels or as a rank on a game ladder (Dota 2, Mobile Legends, League of Legends, Valorant). The rank is a display style on top of one internal rating number. When the displayed rank changes, a full-screen celebration appears.

---

## 2. Design tokens

Dark only. True neutral greys (not navy). Flat fills: **no gradients, no glows, no blur, no shadows at rest**. Shadows only on floating things (toast).

### Colour

| Token | Value | Use |
| --- | --- | --- |
| `ground` | `#111111` | Screen background |
| `surface` | `#1B1B1B` | Panels, step rows, library rows |
| `raised` | `#242424` | Hover/pressed on surface, current ladder row |
| `border` | `#2E2E2E` | 1px borders, tab bar top line, gridlines |
| `control` | `#444444` | Chip outlines, input outline, dashed empty box |
| `track` | `#3A3A37` | Progress track, empty streak dot |
| `text` | `#F5F5F2` | Primary text, primary button fill |
| `muted` | `#A9A9A4` | Secondary text (4.5:1 on ground) |
| `accent` | `#BAED3F` (`oklch(88% 0.2 125)`) | **Only** on progress: XP bar fill, streak dots, ticked steps, today's chart bar, top category bar, done check, rank-up screen field |
| `accentInk` | `#141A00` | Text/icons on accent |
| `error` | `#FF9D8F` | Input error text and outline |
| `barPast` | `#6A6A65` | Past-day bars in the XP chart |

Rule: lime is data. Do not use it on buttons or chrome. The primary button is `text` fill with `#111111` label.

### Quest colours (saturated flat fields, dark text)

Text on these is dark. Never white.

| Category | Fill | Ink (title, icon, progress) | Secondary (category label, hero body) | Pressed/hover |
| --- | --- | --- | --- | --- |
| Body | `#FF7E76` | `#240706` | `#3C1715` | `#FF8E85` |
| Read | `#FAC131` | `#1B0F00` | `#312100` | `#FFD147` |
| Listen | `#0ABFFF` | `#001424` | `#00283D` | `#32D0FF` |
| Nourish | `#66DA85` | `#001705` | `#092C13` | `#77EB94` |
| Mind | `#AD8DFD` | `#140C25` | `#261D3E` | `#BC9DFF` |
| Watch | `#FB83BF` | `#210715` | `#391628` | `#FF93CF` |

Library row icons use a lighter tint of the same hue (`#FF8E86`, `#E3AE28`, `#38C5FF`, `#67D283`, `#BFA2FF`, `#FE8DC5`).
A **completed** quest tile becomes a `surface` tile with `muted` text, a struck-through title, and an `accent` check icon.
Progress segments on a tile: track = ink at 22% alpha, filled = ink at 100%.

### Type

| Role | Font | Size / weight |
| --- | --- | --- |
| Screen title ("Morning, Sam.", "Library", "Stats", "Profile") | Instrument Serif 400 | 34 |
| Quest title (detail hero) | Instrument Serif 400 | 40, line-height 1.02 |
| Rank name on Profile | Instrument Serif 400 | 44 |
| Cover numeral | Instrument Serif 400 | 232 (168 if 3+ characters, e.g. "III") |
| Rank-up tier name / numeral | Instrument Serif 400 | 60 / 250 (176 if 3+ characters) |
| Big figures (XP, KPIs, reward) | Instrument Sans 400 | 32 (KPI 30) |
| Body, step text, row title | Instrument Sans 400/600 | 15 |
| Panel titles, tile category | Instrument Sans 600 / 500 | 14 / 12 |
| Meta, captions | Instrument Sans 400 | 13 |
| Tab labels | Instrument Sans 500 | 11 |

Bundle both fonts in the app (Instrument Sans: 400, 500, 600; Instrument Serif: 400). If fonts are unavailable, fall back to the system sans and a system serif. Use **tabular figures** on every number that changes (Android: `fontFeatureSettings = "tnum"`; iOS: `.monospacedDigit()`).

### Shape and spacing

- Screen side padding 16. Gap between tiles/rows 8. Panels have 16 padding and 16 top margin.
- Radii: panels and quest tiles **22**, rows/inputs **16** (inputs 14), chips and buttons are **pills**, bottom sheet top corners **28**.
- Primary buttons 56 high. Chips and icon buttons at least 44. Library "Add" button 44 high, 78 wide minimum.
- Tab bar: 5 slots (Today, Library, **centre +**, Stats, Profile). The + is a 48 circle, `text` fill, `#111111` icon. Active tab uses `text` colour and 600 weight; inactive `muted`. No indicator pill.
- Icons: 1.8 stroke outline, 22 in tab bar and tiles. The exact paths for all 15 icons, as ready-to-paste Android VectorDrawables and SwiftUI `Path` code, are in section 8.

### Motion

Only feedback, never decoration. Respect reduced motion (Android `Settings.Global.ANIMATOR_DURATION_SCALE == 0`; iOS `accessibilityReduceMotion`).

| Where | Motion |
| --- | --- |
| XP bar fill | width, 500 ms, ease `cubic-bezier(0.2, 0, 0, 1)` |
| Tile press | lift 2dp up, 150 ms |
| New quest sheet | slide up 40dp + fade, 250 ms |
| Rank-up numeral | one-shot scale 0.82 to 1 + fade, 500 ms. Never loops. |
| Toast | fade + 8dp rise, 200 ms. Auto-dismiss after 3.2 s (pause while focused/touched). |

---

## 3. Data model

```
Category   = Body | Read | Listen | Nourish | Mind | Watch
RankStyle  = Level | Dota | Mlbb | Lol | Valorant

Quest {
  id: String, category: Category, title: String, shortTitle: String,
  xp: Int, story: String, steps: [String], isCustom: Boolean
}

AppState {
  name: String                                          // from onboarding, 1-24 characters, trimmed
  lastBackupAt: String? = null
  level: Int = 14, xp: Int = 1648, cap: Int = 2480     // cap grows x1.12, rounded to nearest 10, per level
  streak: Int = 13, securedToday: Boolean = false       // today is a Sunday in the prototype
  quests: [Quest]                                      // seeded with the six below
  stepsDone: Map<QuestId, [Boolean]>
  claimed: Set<QuestId>
  archived: Set<QuestId>                                // removed from today; hidden everywhere, restorable by Undo
  rewards: Map<QuestId, String>                         // the user's own reward text, 1-40 characters, optional per quest
  earned: Map<String, EarnedReward>                     // key = questId, 'streak:14', 'level:15' or 'rank:mlbb:Epic'; EarnedReward { text, kind: small|medium|big, from, earnedAt, enjoyedAt? }
  goals: { streak: Map<Int, String>,                    // medium: 7 / 14 / 30 days
           level: String?,                              // big: for the next level-up only, cleared when it pays out
           rank: Map<String, String> }                  // big: 'dota:Archon', 'dota:Legend', 'mlbb:Epic', 'mlbb:Legend'
  selectedQuestId: QuestId
  rankStyle: RankStyle = Level
  libraryCategory: Category? = null (All)
  addedLibraryIds: Set<String>
}
```

Seed rewards (sample data, replace with the user's own): Body "An iced latte", Watch "Order your favourite takeaway"; streak 14 "A nice dinner out", streak 30 "A weekend day trip"; next level-up "New running shoes"; ranks Mobile Legends Epic "Pizza night with friends", Legend "A new gadget", Dota 2 Archon "A takeaway night", Legend "A new gadget".

Seed quests (title shown as `Category — title`; first quest starts with step 1 done):

| id | Category | Short title (tile) | Detail title | XP | Steps |
| --- | --- | --- | --- | --- | --- |
| body | Body | 20-min strength | Body — strength | 90 | Warm up: 2 min of arm circles and squats; Pushups: 3 rounds of 12; Plank: 3 holds of 45 seconds; Log how it felt |
| read | Read | Four Thousand Weeks | Read — Four Thousand Weeks | 70 | Open the book at your bookmark; Read 15 pages without switching apps; Underline one idea worth keeping |
| listen | Listen | Sleep Architecture | Listen — Huberman Lab | 50 | Queue the Sleep Architecture episode; Listen for 25 minutes; Note one takeaway for tonight |
| nourish | Nourish | High-protein lunch | Nourish — high-protein lunch | 70 | Pick a recipe with 30 g+ protein; Cook it; Eat at the table, phone away |
| mind | Mind | 10-min focus | Mind — focus | 60 | Set a 10-minute timer; Breathe and count to ten, repeat; Note where your attention drifted |
| watch | Watch | The Bear · S3 E4 | Watch — The Bear | 90 | Finish at least two other quests; Watch S3 E4 start to finish; Rate it and jot one line |

Story strings are in the prototype's `QUESTS` array. Seed total: 430 XP. In the detail title, custom quests use their name as typed; seed quests lower-case the title except Read, Listen and Watch.

Library templates (12) live in the prototype's `LIBRARY` array: two per category, each with name, XP (30-80), meta ("30 min") and three steps. Copy that array verbatim.

---

## 4. Screens and behaviour

### 4.0 Onboarding (first launch)

A full-screen "What should we call you?" page asks for a name before anything else, with a restore-from-backup link. Full spec, validation and copy in section 10.1.

### 4.1 Today (home tab)

Top to bottom:

1. **Header.** Serif title "Morning, Sam." and a muted line. Line is `"{streak} days running. Finish one quest today and it reaches {streak+1}."` before securing, and `"Streak secured. {streak+1} days running."` after. Right: 44 bell button (outline icon); tap shows toast "No new notifications."
2. **Level panel** (`surface`). Left: title and subtitle. Right: big figure. Under them an 8dp bar (track `#3A3A37`, fill `accent`) and a muted foot line.
   - `Level` style: title "Level 14", subtitle "Experience", figure `1,648 / 2,480 XP`, foot `832 XP to Level 15`.
   - Rank styles: title = rank label, subtitle = `"{Game} rank"`, figure + unit from the rank engine (section 5), foot from the rank engine. Bar = progress inside the current division.
3. **Streak panel.** Title `"{n}-day streak"`, right note "Today still open" / "Secured for today". Seven day dots Mon-Sun, 30 diameter. Below them a muted line with the next streak reward (section 4.7.1). Mon-Sat are `accent` with a dark check. Sunday (today) is a 1.5 outlined ring until secured, then `accent`.
4. **Reward ready** (only when at least one earned reward has not been enjoyed): `surface` panel, title "Reward ready" and right note "{n} waiting", then up to 3 rows (64 high, `raised` fill, 14 radius): 22 gift icon in `text` colour, reward text (15/600, wraps), muted line "From {quest short title}", and an outlined **Enjoyed** pill (44 high) that marks it enjoyed. More than 3 = a muted line "+{n} more in Profile".
5. **Today's quests.** Header row "Today's quests" and `"{n} left · {xp} XP to earn"` (or "All cleared"). Two-column grid of quest tiles, 8 gap, min height 136. If the count is odd, the last tile spans both columns (min height 104).
   - Tile: glyph (22) top-left; `+{xp} XP` top-right (600, 13); category (12, secondary ink); short title (15/600); at the bottom one 4dp segment per step (filled = done).
   - Tap = select this quest and open **Quest detail**. The selected tile has a 2dp `text` outline offset 2dp outside.
   - When every quest is claimed show a dashed empty box: "Every quest cleared." / "Your streak is safe. New quests arrive at 6:00 tomorrow."

### 4.2 Quest detail

- Top row: 44 back button and "Quest".
- **Hero** (22 radius, quest colour field, dark text): glyph 28, serif title 40, story text 15, then `+{xp}` (32) and "XP when you finish".
- **Reward row** (directly under the hero, `surface`, 1px `border`, 16 radius, 12 margin-top): shows the user's own reward. States: *no reward yet* = bold "Add a reward", muted "Something small to look forward to when you finish." and a **Set reward** pill; *reward set* = muted "Your reward", the text (15/600) and an **Edit** pill; *earned, not enjoyed* = 1px `accent` border, muted "You earned", the text and an **Enjoyed it** pill; *enjoyed* = muted "Your reward", the text and "Enjoyed {date}"; *claimed without a reward* = no row. Behaviour in section 4.2.2.
- **Steps** header "Steps" and `"{n} of {total} done"`. Each step is a 56+ high row (`surface`, 1px `border`, 16 radius) with a 24 round check on the left. Unticked = 1.5 outline `#8C8C88`. Ticked = `accent` fill, dark check, text struck-through and muted. Tap toggles. Rows are disabled after claiming.
- **Remove quest** (last item in the scroll area, below the steps): an outlined pill, 48 high, `#FF9D8F` text and outline, label "Remove quest", with a muted line under it: "Takes it off today's list. Your XP and streak are not affected." For a completed quest the button is disabled and the line reads "Completed quests stay in today's history." Full behaviour in section 4.2.1.
- **Main button** (pinned above the tab bar, 56 high pill, `text` fill):
  - 0 steps done: **Start quest** (ticks the first step)
  - some done: **Mark next step done** (ticks the next unticked step)
  - all done: **Claim +{xp} XP**
  - claimed: **Completed · +{xp} XP earned** (disabled, `#2A2A28` fill, `#B4B4B0` text)

#### 4.2.1 Remove quest

1. Tap **Remove quest** opens a centred `alertdialog` over a 62% scrim: serif title `Remove "{short title}"?` (30), body "It comes off today's list and its step progress is cleared. {xp} XP is no longer on offer. You can undo this straight after." (15, `muted`), a 56 high pill **Remove quest** (fill `#FF9D8F`, label `#2A0A05`) and a text button **Keep it**.
2. **Focus starts on Keep it** (the safe choice). Focus is trapped between the two buttons; Escape/back and tapping the scrim = Keep it. Focus returns to the button that opened the dialog.
3. On **Remove quest**: archive the quest (section 10.2 `archived_at`), select the next unclaimed quest (else the first remaining one, else none), close the dialog, and show a toast with an **Undo** action: `Removed "{short title}".` The toast stays 8 seconds and **pauses while hovered or focused**. The Today phone gets a plain toast, `Removed "{short title}" from today.`
4. **Undo** restores the quest at its old position with its ticked steps, re-selects it and toasts `Restored "{short title}".` One level of undo is enough. If the quest came from the Library, its **Add** button goes back to enabled while it is removed and back to **Added** after Undo.
5. Only quests that are **not yet claimed** can be removed: claimed XP is already in the history and Stats, so removing it would make the numbers disagree.
6. If nothing is left, the detail screen shows an empty box ("No quest selected. Your list is empty. Add a quest with the + button on the Today screen, or pick a habit from the Library.") and hides the main button. Today shows "No quests on today's list. Add one with the + button, or pick a habit from the Library." with the header "Nothing planned".
7. Built-in quests and custom ones behave the same for the user. (In the prototype built-in quests are hidden and custom ones deleted; in the native app both are archived rows.)

#### 4.2.2 Rewards

1. **Set reward / Edit** opens a centred `dialog` (same card as the remove dialog) over the scrim: serif title "Your reward", body "What will you give yourself when you finish this quest? Keep it small and real.", a 48 high text field (max 40 characters, placeholder "e.g. An iced latte"), a row of idea chips (**A coffee out**, **One episode**, **30 min of games**, **A nice snack**; tapping one fills the field), the primary **Save reward**, a text **Remove reward** (only when one exists) and **Cancel**.
2. Save with an empty field shows "Write a reward, or cancel." (alert, field invalid) and keeps focus in the field. Trim the text. Saving toasts "Reward saved: {text}".
3. Only unclaimed quests can have their reward changed. Seed quests and custom quests behave the same.
4. **Claim** (section 4.3): if the quest has a reward, create the earned reward (`text` copied at that moment, so later edits never change it) and append " Reward ready: {text}." to the claim toast.
5. **Enjoyed / Enjoyed it** sets `enjoyedAt`, toasts "Enjoy it, {name}: {text}" and moves the reward to history. The Today panel disappears when nothing is waiting.
6. Removing a quest removes its reward text with it; **Undo** brings it back. Earned rewards are kept (completed quests cannot be removed anyway).
7. Rewards are the user's own words: escape them on display, never interpret them as markup.

### 4.3 Claim logic (must match exactly)

```
r0 = rating(); before = displayInfo()
claimed += quest; xp += quest.xp
msg = "+{xp} XP — {Category} quest complete."
if !securedToday { securedToday = true; msg += " Streak extended to {streak+1} days." }
while xp >= cap { xp -= cap; level += 1; cap = round(cap * 1.12 / 10) * 10; msg = "Level {level} reached! " + msg }
after = displayInfo()
if after.label != before.label { msg = "{Rank up|Level up}: {after.label}. " + msg; show RankUpScreen(before, after, rating() - r0) }
toast(msg)
```

### 4.4 Library tab

Title "Library", muted line "Habits worth starting. Add one and it lands in today's quests." A horizontally scrolling chip row: All plus six categories (44 high, pill, 1px `control` outline; selected = `text` fill with dark label). Below, a list of 68+ high rows (`surface`, 16 radius): category-tinted glyph (24), name (15/600), meta `"{Category} · {meta} · +{xp} XP"` (13 muted), and an **Add** pill (44 high). Add appends a custom-flagged quest to Today and the button becomes disabled **Added**. Toast `"Added to today: {name}."`

### 4.5 New quest sheet (the + button)

Modal bottom sheet, `surface`, 28 top radius, grab handle 40x4, serif title "New quest", scrim 62% black.

- **Quest name**: single line, max 40 characters, 48 high, 14 radius. **Validate on blur and on submit**, not while typing. Empty = show error text "Give your quest a name." (`error` colour, announced as an alert) and set the field invalid. Hide the error as soon as there is text. Never clear the field on error.
- **Category**: radio group of six chips (default Body).
- **XP**: radio group of three equal buttons, 56 high, two lines: Easy 30 XP, **Medium 60 XP (default)**, Hard 90 XP.
- **Your own reward (optional)**: label with a muted "(optional)", text field, max 40 characters, placeholder "e.g. An iced latte". Stored as the quest's reward text when not empty. The sheet scrolls if the keyboard leaves too little room.
- **Create quest** (primary pill) and **Cancel** (text button).
- Create: steps become `["Get started", "Finish: {name}"]`, story "Your own quest. Do the steps, tick them off, then claim the XP." Add to Today, select it, close the sheet, return to the Today tab, scroll the new tile into view, toast `"Quest created: {name} (+{xp} XP)."`
- Focus goes to the name field on open and back to the + button on close. Back/Escape closes it. Trap focus inside.

### 4.6 Stats tab

Title "Stats", line `"Week of 19 Oct · {weekTotal} XP so far"`.

1. **KPI strip** (one `surface` panel, three equal columns split by 1px `border` dividers): Streak `{streak}` days; Longest `max(13, streak)` days; Quests `{29 + claimedToday}` this week. Values 30 regular.
2. **XP per day**: seven bars Mon-Sun. Values `[310, 280, 420, 350, 190, 400, todayXp]` where `todayXp` = sum of XP claimed today. Y scale 0-500 with three faint gridlines (0, 250, 500). Past bars `#6A6A65`, today's bar `accent`. Value label above each bar (11), day letters below (12; today in `text`/600). Bars have 6dp top radius. Right of the panel title: `"Today {todayXp} XP"`. Accessible label: all seven values.
3. **Where the XP came from** (this week): rows sorted by XP descending, each name + `"{n} XP"` and a 4dp bar scaled to the largest. Largest bar `accent`, others `#D9D9D4`. Base values: Body 520, Read 380, Listen 250, Nourish 290, Mind 330, Watch 180 (sum 1,950 = sum of the six past days). Add each claimed quest's XP to its category.
4. **Last 5 weeks**: 35 dots (7 columns, Mon first), 26 diameter. Index 0 is Mon 21 Sep, index 34 is today (Sun 25 Oct). Missed indexes: `[2, 6, 10, 12, 20]` (indexes 21 to 33 are the 13-day streak). Index 34 is on only when secured. Dots: on = `accent`, missed = `#2E2E2E`, today open = outlined ring. Right of title: `"{doneDays} of {34 or 35} days"`.

### 4.7 Profile tab

Title "Profile", line `"Sam · Level {level} · {rating} rating"`.

1. **Show my level as** (panel): radio chips Level, Dota 2, MLBB, LoL, Valorant. Foot note: "Display only. Your level and XP stay the same. Every level you clear is worth 140 rating, every quest you complete today adds 35, and each game's ladder is mapped onto that." Changing it shows a toast and updates Today, Profile and the cover.
2. **Rank card**: muted "Your {Game} rank" (or "Your level"), serif rank name 44, figure + unit and right-aligned `"Level {level} · {rating} rating"`, the bar, and the foot line.
3. **Ladder** (hidden in Level style): one row per tier with a dot, name and the rating range (`"1,540–2,309"`, last tier `"5,420+"`). Passed tiers = `accent` dot, current tier = `raised` row, 600 weight, `accent` dot with a 2dp `text` ring, `aria-current`. Upcoming = `muted`, `track` dot.

4. **My rewards**: title and a right note "{n} ready · {m} enjoyed" (or "None yet"); a list of earned-but-not-enjoyed rewards first (with **Enjoyed** pills, same row as Today), then up to 5 enjoyed ones as quiet history rows (no fill, muted, "Enjoyed {date}"). Empty state: "No rewards yet. Set one on any quest, finish it, and it shows up here."
5. **Milestone rewards**: streak, next level-up and rank rewards (medium and big). Full spec in section 4.7.1.
6. **Backup panel** (below the milestone rewards): Back up now, Restore from file, last-backup status, confirmation and error states. Full spec in section 10.5.

#### 4.7.1 Milestone rewards (medium and big)

Three sizes of reward, all written by the user. **Small** rewards sit on quests (section 4.2.2). **Medium** rewards wait for a streak milestone. **Big** rewards wait for the next level-up or for a rank tier. All of them end up in the same "Reward ready" and "My rewards" lists, labelled "Medium reward · 14-day streak", "Big reward · Reached Level 15" or "Big reward · Reached Mobile Legends Epic".

**Panel:** `surface`, title "Milestone rewards", right note "Medium and big", muted line "Small rewards go on quests. Bigger ones wait for a streak, a level-up or a rank." Then three groups, each with a 13/600 heading and rows (60+ high, `raised` fill, 16 radius): title (15/600), muted line, and an outlined **Set** / **Edit** pill (44 high).

| Group | Rows | Row sub-line | Earned when |
| --- | --- | --- | --- |
| **Streak · medium reward** | 7-day, 14-day, 30-day streak | `{reward or "No reward set"} · in {n} day(s)`; once reached: `Earned: {text}` or `Reached` | The claim that makes the streak equal the milestone (a streak only grows by one a day, so the match is exact) |
| **Next level-up · big reward** | One row: `Level {level+1}` | `{reward or "No reward set"} · {xp to go} XP to go` | The claim that raises the level. The slot is **cleared after it pays out**; the row then names the following level so the user can set a new one |
| **Rank · big reward** | Dota 2: Archon, Legend. Mobile Legends: Epic, Legend | `{reward or "No reward set"} · {rating to go} rating to go`; once reached: `Earned: {text}` or `Reached before a reward was set` | The claim that takes the rating to the tier's floor (see the rank tables in section 5) **while that game is the selected rank style** |

Rank rows only show for Dota 2 and Mobile Legends. In Level, League or Valorant style the group shows the muted note "Rank rewards are for Dota 2 and Mobile Legends. Pick one above to set them." Dota 2 has no Epic tier, so Archon (the tier just below Legend, the same position Epic has in Mobile Legends) stands in for it; Mobile Legends uses Epic and Legend. Rewards are stored per game, so switching style hides the other game's slots but keeps their text.

**Set / Edit dialog** (same card as the quest reward dialog, section 4.2.2): title `14-day streak reward`, `Level 15 reward` or `Epic reward`; body for streak "What will you give yourself when you hold a 14-day streak? A medium treat.", for level "What will you give yourself when you reach Level 15? Make it a big one. It applies to this level-up only.", for rank "What will you give yourself when you reach Epic in Mobile Legends? Make it a big one."; one 48 high text field (max 40 characters); idea chips (medium: A nice dinner out, A new book, A day trip, A spa evening; big: New headphones, A weekend away, Something I've wanted, A big night out); **Save reward**, **Remove reward** (when set), **Cancel**. Same validation as quest rewards ("Write a reward, or cancel."). A milestone that has already been reached has no Set button.

**Hints on Today:** under the week dots, a muted line "Next reward at 14 days: {text}" (nearest unreached streak milestone that has a reward). Under the level bar, a muted line with the nearest big reward: in Dota 2 or Mobile Legends the nearest unreached rank tier with a reward ("Epic reward: {text}"), otherwise "Level {level+1} reward: {text}". Hide the line when there is nothing to show.

**On claim (evaluate in this order, inside the claim transaction):** streak milestone (only if this claim secured today's streak), then level-up (first level gained), then rank tiers for the selected game. Each match creates one earned reward (never twice for the same milestone) and the claim toast ends with "Reward earned: {text}". If several fire, show them one after another.

### 4.8 Rank-up screen

Full-screen over the whole app (including the tab bar), background `accent`, text `accentInk`.
Layout (top padding 64, side 22): kicker "Rank up" or "Level up" (16/600); tier name in serif 60; huge numeral in serif 250 (division/star, or the new level in Level style); line `"From {before.label} to {after.label}."` (16, `#2F3D00`); at the bottom two facts split by a 1.5px `accentInk` rule: `{rating}` "Rating, +{gain} from this quest", and `{n}` "quests done today"; then a dark pill **Continue** (`#141A00` fill, accent label) and a text button **See my ladder** (closes and opens Profile).
Alert-dialog semantics; focus starts on Continue; focus is trapped between the two buttons; back/Escape = Continue. The numeral entrance plays once.

### 4.8.1 Reward-earned screen (streak, level-up, rank)

Shown after a claim earns a medium or big reward. Same lime field and layout as the rank-up screen (section 4.8), so build them from one component. Order when several appear: rank-up first, then each earned reward, one per screen; **Continue** moves to the next.

- Kicker "Medium reward" or "Big reward" (16/600).
- Huge serif numeral (250, 176 if 3+ characters): the streak day count, the new level, or for a rank reward the tier name set as a word at 104.
- Line (16, `#2F3D00`): `{from label}. You earned this one.` (for example "14-day streak. You earned this one.").
- The reward text in serif 46, wraps up to three lines, anchored above the buttons.
- Buttons: **Continue** (dark pill) and **See my rewards** (text; closes and opens Profile).
- Alert-dialog semantics, focus on Continue, focus trapped between the two buttons, Escape = Continue, entrance animation once.

### 4.9 Cover (optional splash)

Huge serif numeral on the left with the tier word (or "Level") rotated 90 degrees beside it, a thin XP bar and a caption row under it, then six full-width quest rows (64 high, quest colours) cropped by the tab bar. In the prototype the numeral, bar and caption follow the chosen rank style. Skip this on native unless you want an onboarding/home hero.

### 4.10 Toast

One toast per screen, pill-ish 14 radius, `text` fill with dark text, 13/500, 16 side inset, above the tab bar. Live region (polite). Auto-dismiss 3.2 s.

---

## 5. Rank engine (single source of truth)

One internal number drives every style:

```
rating = round((level - 1) * 140 + xp / cap * 140 + claimedQuestCountToday * 35)
```

At the seed state rating = 1,913. Each style has ordered tiers `(name, lowRating, divisions)`. `divisions = 0` marks the top tier (no divisions).

| Style | Unit | Tiers (name, low, divisions) |
| --- | --- | --- |
| Dota 2 | MMR | Herald 0/5, Guardian 770/5, Crusader 1540/5, Archon 2310/5, Legend 3080/5, Ancient 3850/5, Divine 4620/5, Immortal 5420/0 |
| Mobile Legends | stars | Warrior 0/3, Elite 400/4, Master 900/4, Grandmaster 1500/5, Epic 2300/5, Legend 3300/5, Mythic 4500/0 |
| League of Legends | LP | Iron 0/4, Bronze 600/4, Silver 1200/4, Gold 1900/4, Platinum 2600/4, Emerald 3300/4, Diamond 4000/4, Master 4800/0 |
| Valorant | RR | Iron 0/3, Bronze 450/3, Silver 900/3, Gold 1400/3, Platinum 1900/3, Diamond 2500/3, Ascendant 3200/3, Immortal 4000/3, Radiant 5000/0 |

Algorithm:

```
ti      = last tier whose low <= rating
if divisions == 0: top tier -> label = name, numeral "I", progress = 1, foot "Top tier. Keep climbing."
hi      = next tier's low
w       = (hi - low) / divisions
di      = min(divisions - 1, floor((rating - low) / w))      // 0-based, lowest division first
dLow    = low + di * w
p       = (rating - dLow) / w                                 // bar fill 0..1
toNext  = max(1, ceil(dLow + w - rating))
nextLabel = next division, or the first division of the next tier

label / numeral:
  Dota      "{Tier} · star {di+1}"          numeral "{di+1}"
  Valorant  "{Tier} {di+1}"                 numeral "{di+1}"
  Mlbb, Lol "{Tier} {roman(divisions - di)}"  numeral roman(divisions - di)   // V..I counts down

figure shown beside the bar:
  Dota      rating, unit "MMR"
  Mlbb      "{min(2, floor(p*3)) + 1} of 3", unit "stars"
  Lol       min(99, floor(p*100)), unit "LP"
  Valorant  min(99, floor(p*100)), unit "RR"
foot: "{toNext} {MMR for Dota, otherwise rating} to {nextLabel}"
```

Check values at the seed state: Dota "Crusader · star 3" (1,913 MMR); MLBB "Grandmaster III"; LoL "Gold IV"; Valorant "Platinum 1".
These are rank *names* only. Do not use the games' logos or badge art.

---

## 6. Android (Kotlin, Compose, Material 3)

### Structure

```
app/
  ui/theme/    Color.kt, Type.kt, Shape.kt, Theme.kt
  ui/components/ QuestTile.kt, LevelPanel.kt, StreakPanel.kt, StepRow.kt, Chip.kt, Toast.kt, XpBar.kt
  ui/screens/  TodayScreen.kt, QuestDetailScreen.kt, LibraryScreen.kt, StatsScreen.kt, ProfileScreen.kt,
               NewQuestSheet.kt, RankUpScreen.kt
  domain/      Models.kt, RankEngine.kt, Reducer.kt
  data/        SeedData.kt (quests, library, stats constants)
  MainViewModel.kt
```

- **Navigation:** `Scaffold` with a custom bottom bar (4 destinations plus the centre +). `NavHost` with routes `today`, `library`, `stats`, `profile`, `quest/{id}`. The + opens `ModalBottomSheet`. The rank-up screen is a `Dialog(properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false))` filling the screen.
- **State:** one `MainViewModel` exposing `StateFlow<AppState>`; actions: `toggleStep`, `advanceQuest`, `claim`, `addFromLibrary`, `createQuest`, `setRankStyle`, `reset`. Unit-test the reducer and the rank engine.
- **Dark only:** set `darkColorScheme` from the tokens; edge-to-edge; status bar icons light.
- **Touch targets:** 48dp minimum (the prototype's 44 is the floor).
- **Accessibility:** the step rows use `Modifier.toggleable(role = Role.Checkbox)`; progress bars use `semantics { progressBarRangeInfo }`; the toast uses `LiveRegionMode.Polite`; the error text uses `LiveRegionMode.Assertive`; the XP chart has a `contentDescription` listing all seven values.

### Tokens (Color.kt, Type.kt)

```kotlin
object Dq {
    val Ground = Color(0xFF111111); val Surface = Color(0xFF1B1B1B); val Raised = Color(0xFF242424)
    val Border = Color(0xFF2E2E2E); val Control = Color(0xFF444444); val Track = Color(0xFF3A3A37)
    val Text = Color(0xFFF5F5F2);   val Muted = Color(0xFFA9A9A4)
    val Accent = Color(0xFFBAED3F); val AccentInk = Color(0xFF141A00)
}

enum class Category(val fill: Color, val ink: Color, val secondary: Color, val pressed: Color, val glyph: Int) {
    Body(Color(0xFFFF7E76), Color(0xFF240706), Color(0xFF3C1715), Color(0xFFFF8E85), R.drawable.ic_body),
    Read(Color(0xFFFAC131), Color(0xFF1B0F00), Color(0xFF312100), Color(0xFFFFD147), R.drawable.ic_read),
    Listen(Color(0xFF0ABFFF), Color(0xFF001424), Color(0xFF00283D), Color(0xFF32D0FF), R.drawable.ic_listen),
    Nourish(Color(0xFF66DA85), Color(0xFF001705), Color(0xFF092C13), Color(0xFF77EB94), R.drawable.ic_nourish),
    Mind(Color(0xFFAD8DFD), Color(0xFF140C25), Color(0xFF261D3E), Color(0xFFBC9DFF), R.drawable.ic_mind),
    Watch(Color(0xFFFB83BF), Color(0xFF210715), Color(0xFF391628), Color(0xFFFF93CF), R.drawable.ic_watch),
}

val Serif = FontFamily(Font(R.font.instrument_serif))
val Sans = FontFamily(Font(R.font.instrument_sans_regular), Font(R.font.instrument_sans_medium, FontWeight.Medium), Font(R.font.instrument_sans_semibold, FontWeight.SemiBold))
```

### Rank engine

```kotlin
enum class RankStyle(val label: String, val unit: String, val tiers: List<Tier>) {
    Level("Level", "", emptyList()),
    Dota("Dota 2", "MMR", listOf(Tier("Herald",0,5),Tier("Guardian",770,5),Tier("Crusader",1540,5),Tier("Archon",2310,5),
        Tier("Legend",3080,5),Tier("Ancient",3850,5),Tier("Divine",4620,5),Tier("Immortal",5420,0))),
    Mlbb("Mobile Legends", "stars", listOf(Tier("Warrior",0,3),Tier("Elite",400,4),Tier("Master",900,4),Tier("Grandmaster",1500,5),
        Tier("Epic",2300,5),Tier("Legend",3300,5),Tier("Mythic",4500,0))),
    Lol("League of Legends", "LP", listOf(Tier("Iron",0,4),Tier("Bronze",600,4),Tier("Silver",1200,4),Tier("Gold",1900,4),
        Tier("Platinum",2600,4),Tier("Emerald",3300,4),Tier("Diamond",4000,4),Tier("Master",4800,0))),
    Valorant("Valorant", "RR", listOf(Tier("Iron",0,3),Tier("Bronze",450,3),Tier("Silver",900,3),Tier("Gold",1400,3),
        Tier("Platinum",1900,3),Tier("Diamond",2500,3),Tier("Ascendant",3200,3),Tier("Immortal",4000,3),Tier("Radiant",5000,0)));
}
data class Tier(val name: String, val low: Int, val divisions: Int)

data class RankInfo(val label: String, val tier: String, val numeral: String, val figure: String,
                    val unit: String, val progress: Float, val foot: String, val tierIndex: Int)

private val ROMAN = listOf("", "I", "II", "III", "IV", "V")

fun rating(s: AppState): Int =
    Math.round((s.level - 1) * 140f + s.xp.toFloat() / s.cap * 140f + s.claimed.size * 35f)

private fun divLabel(style: RankStyle, ti: Int, di: Int): String {
    val t = style.tiers[ti]; val n = t.divisions
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
    val r = rating(s); val tiers = style.tiers
    var ti = 0
    while (ti + 1 < tiers.size && r >= tiers[ti + 1].low) ti++
    val t = tiers[ti]
    if (t.divisions == 0)
        return RankInfo(t.name, t.name, "I", "%,d".format(r - t.low),
            "${if (style == RankStyle.Dota) "MMR" else "rating"} over ${"%,d".format(t.low)}", 1f, "Top tier. Keep climbing.", ti)
    val w = (tiers[ti + 1].low - t.low).toFloat() / t.divisions
    val di = minOf(t.divisions - 1, ((r - t.low) / w).toInt())
    val dLow = t.low + di * w
    val p = (r - dLow) / w
    val toNext = maxOf(1, Math.ceil((dLow + w - r).toDouble()).toInt())
    val (nti, ndi) = if (di + 1 < t.divisions) ti to di + 1 else ti + 1 to 0
    val unitWord = if (style == RankStyle.Dota) "MMR" else "rating"
    val figure = when (style) {
        RankStyle.Dota -> "%,d".format(r)
        RankStyle.Mlbb -> "${minOf(2, (p * 3).toInt()) + 1} of 3"
        else -> minOf(99, (p * 100).toInt()).toString()
    }
    val numeral = if (style == RankStyle.Dota || style == RankStyle.Valorant) "${di + 1}" else ROMAN[t.divisions - di]
    return RankInfo(divLabel(style, ti, di), t.name, numeral, figure, style.unit, p,
        "%,d $unitWord to ${divLabel(style, nti, ndi)}".format(toNext), ti)
}
```

(`Math.round` and `%,d` behave like the prototype for these ranges; the tests in section 9 cover the four seed values.)

### Composables to build first

```kotlin
@Composable
fun QuestTile(q: Quest, done: Boolean, stepsDone: List<Boolean>, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = q.category
    val bg = if (done) Dq.Surface else c.fill
    val ink = if (done) Dq.Muted else c.ink
    Column(
        modifier.heightIn(min = 136.dp).clip(RoundedCornerShape(22.dp)).background(bg)
            .then(if (selected) Modifier.border(2.dp, Dq.Text, RoundedCornerShape(22.dp)) else Modifier)
            .clickable(onClick = onClick).padding(14.dp)
            .semantics { contentDescription = "${c.name}, ${q.shortTitle}, ${if (done) "completed" else "${q.xp} XP"}" }
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Icon(painterResource(if (done) R.drawable.ic_check else c.glyph), null, tint = if (done) Dq.Accent else ink, modifier = Modifier.size(22.dp))
            Text(if (done) "Done" else "+${q.xp} XP", color = ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(c.name, color = if (done) Dq.Muted else c.secondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(q.shortTitle, color = ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            textDecoration = if (done) TextDecoration.LineThrough else null)
        Spacer(Modifier.weight(1f))
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            stepsDone.forEach { on -> Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp))
                .background(if (on) ink else ink.copy(alpha = 0.22f))) }
        }
    }
}
```

Build the rest the same way: `LevelPanel` (title, big figure, `XpBar` with `animateFloatAsState(tween(500, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))`), `StreakPanel`, `StepRow`, `Chip`, `StatsChart` (draw with `Canvas`, no chart library), `RankUpScreen`.

---

## 7. iOS (Swift, SwiftUI)

### Structure

```
Dayquest/
  Theme/        Tokens.swift (Color, Font), Category.swift, DqIcon.swift
  Domain/       Models.swift, RankEngine.swift, AppStore.swift  (@Observable)
  Views/        RootView.swift, TodayView.swift, QuestDetailView.swift, LibraryView.swift,
                StatsView.swift, ProfileView.swift, NewQuestSheet.swift, RankUpView.swift
  Components/   QuestTile.swift, LevelPanel.swift, StreakPanel.swift, StepRow.swift, XpBar.swift, ToastView.swift
  Resources/    InstrumentSans-*.ttf, InstrumentSerif-Regular.ttf   (icons are code, see section 8.2)
```

- **Navigation:** a `ZStack` root holding the current tab view in a `NavigationStack` plus a custom bottom bar (system `TabView` cannot host the centre + button cleanly). Quest detail is `navigationDestination(for: QuestID.self)`. New quest = `.sheet` with `.presentationDetents([.fraction(0.82)])`, `.presentationCornerRadius(28)`. Rank-up = `.fullScreenCover`.
- **State:** a single `@Observable final class AppStore` injected with `.environment(store)`. Actions mirror the Android list. Persist `rankStyle` and quest progress with `@AppStorage` / SwiftData once the prototype behaviour is matched.
- **Dark only:** `.preferredColorScheme(.dark)`; hide the system tab bar; respect safe areas.
- **Hit targets:** 44pt minimum. Use `.contentShape(Rectangle())` on tile and row buttons.
- **Accessibility:** step rows use `.accessibilityAddTraits(.isButton)` plus `.accessibilityValue(done ? "done" : "not done")`; XP bars use `.accessibilityValue("66 percent")`; announce the toast with `AccessibilityNotification.Announcement`; the rank-up cover uses `.accessibilityAddTraits(.isModal)`. Respect `@Environment(\.accessibilityReduceMotion)`.

### Tokens

```swift
extension Color {
    static let dqGround  = Color(hex: 0x111111), dqSurface = Color(hex: 0x1B1B1B), dqRaised = Color(hex: 0x242424)
    static let dqBorder  = Color(hex: 0x2E2E2E), dqControl = Color(hex: 0x444444), dqTrack = Color(hex: 0x3A3A37)
    static let dqText    = Color(hex: 0xF5F5F2), dqMuted   = Color(hex: 0xA9A9A4)
    static let dqAccent  = Color(hex: 0xBAED3F), dqAccentInk = Color(hex: 0x141A00)
    init(hex: UInt32) { self.init(red: Double((hex >> 16) & 255)/255, green: Double((hex >> 8) & 255)/255, blue: Double(hex & 255)/255) }
}

enum Category: String, CaseIterable, Codable {
    case body = "Body", read = "Read", listen = "Listen", nourish = "Nourish", mind = "Mind", watch = "Watch"
    var fill: Color      { [ .body: 0xFF7E76, .read: 0xFAC131, .listen: 0x0ABFFF, .nourish: 0x66DA85, .mind: 0xAD8DFD, .watch: 0xFB83BF ][self].map(Color.init(hex:))! }
    var ink: Color       { [ .body: 0x240706, .read: 0x1B0F00, .listen: 0x001424, .nourish: 0x001705, .mind: 0x140C25, .watch: 0x210715 ][self].map(Color.init(hex:))! }
    var secondary: Color { [ .body: 0x3C1715, .read: 0x312100, .listen: 0x00283D, .nourish: 0x092C13, .mind: 0x261D3E, .watch: 0x391628 ][self].map(Color.init(hex:))! }
}

extension Font {
    static func serif(_ size: CGFloat) -> Font { .custom("InstrumentSerif-Regular", size: size) }
    static func sans(_ size: CGFloat, _ weight: Font.Weight = .regular) -> Font {
        .custom(weight == .semibold ? "InstrumentSans-SemiBold" : weight == .medium ? "InstrumentSans-Medium" : "InstrumentSans-Regular", size: size)
    }
}
```

### Rank engine

```swift
struct Tier { let name: String; let low: Int; let divisions: Int }

enum RankStyle: String, CaseIterable, Codable {
    case level, dota, mlbb, lol, valorant
    var title: String { ["level": "Level", "dota": "Dota 2", "mlbb": "Mobile Legends", "lol": "League of Legends", "valorant": "Valorant"][rawValue]! }
    var chipTitle: String { ["level": "Level", "dota": "Dota 2", "mlbb": "MLBB", "lol": "LoL", "valorant": "Valorant"][rawValue]! }
    var unit: String { ["level": "", "dota": "MMR", "mlbb": "stars", "lol": "LP", "valorant": "RR"][rawValue]! }
    var tiers: [Tier] {
        switch self {
        case .level: return []
        case .dota: return [Tier(name:"Herald",low:0,divisions:5), Tier(name:"Guardian",low:770,divisions:5), Tier(name:"Crusader",low:1540,divisions:5),
            Tier(name:"Archon",low:2310,divisions:5), Tier(name:"Legend",low:3080,divisions:5), Tier(name:"Ancient",low:3850,divisions:5),
            Tier(name:"Divine",low:4620,divisions:5), Tier(name:"Immortal",low:5420,divisions:0)]
        case .mlbb: return [Tier(name:"Warrior",low:0,divisions:3), Tier(name:"Elite",low:400,divisions:4), Tier(name:"Master",low:900,divisions:4),
            Tier(name:"Grandmaster",low:1500,divisions:5), Tier(name:"Epic",low:2300,divisions:5), Tier(name:"Legend",low:3300,divisions:5),
            Tier(name:"Mythic",low:4500,divisions:0)]
        case .lol: return [Tier(name:"Iron",low:0,divisions:4), Tier(name:"Bronze",low:600,divisions:4), Tier(name:"Silver",low:1200,divisions:4),
            Tier(name:"Gold",low:1900,divisions:4), Tier(name:"Platinum",low:2600,divisions:4), Tier(name:"Emerald",low:3300,divisions:4),
            Tier(name:"Diamond",low:4000,divisions:4), Tier(name:"Master",low:4800,divisions:0)]
        case .valorant: return [Tier(name:"Iron",low:0,divisions:3), Tier(name:"Bronze",low:450,divisions:3), Tier(name:"Silver",low:900,divisions:3),
            Tier(name:"Gold",low:1400,divisions:3), Tier(name:"Platinum",low:1900,divisions:3), Tier(name:"Diamond",low:2500,divisions:3),
            Tier(name:"Ascendant",low:3200,divisions:3), Tier(name:"Immortal",low:4000,divisions:3), Tier(name:"Radiant",low:5000,divisions:0)]
        }
    }
}

struct RankInfo { let label, tier, numeral, figure, unit: String; let progress: Double; let foot: String; let tierIndex: Int }

private let roman = ["", "I", "II", "III", "IV", "V"]

extension AppStore {
    var rating: Int { Int(((Double(level - 1) * 140) + Double(xp) / Double(cap) * 140 + Double(claimed.count) * 35).rounded()) }

    private func divLabel(_ s: RankStyle, _ ti: Int, _ di: Int) -> String {
        let t = s.tiers[ti]; let n = t.divisions
        if n == 0 { return t.name }
        switch s {
        case .dota: return "\(t.name) · star \(di + 1)"
        case .valorant: return "\(t.name) \(di + 1)"
        default: return "\(t.name) \(roman[n - di])"
        }
    }

    var rankInfo: RankInfo? {
        guard rankStyle != .level else { return nil }
        let r = rating, tiers = rankStyle.tiers
        var ti = 0
        while ti + 1 < tiers.count && r >= tiers[ti + 1].low { ti += 1 }
        let t = tiers[ti]
        if t.divisions == 0 {
            return RankInfo(label: t.name, tier: t.name, numeral: "I", figure: (r - t.low).formatted(),
                            unit: "\(rankStyle == .dota ? "MMR" : "rating") over \(t.low.formatted())",
                            progress: 1, foot: "Top tier. Keep climbing.", tierIndex: ti)
        }
        let w = Double(tiers[ti + 1].low - t.low) / Double(t.divisions)
        let di = min(t.divisions - 1, Int(Double(r - t.low) / w))
        let dLow = Double(t.low) + Double(di) * w
        let p = (Double(r) - dLow) / w
        let toNext = max(1, Int((dLow + w - Double(r)).rounded(.up)))
        let (nti, ndi) = di + 1 < t.divisions ? (ti, di + 1) : (ti + 1, 0)
        let word = rankStyle == .dota ? "MMR" : "rating"
        let figure: String = {
            switch rankStyle {
            case .dota: return r.formatted()
            case .mlbb: return "\(min(2, Int(p * 3)) + 1) of 3"
            default: return String(min(99, Int(p * 100)))
            }
        }()
        let numeral = (rankStyle == .dota || rankStyle == .valorant) ? "\(di + 1)" : roman[t.divisions - di]
        return RankInfo(label: divLabel(rankStyle, ti, di), tier: t.name, numeral: numeral, figure: figure, unit: rankStyle.unit,
                        progress: p, foot: "\(toNext.formatted()) \(word) to \(divLabel(rankStyle, nti, ndi))", tierIndex: ti)
    }
}
```

### Core views to build first

```swift
struct QuestTile: View {
    let quest: Quest; let done: Bool; let stepsDone: [Bool]; let selected: Bool
    var body: some View {
        let c = quest.category
        let ink = done ? Color.dqMuted : c.ink
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                DqIconView(icon: done ? .check : DqIcon.glyph(for: c), size: 22)
                    .foregroundStyle(done ? Color.dqAccent : ink)
                Spacer()
                Text(done ? "Done" : "+\(quest.xp) XP").font(.sans(13, .semibold))
            }
            Text(c.rawValue).font(.sans(12, .medium)).foregroundStyle(done ? Color.dqMuted : c.secondary).padding(.top, 12)
            Text(quest.shortTitle).font(.sans(15, .semibold)).strikethrough(done)
            Spacer(minLength: 10)
            HStack(spacing: 3) {
                ForEach(stepsDone.indices, id: \.self) { i in
                    Capsule().fill(stepsDone[i] ? ink : ink.opacity(0.22)).frame(height: 4)
                }
            }
        }
        .foregroundStyle(ink)
        .padding(14)
        .frame(maxWidth: .infinity, minHeight: 136, alignment: .topLeading)
        .background(done ? Color.dqSurface : c.fill, in: RoundedRectangle(cornerRadius: 22))
        .overlay { if selected { RoundedRectangle(cornerRadius: 22).stroke(Color.dqText, lineWidth: 2) } }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(c.rawValue), \(quest.shortTitle), \(done ? "completed" : "\(quest.xp) XP")")
    }
}
```

The XP bar: `GeometryReader` + `Capsule`, animate with `.animation(.timingCurve(0.2, 0, 0, 1, duration: 0.5), value: progress)`. The stats chart: use SwiftUI `Chart` (Swift Charts) with `BarMark`; colour today's bar `.dqAccent` and the others `Color(hex: 0x6A6A65)`; hide the Y axis line, keep three faint gridlines at 0/250/500.

---

## 8. Icons (exact paths)

All 15 icons are drawn on a **24 x 24** grid as **round-capped, round-joined strokes with no fill**. Stroke width differs per icon (listed below). Tint them with the text colour at the call site: `muted` for inactive tabs, `text` for active tabs, the quest ink colour on tiles. Sizes used: 22 in the tab bar, tiles and rows, 24 in the library list, 28 in the quest hero, 20 for the bell, 22 for Back.

| Icon | Name | Stroke | Used for |
| --- | --- | --- | --- |
| `i-today` | Today | 1.8 | Today tab |
| `i-lib` | Library | 1.8 | Library tab |
| `i-stats` | Stats | 1.8 | Stats tab |
| `i-plus` | Plus | 2.2 | Centre + button |
| `i-user` | Profile | 1.8 | Profile tab |
| `i-bell` | Bell | 1.8 | Notifications |
| `i-back` | Back | 2 | Quest detail back |
| `i-check` | Check | 3 | Ticked step, done tile, streak dot |
| `i-gift` | Gift | 1.8 | Rewards (ready panel, rewards list) |
| `g-body` | Body | 1.8 | Body quests |
| `g-read` | Read | 1.8 | Read quests |
| `g-listen` | Listen | 1.8 | Listen quests |
| `g-nourish` | Nourish | 1.8 | Nourish quests |
| `g-mind` | Mind | 1.8 | Mind quests |
| `g-watch` | Watch | 1.8 | Watch quests |

Each rect and circle in the source art has been converted to path data so every icon is one path. Where the original used arcs, the Android version keeps the arcs (VectorDrawable supports `A`/`a`); the Swift version converts them to cubic curves because SwiftUI `Path` has no SVG arc command.

### 8.1 Android: VectorDrawable

One file per icon in `res/drawable/`. Names: `ic_today.xml`, `ic_library.xml`, `ic_stats.xml`, `ic_plus.xml`, `ic_profile.xml`, `ic_bell.xml`, `ic_back.xml`, `ic_check.xml`, `ic_gift.xml`, `ic_body.xml`, `ic_read.xml`, `ic_listen.xml`, `ic_nourish.xml`, `ic_mind.xml`, `ic_watch.xml`. The stroke colour in the file is a placeholder; always tint at the call site with `Icon(tint = ...)`.

`ic_today.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:pathData="M7.5 5H16.5A4 4 0 0 1 20.5 9V16A4 4 0 0 1 16.5 20H7.5A4 4 0 0 1 3.5 16V9A4 4 0 0 1 7.5 5Z M8 3v4M16 3v4M8.5 13l2.5 2.5 4.5-5"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="1.8"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

`ic_library.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:pathData="M5 4h5a2 2 0 0 1 2 2v14a2 2 0 0 0-2-2H5zM19 4h-5a2 2 0 0 0-2 2v14a2 2 0 0 1 2-2h5z"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="1.8"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

`ic_stats.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:pathData="M5 20V12M12 20V5M19 20v-5"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="1.8"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

`ic_plus.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:pathData="M12 5v14M5 12h14"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="2.2"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

`ic_profile.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:pathData="M8 8A4 4 0 1 0 16 8A4 4 0 1 0 8 8Z M4.5 20a7.5 7.5 0 0 1 15 0"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="1.8"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

`ic_bell.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:pathData="M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15zM10 21h4"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="1.8"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

`ic_back.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:pathData="M15 5l-7 7 7 7"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="2"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

`ic_check.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:pathData="M5 12.5l4.5 4.5L19 7.5"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="3"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

`ic_gift.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:pathData="M6 9H18A2.5 2.5 0 0 1 20.5 11.5V17.5A2.5 2.5 0 0 1 18 20H6A2.5 2.5 0 0 1 3.5 17.5V11.5A2.5 2.5 0 0 1 6 9Z M12 9V20 M3.5 14H20.5 M12 9C10 9 7.5 8.2 7.5 6.5C7.5 4.8 9.5 4 11 5.2C11.8 5.9 12 7.5 12 9Z M12 9C14 9 16.5 8.2 16.5 6.5C16.5 4.8 14.5 4 13 5.2C12.2 5.9 12 7.5 12 9Z"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="1.8"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

`ic_body.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:pathData="M3 9v6M6.5 6.5v11M17.5 6.5v11M21 9v6M6.5 12h11"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="1.8"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

`ic_read.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:pathData="M4 5.5C6.5 4.5 9.5 4.5 12 6.5c2.5-2 5.5-2 8-1v12c-2.5-1-5.5-1-8 1-2.5-2-5.5-2-8-1z M12 6.5v12"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="1.8"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

`ic_listen.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:pathData="M4 15v-3a8 8 0 0 1 16 0v3 M5 14H6A1.5 1.5 0 0 1 7.5 15.5V18.5A1.5 1.5 0 0 1 6 20H5A1.5 1.5 0 0 1 3.5 18.5V15.5A1.5 1.5 0 0 1 5 14Z M18 14H19A1.5 1.5 0 0 1 20.5 15.5V18.5A1.5 1.5 0 0 1 19 20H18A1.5 1.5 0 0 1 16.5 18.5V15.5A1.5 1.5 0 0 1 18 14Z"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="1.8"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

`ic_nourish.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:pathData="M5 20C5 11 10 5 20 4c0 10-5 16-15 16zM5 20c3-5 6-8 10-10"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="1.8"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

`ic_mind.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:pathData="M10 12A2 2 0 1 0 14 12A2 2 0 1 0 10 12Z M12 8.5a3.5 3.5 0 1 1 3.5 3.5M12 4a8 8 0 1 1-8 8"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="1.8"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

`ic_watch.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp"
    android:viewportWidth="24" android:viewportHeight="24">
    <path
        android:pathData="M7 5H17A4 4 0 0 1 21 9V15A4 4 0 0 1 17 19H7A4 4 0 0 1 3 15V9A4 4 0 0 1 7 5Z M10.5 9.5v5l4-2.5z"
        android:strokeColor="#FFFFFF"
        android:strokeWidth="1.8"
        android:strokeLineCap="round"
        android:strokeLineJoin="round" />
</vector>
```

### 8.2 iOS: SwiftUI Path

Paste this file as `Theme/DqIcon.swift`. Coordinates are on the 24 x 24 grid and are scaled to whatever frame the icon is given. Colour the icon with `.foregroundStyle(...)`.

```swift
import SwiftUI

enum DqIcon: CaseIterable {
    case today, library, stats, plus, profile, bell, back, check, gift
    case body, read, listen, nourish, mind, watch

    /// Stroke width on the 24-unit grid.
    var strokeWidth: CGFloat {
        switch self {
        case .plus: return 2.2
        case .back: return 2
        case .check: return 3
        default: return 1.8
        }
    }

    /// Glyph for a quest category.
    static func glyph(for category: Category) -> DqIcon {
        switch category {
        case .body: return .body
        case .read: return .read
        case .listen: return .listen
        case .nourish: return .nourish
        case .mind: return .mind
        case .watch: return .watch
        }
    }
}

struct DqIconShape: Shape {
    let icon: DqIcon

    func path(in rect: CGRect) -> Path {
        let s = min(rect.width, rect.height) / 24
        func P(_ x: CGFloat, _ y: CGFloat) -> CGPoint { CGPoint(x: rect.minX + x * s, y: rect.minY + y * s) }
        var p = Path()
        switch icon {
        case .today:
            p.move(to: P(7.5, 5))
            p.addLine(to: P(16.5, 5))
            p.addCurve(to: P(20.5, 9), control1: P(18.709, 5), control2: P(20.5, 6.791))
            p.addLine(to: P(20.5, 16))
            p.addCurve(to: P(16.5, 20), control1: P(20.5, 18.209), control2: P(18.709, 20))
            p.addLine(to: P(7.5, 20))
            p.addCurve(to: P(3.5, 16), control1: P(5.291, 20), control2: P(3.5, 18.209))
            p.addLine(to: P(3.5, 9))
            p.addCurve(to: P(7.5, 5), control1: P(3.5, 6.791), control2: P(5.291, 5))
            p.closeSubpath()
            p.move(to: P(8, 3))
            p.addLine(to: P(8, 7))
            p.move(to: P(16, 3))
            p.addLine(to: P(16, 7))
            p.move(to: P(8.5, 13))
            p.addLine(to: P(11, 15.5))
            p.addLine(to: P(15.5, 10.5))
        case .library:
            p.move(to: P(5, 4))
            p.addLine(to: P(10, 4))
            p.addCurve(to: P(12, 6), control1: P(11.105, 4), control2: P(12, 4.895))
            p.addLine(to: P(12, 20))
            p.addCurve(to: P(10, 18), control1: P(12, 18.895), control2: P(11.105, 18))
            p.addLine(to: P(5, 18))
            p.closeSubpath()
            p.move(to: P(19, 4))
            p.addLine(to: P(14, 4))
            p.addCurve(to: P(12, 6), control1: P(12.895, 4), control2: P(12, 4.895))
            p.addLine(to: P(12, 20))
            p.addCurve(to: P(14, 18), control1: P(12, 18.895), control2: P(12.895, 18))
            p.addLine(to: P(19, 18))
            p.closeSubpath()
        case .stats:
            p.move(to: P(5, 20))
            p.addLine(to: P(5, 12))
            p.move(to: P(12, 20))
            p.addLine(to: P(12, 5))
            p.move(to: P(19, 20))
            p.addLine(to: P(19, 15))
        case .plus:
            p.move(to: P(12, 5))
            p.addLine(to: P(12, 19))
            p.move(to: P(5, 12))
            p.addLine(to: P(19, 12))
        case .profile:
            p.move(to: P(8, 8))
            p.addCurve(to: P(12, 12), control1: P(8, 10.209), control2: P(9.791, 12))
            p.addCurve(to: P(16, 8), control1: P(14.209, 12), control2: P(16, 10.209))
            p.addCurve(to: P(12, 4), control1: P(16, 5.791), control2: P(14.209, 4))
            p.addCurve(to: P(8, 8), control1: P(9.791, 4), control2: P(8, 5.791))
            p.closeSubpath()
            p.move(to: P(4.5, 20))
            p.addCurve(to: P(12, 12.5), control1: P(4.5, 15.858), control2: P(7.858, 12.5))
            p.addCurve(to: P(19.5, 20), control1: P(16.142, 12.5), control2: P(19.5, 15.858))
        case .bell:
            p.move(to: P(6, 16))
            p.addLine(to: P(6, 11))
            p.addCurve(to: P(12, 5), control1: P(6, 7.686), control2: P(8.686, 5))
            p.addCurve(to: P(18, 11), control1: P(15.314, 5), control2: P(18, 7.686))
            p.addLine(to: P(18, 16))
            p.addLine(to: P(19.5, 18))
            p.addLine(to: P(4.5, 18))
            p.closeSubpath()
            p.move(to: P(10, 21))
            p.addLine(to: P(14, 21))
        case .back:
            p.move(to: P(15, 5))
            p.addLine(to: P(8, 12))
            p.addLine(to: P(15, 19))
        case .check:
            p.move(to: P(5, 12.5))
            p.addLine(to: P(9.5, 17))
            p.addLine(to: P(19, 7.5))
        case .gift:
            p.move(to: P(6, 9))
            p.addLine(to: P(18, 9))
            p.addCurve(to: P(20.5, 11.5), control1: P(19.381, 9), control2: P(20.5, 10.119))
            p.addLine(to: P(20.5, 17.5))
            p.addCurve(to: P(18, 20), control1: P(20.5, 18.881), control2: P(19.381, 20))
            p.addLine(to: P(6, 20))
            p.addCurve(to: P(3.5, 17.5), control1: P(4.619, 20), control2: P(3.5, 18.881))
            p.addLine(to: P(3.5, 11.5))
            p.addCurve(to: P(6, 9), control1: P(3.5, 10.119), control2: P(4.619, 9))
            p.closeSubpath()
            p.move(to: P(12, 9))
            p.addLine(to: P(12, 20))
            p.move(to: P(3.5, 14))
            p.addLine(to: P(20.5, 14))
            p.move(to: P(12, 9))
            p.addCurve(to: P(7.5, 6.5), control1: P(10, 9), control2: P(7.5, 8.2))
            p.addCurve(to: P(11, 5.2), control1: P(7.5, 4.8), control2: P(9.5, 4))
            p.addCurve(to: P(12, 9), control1: P(11.8, 5.9), control2: P(12, 7.5))
            p.closeSubpath()
            p.move(to: P(12, 9))
            p.addCurve(to: P(16.5, 6.5), control1: P(14, 9), control2: P(16.5, 8.2))
            p.addCurve(to: P(13, 5.2), control1: P(16.5, 4.8), control2: P(14.5, 4))
            p.addCurve(to: P(12, 9), control1: P(12.2, 5.9), control2: P(12, 7.5))
            p.closeSubpath()
        case .body:
            p.move(to: P(3, 9))
            p.addLine(to: P(3, 15))
            p.move(to: P(6.5, 6.5))
            p.addLine(to: P(6.5, 17.5))
            p.move(to: P(17.5, 6.5))
            p.addLine(to: P(17.5, 17.5))
            p.move(to: P(21, 9))
            p.addLine(to: P(21, 15))
            p.move(to: P(6.5, 12))
            p.addLine(to: P(17.5, 12))
        case .read:
            p.move(to: P(4, 5.5))
            p.addCurve(to: P(12, 6.5), control1: P(6.5, 4.5), control2: P(9.5, 4.5))
            p.addCurve(to: P(20, 5.5), control1: P(14.5, 4.5), control2: P(17.5, 4.5))
            p.addLine(to: P(20, 17.5))
            p.addCurve(to: P(12, 18.5), control1: P(17.5, 16.5), control2: P(14.5, 16.5))
            p.addCurve(to: P(4, 17.5), control1: P(9.5, 16.5), control2: P(6.5, 16.5))
            p.closeSubpath()
            p.move(to: P(12, 6.5))
            p.addLine(to: P(12, 18.5))
        case .listen:
            p.move(to: P(4, 15))
            p.addLine(to: P(4, 12))
            p.addCurve(to: P(12, 4), control1: P(4, 7.582), control2: P(7.582, 4))
            p.addCurve(to: P(20, 12), control1: P(16.418, 4), control2: P(20, 7.582))
            p.addLine(to: P(20, 15))
            p.move(to: P(5, 14))
            p.addLine(to: P(6, 14))
            p.addCurve(to: P(7.5, 15.5), control1: P(6.828, 14), control2: P(7.5, 14.672))
            p.addLine(to: P(7.5, 18.5))
            p.addCurve(to: P(6, 20), control1: P(7.5, 19.328), control2: P(6.828, 20))
            p.addLine(to: P(5, 20))
            p.addCurve(to: P(3.5, 18.5), control1: P(4.172, 20), control2: P(3.5, 19.328))
            p.addLine(to: P(3.5, 15.5))
            p.addCurve(to: P(5, 14), control1: P(3.5, 14.672), control2: P(4.172, 14))
            p.closeSubpath()
            p.move(to: P(18, 14))
            p.addLine(to: P(19, 14))
            p.addCurve(to: P(20.5, 15.5), control1: P(19.828, 14), control2: P(20.5, 14.672))
            p.addLine(to: P(20.5, 18.5))
            p.addCurve(to: P(19, 20), control1: P(20.5, 19.328), control2: P(19.828, 20))
            p.addLine(to: P(18, 20))
            p.addCurve(to: P(16.5, 18.5), control1: P(17.172, 20), control2: P(16.5, 19.328))
            p.addLine(to: P(16.5, 15.5))
            p.addCurve(to: P(18, 14), control1: P(16.5, 14.672), control2: P(17.172, 14))
            p.closeSubpath()
        case .nourish:
            p.move(to: P(5, 20))
            p.addCurve(to: P(20, 4), control1: P(5, 11), control2: P(10, 5))
            p.addCurve(to: P(5, 20), control1: P(20, 14), control2: P(15, 20))
            p.closeSubpath()
            p.move(to: P(5, 20))
            p.addCurve(to: P(15, 10), control1: P(8, 15), control2: P(11, 12))
        case .mind:
            p.move(to: P(10, 12))
            p.addCurve(to: P(12, 14), control1: P(10, 13.105), control2: P(10.895, 14))
            p.addCurve(to: P(14, 12), control1: P(13.105, 14), control2: P(14, 13.105))
            p.addCurve(to: P(12, 10), control1: P(14, 10.895), control2: P(13.105, 10))
            p.addCurve(to: P(10, 12), control1: P(10.895, 10), control2: P(10, 10.895))
            p.closeSubpath()
            p.move(to: P(12, 8.5))
            p.addCurve(to: P(15.5, 5), control1: P(12, 6.567), control2: P(13.567, 5))
            p.addCurve(to: P(19, 8.5), control1: P(17.433, 5), control2: P(19, 6.567))
            p.addCurve(to: P(15.5, 12), control1: P(19, 10.433), control2: P(17.433, 12))
            p.move(to: P(12, 4))
            p.addCurve(to: P(20, 12), control1: P(16.418, 4), control2: P(20, 7.582))
            p.addCurve(to: P(12, 20), control1: P(20, 16.418), control2: P(16.418, 20))
            p.addCurve(to: P(4, 12), control1: P(7.582, 20), control2: P(4, 16.418))
        case .watch:
            p.move(to: P(7, 5))
            p.addLine(to: P(17, 5))
            p.addCurve(to: P(21, 9), control1: P(19.209, 5), control2: P(21, 6.791))
            p.addLine(to: P(21, 15))
            p.addCurve(to: P(17, 19), control1: P(21, 17.209), control2: P(19.209, 19))
            p.addLine(to: P(7, 19))
            p.addCurve(to: P(3, 15), control1: P(4.791, 19), control2: P(3, 17.209))
            p.addLine(to: P(3, 9))
            p.addCurve(to: P(7, 5), control1: P(3, 6.791), control2: P(4.791, 5))
            p.closeSubpath()
            p.move(to: P(10.5, 9.5))
            p.addLine(to: P(10.5, 14.5))
            p.addLine(to: P(14.5, 12))
            p.closeSubpath()
        }
        return p
    }
}

struct DqIconView: View {
    let icon: DqIcon
    var size: CGFloat = 22
    var body: some View {
        DqIconShape(icon: icon)
            .stroke(style: StrokeStyle(lineWidth: icon.strokeWidth * size / 24, lineCap: .round, lineJoin: .round))
            .frame(width: size, height: size)
            .accessibilityHidden(true)
    }
}
```

Usage: `DqIconView(icon: .today, size: 22).foregroundStyle(Color.dqMuted)`. In `QuestTile` (section 7) replace the `Image(c.glyph)` call with `DqIconView(icon: DqIcon.glyph(for: c), size: 22)` and the check with `DqIconView(icon: .check, size: 22)`. No image assets are needed.

Pairing notes: the Android drawables use the names in the `Category` enum (`R.drawable.ic_body` and so on), the Swift icons use `DqIcon.glyph(for:)`.

## 9. Unit tests for the rank engine

The rank engine (section 5) is the one piece of logic that is easy to get subtly wrong, so test it before building UI on top. Both suites use the same cases and the same expected values, which were produced by running the prototype's own `rankInfo` and `rating` functions. Standalone copies are in `tests/RankEngineTest.kt` and `tests/RankEngineTests.swift`.

**Seed state:** level 14, 1,648 / 2,480 XP, nothing claimed today, rating 1,913.

| Style | Label | Numeral | Figure | Unit | Bar | Foot | Tier index |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Dota 2 | Crusader · star 3 | 3 | 1,913 | MMR | 0.4221 | 89 MMR to Crusader · star 4 | 2 |
| Mobile Legends | Grandmaster III | III | 2 of 3 | stars | 0.5813 | 67 rating to Grandmaster II | 3 |
| League of Legends | Gold IV | IV | 7 | LP | 0.0743 | 162 rating to Gold III | 3 |
| Valorant | Platinum 1 | 1 | 6 | RR | 0.065 | 187 rating to Platinum 2 | 4 |

Also covered: Level style returns no rank; each claimed quest adds 35 rating; in Dota 2 two claims (1,992 rating) stay on star 3 and the third claim (2,030) reaches star 4, which is what opens the rank-up screen; the exact boundary (1,540 = Crusader star 1); one below the boundary (1,539 = Guardian star 5, foot points at "Crusader · star 1"); the top tiers Immortal, Master and Radiant; and an invariant sweep over levels 1-60 for every style (rating and tier never decrease, bar stays in 0 to 1, label never empty).

**What each suite assumes about your code**

- Android: `AppState` is a data class with named parameters `level`, `xp`, `cap`, `claimed: Set<String>` and `rankStyle`; `rating(AppState)` and `rankInfo(AppState)` are the top-level functions from section 6. Dependency: `kotlin.test` (JUnit 4 or 5). Run: `./gradlew testDebugUnitTest`.
- iOS: `AppStore` is the `@Observable` class from section 7 with mutable `level`, `xp`, `cap`, `claimed: Set<String>` and `rankStyle`, and its extension provides `rating` and `rankInfo`. Keep `@MainActor` on the test class if the store is main-actor isolated. Run: `xcodebuild test -scheme Dayquest -destination "platform=iOS Simulator,name=iPhone 15"`. Replace `Dayquest` in `@testable import` with your module name.

### 9.1 Kotlin: `RankEngineTest.kt`

```kotlin
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
```

### 9.2 Swift: `RankEngineTests.swift`

```swift
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
```

## 10. Local database (SQLite), onboarding and backup

The prototype keeps its state in the browser's local storage as a stand-in. **The native apps store everything in an on-device SQLite database.** Behaviour, copy and validation below are what the prototype does; the schema and code are the native version of it.

### 10.1 Onboarding (first launch)

Shown once, when the database has no `profile` row. Full-screen over everything, on `ground`. Prototype ids: `onb`, `onbName`.

| Part | Spec |
| --- | --- |
| Brand | "Dayquest" in Instrument Serif 26, top left |
| Title | "What should we call you?" Instrument Serif 46, line-height 1.02 (max two lines) |
| Lede | "Your name goes on your daily quests and into your backups. Everything stays on this device." 15, `muted`, max 30 characters wide |
| Field | Label "Your name"; 52 high, 16 radius, `surface` fill, 1px `control` outline, 18 text; max 24 characters; given-name autofill; placeholder "e.g. Sam" |
| Live preview | `surface` panel: "Your first screen", then the greeting in serif 34 ("Morning, Sam." and "Morning, …" while empty), then "6 quests are waiting for you." |
| Primary action | **Start my first day** (white pill, 56 high). Pinned to the bottom |
| Secondary action | **I have a backup file** (text button, 44 high). Opens the file picker |

Rules:

- **Validate on blur and on submit**, never on the first keystroke. Empty or spaces only = "Tell us what to call you." (error colour, announced as an alert, field marked invalid, focus returns to the field). Trim before saving. Keep what the user typed on every error.
- Submit creates the `profile` row (name, level 1 in the real app; the prototype starts at its demo level 14), seeds the quests, then lands on Today with the toast "Welcome, {name}. Your first six quests are ready."
- **I have a backup file** runs the restore flow (10.6) without a confirmation step, because there is nothing to overwrite yet. A bad file shows the reason inline above the buttons ("Couldn't restore from that file. {reason}") and stays on onboarding.
- Focus starts in the name field. The keyboard's Done/Go submits. No back button, no skip.
- The greeting everywhere ("Morning, {name}.") and the Profile subtitle (`{name} · Level {level} · {rating} rating`) read from the stored name.

### 10.2 Schema (version 1)

Dates are ISO-8601 text. `day` is a local calendar date (`YYYY-MM-DD`). Set `PRAGMA foreign_keys = ON` on every connection and `PRAGMA user_version = 1` after creating the tables.

```sql
CREATE TABLE profile (
  id               INTEGER PRIMARY KEY CHECK (id = 1),          -- exactly one row
  name             TEXT    NOT NULL CHECK (length(trim(name)) BETWEEN 1 AND 24),
  level            INTEGER NOT NULL CHECK (level >= 1),
  xp               INTEGER NOT NULL CHECK (xp >= 0),
  cap              INTEGER NOT NULL CHECK (cap > xp),            -- grows x1.12 per level, rounded to 10
  streak_base      INTEGER NOT NULL CHECK (streak_base >= 0),    -- days before today
  rank_style       TEXT    NOT NULL DEFAULT 'level'
                   CHECK (rank_style IN ('level','dota','mlbb','lol','valorant')),
  created_at       TEXT    NOT NULL,
  last_backup_at   TEXT
);

CREATE TABLE quest (
  id          TEXT    PRIMARY KEY,                               -- 'body', 'read', ... or generated for custom
  category    TEXT    NOT NULL CHECK (category IN ('Body','Read','Listen','Nourish','Mind','Watch')),
  title       TEXT    NOT NULL CHECK (length(trim(title)) BETWEEN 1 AND 60),
  xp          INTEGER NOT NULL CHECK (xp BETWEEN 1 AND 500),
  story       TEXT    NOT NULL,
  is_custom   INTEGER NOT NULL DEFAULT 0,
  library_id  TEXT,                                              -- set when added from the Library tab
  sort_order  INTEGER NOT NULL,
  archived_at TEXT,                                              -- set by Remove quest, cleared by Undo
  reward_text TEXT CHECK (reward_text IS NULL OR length(trim(reward_text)) BETWEEN 1 AND 40)  -- the user's own reward
);
CREATE INDEX idx_quest_active ON quest(archived_at) WHERE archived_at IS NULL;

CREATE TABLE quest_step (
  quest_id  TEXT    NOT NULL REFERENCES quest(id) ON DELETE CASCADE,
  position  INTEGER NOT NULL,
  text      TEXT    NOT NULL,
  PRIMARY KEY (quest_id, position)
);

CREATE TABLE daily_progress (                                    -- one row per quest per day
  day         TEXT    NOT NULL,
  quest_id    TEXT    NOT NULL REFERENCES quest(id) ON DELETE CASCADE,
  steps_done  INTEGER NOT NULL DEFAULT 0,                        -- bitmask, bit 0 = first step
  claimed_at  TEXT,
  PRIMARY KEY (day, quest_id)
);

CREATE TABLE xp_ledger (                                         -- append-only; feeds Stats
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  day         TEXT    NOT NULL,
  quest_id    TEXT    REFERENCES quest(id) ON DELETE SET NULL,
  category    TEXT    NOT NULL,                                  -- copied so history survives quest deletion
  xp          INTEGER NOT NULL,
  created_at  TEXT    NOT NULL
);
CREATE INDEX idx_ledger_day ON xp_ledger(day);

CREATE TABLE reward_earned (                                     -- one row per reward earned: small (quest), medium (streak), big (level / rank)
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  kind         TEXT    NOT NULL DEFAULT 'small' CHECK (kind IN ('small','medium','big')),
  source       TEXT    NOT NULL UNIQUE,                          -- 'quest:body:2026-10-26' | 'streak:14' | 'level:15' | 'rank:mlbb:Epic'
  quest_id     TEXT    REFERENCES quest(id) ON DELETE SET NULL,
  from_label   TEXT    NOT NULL,                                 -- 'Body quest', '14-day streak', 'Reached Level 15', ...; copied so history survives deletion
  reward_text  TEXT    NOT NULL CHECK (length(trim(reward_text)) BETWEEN 1 AND 40),
  earned_at    TEXT    NOT NULL,
  enjoyed_at   TEXT
);
CREATE TABLE milestone_reward (                                  -- the user's medium and big rewards waiting to be earned
  kind         TEXT NOT NULL CHECK (kind IN ('streak','level','rank')),
  key          TEXT NOT NULL,                                    -- '14' | 'next' | 'mlbb:Epic'
  reward_text  TEXT NOT NULL CHECK (length(trim(reward_text)) BETWEEN 1 AND 40),
  PRIMARY KEY (kind, key)
);
CREATE INDEX idx_reward_open ON reward_earned(enjoyed_at) WHERE enjoyed_at IS NULL;

CREATE TABLE day_log (                                           -- one row per day the streak was secured
  day  TEXT PRIMARY KEY
);
```

Derived, not stored: today's streak (`streak_base` plus 1 if `day_log` has today), the 7 bars and category split on Stats (sum `xp_ledger` by `day` / `category`), the 35-day grid (`day_log` for the last 35 days), the rating (`(level-1)*140 + xp/cap*140 + claimed-today*35`). `day_log` replaces `streak_base` as the source of truth once the app has more than a few days of history; recompute the streak by walking back from yesterday while rows exist.

### 10.3 Writes

- **Claim** is one transaction: set `claimed_at` in `daily_progress`, insert into `xp_ledger`, insert a `reward_earned` row (copying `reward_text` and the quest title) when the quest has a reward, evaluate the streak / level / rank milestone rewards (section 4.7.1: insert `reward_earned` rows with a unique `source`, delete the `('level','next')` slot after it pays out), run the level-up loop (section 4.3) and update `profile`, insert today into `day_log`. All or nothing.
- **Remove quest** sets `quest.archived_at` (soft delete; history in `xp_ledger` is untouched) in one statement, and only if there is no `daily_progress` row with `claimed_at` for today (reject otherwise). **Undo** sets it back to `NULL`. Every query that lists quests filters `archived_at IS NULL`. The Library tab shows **Added** only when a non-archived quest with that `library_id` exists. On app start, delete archived custom quests older than 30 days (`DELETE FROM quest WHERE is_custom = 1 AND archived_at < date('now','-30 day')`; steps and progress cascade).
- Step toggles update `steps_done` only. Quest creation inserts `quest` plus `quest_step` rows in one transaction.
- Day rollover: at local midnight (or on next open) today's `daily_progress` rows simply stop being "today". A new day starts with all steps unticked.
- Always run database work off the main thread (Room suspend DAOs / GRDB `DatabaseQueue` on a background executor).

### 10.4 Backup file

- **Native format:** a complete SQLite database file, named `dayquest-backup-YYYY-MM-DD.db`, produced from the live database as a consistent snapshot (`VACUUM INTO` or SQLite's online backup API, never a raw copy of a database that might be mid-write or have a WAL file).
- Contents: everything in 10.2. `profile.last_backup_at` is set before the snapshot is taken so the file knows when it was made.
- **Prototype format** (for reference only): JSON `{ "app": "dayquest", "version": 1, "savedAt": ISO, "data": { name, level, xp, cap, streak, secured, sys, claimed, steps, added, nextId, lastBackup, removed[], rewards{}, earned{}, goals{ streak, level, rank }, customQuests[] (each with lib) } }`, file `dayquest-backup-YYYY-MM-DD.json`.
- The file contains the user's name and habits in plain form. Say so in the share sheet text; do not upload it anywhere automatically. (Optional later: encrypt with SQLCipher and a passphrase.)

### 10.5 Profile > Backup panel

A `surface` panel under the ladder. Prototype ids: `backupPanel`, `bkNow`, `bkRestore`.

- Header: **Backup**, right-aligned status: "Never backed up" or "Last backup 26 Oct, 09:41".
- Line 1 (muted): "Saved on this device · {n} quests · Level {level}".
- Line 2 (muted): "Back up to keep a copy of your quests, XP and streak in one file, or to move to a new phone."
- Two equal buttons, 48 high: **Back up now** (white, primary) and **Restore from file** (outline).
- **Back up now** opens the system save/share sheet with the suggested name, then on success updates the status and toasts "Backup saved: {file name}".
- **Restore from file** opens the file picker, validates (10.6) and then shows an inline confirmation inside the panel: title "Replace your current progress?", body "{name} · Level {level} · {n} quests · saved {date}. Your current quests, XP and streak will be overwritten.", buttons **Replace** (white) and **Cancel**. Focus lands on Replace.
- **Error state:** an inline box with a 1px error-colour outline: bold "Couldn't restore from that file.", the specific reason, and a text button **Choose another file**. Current data is untouched. Announce as an alert.

### 10.6 Restore: validation order and swap

Never touch the live database until every check has passed.

1. Copy the picked file to a temp file in app storage (Android: `contentResolver.openInputStream`; iOS: `startAccessingSecurityScopedResource()` then copy; always `stopAccessing...` afterwards). Reject files over 50 MB.
2. Open the temp copy **read-only**. Failure to open = "The file isn't valid backup data."
3. `PRAGMA integrity_check` must return `ok`; `PRAGMA foreign_key_check` must return no rows.
4. `PRAGMA user_version`: greater than the app's version = "This backup was made by a newer version of Dayquest."; lower = migrate the temp copy up first; 0 or missing tables = "This isn't a Dayquest backup."
5. The six tables exist; exactly one `profile` row; its `CHECK` constraints hold (re-validate name length, `xp < cap`, rank style known); every `quest_step` and `daily_progress` row points at a real quest.
6. Build the summary (name, level, quest count, `last_backup_at`/file date) for the confirmation box. From onboarding, skip the confirmation.
7. **Swap:** write a safety copy of the current database (`pre-restore.db`, keep one), then replace the live database from the validated copy in a single operation (Android: close Room, atomically rename the temp file over the database file, reopen; iOS/GRDB: `source.backup(to: liveQueue)`). If anything fails, restore from `pre-restore.db` and show the error state above.
8. Reload all state flows/stores from the new database and go to Today (onboarding) or stay on Profile (panel). Toast "Restored {name}'s progress."

Error copy (exactly the prototype's wording where it exists): "This isn't a Dayquest backup." / "This backup was made by a newer version of Dayquest." / "The file isn't valid backup data." / "The file is too large to be a Dayquest backup." / "Quest progress in the backup is damaged." / "The backup has more XP than its level allows."

### 10.7 Android (Room)

```kotlin
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String, val level: Int, val xp: Int, val cap: Int,
    @ColumnInfo(name = "streak_base") val streakBase: Int,
    @ColumnInfo(name = "rank_style") val rankStyle: String = "level",
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "last_backup_at") val lastBackupAt: String? = null,
)

@Database(
    entities = [ProfileEntity::class, QuestEntity::class, QuestStepEntity::class,
                DailyProgressEntity::class, XpLedgerEntity::class, DayLogEntity::class],
    version = 1, exportSchema = true,
)
abstract class DayquestDb : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    // plus a DAO each for quests, progress, ledger and day_log
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1") fun observe(): Flow<ProfileEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(p: ProfileEntity)
    @Query("UPDATE profile SET last_backup_at = :at WHERE id = 1") suspend fun touchLastBackup(at: String)
}
```

First launch check: `profileDao.observe().first() == null` means show onboarding.

Backup and restore (in `BackupManager`; use `ActivityResultContracts.CreateDocument("application/octet-stream")` and `OpenDocument()` for the pickers, so no storage permission is needed):

```kotlin
class BackupManager(private val context: Context, private val dbHolder: DbHolder) {

    /** Writes a consistent snapshot of the live database to [dest]. */
    suspend fun backupTo(dest: Uri): Result<Unit> = withContext(Dispatchers.IO) { runCatching {
        dbHolder.db.profileDao().touchLastBackup(Instant.now().toString())
        val tmp = File(context.cacheDir, "backup-${System.nanoTime()}.db").also { it.delete() }
        // VACUUM INTO needs SQLite 3.27+ (Android 11+). On older versions: checkpoint the WAL, then copy the file.
        if (Build.VERSION.SDK_INT >= 30) {
            dbHolder.db.openHelper.writableDatabase.execSQL("VACUUM INTO '${tmp.absolutePath}'")
        } else {
            dbHolder.db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            context.getDatabasePath(DB_NAME).copyTo(tmp, overwrite = true)
        }
        context.contentResolver.openOutputStream(dest, "wt")!!.use { out -> tmp.inputStream().use { it.copyTo(out) } }
        tmp.delete()
    } }

    /** Validates [src] without touching the live database. Returns the summary for the confirmation box. */
    suspend fun inspect(src: Uri): Result<BackupSummary> = withContext(Dispatchers.IO) { runCatching {
        val tmp = File(context.cacheDir, "restore.db").also { it.delete() }
        context.contentResolver.openInputStream(src)!!.use { i -> tmp.outputStream().use { i.copyTo(it) } }
        if (tmp.length() > 50L * 1024 * 1024) throw BackupError("The file is too large to be a Dayquest backup.")
        SQLiteDatabase.openDatabase(tmp.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            if (db.version > CURRENT_VERSION) throw BackupError("This backup was made by a newer version of Dayquest.")
            check(db.rawQuery("PRAGMA integrity_check", null).use { it.moveToFirst() && it.getString(0) == "ok" })
            check(db.rawQuery("PRAGMA foreign_key_check", null).use { !it.moveToFirst() })
            db.rawQuery("SELECT name, level, last_backup_at FROM profile WHERE id = 1", null).use { c ->
                if (!c.moveToFirst()) throw BackupError("This isn't a Dayquest backup.")
                BackupSummary(c.getString(0), c.getInt(1),
                    db.rawQuery("SELECT COUNT(*) FROM quest", null).use { q -> q.moveToFirst(); q.getInt(0) },
                    c.getString(2), tmp)
            }
        }
    } }

    /** Swaps the validated copy in. Keeps pre-restore.db so a failure can be rolled back. */
    suspend fun restore(summary: BackupSummary): Result<Unit> = withContext(Dispatchers.IO) { runCatching {
        val live = context.getDatabasePath(DB_NAME)
        val safety = File(live.parentFile, "pre-restore.db")
        dbHolder.close()
        live.copyTo(safety, overwrite = true)
        try {
            File(live.path + "-wal").delete(); File(live.path + "-shm").delete()
            summary.file.copyTo(live, overwrite = true)
            dbHolder.open()                       // runs Room migrations if the backup is older
        } catch (t: Throwable) {
            safety.copyTo(live, overwrite = true); dbHolder.open(); throw t
        }
    } }
}
```

`BackupError(message)` carries the user-facing sentence; map any other exception to "The file isn't valid backup data." Run `inspect` before showing the confirmation, `restore` only after **Replace**. After a successful restore, re-collect the Flows (the `DbHolder` should expose a new `DayquestDb` instance).

### 10.8 iOS (GRDB)

GRDB is the recommended SQLite layer: it has the online backup API built in. (Plain `SQLite3` or SwiftData also work, but then you must write the backup yourself with `sqlite3_backup_init`.)

```swift
import GRDB

struct Profile: Codable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "profile"
    var id = 1
    var name: String; var level: Int; var xp: Int; var cap: Int
    var streakBase: Int; var rankStyle: String
    var createdAt: String; var lastBackupAt: String?
}

final class Database {
    let queue: DatabaseQueue
    init(path: String) throws {
        var config = Configuration(); config.foreignKeysEnabled = true
        queue = try DatabaseQueue(path: path, configuration: config)
        var migrator = DatabaseMigrator()
        migrator.registerMigration("v1") { db in /* run the CREATE TABLE statements from 10.2 */ }
        try migrator.migrate(queue)
    }
    var needsOnboarding: Bool { (try? queue.read { try Profile.fetchOne($0) }) == nil }
}

enum BackupError: LocalizedError {
    case notDayquest, newer, invalid, tooLarge
    var errorDescription: String? {
        switch self {
        case .notDayquest: "This isn't a Dayquest backup."
        case .newer: "This backup was made by a newer version of Dayquest."
        case .invalid: "The file isn't valid backup data."
        case .tooLarge: "The file is too large to be a Dayquest backup."
        }
    }
}

struct BackupSummary { let name: String; let level: Int; let quests: Int; let savedAt: String?; let tempURL: URL }

struct BackupManager {
    let db: Database

    /// Consistent snapshot of the live database, written to a temp file you then hand to `.fileExporter`.
    func makeBackup() throws -> URL {
        try db.queue.write {
            try $0.execute(sql: "UPDATE profile SET last_backup_at = ? WHERE id = 1",
                           arguments: [ISO8601DateFormatter().string(from: .now)])
        }
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent("dayquest-backup-\(Date.now.formatted(.iso8601.year().month().day())).db")
        try? FileManager.default.removeItem(at: url)
        let dest = try DatabaseQueue(path: url.path)
        try db.queue.backup(to: dest)                       // SQLite online backup API
        return url
    }

    /// Validates a picked file. The caller must have started security-scoped access on `picked`.
    func inspect(_ picked: URL) throws -> BackupSummary {
        let temp = FileManager.default.temporaryDirectory.appendingPathComponent("restore.db")
        try? FileManager.default.removeItem(at: temp)
        try FileManager.default.copyItem(at: picked, to: temp)
        guard (try temp.resourceValues(forKeys: [.fileSizeKey]).fileSize ?? 0) <= 50_000_000 else { throw BackupError.tooLarge }
        var config = Configuration(); config.readonly = true
        guard let src = try? DatabaseQueue(path: temp.path, configuration: config) else { throw BackupError.invalid }
        return try src.read { d in
            guard try String.fetchOne(d, sql: "PRAGMA integrity_check") == "ok",
                  try Row.fetchAll(d, sql: "PRAGMA foreign_key_check").isEmpty else { throw BackupError.invalid }
            let version = try Int.fetchOne(d, sql: "PRAGMA user_version") ?? 0
            if version > Database.currentVersion { throw BackupError.newer }
            guard let p = try Row.fetchOne(d, sql: "SELECT name, level, last_backup_at FROM profile WHERE id = 1") else { throw BackupError.notDayquest }
            let quests = try Int.fetchOne(d, sql: "SELECT COUNT(*) FROM quest") ?? 0
            return BackupSummary(name: p["name"], level: p["level"], quests: quests, savedAt: p["last_backup_at"], tempURL: temp)
        }
    }

    /// Overwrites the live database from the validated copy, keeping a safety copy first.
    func restore(_ summary: BackupSummary) throws {
        let safety = FileManager.default.temporaryDirectory.appendingPathComponent("pre-restore.db")
        try? FileManager.default.removeItem(at: safety)
        try db.queue.backup(to: DatabaseQueue(path: safety.path))
        do { try DatabaseQueue(path: summary.tempURL.path).backup(to: db.queue) }
        catch { try? DatabaseQueue(path: safety.path).backup(to: db.queue); throw error }
    }
}
```

UI wiring: `.fileExporter(isPresented:document:contentType:defaultFilename:)` with a `FileDocument` wrapping the temp URL for **Back up now**; `.fileImporter(isPresented:allowedContentTypes:)` for **Restore from file**. Define a `UTType` for `.db` (`UTType(filenameExtension: "db")`). After `restore`, rebuild the `AppStore` from the database and publish.

### 10.9 Tests for this layer

- Claim transaction: after a claim, `xp_ledger`, `daily_progress`, `profile` and `day_log` agree; a forced failure mid-way changes nothing.
- Migration: opening a version-0 or older fixture file upgrades it; opening a newer-version fixture is rejected with the "newer version" sentence.
- Backup round trip: create data, back up, wipe, restore: name, level, XP, custom quests, progress and `last_backup_at` match.
- Rejections: a random binary file, a valid SQLite file with no `profile` row, a file with `xp >= cap`, a file that fails `integrity_check`, and a file over 50 MB each produce the exact sentence listed in 10.6 and leave the live database unchanged.
- Restore failure mid-swap rolls back to `pre-restore.db`.
- Rewards: claiming a quest with a reward creates exactly one `reward_earned` row with the text as it was at that moment; editing the quest's reward afterwards does not change it; Enjoyed sets `enjoyed_at` once; a quest without a reward creates nothing; reward text over 40 characters or empty is rejected; restore rejects an earned reward that points at a quest that was never claimed.
- Milestone rewards: with a 13-day streak, the claim that secures today creates exactly one medium `reward_earned` row for the 14-day slot and a second claim the same day creates none; a level-up creates one big row for the level reached, deletes the `('level','next')` slot, and two level-ups in one claim pay out once; a rank reward fires only for the selected game (Dota 2: Archon and Legend, Mobile Legends: Epic and Legend), only when the rating first reaches the tier floor, and never for a tier that was already passed when the slot was written; the earned text is a copy, so editing or removing the slot afterwards changes nothing; backup and restore keep slots and earned rows, and reject a malformed key.
- Remove quest: archiving hides the quest from Today, the detail selection moves to the next unclaimed quest, Undo restores position and ticked steps, a claimed quest cannot be archived, and archived rows survive a backup and restore.
- Onboarding: empty/space-only name blocks submit and keeps the field; a 24-character name saves; a 25th character cannot be typed.

## 11. States to add beyond the prototype

(Remove quest is already designed: the confirmation, undo toast and empty states are in section 4.2.1.)

The prototype only shows the populated state. Build these natively:

- **Loading:** keep the layout; replace values with flat grey bars at real size (no shimmer). Panel bars at 0.
- **Empty:** all quests cleared (already designed, section 4.1); Library filter always has items.
- **Error:** inside the failed block, one sentence with cause and time ("Couldn't load your quests. Last updated 09:14.") and a **Retry** text button. Keep user input. After three failed retries, show "Contact support" and a copyable error ID.
- **Edge:** 40-character quest names (truncate to 2 lines in the tile, one line in the cover rows); 100+ quests (grid is lazy; cap the Library at the chosen category); level 50+ (the cap grows 12% per level; format with separators).
- **Offline:** claiming queues locally and syncs later; never block the celebration.

## 12. Acceptance checklist

- [ ] Seed state shows: Level 14, `1,648 / 2,480 XP`, `832 XP to Level 15`, streak 13, six tiles, 430 XP to earn.
- [ ] Claiming the first quest (all four steps) adds 90 XP, secures the streak (14), earns the seed 14-day streak reward and toasts "+90 XP — Body quest complete. Reward ready: An iced latte. Streak extended to 14 days. Reward earned: A nice dinner out."
- [ ] In Dota 2 style the third claim changes "Crusader · star 3" to "Crusader · star 4" and shows the rank-up screen with the numeral 4.
- [ ] The rank engine test suite in section 9 passes (the four seed values, boundaries, top tiers and the invariant sweep).
- [ ] Switching rank style updates Today panel, Profile card, ladder and the cover numeral at once.
- [ ] New quest: empty name shows the error on blur and keeps the sheet open; a valid name creates, selects, scrolls to the tile.
- [ ] All text meets 4.5:1; all targets 44pt/48dp; focus rings visible; reduced motion removes movement but keeps the information.
- [ ] No gradients, glows, blur, or shadows at rest anywhere. Lime appears only on progress.
- [ ] No game logos or badge art are used; ranks are names and numerals only.
- [ ] All 15 icons render from the section 8 code (no image assets), with round caps and joins, tinted by the surrounding text colour.
- [ ] First launch shows the name page; an empty name keeps the field and shows "Tell us what to call you."; the name then appears in the greeting and on Profile.
- [ ] Quitting and reopening the app restores every quest, step, XP, streak and setting from the SQLite database.
- [ ] Back up now writes a valid `.db`; Restore from file validates it, asks before replacing, and a bad file leaves current data untouched with the exact error sentences from section 10.6.
- [ ] Remove quest asks first (focus on Keep it), shows an Undo toast that pauses on hover/focus, restores steps and position on Undo, is disabled for completed quests, and leaves XP, streak and Stats unchanged.
- [ ] A user can write their own reward on a quest (Set reward, or the optional field when creating one), edit or remove it before finishing, earn it by claiming, see it under Reward ready, and mark it enjoyed; the text is never changed by later edits and is escaped on display.
- [ ] Streak rewards (medium), the next level-up reward and Dota 2 / Mobile Legends rank rewards (big) can be written in Profile, show their progress, earn exactly once at the right claim, appear on the reward-earned screen and in Reward ready, and survive backup and restore.

---

# Part 2: what is still to build (Android first)

Part 1 (sections 1 to 12 above) is the specification. You have already built the rank engine and its tests, so Part 2 covers only what is left, in the order to build it. Everything here is Kotlin first; section P2.11 maps it to Swift.

## P2.1 Where you are

| Piece | Status | Where |
| --- | --- | --- |
| Rank engine: `RankStyle`, `Tier`, `RankInfo`, `rating()`, `rankInfo()` | **Done** (yours) | `RankEngine.kt`, guide section 6 |
| Rank engine tests (12 tests: seed values, claims, boundaries, top tiers, invariants) | **Done** (yours) | `tests/RankEngineTest.kt`, guide section 9 |
| Domain models and seed data | To do | P2.3, `Domain.kt` |
| Reducer: claim, steps, quests, remove/undo, rewards, milestone rewards, onboarding | To do | P2.4, `Reducer.kt` |
| Reducer tests | To do | P2.5, `ReducerTest.kt` |
| Persistence (Room, schema v1, repository) | To do | P2.6 |
| ViewModel, UI state, celebration queue | To do | P2.7 |
| Theme, components, icons, screens | To do | P2.8 |
| Onboarding, backup and restore | To do | P2.9 |
| Repository, ViewModel and UI tests | To do | P2.10 |

**Two things to check in your rank engine before you continue**

1. `AppState` must come from `Domain.kt` (P2.3). If you wrote a temporary `AppState` for the tests, delete it. Every field has a default, so `RankEngineTest` compiles unchanged.
2. The guide's top-tier line was corrected after the first version: the figure is formatted (`"%,d".format(r - t.low)`) and the unit is `"MMR over 5,420"` for Dota 2 or `"rating over 4,800"` for the other styles. Compare with your `RankEngine.kt`. The existing test only checks the figure, so it will not catch a difference in the unit; add `assertEquals("MMR over 5,420", info.unit)` to the Dota case if you want it covered.

Files in this repository for Part 2: `part2/Domain.kt`, `part2/Reducer.kt`, `part2/ReducerTest.kt`. They are also printed below. Put them in `domain/` (and `test/.../domain/`) next to `RankEngine.kt`, same package `com.dayquest.domain`.

## P2.2 Build order

Do these in order. Run the whole test suite after every step; do not start a step with red tests.

| # | Step | Done when |
| --- | --- | --- |
| 1 | Add `Domain.kt` and `Reducer.kt` | Project compiles; `RankEngineTest` still passes unchanged |
| 2 | Add `ReducerTest.kt` | All 12 reducer tests pass |
| 3 | Room schema v1, entities, DAOs, mappers, `DayquestRepository` | In-memory database round-trips an `AppState`; claim is one transaction |
| 4 | `MainViewModel` and `UiState` | Claiming in a ViewModel test produces the toast and the celebration queue in order |
| 5 | Theme, tokens, fonts, 15 icons, shared components | A `@Preview` of every component in section 2 and 4 specs |
| 6 | Today and Quest detail with the claim flow | You can start a quest, tick steps, claim, see the toast, the rank-up and reward screens |
| 7 | Library, New quest sheet, Remove quest with Undo | Sections 4.4, 4.5, 4.2.1 behave as written |
| 8 | Stats and Profile (rank style, rewards, milestone rewards) | Sections 4.6, 4.7, 4.7.1 |
| 9 | Onboarding and Backup/Restore | Section 10 checklist |
| 10 | States, accessibility, motion, polish | Sections 11 and 12 |

## P2.3 `Domain.kt`: models and seed data

`AppState` is the single source of truth and is a plain immutable data class. The reducer never touches Android, the database or the clock.

```kotlin
package com.dayquest.domain

// Domain models for everything the rank engine does not cover.
// RankStyle, Tier, RankInfo, rating() and rankInfo() already exist in RankEngine.kt (guide section 6).

enum class Category { Body, Read, Listen, Nourish, Mind, Watch }

enum class RewardKind { Small, Medium, Big }

data class Quest(
    val id: String,
    val category: Category,
    val title: String,
    val shortTitle: String,
    val xp: Int,
    val story: String,
    val steps: List<String>,
    val isCustom: Boolean = false,
    val libraryId: String? = null,
)

/** A reward the user wrote and has now earned. [text] is a copy, so later edits never change it. */
data class EarnedReward(
    val text: String,
    val kind: RewardKind,
    val from: String,            // "20-min strength", "14-day streak", "Reached Level 15", "Reached Mobile Legends Epic"
    val earnedAt: String,        // ISO-8601
    val enjoyedAt: String? = null,
) {
    val enjoyed: Boolean get() = enjoyedAt != null
}

/** Medium and big rewards waiting to be earned. */
data class Goals(
    val streak: Map<Int, String> = emptyMap(),     // 7, 14, 30
    val level: String? = null,                     // next level-up only, cleared when it pays out
    val rank: Map<String, String> = emptyMap(),    // "dota:Archon", "dota:Legend", "mlbb:Epic", "mlbb:Legend"
)

/**
 * Every parameter has a default so tests can build a state with just the fields they care about
 * (RankEngineTest passes level, xp, cap, claimed and rankStyle only).
 * Defaults match the prototype's seed numbers.
 */
data class AppState(
    val level: Int = 14,
    val xp: Int = 1648,
    val cap: Int = 2480,
    val streakBase: Int = 13,                       // days before today
    val securedToday: Boolean = false,
    val claimed: Set<String> = emptySet(),          // quest ids claimed today
    val rankStyle: RankStyle = RankStyle.Level,
    val name: String = "",
    val quests: List<Quest> = emptyList(),
    val stepsDone: Map<String, List<Boolean>> = emptyMap(),
    val archived: Set<String> = emptySet(),         // removed from today, restorable by Undo
    val rewards: Map<String, String> = emptyMap(),  // questId -> the user's own reward text
    val earned: Map<String, EarnedReward> = emptyMap(),   // key: questId | "streak:14" | "level:15" | "rank:mlbb:Epic"
    val goals: Goals = Goals(),
    val selectedQuestId: String? = null,
    val lastBackupAt: String? = null,
) {
    val streak: Int get() = streakBase + if (securedToday) 1 else 0
    val visibleQuests: List<Quest> get() = quests.filter { it.id !in archived }

    companion object {
        /** The prototype's starting state: Level style, Body quest first step ticked, sample rewards. */
        fun seed(name: String = "Sam"): AppState {
            val quests = SeedData.quests
            return AppState(
                name = name,
                quests = quests,
                stepsDone = quests.associate { q -> q.id to q.steps.mapIndexed { i, _ -> q.id == "body" && i == 0 } },
                rewards = mapOf("body" to "An iced latte", "watch" to "Order your favourite takeaway"),
                goals = Goals(
                    streak = mapOf(14 to "A nice dinner out", 30 to "A weekend day trip"),
                    level = "New running shoes",
                    rank = mapOf(
                        "mlbb:Epic" to "Pizza night with friends", "mlbb:Legend" to "A new gadget",
                        "dota:Archon" to "A takeaway night", "dota:Legend" to "A new gadget",
                    ),
                ),
                selectedQuestId = "body",
            )
        }
    }
}

object SeedData {
    val quests: List<Quest> = listOf(
        Quest("body", Category.Body, "Strength", "20-min strength", 90,
            "Twenty minutes, no equipment. Two moves, three rounds, and a stronger you before lunch.",
            listOf("Warm up: 2 min of arm circles and squats", "Pushups: 3 rounds of 12", "Plank: 3 holds of 45 seconds", "Log how it felt")),
        Quest("read", Category.Read, "Four Thousand Weeks", "Four Thousand Weeks", 70,
            "Fifteen pages of Oliver Burkeman on spending a finite life well. Pick up where your bookmark is.",
            listOf("Open the book at your bookmark", "Read 15 pages without switching apps", "Underline one idea worth keeping")),
        Quest("listen", Category.Listen, "Huberman Lab", "Sleep Architecture", 50,
            "One Huberman Lab episode on how sleep is built, ideally on a walk. Eyes up, screen off.",
            listOf("Queue the Sleep Architecture episode", "Listen for 25 minutes", "Note one takeaway for tonight")),
        Quest("nourish", Category.Nourish, "High-protein lunch", "High-protein lunch", 70,
            "Cook something real instead of grabbing something fast. Aim for at least 30 g of protein.",
            listOf("Pick a recipe with 30 g+ protein", "Cook it", "Eat at the table, phone away")),
        Quest("mind", Category.Mind, "Focus", "10-min focus", 60,
            "Ten quiet minutes to train attention. Count breaths, notice the drift, return.",
            listOf("Set a 10-minute timer", "Breathe and count to ten, repeat", "Note where your attention drifted")),
        Quest("watch", Category.Watch, "The Bear", "The Bear · S3 E4", 90,
            "Earn the couch. One episode after your other quests are done, no second-screening.",
            listOf("Finish at least two other quests", "Watch S3 E4 start to finish", "Rate it and jot one line")),
    )
}
```

## P2.4 `Reducer.kt`: every rule that changes state

The reducer mirrors the prototype's functions one to one. `claim()` is the important one: it applies the quest reward, streak, level-up loop, rank change and milestone rewards in the same order the prototype does, and builds the exact toast sentence. The celebration order is fixed: rank-up first, then each earned reward.

```kotlin
package com.dayquest.domain

// Pure functions: state in, state out. No Android, no database, no clock (callers pass `nowIso`).
// Mirrors claim(), removeQuest(), checkGoals() etc. in dayquest-mobile-app.html.

val STREAK_GOALS = listOf(7, 14, 30)

/** Dota 2 has no Epic tier, so Archon (the tier just below Legend) takes the same place. */
val RANK_REWARD: Map<RankStyle, List<String>> = mapOf(
    RankStyle.Dota to listOf("Archon", "Legend"),
    RankStyle.Mlbb to listOf("Epic", "Legend"),
)

val RankStyle.key: String get() = name.lowercase()          // "dota", "mlbb", "lol", "valorant", "level"
fun tierLow(style: RankStyle, tier: String): Int = style.tiers.first { it.name == tier }.low

data class DisplayRank(val label: String, val tier: String, val numeral: String)

fun displayRank(s: AppState): DisplayRank =
    rankInfo(s)?.let { DisplayRank(it.label, it.tier, it.numeral) }
        ?: DisplayRank("Level ${s.level}", "Level", s.level.toString())

sealed interface Event {
    data class RankUp(val before: DisplayRank, val after: DisplayRank, val gain: Int) : Event
    /** [numeral] is the streak day, the level, or the tier name when [isWord] is true. */
    data class RewardEarned(val key: String, val reward: EarnedReward, val numeral: String, val isWord: Boolean) : Event
}

data class ClaimResult(val state: AppState, val message: String, val events: List<Event>)

/** Claims a quest whose steps are all done. Returns null when it cannot be claimed. */
fun claim(s: AppState, questId: String, nowIso: String): ClaimResult? {
    val quest = s.quests.firstOrNull { it.id == questId && it.id !in s.archived } ?: return null
    if (questId in s.claimed) return null
    if (s.stepsDone[questId]?.all { it } != true) return null

    val beforeRank = displayRank(s)
    val rating0 = rating(s)
    var st = s.copy(claimed = s.claimed + questId, xp = s.xp + quest.xp)
    var msg = "+${quest.xp} XP — ${quest.category} quest complete."

    // small reward (written on the quest)
    s.rewards[questId]?.let { text ->
        st = st.copy(earned = st.earned + (questId to EarnedReward(text, RewardKind.Small, quest.shortTitle, nowIso)))
        msg += " Reward ready: $text."
    }

    // streak
    var newDay = 0
    if (!st.securedToday) {
        newDay = st.streakBase + 1
        st = st.copy(securedToday = true)
        msg += " Streak extended to $newDay days."
    }

    // level-up loop (cap grows 12% per level, rounded to the nearest 10)
    val gained = mutableListOf<Int>()
    var xp = st.xp; var level = st.level; var cap = st.cap
    while (xp >= cap) {
        xp -= cap; level += 1; gained += level
        cap = (Math.round(cap * 1.12 / 10.0) * 10).toInt()
        msg = "Level $level reached! $msg"
    }
    st = st.copy(xp = xp, level = level, cap = cap)

    // rank or level label changed?
    val events = mutableListOf<Event>()
    val after = displayRank(st)
    if (after.label != beforeRank.label) {
        msg = "${if (st.rankStyle == RankStyle.Level) "Level up" else "Rank up"}: ${after.label}. $msg"
        events += Event.RankUp(beforeRank, after, rating(st) - rating0)
    }

    // medium and big rewards
    val (st2, earnedEvents) = checkGoals(st, newDay, gained, nowIso)
    if (earnedEvents.isNotEmpty()) msg += " Reward earned: ${earnedEvents.joinToString(", ") { it.reward.text }}."
    return ClaimResult(st2, msg, events + earnedEvents)
}

/** Order matters: streak, then level-up (first level gained), then rank tiers of the selected game. */
fun checkGoals(s: AppState, newDay: Int, gained: List<Int>, nowIso: String): Pair<AppState, List<Event.RewardEarned>> {
    var earned = s.earned
    var goals = s.goals
    val out = mutableListOf<Event.RewardEarned>()

    fun earn(key: String, kind: RewardKind, text: String, from: String, numeral: String, isWord: Boolean): Boolean {
        if (key in earned) return false
        val reward = EarnedReward(text, kind, from, nowIso)
        earned = earned + (key to reward)
        out += Event.RewardEarned(key, reward, numeral, isWord)
        return true
    }

    if (newDay > 0) goals.streak[newDay]?.let { earn("streak:$newDay", RewardKind.Medium, it, "$newDay-day streak", "$newDay", false) }

    if (gained.isNotEmpty()) goals.level?.let { text ->
        if (earn("level:${gained[0]}", RewardKind.Big, text, "Reached Level ${gained[0]}", "${gained[0]}", false)) goals = goals.copy(level = null)
    }

    val style = s.rankStyle                        // rank rewards belong to the game in use
    RANK_REWARD[style]?.forEach { tier ->
        val key = "${style.key}:$tier"
        val text = goals.rank[key] ?: return@forEach
        if (rating(s) >= tierLow(style, tier)) earn("rank:$key", RewardKind.Big, text, "Reached ${style.label} $tier", tier, true)
    }
    return s.copy(earned = earned, goals = goals) to out
}

// ---- steps -----------------------------------------------------------------------------------

fun toggleStep(s: AppState, questId: String, index: Int): AppState {
    if (questId in s.claimed) return s
    val steps = s.stepsDone[questId] ?: return s
    if (index !in steps.indices) return s
    return s.copy(stepsDone = s.stepsDone + (questId to steps.mapIndexed { i, v -> if (i == index) !v else v }))
}

/** "Start quest" / "Mark next step done": ticks the first unticked step. */
fun advanceQuest(s: AppState, questId: String): AppState {
    val i = s.stepsDone[questId]?.indexOf(false) ?: return s
    return if (i < 0) s else toggleStep(s, questId, i)
}

// ---- quests ----------------------------------------------------------------------------------

fun validReward(text: String?): String? = text?.trim()?.takeIf { it.isNotEmpty() }?.take(40)

fun addQuest(s: AppState, quest: Quest, reward: String? = null): AppState = s.copy(
    quests = s.quests + quest,
    stepsDone = s.stepsDone + (quest.id to quest.steps.map { false }),
    rewards = validReward(reward)?.let { s.rewards + (quest.id to it) } ?: s.rewards,
    selectedQuestId = quest.id,
)

/** Soft delete. Only unclaimed quests can be removed; the caller keeps the id for Undo. */
fun removeQuest(s: AppState, questId: String): AppState? {
    if (s.quests.none { it.id == questId } || questId in s.claimed || questId in s.archived) return null
    val archived = s.archived + questId
    val next = s.quests.firstOrNull { it.id !in archived && it.id !in s.claimed }
        ?: s.quests.firstOrNull { it.id !in archived }
    return s.copy(archived = archived, selectedQuestId = next?.id)
}

fun undoRemove(s: AppState, questId: String): AppState =
    s.copy(archived = s.archived - questId, selectedQuestId = questId)

// ---- rewards ---------------------------------------------------------------------------------

/** Small reward on a quest. Ignored once the quest is claimed. Blank text removes it. */
fun setQuestReward(s: AppState, questId: String, text: String?): AppState {
    if (questId in s.claimed || s.quests.none { it.id == questId }) return s
    val t = validReward(text)
    return s.copy(rewards = if (t == null) s.rewards - questId else s.rewards + (questId to t))
}

fun enjoy(s: AppState, key: String, nowIso: String): AppState {
    val e = s.earned[key] ?: return s
    return if (e.enjoyed) s else s.copy(earned = s.earned + (key to e.copy(enjoyedAt = nowIso)))
}

sealed interface GoalSlot {
    data class Streak(val days: Int) : GoalSlot
    object Level : GoalSlot
    data class Rank(val key: String) : GoalSlot       // "mlbb:Epic"
}

/** Medium and big rewards. A milestone already reached cannot be given a reward. Blank text removes it. */
fun setGoal(s: AppState, slot: GoalSlot, text: String?): AppState {
    val t = validReward(text)
    val g = s.goals
    return when (slot) {
        is GoalSlot.Streak -> {
            if (slot.days !in STREAK_GOALS || slot.days <= s.streak) s
            else s.copy(goals = g.copy(streak = if (t == null) g.streak - slot.days else g.streak + (slot.days to t)))
        }
        GoalSlot.Level -> s.copy(goals = g.copy(level = t))
        is GoalSlot.Rank -> {
            val parts = slot.key.split(":")
            if (parts.size != 2) return s
            val (game, tier) = parts
            val style = RANK_REWARD.keys.firstOrNull { it.key == game }
            if (style == null || tier !in RANK_REWARD.getValue(style) || rating(s) >= tierLow(style, tier)) s
            else s.copy(goals = g.copy(rank = if (t == null) g.rank - slot.key else g.rank + (slot.key to t)))
        }
    }
}

fun completeOnboarding(s: AppState, rawName: String): AppState? {
    val name = rawName.trim()
    return if (name.isEmpty() || name.length > 24) null else s.copy(name = name)
}
```

Rules baked in that are easy to miss:

- A milestone reward already passed cannot be set (`setGoal` ignores streak days at or below the current streak, and rank tiers the rating has reached).
- A level-up reward is for the next level-up only; the slot is cleared when it pays out.
- Rank rewards only fire for the game that is selected. Dota 2 uses Archon and Legend because it has no Epic tier.
- Earned rewards keep their own copy of the text, so editing a quest's reward afterwards changes nothing.
- A claimed quest can neither be removed nor have its reward changed.

## P2.5 `ReducerTest.kt`

The expected numbers and sentences were produced by running the prototype's own `claim()`, `removeQuest()` and `checkGoals()` on the same starting states, so a green test means the Kotlin matches the prototype.

| Scenario (start from the seed) | Result |
| --- | --- |
| Claim Body, Level style | xp 1,738; streak 14; earned `body` (small) and `streak:14` (medium); rating 1,953; toast `+90 XP — Body quest complete. Reward ready: An iced latte. Streak extended to 14 days. Reward earned: A nice dinner out.` |
| Claim Body when today is already secured | no streak reward; toast `+90 XP — Body quest complete. Reward ready: An iced latte.`; rating 1,988 |
| Claim Body at xp 2,450 | level 15, xp 60, cap 2,780; `level:15` big reward; slot cleared; toast starts `Level up: Level 15. Level 15 reached! ...` and ends `Reward earned: A nice dinner out, New running shoes.` |
| Claim Body at level 17, xp 1,000, Mobile Legends | rating 2,337; toast starts `Rank up: Epic V. ...` and ends `..., Pizza night with friends.`; event order: rank-up, streak reward, Epic reward |
| Remove Read then Undo | archived set changes; selection goes to Body, then back to Read |

```kotlin
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
```

## P2.6 Persistence (Room)

Keep `AppState` pure and map to and from the tables in section 10.2. Do not let entities leak into the reducer or the UI.

| `AppState` field | Stored as |
| --- | --- |
| `name`, `level`, `xp`, `cap`, `rankStyle`, `lastBackupAt` | `profile` (one row, `id = 1`) |
| `quests`, `archived`, `rewards` | `quest` (`archived_at`, `reward_text`) plus `quest_step` |
| `stepsDone`, `claimed` | `daily_progress` for **today**: `steps_done` is a bitmask (bit 0 = first step), `claimed_at` set when claimed |
| `securedToday`, `streakBase` | `day_log`: secured if it has today; `streakBase` = consecutive `day_log` days ending yesterday, plus `profile.streak_base` for the days before the app existed |
| `earned` | `reward_earned`; map `source` to the state key (`quest:body:2026-10-26` becomes `body`, others stay as they are) |
| `goals` | `milestone_reward`: `('streak','14')`, `('level','next')`, `('rank','mlbb:Epic')` |
| Stats data | derived from `xp_ledger` and `day_log`, never stored twice |

The repository loads an `AppState`, runs the reducer, then writes the difference in **one transaction**:

```kotlin
class DayquestRepository(private val db: DayquestDb, private val clock: Clock = Clock.systemDefaultZone()) {

    fun observe(): Flow<AppState?> = /* combine the DAO flows and map; emit null before onboarding */ TODO()

    suspend fun claim(questId: String): ClaimResult? = db.withTransaction {
        val before = loadState()
        val today = LocalDate.now(clock).toString()
        val result = claim(before, questId, Instant.now(clock).toString()) ?: return@withTransaction null
        persistClaim(before, result.state, today)
        result
    }

    private suspend fun persistClaim(before: AppState, after: AppState, today: String) {
        // 1. daily_progress.claimed_at for every id in after.claimed - before.claimed
        // 2. one xp_ledger row per newly claimed quest (day, quest_id, category copied, xp)
        // 3. profile: level, xp, cap
        // 4. day_log: insert today when after.securedToday && !before.securedToday
        // 5. reward_earned: one row per key in after.earned.keys - before.earned.keys (unique `source`)
        // 6. milestone_reward: delete ('level','next') when before.goals.level != null && after.goals.level == null
    }

    // The other actions follow the same pattern: load, run the pure function, write the diff.
    suspend fun toggleStep(questId: String, index: Int) { /* ... */ }
    suspend fun addQuest(quest: Quest, reward: String?) { /* ... */ }
    suspend fun removeQuest(questId: String): Boolean { /* sets archived_at, returns false if claimed */ TODO() }
    suspend fun undoRemove(questId: String) { /* clears archived_at */ }
    suspend fun setQuestReward(questId: String, text: String?) { /* ... */ }
    suspend fun setGoal(slot: GoalSlot, text: String?) { /* ... */ }
    suspend fun enjoy(key: String) { /* sets enjoyed_at */ }
    suspend fun completeOnboarding(name: String): Boolean { /* creates profile, seeds quests and sample rewards */ TODO() }
}
```

Day rollover: compute "today" from the clock on every call, never cache it. At the first call of a new day, `claimed`, `stepsDone` and `securedToday` read as empty because there are no `daily_progress` or `day_log` rows for the new date.

## P2.7 ViewModel and UI state

One ViewModel holds `AppState` plus the things that only exist on screen. Celebrations are a queue; the head is on screen, and dismissing it reveals the next one.

```kotlin
data class Toast(val message: String, val undoQuestId: String? = null)

sealed interface Celebration {
    data class RankUp(val before: DisplayRank, val after: DisplayRank, val gain: Int) : Celebration
    data class Reward(val key: String, val reward: EarnedReward, val numeral: String, val isWord: Boolean) : Celebration
}

sealed interface Dialog {
    data class SetQuestReward(val questId: String) : Dialog
    data class RemoveQuest(val questId: String) : Dialog
    data class SetGoal(val slot: GoalSlot) : Dialog
    object NewQuest : Dialog
}

data class UiState(
    val loaded: Boolean = false,
    val app: AppState? = null,                    // null = no profile yet, show onboarding
    val toast: Toast? = null,
    val celebrations: List<Celebration> = emptyList(),
    val dialog: Dialog? = null,
    val restoreError: String? = null,
)

class MainViewModel(private val repo: DayquestRepository) : ViewModel() {
    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    init { viewModelScope.launch { repo.observe().collect { app -> _ui.update { it.copy(loaded = true, app = app) } } } }

    fun claim() = viewModelScope.launch {
        val id = _ui.value.app?.selectedQuestId ?: return@launch
        val result = repo.claim(id) ?: return@launch
        _ui.update { it.copy(toast = Toast(result.message), celebrations = result.events.map(::toCelebration)) }
    }

    fun dismissCelebration() = _ui.update { it.copy(celebrations = it.celebrations.drop(1)) }
    fun removeQuest(id: String) = viewModelScope.launch {
        if (repo.removeQuest(id)) _ui.update { it.copy(dialog = null, toast = Toast("Removed.", undoQuestId = id)) }
    }
    fun undoRemove(id: String) = viewModelScope.launch { repo.undoRemove(id); _ui.update { it.copy(toast = null) } }
    // toggleStep, advance, addQuest, setQuestReward, setGoal, enjoy, setRankStyle, onboarding, backup: one function each
}

private fun toCelebration(e: Event): Celebration = when (e) {
    is Event.RankUp -> Celebration.RankUp(e.before, e.after, e.gain)
    is Event.RewardEarned -> Celebration.Reward(e.key, e.reward, e.numeral, e.isWord)
}
```

Toast behaviour (section 4.10): one visible at a time, auto-dismiss after 3.2 seconds, 8 seconds when it carries an Undo action, and the timer pauses while the toast is touched or focused.

## P2.8 Screens and components to build

Build shared components first, then screens. Each row lists the spec section to follow.

| Composable | Spec | Notes |
| --- | --- | --- |
| `DqTheme`, tokens, `Serif`, `Sans` | 2 | Dark only; bundle both fonts; tabular figures |
| `DqIcon(icon, size)` | 8 | 15 icons from the VectorDrawables; tint at the call site |
| `XpBar(progress)` | 4.1 | 8dp, `accent` fill, 500 ms easing |
| `QuestTile`, `StepRow`, `Chip`, `Panel`, `RewardRow` | 4.1, 4.2, 4.7 | Quest colours from the table in section 2; dark ink text |
| `BottomBar` (4 tabs + centre +) | 2 | Custom; no indicator pill |
| `TodayScreen` | 4.1 | Level panel, streak panel, Reward ready, grid; hints from 4.7.1 |
| `QuestDetailScreen` | 4.2, 4.2.1, 4.2.2 | Reward row, steps, Remove quest, main button |
| `LibraryScreen`, `NewQuestSheet` | 4.4, 4.5 | `ModalBottomSheet`; validate on blur and submit |
| `StatsScreen` | 4.6 | Chart drawn with `Canvas` |
| `ProfileScreen` | 4.7, 4.7.1 | Rank style chips, rank card, ladder, rewards, milestone rewards, backup |
| `OnboardingScreen` | 10.1 | Name page, restore link |
| `RankUpScreen`, `RewardEarnedScreen` | 4.8, 4.8.1 | One shared component; plays once |
| `ConfirmDialog`, `RewardDialog` | 4.2.1, 4.2.2, 4.7.1 | Focus rules in the spec |

For every screen add: a `@Preview` for populated, empty and error states, a TalkBack check (labels, roles, live regions), and a large-font check.

## P2.9 Onboarding, backup and restore

Follow section 10 exactly; the order inside the step is:

1. First launch detection: `observe()` emits `null` until a `profile` row exists, which shows `OnboardingScreen`.
2. `completeOnboarding(name)` creates the profile, seeds the six sample quests and sample rewards, then the app lands on Today with the toast "Welcome, {name}. Your first six quests are ready."
3. `BackupManager.backupTo(uri)` and `inspect(uri)` / `restore(summary)` as in section 10.7, wired to `ActivityResultContracts.CreateDocument` and `OpenDocument`.
4. Profile panel states: never backed up, last backup date, confirm replace, restore error.
5. Tests from section 10.9.

## P2.10 Tests for Part 2

| File | Kind | Covers |
| --- | --- | --- |
| `ReducerTest.kt` | JVM | P2.5 (given) |
| `MapperTest.kt` | JVM | `AppState` to entities and back: step bitmask, reward `source` keys, goals rows, streak from `day_log` |
| `RepositoryTest.kt` | Room in-memory (Robolectric or `androidTest`) | claim is atomic (force a failure and check nothing changed), day rollover, remove/undo, onboarding seeds data |
| `BackupManagerTest.kt` | JVM or `androidTest` | The rejection list and round trip in section 10.9 |
| `MainViewModelTest.kt` | JVM with Turbine and `kotlinx-coroutines-test` | claim produces toast plus celebrations in order; Undo toast lifetime; dismiss reveals the next celebration |
| `OnboardingScreenTest.kt`, `QuestFlowTest.kt` | Compose UI tests | Name validation (blur and submit); start, tick, claim; dialog focus lands on **Keep it**; reduced motion |

Gradle test dependencies (use the current stable versions): `kotlin-test`, `kotlinx-coroutines-test`, `app.cash.turbine:turbine`, `androidx.room:room-testing`, `org.robolectric:robolectric`, and for Compose `androidx.compose.ui:ui-test-junit4` and `ui-test-manifest`.

## P2.11 Swift parity

Names stay the same so the two apps can be compared line by line.

| Kotlin | Swift |
| --- | --- |
| `Domain.kt` | `Domain.swift`: `struct AppState` with default values for every property (the existing `RankEngineTests.swift` builds an `AppStore`; keep `AppStore` as the observable wrapper that holds an `AppState` and forwards `level`, `xp`, `cap`, `claimed`, `rankStyle`) |
| `Reducer.kt` | `Reducer.swift`: free functions or `extension AppState` with the same names and the same sentences |
| `ReducerTest.kt` | `ReducerTests.swift`: port each test with the identical expected strings and numbers from P2.5 |
| Room entities and `DayquestRepository` | GRDB records and a `Repository` actor (section 10.8) |
| `MainViewModel` + `StateFlow` | `@Observable` store with the same `UiState` fields |
| Composables | SwiftUI views with the same names |

## P2.12 Definition of done for Part 2

- [ ] `RankEngineTest` and `ReducerTest` pass without edits to either test.
- [ ] Every row of the P2.2 table is ticked and its spec section is satisfied.
- [ ] The acceptance checklist in section 12 passes on a real device.
- [ ] The Kotlin and Swift reducers produce byte-identical toast sentences for the five scenarios in P2.5.
