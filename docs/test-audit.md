# Test audit

Kept by this repo's owning session, per `~/Code/CLAUDE.md`, "Test self-audits". Newest first.

## 2026-11-24: The Ten Winds (0.11.0)

- Promises added: README "The Ten Winds", "Setting up the Ten Winds" and the hooks, CHANGELOG 0.11.0, D289 and the 0.11.0 decisions (automatic paths, same rewards, kid or adult fails closed, glow untouched, ships off until the admin's switch).
- Covered by unit tests (TenWindsTest, ChaptersTest; 82 in all): the play style picks the biggest share and a tie or nothing is all; `%mobsquad_top%` is the highest level owned and 0 with nothing; the Ranger's books are written books by Ranger Juniper, tattered, never a map, with no name, place or placeholder; typed words are held only on an exact true and never a bare number; the package: 21 unset spots and no coordinates or world names anywhere else, the adult version needs `permission group.adult` and every other case gets the kid version, kid and shared lines are 12 words or fewer and gentle, no line asks for typing or a command, every action reaches only that player and runs only `mobsquad` commands on `%player%`, the rings gate at 10, 20 and 30 on the top level and a quiet ring offers nothing, kids get 8 kills at any hour and adults 15 at dusk or night, every mob in the kill list is a Monster, the watch is optional for kids (nothing else waits for it), the finale needs any two of the four new chapters, "Maybe later" leads on to the next open offer and ends at goodbye, no new chapter gives a level, tier, page or critter, and every path gets the same books.
- Covered by bots (BetonQuest 3.3.0-DEV-61, LuckPerms 5.5.71, PlaceholderAPI; 98 checks on 26.1.2 and 26.2, 91 on 26.3): the new chapters stay off until the admin's switch; a player with no group gets the kid lines; the style is set once; the kid's wind-signs path (explorer), a builder's 40 placed stone blocks (39 is not enough; 26.1.2 and 26.2) with the style read from the game's own count; the Ranger's trail book; the three wind-rings with level 10, 20 and 30 gates with the critter never out, the ring left waiting when it is quiet, a bystander in the ring getting nothing, a kid at an adult's spot and an adult at a kid's doing nothing; the adult's rings in grown-up words; the watch with real kills by hand (7 of 8 leaves one to go, a cow adds nothing, noon kills don't count for an adult, 14 of 15 at night is not enough); 3 and 4 landmarks; the finale offered by "Maybe later" with the watch skipped; the dinnerbell handed over by console, ringing away from the meadow or without the bell does nothing, a lost bell is replaced and only one is held, ringing it in the meadow finishes the story; each of the four books read off the server; only admins give books and bells; typed words in a conversation stay off public chat and a bare number still answers; the switch off keeps a finished player's story.
- Covered earlier and still relied on: the dinnerbell recalls only the ringer's critters (0.1.0 and 0.5.0 checks in the full suite, which passed on 26.1.2 and 26.2).
- Gaps added: the placed-block count, the kill counts and the typed-text hold are the checks that found the three real problems of this release (BetonQuest sends a conversation's typed words to public chat; the ring and landmark walk-ins need one move outside first, so a test moves away before it walks in; my first kill test attacked nothing because a console `distance` is from the world's origin).
- Gaps skipped: the full suite and a rerun of the tier chapters section on 26.3 (the free disk fell to 39 GB, under the 42 GB limit; they ran on 26.1.2 and 26.2, and the tier section ran 50 of 51 on all three with one stale count, corrected); builder block placing on 26.3 (bots can't place blocks there); clicking a Citizens NPC (not on the test server); how the books, rings and Ranger text look (bots draw nothing); a second bot beating mobs next to the first to show kills don't cross players (BetonQuest credits the killer, a player, and the objective is per player).
- Gaps for later: a real kid reading the text; the live install, which needs the owner's go; whether the adult group is really named `adult` on the live server; BetonQuest's own journal book carrying its owner's name.

## 2026-11-15: 0.6.0

- Promises added: README "AuraSkills" and "World groups", CHANGELOG 0.6.0, the 0.6.0 AuraSkills decision.
- Covered by bots with AuraSkills 2.4.0 (30 checks on 26.1.2 and 26.2, 25 on 26.3): Wranglin' registers as a skill; a level-up from coaxing shows in AuraSkills; a MobSquad level shows in AuraSkills when a player joins; MobSquad's own level counts whatever AuraSkills says; `/mobsquad xp` sets both; MobSquad keeps its own count.
- Covered by bots with Multiverse-Core 5.8.2 and Multiverse-Inventories 5.3.6 (22 checks on 26.2): inventories are really kept apart; crossing sends a critter home, in neither world; the sidekick comes along as one copy and comes back; nothing turns into an item; every critter stays in the file once; an enderman block fetched as you cross groups stays behind and drops where it was taken.
- Full suite after the change: 164 on 26.1.2 and 26.2, 75 on 26.3. The fox pounce check now looks for one double hit among the first three, since a plain hit can land first; the README says so.
- Gaps skipped: AuraSkills' own menus and messages (AuraSkills draws them from the registered skill); the world-group checks on 26.1.2 and 26.3 (the same MobSquad code as 26.2; on 26.3 the bots can't open the Bunkhouse).
- Gaps for later: crossing groups through a Waystation gate, with the Waystation hook.

## 2026-11-15: 0.5.0

- Promises added: README "The sidekick" and the level 25 additions to four specials, CHANGELOG 0.5.0, the sidekick and later-specials decisions.
- Covered by unit tests (ModelTest): 10 hours needed; a day with nobody eligible doesn't use the roll; the roll is small and once a day; one sidekick per player, only from critters out; sidekick, roll day and minutes round-trip; a sidekick not in the list is dropped; minutes out add up with their experience.
- Covered by bots (164 checks on 26.1.2 and 26.2, 75 on 26.3): an admin sets and clears a sidekick; it comes out on its own and is saved; it doesn't count toward the posse; the menu, its bell and the dinnerbell leave it out (the menu parts on 26.1.2 and 26.2); it goes when its owner leaves and comes back when they return; tuckered out, it rests and comes back once rested; cleared, the dinnerbell calls it home. From level 25: the bee's sting makes a husk glow; the fox's first hit is double; the chicken lays a speedy egg at your feet, and using it gives speed without throwing it; the skeleton shares two arrows when you carry a bow.
- Gaps skipped: the real daily roll (needs 10 hours out; the logic is unit tested and the server only feeds it the day and the critters out); milestone growth of the four additions (plain numbers from the stage, same path as covered); a sidekick following through a world change (the same keep-beside-you loop as rejoining, which is covered).
- Gaps for later: travelling with a sidekick through a gate, when the Waystation hook lands.

## 2026-11-15: 0.4.0

- Promises added: README "La Ménagerie", the journal command, CHANGELOG 0.4.0, the Critterbook decisions.
- Covered by unit tests (ModelTest): Critterbook numbers run 1 to 88 with no gaps; the nine launch numbers are pinned so they can't change; page lines round-trip; an older file opens pages for critters already in it.
- Covered by bots (147 checks on 26.1.2 and 26.2, 65 on 26.3; the journal runs on all three): the command opens the journal; a new player's pages are all locked, in Critterbook order; meeting a fox opens and saves its page; a creeper behind a wall and a chicken 25 blocks away aren't met, and the creeper is once the wall goes; getting a critter opens its page; open pages show the critter and locked ones stay hidden; clicking an open page sends a book and a locked one doesn't; nothing can be taken from the journal; the Bunkhouse menu opens it (26.1.2 and 26.2).
- Gaps skipped: what the book pages say and how they look (bots get the book packet but don't render it; the text is plain data in Journal.java); meeting while MobSquad is off in a world (same settings check as the Bunkhouse, covered).
- Gaps for later: spirit coats and quest chapters when they land.

## 2026-11-15: 0.3.0

- Promises added: README "Levels" and "Specials", the Safety line on the two block-touching specials, CHANGELOG 0.3.0, the new decisions entries.
- Covered by unit tests (ModelTest): the level curve both ways for every level 1 to 100, the cap at 100, stat growth at the ends and slowing near the top, milestone stages, experience setting the level, every species has a special.
- Covered by bots (133 checks on 26.1.2 and 26.2; the specials section needs the Bunkhouse menu, so it's skipped on 26.3): the admin level command and the saved level and experience; a level 50 chicken's health and full health on going out; slow falling with the chicken; water breathing and regeneration near the axolotl, and it plays dead and heals when badly hurt; the bee grows wheat with random ticks off; the fox fetches a dropped item and leaves another player's alone; the allay finds iron ore and says when there's no diamond ore; the enderman fetches a pumpkin, refuses stone and refuses a block in someone else's GriefPrevention claim; the creeper's blast hurts two hostile mobs at once, spares a cow and breaks nothing, and with a player close by it only hits one at a time; beating a husk levels a golem up and saves it; the golem takes a quarter of a mob's hit on its owner.
- Gaps skipped: the skeleton's range and fire rate at milestones and the golem's throw (same fight code as covered shots and hits; the numbers are plain functions); the enderman's teleport to you (a scheduled teleport through the same path as catching up, which is covered); walking experience (a once-a-minute timer; the experience path is covered by the golem's kill); milestone messages (covered by the level-up message path).
- Gaps for later: the later parts of four specials when they land.

## 2026-11-15: 0.2.0

- Promises added: README "Coaxing", "Fighting" and "Vittles", CHANGELOG 0.2.0.
- Covered by bots (111 checks on 26.1.2 and 26.2, 52 on 26.3): every launch puzzle succeeds; tier refusal; the creeper standoff never blows up, and standing up mid-swell, gunpowder too soon and forgetting the gunpowder each start it over; a refused or failed creeper doesn't go off afterward; the enderman coaxes when not looked at and starts over when stared at; the skeleton coaxes on three target hits; a village golem is refused and a player-built golem coaxes; the fox needs a crouch and coaxes after 15 seconds; the bee coaxes standing still and starts over when you move; the allay coaxes on three notes; the axolotl is refused out of water, coaxes in it, and the bucket is kept; every coaxed critter is saved; /mobsquad hints. Fighting: a golem critter kills a hostile husk and leaves a cow, a villager, a named husk, a neutral enderman and another player's wolf alone; a skeleton critter shoots a husk down and never hurts a nearby player; the allay never fights. Vittles: revive the latest tuckered critter and send it out, clear its rest in the file, use one bundle, and say so when nobody needs them.
- Gaps skipped: real crouching (bots can't crouch where the server sees it; the creeper and fox puzzles use a test-only tag under a JVM flag); block-use sections on 26.3; crafting vittles (the recipe is registered from the same item the admin command gives; bots can't craft on 26.3); piglin and zombified piglin neutrality (same rule as the enderman, which is covered).
- Gaps for later: critter levels and specials (0.3.0).

## 2026-11-15: 0.1.0

- Promises: README "What it does", CHANGELOG 0.1.0, the brief's safety rules.
- Covered by unit tests (ModelTest): Wranglin' levels, posse size, tier unlocks, the player file round trip, bad lines skipped, a new player empty.
- Covered by bots (test/suite.mjs, 60 checks on 26.1.2 and 26.2, 16 on 26.3): marking Bunkhouses and refusing non-admins; the menu shows only your own critters, gives no vault loot, and its icons can't be taken; the chicken trail starts, uses one seed, coaxes after 10 blocks, saves to the player's file and earns experience; named chickens, putting seeds away, leaving the chicken, running and someone else's GriefPrevention claim all refuse or end it; sending out, posse size at levels 1 and 10, the Yonder tier lock; following and catching up; the menu bell, the dinnerbell and /mobsquad list; creeper and skeleton critters never explode, hurt either player, burn or change blocks; players can't hurt critters; tuckering out drops nothing, saves the rest time and refuses a resting critter; leaving, restarting and kill -9 leave no critter in the world and no copy in the list; a disabled world refuses the Bunkhouse.
- Gaps skipped: block-use checks on 26.3 (bots can't use blocks there; covered on 26.1.2 and 26.2). Breaking a Bunkhouse by hand (vault hardness makes it slow for a bot; the unmark path is the same code as `/mobsquad bunkhouse remove`). Reload of every setting (only the two used are exercised).
- Gaps for later: endermen and iron golems out as critters, when fighting lands in 0.2.0.

## 2026-11-14: first audit (starter kit)

- Promises: none yet.
- Covered: none yet.
- Gaps added: none.
- Gaps skipped (and why): none.

## 2026-11-15: API (0.7.0)

| Promise | Test |
| --- | --- |
| The allay never fights; every other critter has damage | unit `theAllayNeverFightsAndEveryOtherCritterDoes` |
| Stats grow the same way as in the world | unit `statsGrowWithLevelTheSameWayAsInTheWorld`, `theSkeletonsDamageIsItsArrow` |
| An unknown species does no damage | unit `anUnknownSpeciesIsHarmless` |
| The critter view is a read-only copy with the resting state | unit `infoCopiesTheCritterAndSaysWhenItsResting` |

**Gaps:** the services-manager registration, `sendHome` and `keepOff` on a running server. MobShowdown's bot tests cover them, since they're what calls them.

## 2026-11-16: cleanup (D226)

- **Covered:** the harness records each server folder it creates and each Java process it starts. When the run ends (pass, fail, an uncaught error, Ctrl-C, SIGTERM or SIGHUP) it kills those processes and deletes those folders, and nothing else. A folder from an earlier run that's in the way stops the run; it isn't deleted. `KEEP_TEST_SERVERS=1` keeps the folders for debugging.
- **Checked by hand:** on MobShowdown, a full run (42/42) and a run interrupted after 45 seconds both left the work folder empty, with no Java left and the test port closed. The other harnesses use the same code.
- **Gap skipped:** the suite can't check its own leftovers, because cleanup runs as the process exits. A wrapper that checks after the suite exits would add more machinery than the check is worth here.

## 2026-11-24: quest chapters for BetonQuest (0.10.0)

| Promise | Test |
| --- | --- |
| A tier opens by level or by its chapter, nothing else | unit `eitherOpensATierByLevelOrByQuestAndNothingElse`; bot: each chapter opens its tier at level 1 and no other |
| With BetonQuest missing, `either` is the level-only answer and nothing errors | unit `withoutBetonQuestNoTierIsEverQuestedSoEitherActsLikeLevel` (every tier, levels 1 to 100), `mobSquadHasNoCodeInTheBetonQuestPlugin` |
| The default is `either`; a bad value falls back to `level` | unit `theSettingDefaultsToEitherAndABadValueIsWarnedAbout` |
| The package hangs together: every action, objective, item, journal entry, condition and conversation line it names exists; each objective is started once | unit `everyNameTheFileUsesExists`, `chaptersRunInOrderAndEachHasThreeSteps`; bot: BetonQuest loads all of it |
| Each chapter unlocks only its own tier and a page of the same tier, and runs only MobSquad's `unlock` and `page` | unit `eachTierHasOneChapterThatEndsByUnlockingThatTierAndOpeningAPage`, `thePackageOnlyRunsMobSquadCommandsAndNeverChats` |
| Text is short, gentle, kind to young readers, asks for nothing personal, has no command in it | unit `textIsShortGentleAndAsksForNothingPersonal` |
| The chapters never change a level and open no other tier | bot: level stays 1, later tiers shut until their own chapter |
| Steps run in order; partial progress doesn't skip ahead (3 of 6 berries) | bot |
| The Ranger offers the next chapter only after the last, and reminds a player mid-chapter | bot |
| Finishing is saved in the player's file; in `level` mode a finished chapter opens nothing | bot |
| MobSquad's copy of the chapters survives a restart with an admin's edit | bot |

**Gaps:** clicking a Citizens NPC (Citizens not on the test server; the conversation started from an action instead). Real crafting and smelting: a bot's click at a crafting table made no item on 26.2, so those steps were completed with BetonQuest's objective command. Real game client: Ranger text, journal book and chat lines. **Skipped:** BetonQuest's own objective logic (pickup, craft, smelt) is its own tested code.

## 2026-11-22: quest chapters (0.9.0)

| Promise | Test |
| --- | --- |
| By default tiers open by level alone; quest unlocks change nothing | unit `levelModeIgnoresQuestsSoNothingChangesByDefault`, `theSettingDefaultsToLevelAndABadValueIsWarnedAbout` (shipped config is `level`) |
| `either`: a quest opens a tier early, the level still works, one tier's quest doesn't open another | unit `eitherModeLetsAQuestOpenATierEarly` |
| `quest`: level alone doesn't open a tier; Yard always open in every mode (fails safe, nobody locked out) | unit `questModeNeedsTheQuestButYardIsAlwaysOpen` |
| Locked messages say how a tier opens | unit `theLockedMessageSaysHowToOpenIt` |
| Quest unlocks are saved; old files load with none; a bad tier line is skipped, never guessed | unit `questUnlocksAreSavedWithThePlayer`, `filesFromBeforeQuestsLoadWithNoneAndABadTierLineIsSkipped` |
| Placeholders give only the player's own progress; unknown ones are left as written | unit `placeholdersReportOnlyThePlayersOwnProgress` |

**Gaps:** the `unlock`, `lock` and `page` commands and the PlaceholderAPI registration haven't run on a server (no throwaway servers on 2026-11-22); bot checks for them go in with the next bot run. Who may run the commands is `mobsquad.admin`, the same Brigadier `requires` as the other admin commands, which the bot suite already checks for a player without it. A real BetonQuest chapter is untested until one is written.

## 2026-11-22: spirit coats (0.8.0)

| Promise | Test |
| --- | --- |
| A coat every 10 levels, 1 at level 10, 10 at level 100, never more | unit `aCoatIsEarnedEveryTenLevels` (9, 10, 99, 100, 150) |
| The default is the newest earned; a pick counts only if earned | unit `theNewestEarnedCoatIsTheDefault`, `aChosenCoatIsWornOnlyIfEarned`; bot "picking an earned coat", "a locked coat can't be picked" |
| The choice is saved with the critter and survives growth | unit `theChoiceIsSavedWithTheCritter`, `aCoatChoiceSurvivesLevelingAndRestingAndStaysInRange`; bot "the choice is saved with the critter" |
| Existing saves stay valid; a bad field never costs a critter | unit `savesFromBeforeCoatsStillLoad`, `aBadCoatFieldNeverCostsTheCritter` |
| Coats show nowhere until an admin lists a world | unit `coatsShowNowhereUntilAnAdminListsAWorld`, `theBundledConfigListsNoWorlds`; bot "by default no world is magic" |
| Leaving or unlisting a world takes the look off at once; entering applies it | bot: unlist and relist, and the nether hop |
| Real variants for chickens (and the other five, in the data file) | bot (chicken, 26.1.2, 26.2, 26.3); unit data tests for the rest |
| Glow and trail for the others; cheap; none beyond view distance or on minimal | bot: team and glow, packet counts, a far bot, minimal particles |
| Pigs, cows, chickens and frogs also glow (glow-too, 0.8.1); wolves and cats don't; the list can be changed and bad entries are skipped | unit `theBundledCoatsLoadCleanlyAndReadWell`, `glowTooCanBeChangedAndBadEntriesAreSkipped`; bot: the chicken's glow and team (written 2026-11-22, not yet run) |
| No-glow worlds drop only the outline, keeping the coat and trail (kids' safety) | unit `noGlowWorldsTurnOnlyTheOutlineOff`, `theBundledConfigListsNoWorlds`; bot: no-glow on and off with the bee and chicken (written 2026-11-22, not yet run) |
| Bad lines in coats.yml are skipped with a warning, never a crash | unit `badEntriesAreSkippedWithAWarningAndTheRestLoad`, `aFileThatIsNotYamlLeavesTheBuiltInCoats`, `theBundledCoatsLoadCleanlyAndReadWell` |
| Journal shows earned coats by name and locked ones as "Level 30" | unit `CoatJournalTest` |
| Names and menu text readable, never color alone | unit `theBundledCoatsLoadCleanlyAndReadWell` (readable colors, distinct names); every coat line carries its name |
| API shows the coat index and name | unit `theApiReportsTheWornCoatByNumberAndName` |

**Gaps:** how it looks in a real client (glow, trail, variants, menu icons); the level-up chat message (not exercised by a bot, since the admin commands skip it); the coat menu on 26.3, where bots can't use a block; world changes between two listed worlds (the suite has only the nether to hop to); wolves, cats, pigs, cows and frogs on a server, since they aren't critters yet.
