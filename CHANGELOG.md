# Changelog

## Unreleased

- **LevelledMobs leaves critters alone.** LevelledMobs 4 levels every mob by default, named or tamed, so a critter sent out got its own level on top of MobSquad's growth: a level 1 creeper critter came out with 34 health where it should have 20, along with LevelledMobs' extra damage and speed and a second level in its nametag. Each critter is now marked with LevelledMobs' own opt-out key, `levelledmobs:nolevel`, before it enters the world, so LevelledMobs never touches it. Nothing changes without LevelledMobs, and no LevelledMobs setting is needed. Critters are never saved with the world, so there are no old levelled critters to fix.
- Tested: unit tests (`./gradlew clean build`, including the key's exact name). A new bot section (`MS_TEST_LM=1`, `only=bunkhouse,levelledmobs`, LevelledMobs 4.5.3.2 with its default rules, fetched by `test/fetch-jars.mjs`) checks that LevelledMobs levels a wild zombie without giving it a real name (so coaxing and fights still see it as wild), then that a creeper critter carries the key, has no LevelledMobs level, keeps max health 20 and keeps its name. On throwaway Paper 26.1.2 it passed 44 of 44; the same run without the fix failed the three critter checks (health 34). Not tested: 26.2 and 26.3 (the run was stopped to give way to another test server; on 26.3 bots can't use the Bunkhouse anyway, so only the wild-zombie half runs there); the default suite (nothing else changed); a LevelledMobs config with custom rules; a real game client.

## 0.13.0

Season 1.

- **Bunkhouses from the console.** `/mobsquad bunkhouse add`, `remove` and `check` take `<x> <y> <z> [world]`, for admins and the console. Each makes the same vault check, loads the chunk if it has to, and refuses anything that isn't a vault, a height outside the world and a part of the world that was never generated (without generating it). A second `add` says it's already a Bunkhouse. The look-at-block form works as before.
- **Season 1.** Holler, Yonder and Backcountry keep their tags, steps and old rewards, so anyone partway through carries on. A fourth chapter, the Round-up at the Bunkhouse, asks the player to coax a new critter, send it out from a Bunkhouse and bring it to Ranger Juniper. It sets `season1_done`. Anyone who has finished Backcountry is offered it, and it has no switch.
- **Rewards.** Each of the four chapters adds a keepsake journal line and two vittles. The finale also gives a mail-carrier fox through `/mobsquad give`. Earlier finishes aren't paid back.
- **Help.** After 15 minutes on one of the finale's first two steps, Juniper leaves a tip in the journal. Nothing gets harder and no goal changes. There's no help tied to dying.
- **New hooks:** `/mobsquad roundup <player> start|stop` (admins and the console), `%mobsquad_roundup_coaxed%`, `%mobsquad_roundup_sent%` and `%mobsquad_roundup_ready%`. Progress is saved in the player's file. BetonQuest checks the placeholders once a second with its own `delay` objective, and MobSquad still has no BetonQuest code.
- **Changed for the Ten Winds:** besides `winds_open`, each of those offers now needs `season1_done` and its Season's switch: `season2_open` for Wind-Signs, Holding the Line and the Far Meadow, `season3_open` for the wind-rings and the Ten Winds finale. Their steps, ring levels, the adults' watch and their rewards are unchanged. With `winds_open` off they stay hidden whatever the other switches say. Seasons 2 and 3 aren't built: no Season finales, no `season2_done`, so Season 3's offers don't wait for Season 2.
- **Upgrading:** replace the BetonQuest copy of `package.yml` by hand, keeping your Ranger lines and spots (README).
- New player-facing lines are in `docs/season1-text.md`. The owner approved the player lines on 2026-11-27.
- Tested: 97 unit tests (`./gradlew clean build`). Bots on throwaway Paper 26.1.2 (build 74), 26.2 (135) and 26.3 (263) servers, 2026-11-27, one at a time, every run below passing: the default suite 242, 242 and 138 checks; `only=bunkhouse,season1` with BetonQuest 3.3.0-DEV-61 and PlaceholderAPI 83, 83 and 70; `only=chapters` 52, 52 and 52; `only=winds` with LuckPerms 103, 103 and 96. The `season1` section starts Holler on the 0.12.0 package, swaps in this one mid-chapter and plays to the finale with a real coax and a restart in the middle. Run `chapters` and `winds` separately, since `chapters` ends by restarting the server without its bots. Not tested: on 26.3 the Bunkhouse menu, sending out, bringing the critter to Juniper and the claim check (bots can't use blocks; the finishing actions ran by command); the AuraSkills and Multiverse runs; clicking a Citizens NPC; the real 15-minute wait; real crafting and smelting; a GriefPrevention admin claim (a player's claim was used); an upgrade on a copy of a real server; a real game client; a child reading the lines.

## 0.12.0

Works with Posse 0.11.0 (formerly Mayor); the Mayor name is no longer recognized (D196, D292).

- **The Ten Winds adult condition is `permission posse.circle.adults`.** It was `permission mayor.circle.adults`. A player who holds only the old permission is not an adult here: they get the kid lines and the kids' goals, so nothing widens. Before the first start, give adults `posse.circle.adults` (Posse's release notes give the safe order). A server that changed the condition to its own permission is unaffected.
- **Typed chat in a Ranger conversation now fails closed (kids' safety).** MobSquad holds back what a player types while they talk to the Ranger. It used to hold a message only when the `quests.hold-chat-when` placeholder came back as exactly `true`, so a missing BetonQuest PlaceholderAPI expansion, an unresolved placeholder or a slow answer let typed words reach public chat. Now only a clear `false` lets a message through. With BetonQuest installed and the expansion missing, chat is held for everyone and the log warns once, naming the fix (`/papi ecloud download BetonQuest`). A bare number still answers a conversation. Without BetonQuest nothing is held. A unit test covers the unresolved placeholder, and the winds section (which checks that typed words are held during a conversation) passed again with this code: 99, 99 and 92 on 26.1.2, 26.2 and 26.3. The missing-expansion case has no bot test.
- No other behavior changes.
- Tested: unit tests; the Ten Winds bot section (`only=winds` with BetonQuest, LuckPerms and PlaceholderAPI) on throwaway Paper 26.1.2, 26.2 and 26.3 (99, 99 and 92 checks, all passed; the winds section counts differ on 26.3 where bots can't place blocks), with the permission `posse.circle.adults` set through LuckPerms, and a check that a player holding only `mayor.circle.adults` still gets the kid lines. Not tested: the rest of the MobSquad suite (nothing else changed), a Posse jar on the server (the condition is a plain permission, so the bots use LuckPerms alone), a real game client.

## 0.11.0

The Ten Winds story arc (D289), a story that follows the three tier chapters.

- **Five new chapters in the same BetonQuest package,** one at a time, in this order: Wind-Signs, the Ten Winds, Holding the Line, the Far Meadow and the finale. Ranger Juniper offers the next one, and "Maybe later" moves on to the next open offer, so nobody is stuck behind one job. The finale needs the three tier chapters and any two of the four new ones. None of it changes a level, a tier or a critter; the rewards are journal lines and the Ranger's books.
- **The path picks itself.** The story reads a player's own play once (blocks placed, hostile mobs beaten, distance walked, animals bred) and keeps the result as a tag. It changes the Wind-Signs step (stone or plank base with 40 blocks, chase off five mobs, bring four berries, or visit three signs), the Ranger's wording and which offer comes first. It never changes a reward. A tie, or nothing counted yet, is the all-of-the-above path.
- **Kids and adults get the same rewards.** Adults get larger goals and grown-up wording: 15 hostile mobs at dusk or night against 8 at any hour, four landmarks against three. Kid or adult fails closed: the adult version shows only to a player with the permission `mayor.circle.adults`; no group, or a permission that can't be read, gets the kid lines, which are 12 words or fewer, and goals stay in the kids' own worlds. A kid at an adult's spot, or an adult at a kid's, does nothing.
- **Wind-rings read what a player owns.** Ring one opens at a critter of level 10, ring two at 20, ring three at 30, through the new `%mobsquad_top%`, and never depends on a coated critter being out. The spirit coat glow in `ninos_magic` is untouched.
- **Spots are named constants an admin sets.** 21 named locations (wind-signs, rings, landmarks, meadows) sit under `constants:` in the package, all shipped unset, so none is hardcoded and nothing is reachable until an admin sets it. The README has the placement steps, with claim protection. The new chapters stay off until an admin sets the `winds_open` global tag.
- **New hooks, all admin or read-only and fail closed:** `%mobsquad_top%` (highest level owned), `%mobsquad_style%` and `%mobsquad_holding_bell%`; `/mobsquad book <player> <id>` hands over one of the Ranger's four written books (by Ranger Juniper, tattered so they can't be copied, never a map, no name and no place); `/mobsquad dinnerbell <player>` gives the finale's bell by console, so nobody types a command (D284).
- **Typed words stay private during a conversation (found while testing).** BetonQuest sends whatever a player types in a conversation to public chat; only a typed number answers it. MobSquad now holds back typed text while BetonQuest says the player is in the Ranger's conversation, and tells them to click. `quests.hold-chat-when` in `config.yml` sets the check (empty turns it off).
- **Rewrites to the three tier chapters,** words only: the journal and the Ranger say the first wind stirs, the campfire is a signal fire and the Iron Golem guards the first wind-gate. Steps, rewards and unlocks are unchanged.
- **Found while testing, not fixed here:** BetonQuest's own quest journal book names its owner as author. That is BetonQuest's, and 0.10.0 had it too; the Ranger's books don't.
- Not built as drafted: the Iron Golem's journal page gets no second line about the watch (a page is MobSquad's own text, so the watch is a quest journal entry), and the finale book is one book with each finished chapter's line in the quest journal. See `docs/decisions.md`.
- Tested: unit tests (82, 20 of them new: the play style, the top level, the books, kid and adult failing closed, 12-word kid lines, no hardcoded spots, effects to one player, ring gates, 8 and 15 kills, optional kid watch, the chain of offers, no typing). Bots on throwaway Paper servers with BetonQuest 3.3.0-DEV-61, LuckPerms 5.5.71 and PlaceholderAPI, 2026-11-24: a new "The Ten Winds" section passed 98 of 98 on 26.1.2 and 26.2 and 91 of 91 on 26.3 (kid path with no group, adult path with LuckPerms, the rings' level gates with nothing out, a second player in the ring getting nothing, the watch with real kills of 8 and 15, landmarks 3 and 4, the dinnerbell finale, every book read off the server, typed words held back). The full suite passed on 26.1.2 and 26.2 (215 of 215 each). Not tested: the full suite and a rerun of the three tier chapters' section on 26.3 (the free disk fell under the 42 GB limit during the run; the tier chapters section passed 50 of 51 on all three, the one miss was an out-of-date count in the test, since corrected), block placing on 26.3 (bots can't), clicking a Citizens NPC (a console action started the conversation), real crouching, anything in a real game client, and the one Ranger line changed after the runs (the fighter's watch offer, now gentler). A tag stood in for the three tier chapters and for a builder's play history where a test needed them.

## 0.10.0

Quest chapters for Holler, Yonder and Backcountry.

- **Three BetonQuest chapters, one per tier.** Ranger Juniper gives them in order, three steps each. Holler: pick six sweet berries, gather four seeds from tall grass, craft a note block (opens the fox page). Yonder: craft four arrows, a target block and a campfire (opens the creeper page). Backcountry: pick a poppy, smelt four iron ingots, craft shears (opens the Iron Golem page). Finishing a chapter runs `/mobsquad unlock` and `/mobsquad page` from the console; it never changes a level. Players only talk to the Ranger and read their journal, and never type a command.
- **Installing them.** MobSquad copies `package.yml` to `plugins/MobSquad/quests/mobsquad/` the first time it starts and never overwrites it. An admin copies that folder into BetonQuest's `QuestPackages`, points the Ranger at a Citizens NPC and runs `/bq reload`. The README has the steps.
- **`quests.tier-unlock` now defaults to `either`:** a tier opens at its level or by its chapter. Without BetonQuest nothing ever runs the unlock command, so `either` gives exactly the level-only result. A server whose `config.yml` already says `level` keeps it until an admin changes it. A bad value still falls back to `level`.
- MobSquad still has no code in BetonQuest and no dependency on it.
- The message when a tier opens now points at La Ménagerie, your critter journal, instead of naming a command (D284).
- Found while testing: holding seeds next to a wild chicken starts MobSquad's coaxing puzzle and cancels vanilla breeding (already the case before, noted in the code). So no chapter step asks for breeding chickens.
- Tested: unit tests (62, 8 of them new: the unlock rule in every mode, either equals level when no quest ever ran, no BetonQuest code in MobSquad, the package's names all exist, each chapter unlocks only its own tier and a page of the same tier, text is short and gentle and asks for nothing personal, no command in any player text). Bots on throwaway Paper servers with BetonQuest 3.3.0-DEV-61 (the live build) and PlaceholderAPI: the chapters section passed 51 of 51 on Paper 26.1.2, 26.2 and 26.3, and the rest of the suite passed again on all three (counts in the README). The bots ran both the conversation and the steps. Not tested: clicking a Citizens NPC (Citizens wasn't on the test server, so the conversation was started by an action), real crafting and smelting (those two kinds of step were completed with BetonQuest's own objective command), the Ranger's text and the journal book in a real game client.

## 0.9.0

Quest chapters.

- **Quests can open tiers.** `quests.tier-unlock` in `config.yml`: `level` (the default; tiers open by Wranglin' level, as before), `either` (the level, or sooner through a quest) or `quest` (only a quest opens Holler, Yonder and Backcountry; Yard is always open, so nobody is locked out of everything). Messages about a locked critter say which way opens it.
- **New admin commands for quests:** `/mobsquad unlock <player> <tier>`, `/mobsquad lock <player> <tier>` and `/mobsquad page <player> <critter>`. A quest plugin runs them from the console, for example through BetonQuest's command event. The player is told when a tier or page opens. A quest unlock is saved in the player's file as a `tier` line; older versions skip that line, and older files load with none.
- **Placeholders, if PlaceholderAPI is installed:** `%mobsquad_level%`, `critters`, `pages`, `tier_<tier>`, `page_<critter>`, `has_<critter>` and `best_<critter>`, for quest conditions. Only the player's own progress, only while they're online, and nothing about anyone else or where they are.
- MobSquad doesn't link to BetonQuest's code, so it works with any BetonQuest version (the live server runs a 3.3 development build) and with other quest plugins.
- Tested: unit tests (54: every unlock mode, Yard always open, the setting and a bad value, saving and old files, every placeholder and unknown ones). Bots on throwaway Paper servers (2026-11-22): the full suite passed on 26.2 (215 of 215) and on 26.3 (113 of 113, where the menu sections can't run), including a new quests section with PlaceholderAPI 2.12.3: every unlock mode, `unlock` and `lock`, the level placeholder, an unknown placeholder and the Yard refused. Not tested yet: a real BetonQuest chapter, and anything in a real game client. No chapters are written yet; they're server content.

## 0.8.1

- **Pigs, cows, chickens and frogs also glow in their coat.** They have only three looks each, so a look repeats for three or four coats; now they also glow in the coat's color and leave its trail, so every coat looks different. Wolves and cats keep the variant only. The list is `glow-too` in `coats.yml`. A copy of `coats.yml` already in `plugins/MobSquad` without that line gets the built-in list. A name that isn't a variant critter is skipped with a warning.
- **No-glow worlds.** A glow outline shows through walls, so in a kids' world it could lead others to where a kid is building. Worlds listed under `spirit-coats.no-glow-worlds` in `config.yml` show coats without the outline: critters keep their variant and their coat's trail, and particles don't show through blocks. The outline goes off as soon as the owner enters such a world, the trail there is sent only within 32 blocks, and a no-glow world that isn't also a coat world (a likely typo) is warned about in the log. Empty by default.
- Tested: unit tests (46). The bot suite's coat checks were updated for the chicken's glow and no-glow worlds but haven't been rerun yet; that run comes before any live install. Not seen in a real game client.

## 0.8.0

Spirit coats.

- **A new coat every 10 levels**, ten per critter: coat 1 at level 10 up to coat 10 at level 100. Coats are earned by level and a critter keeps them. Leveling past a multiple of 10 tells the owner the coat's name.
- **Shown only in magic worlds.** `spirit-coats.worlds` in `config.yml` lists them, by folder name, and it's empty by default, so nobody sees a coat until an admin lists a world. Elsewhere critters look as they always did. Unlisting a world (`/mobsquad reload`) or leaving one takes the look off at once, and entering one puts it on.
- **What a coat looks like** is in the new `coats.yml` (copied to `plugins/MobSquad` the first time; edit it there). Each coat has a plain name, a glow color and a particle: Ember, Marigold, Sunbeam, Meadow, Pine, Lagoon, Frost, Twilight, Blossom and Starlight. Wolves, cats, pigs, cows, chickens and frogs wear a real variant, set per coat in the same file. Every other critter glows in the coat's color and leaves a light particle trail. A bad line in the file is skipped with a warning in the log, and the built-in value stays.
- **Cheap particles.** About 4 a second per glowing critter, sent only to players inside their own view distance, and never to a player who set particles to minimal in their game options. Players on decreased get half.
- **Choosing a coat.** Right-click a critter in the Bunkhouse for its coats: earned ones by name, locked ones as "Level 30", the one it wears marked in words, and "Newest coat", which is the default and follows each new coat. Left-click still sends it out. The choice is saved with the critter as a ninth field on its line in the player's file; older files read as "newest", so every existing save loads unchanged.
- **La Ménagerie** has a fourth page for each critter that lists its coats (earned ones by name, the rest as the level that earns them), in place of "Spirit coats come later."
- **API version 2.** `CritterInfo` has `coat` (0 before level 10, otherwise 1 to 10) and `coatName`, for MobShowdown. It's the coat the owner picked or the newest earned, whether or not the world shows it. The old seven-argument constructor still works.
- Colors never carry the meaning: every menu line, chat message and journal line has the coat's name, and the menu's dark glow colors are written in a lighter color so they read on the dark background.
- Tested: unit tests (earning at levels 9, 10, 99 and 100, the choice and the migration, magic-world gating, the data file with bad entries, the journal page, the API) and the bot suite (see the README for versions and counts). Not seen in a real game client: how the glow, the trail and the variants look. Wolves, cats, pigs, cows and frogs aren't critters yet, so their variants are covered by the data file's tests only; the chicken's variants ran for real.

## 0.7.1

- Menu and chat hints are easier to read (K681). Lore lines in the Bunkhouse, Critterbook and vittles menus, and the locked lines in the coaxing list, move from dark gray (under 3:1 on the dark tooltip) to gray (about 9:1). Unlocked coaxing hints are now white so they still stand apart. Critterbook book pages keep dark gray, since they sit on a light page.
- Tested: unit tests. Not seen in a real game client.

## 0.7.0

An API for other plugins, for MobShowdown.

- **MobSquadApi**, from Bukkit's services manager: a player's critters (read-only), whether they have one out, sending them home, whether MobSquad is on in a world, keeping it off in another plugin's private world until the server stops, and a critter's strength at a level. Nothing in it lets another plugin add, remove or change a critter.
- Nothing changes for players.

## 0.6.0

Wranglin' in AuraSkills.

- **With AuraSkills installed**, Wranglin' is one of its skills, with a lead as its icon and MobSquad's level. MobSquad still keeps the level in the player's file and copies it to AuraSkills when it changes, when the player joins and when an admin uses `/mobsquad xp`. A level set with AuraSkills' commands is put back at the next change or join. AuraSkills' XP multipliers and rewards don't apply.
- The skill's AuraSkills settings are written to `plugins/MobSquad/auraskills` the first time. MobSquad declares `auraskills.skill.wranglin` (default true), which AuraSkills checks for each skill.
- Without AuraSkills nothing changes. Built against AuraSkills 2.4.0.
- **World groups.** Tested with Multiverse-Inventories 5.3.6: crossing to another inventory group sends critters home, the sidekick comes along as one copy, and nothing turns into an item.
- **Fixed:** a block the enderman fetched went into your inventory in whatever world you were in a second later, so it could cross world groups, and it was lost if the enderman went home in that second. It now goes to you only in the world it came from, and otherwise drops where the block was.

## 0.5.0

The sidekick and the later parts of four specials.

- **The sidekick.** A critter with 10 hours out with you has a 1 in 10 chance each day of becoming your sidekick, rolled once a day while it's out. It comes out on its own when you join, change worlds or it's rested, doesn't count toward your posse, and stays when you click it in the Bunkhouse menu, use the menu's bell or ring the dinnerbell. One per player. Admins can set or clear it with `/mobsquad sidekick <player> <critter|none>`.
- **Time together** is counted per critter and saved as an eighth field on its line in the player's file; older files read as 0.
- **From level 25:** the chicken lays speedy eggs, the bee's sting makes hostile mobs glow, the fox pounces for a double first hit, and the skeleton shares arrows when you carry a bow or crossbow. Each gets stronger at the later milestones; see the README.
- Not yet: travelling with your sidekick through a gate (that waits for the Waystation hook) and the small model of your sidekick above the Bunkhouse.

## 0.4.0

La Ménagerie and the Critterbook.

- **La Ménagerie**, the journal: its own menu, opened with `/mobsquad journal` or from the book in the Bunkhouse menu. Each launch critter has a page with its Critterbook number, a half-ridiculous line about it, how to coax it, its special and your best one's level. Locked pages show only their number.
- **Pages open when you meet a critter**: within 10 blocks with a clear line of sight, or when you coax or are given one. You're told, with a link to the journal. Pages are saved in the player's file as `page` lines; files from before 0.4.0 open pages for the critters already in them.
- **Critterbook numbers** for all 88 mobs in Java Edition, in the order each first appeared, from the Minecraft Wiki's history sections. Same-version ties go by first snapshot, then alphabetically. The launch critters: creeper 1, skeleton 3, chicken 9, enderman 15, iron golem 25, fox 62, bee 63, axolotl 69, allay 75. The list ships in the jar as `critterbook.txt`.
- Not yet: spirit coats (the page says they come later) and quest chapters opening pages.

## 0.3.0

Levels and specials.

- **Critter levels, 1 to 100.** A critter earns experience for each hostile mob it helps beat (the mob's max health, up to 100) and 2 for each minute out. Level n needs 10 (n - 1)² experience: level 10 at 810, level 50 at 24,010, level 100 at 98,010. You're told when a critter levels up, and again when its special grows.
- **Stats grow with the level** and slow near the top: health up to four times, attack up to three times, speed up to a fifth more at level 100.
- **Every launch critter's special**, growing at levels 25, 50, 75 and 100: the chicken's slow falling, the axolotl's water breathing and regeneration (and playing dead), the bee's crop growth, the fox's fetching, the allay's ore finding, the creeper's blast that hurts only hostile mobs, the enderman's block fetching and its teleport out of trouble, the skeleton's longer, faster covering fire, and the iron golem's guard and throw. See the README for the numbers.
- The Bunkhouse menu shows each critter's experience to the next level and its special.
- `/mobsquad level <player> <critter> <level>` for admins.
- Not yet: the brief's later parts of four specials (the chicken's speed eggs, the bee's glowing sting, the fox's pounce, the skeleton sharing arrows).

## 0.2.0

The rest of the launch puzzles, fighting and vittles.

- **Every launch critter can be coaxed.** The creeper standoff (gunpowder starts it; crouch through three swells and feed it gunpowder each time it calms) and the enderman's no-staring rule (offer it a block it could carry, then stay near it for 20 seconds without looking it in the eye) follow the brief. The owner kept the other six as drafted (2026-11-15): the axolotl (show it a bucket of tropical fish while you're both in water, then swim with it for 15 seconds; the bucket isn't used), the bee (show it a flower, then stand still for 10 seconds), the fox (crouch, offer berries, and stay crouched for 15 seconds), the allay (an amethyst shard, then three notes from a note block near it), the skeleton (hand it an arrow, then hit a target block three times) and the iron golem (only one a player built: four iron ingots, then a poppy). `/mobsquad hints` lists them.
- **Coaxing is safe.** A mob being coaxed never targets you, and a creeper can't go off during the standoff. After a failed or refused attempt, the mob leaves you alone for 10 seconds, and a creeper backs away.
- **Critters fight hostile mobs only.** They go after a hostile mob near you that's after you, or has hurt you or one of your critters, or any other hostile mob near you that isn't neutral (endermen and piglins) or named. They never stray more than 16 blocks from you. The skeleton shoots a steady arrow every second and a half within 15 blocks; the allay never fights; the rest close in and hit. A critter never hurts a player, a pet, a villager, a farm animal or another player's critter, whatever its own instincts say, and its arrows never break a painting or an item frame.
- **Vittles** perk up the critter that was tuckered out most recently, and send it out beside you if there's room. Two from bread, a carrot and sweet berries. They can't be eaten. Admins can give them with `/mobsquad vittles <player> [amount]`.
- An axolotl critter doesn't dry out on land.

## 0.1.0

The core loop.

- **Bunkhouses.** An admin marks a vault block with `/mobsquad bunkhouse add`. Right-clicking it opens a menu of your own critters, never anyone else's, and never hands out vault loot. Breaking a Bunkhouse forgets it.
- **Sending out and calling back.** Click a critter to send it out; it follows you, stops when it's close and steps beside you if it's left more than 24 blocks behind. Click it again, use the bell in the menu, or ring the dinnerbell (`/mobsquad dinnerbell`) to call it home.
- **Posse size by Wranglin' level:** one critter out at once, two from level 10, three from level 25. Tiers unlock by level: Yard at 1, Holler at 10, Yonder at 25, Backcountry at 40.
- **Safety.** A critter never targets or hurts a player and can't be hurt by one. It never changes a block, explodes, burns in the sun, teleports on its own, breeds, or gets leashed, named, ridden or traded with. Out of health, it's tuckered out instead of dying, drops nothing, and rests in the Bunkhouse for `recovery-minutes` (5) real minutes.
- **The chicken's puzzle.** Hold seeds, right-click a chicken and walk it 10 blocks within 40 seconds without running. A named or leashed chicken, or one on a claim where you can't interact, can't be coaxed.
- **Plain files.** Each player's critters and Wranglin' experience live in `plugins/MobSquad/players/<uuid>.txt`. Critters out in the world are never saved with it, so a restart, a crash or a player leaving puts them back in the Bunkhouse exactly once.
- Admin commands: `/mobsquad give <player> <critter>`, `/mobsquad xp <player> <amount>`, `/mobsquad reload`.

Not in this release: the other launch critters' puzzles, fighting, vittles, critter levels and specials, the journal, and the AuraSkills hook. Wranglin' is MobSquad's own level for now.
