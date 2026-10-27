# Dayquest — build guide for Android (Kotlin) and iOS (Swift)

Reference UI: `dayquest-mobile-app.html`. Open it in a browser. It is a working prototype with three phones side by side:

1. **Cover** (a poster, not an app screen; skip it or use it as a splash/onboarding page)
2. **Today** (the app: Today, Library, Stats and Profile tabs, plus the New quest sheet)
3. **Quest detail** (steps, claim button, rank-up screen)

Build the app screens (2 and 3) natively. Match the prototype's behaviour and numbers exactly; every value below is taken from it. Use the prototype's functions as the logic reference: `renderToday`, `claim`, `rankInfo`, `rating`, `renderStats`, `showRankUp`.

If this guide and the prototype ever disagree, the prototype wins. The Kotlin and Swift snippets are reference code written without a compiler: expect small fixes when you build them.

Targets: **Android** Kotlin + Jetpack Compose + Material 3 (minSdk 26). **iOS** Swift + SwiftUI (iOS 17+, uses `@Observable`).

---

## 1. Product in one paragraph

Habits become **quests**. Each quest has steps and an XP reward. Finishing all steps and tapping *Claim* adds XP, fills the **level** bar, and secures today's **streak**. The user can choose to *see* their progress as plain levels or as a rank on a game ladder (Dota 2, Mobile Legends, League of Legends, Valorant). The rank is a display style on top of one internal rating number. When the displayed rank changes, a full-screen celebration appears.

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
- Icons: 1.8 stroke outline, 22 in tab bar and tiles. The exact paths for all 14 icons, as ready-to-paste Android VectorDrawables and SwiftUI `Path` code, are in section 8.

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
  level: Int = 14, xp: Int = 1648, cap: Int = 2480     // cap grows x1.12, rounded to nearest 10, per level
  streak: Int = 12, securedToday: Boolean = false       // today is a Sunday in the prototype
  quests: [Quest]                                      // seeded with the six below
  stepsDone: Map<QuestId, [Boolean]>
  claimed: Set<QuestId>
  selectedQuestId: QuestId
  rankStyle: RankStyle = Level
  libraryCategory: Category? = null (All)
  addedLibraryIds: Set<String>
}
```

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

### 4.1 Today (home tab)

Top to bottom:

1. **Header.** Serif title "Morning, Sam." and a muted line. Line is `"{streak} days running. Finish one quest today and it reaches {streak+1}."` before securing, and `"Streak secured. {streak+1} days running."` after. Right: 44 bell button (outline icon); tap shows toast "No new notifications."
2. **Level panel** (`surface`). Left: title and subtitle. Right: big figure. Under them an 8dp bar (track `#3A3A37`, fill `accent`) and a muted foot line.
   - `Level` style: title "Level 14", subtitle "Experience", figure `1,648 / 2,480 XP`, foot `832 XP to Level 15`.
   - Rank styles: title = rank label, subtitle = `"{Game} rank"`, figure + unit from the rank engine (section 5), foot from the rank engine. Bar = progress inside the current division.
3. **Streak panel.** Title `"{n}-day streak"`, right note "Today still open" / "Secured for today". Seven day dots Mon-Sun, 30 diameter. Mon-Sat are `accent` with a dark check. Sunday (today) is a 1.5 outlined ring until secured, then `accent`.
4. **Today's quests.** Header row "Today's quests" and `"{n} left · {xp} XP to earn"` (or "All cleared"). Two-column grid of quest tiles, 8 gap, min height 136. If the count is odd, the last tile spans both columns (min height 104).
   - Tile: glyph (22) top-left; `+{xp} XP` top-right (600, 13); category (12, secondary ink); short title (15/600); at the bottom one 4dp segment per step (filled = done).
   - Tap = select this quest and open **Quest detail**. The selected tile has a 2dp `text` outline offset 2dp outside.
   - When every quest is claimed show a dashed empty box: "Every quest cleared." / "Your streak is safe. New quests arrive at 6:00 tomorrow."

### 4.2 Quest detail

- Top row: 44 back button and "Quest".
- **Hero** (22 radius, quest colour field, dark text): glyph 28, serif title 40, story text 15, then `Reward` + `+{xp}` (32) + `XP`.
- **Steps** header "Steps" and `"{n} of {total} done"`. Each step is a 56+ high row (`surface`, 1px `border`, 16 radius) with a 24 round check on the left. Unticked = 1.5 outline `#8C8C88`. Ticked = `accent` fill, dark check, text struck-through and muted. Tap toggles. Rows are disabled after claiming.
- **Main button** (pinned above the tab bar, 56 high pill, `text` fill):
  - 0 steps done: **Start quest** (ticks the first step)
  - some done: **Mark next step done** (ticks the next unticked step)
  - all done: **Claim +{xp} XP**
  - claimed: **Completed · +{xp} XP earned** (disabled, `#2A2A28` fill, `#B4B4B0` text)

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
- **Reward**: radio group of three equal buttons, 56 high, two lines: Easy 30 XP, **Medium 60 XP (default)**, Hard 90 XP.
- **Create quest** (primary pill) and **Cancel** (text button).
- Create: steps become `["Get started", "Finish: {name}"]`, story "Your own quest. Do the steps, tick them off, then claim the XP." Add to Today, select it, close the sheet, return to the Today tab, scroll the new tile into view, toast `"Quest created: {name} (+{xp} XP)."`
- Focus goes to the name field on open and back to the + button on close. Back/Escape closes it. Trap focus inside.

### 4.6 Stats tab

Title "Stats", line `"Week of 19 Oct · {weekTotal} XP so far"`.

1. **KPI strip** (one `surface` panel, three equal columns split by 1px `border` dividers): Streak `{streak}` days; Longest `max(12, streak)` days; Quests `{29 + claimedToday}` this week. Values 30 regular.
2. **XP per day**: seven bars Mon-Sun. Values `[310, 280, 420, 350, 190, 400, todayXp]` where `todayXp` = sum of XP claimed today. Y scale 0-500 with three faint gridlines (0, 250, 500). Past bars `#6A6A65`, today's bar `accent`. Value label above each bar (11), day letters below (12; today in `text`/600). Bars have 6dp top radius. Right of the panel title: `"Today {todayXp} XP"`. Accessible label: all seven values.
3. **Where the XP came from** (this week): rows sorted by XP descending, each name + `"{n} XP"` and a 4dp bar scaled to the largest. Largest bar `accent`, others `#D9D9D4`. Base values: Body 520, Read 380, Listen 250, Nourish 290, Mind 330, Watch 180 (sum 1,950 = sum of the six past days). Add each claimed quest's XP to its category.
4. **Last 5 weeks**: 35 dots (7 columns, Mon first), 26 diameter. Index 0 is Mon 21 Sep, index 34 is today (Sun 25 Oct). Missed indexes: `[2, 6, 10, 12, 21]`. Index 34 is on only when secured. Dots: on = `accent`, missed = `#2E2E2E`, today open = outlined ring. Right of title: `"{doneDays} of {34 or 35} days"`.

### 4.7 Profile tab

Title "Profile", line `"Sam · Level {level} · {rating} rating"`.

1. **Show my level as** (panel): radio chips Level, Dota 2, MLBB, LoL, Valorant. Foot note: "Display only. Your level and XP stay the same. Every level you clear is worth 140 rating, every quest you complete today adds 35, and each game's ladder is mapped onto that." Changing it shows a toast and updates Today, Profile and the cover.
2. **Rank card**: muted "Your {Game} rank" (or "Your level"), serif rank name 44, figure + unit and right-aligned `"Level {level} · {rating} rating"`, the bar, and the foot line.
3. **Ladder** (hidden in Level style): one row per tier with a dot, name and the rating range (`"1,540–2,309"`, last tier `"5,420+"`). Passed tiers = `accent` dot, current tier = `raised` row, 600 weight, `accent` dot with a 2dp `text` ring, `aria-current`. Upcoming = `muted`, `track` dot.

### 4.8 Rank-up screen

Full-screen over the whole app (including the tab bar), background `accent`, text `accentInk`.
Layout (top padding 64, side 22): kicker "Rank up" or "Level up" (16/600); tier name in serif 60; huge numeral in serif 250 (division/star, or the new level in Level style); line `"From {before.label} to {after.label}."` (16, `#2F3D00`); at the bottom two facts split by a 1.5px `accentInk` rule: `{rating}` "Rating, +{gain} from this quest", and `{n}` "quests done today"; then a dark pill **Continue** (`#141A00` fill, accent label) and a text button **See my ladder** (closes and opens Profile).
Alert-dialog semantics; focus starts on Continue; focus is trapped between the two buttons; back/Escape = Continue. The numeral entrance plays once.

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

All 14 icons are drawn on a **24 x 24** grid as **round-capped, round-joined strokes with no fill**. Stroke width differs per icon (listed below). Tint them with the text colour at the call site: `muted` for inactive tabs, `text` for active tabs, the quest ink colour on tiles. Sizes used: 22 in the tab bar, tiles and rows, 24 in the library list, 28 in the quest hero, 20 for the bell, 22 for Back.

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
| `g-body` | Body | 1.8 | Body quests |
| `g-read` | Read | 1.8 | Read quests |
| `g-listen` | Listen | 1.8 | Listen quests |
| `g-nourish` | Nourish | 1.8 | Nourish quests |
| `g-mind` | Mind | 1.8 | Mind quests |
| `g-watch` | Watch | 1.8 | Watch quests |

Each rect and circle in the source art has been converted to path data so every icon is one path. Where the original used arcs, the Android version keeps the arcs (VectorDrawable supports `A`/`a`); the Swift version converts them to cubic curves because SwiftUI `Path` has no SVG arc command.

### 8.1 Android: VectorDrawable

One file per icon in `res/drawable/`. Names: `ic_today.xml`, `ic_library.xml`, `ic_stats.xml`, `ic_plus.xml`, `ic_profile.xml`, `ic_bell.xml`, `ic_back.xml`, `ic_check.xml`, `ic_body.xml`, `ic_read.xml`, `ic_listen.xml`, `ic_nourish.xml`, `ic_mind.xml`, `ic_watch.xml`. The stroke colour in the file is a placeholder; always tint at the call site with `Icon(tint = ...)`.

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
    case today, library, stats, plus, profile, bell, back, check
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

## 10. States to add beyond the prototype

The prototype only shows the populated state. Build these natively:

- **Loading:** keep the layout; replace values with flat grey bars at real size (no shimmer). Panel bars at 0.
- **Empty:** all quests cleared (already designed, section 4.1); Library filter always has items.
- **Error:** inside the failed block, one sentence with cause and time ("Couldn't load your quests. Last updated 09:14.") and a **Retry** text button. Keep user input. After three failed retries, show "Contact support" and a copyable error ID.
- **Edge:** 40-character quest names (truncate to 2 lines in the tile, one line in the cover rows); 100+ quests (grid is lazy; cap the Library at the chosen category); level 50+ (the cap grows 12% per level; format with separators).
- **Offline:** claiming queues locally and syncs later; never block the celebration.

## 11. Acceptance checklist

- [ ] Seed state shows: Level 14, `1,648 / 2,480 XP`, `832 XP to Level 15`, streak 12, six tiles, 430 XP to earn.
- [ ] Claiming the first quest (all four steps) adds 90 XP, secures the streak (13), toasts "+90 XP — Body quest complete. Streak extended to 13 days."
- [ ] In Dota 2 style the third claim changes "Crusader · star 3" to "Crusader · star 4" and shows the rank-up screen with the numeral 4.
- [ ] The rank engine test suite in section 9 passes (the four seed values, boundaries, top tiers and the invariant sweep).
- [ ] Switching rank style updates Today panel, Profile card, ladder and the cover numeral at once.
- [ ] New quest: empty name shows the error on blur and keeps the sheet open; a valid name creates, selects, scrolls to the tile.
- [ ] All text meets 4.5:1; all targets 44pt/48dp; focus rings visible; reduced motion removes movement but keeps the information.
- [ ] No gradients, glows, blur, or shadows at rest anywhere. Lime appears only on progress.
- [ ] No game logos or badge art are used; ranks are names and numerals only.
- [ ] All 14 icons render from the section 8 code (no image assets), with round caps and joins, tinted by the surrounding text colour.
