# Test audit

Kept by this repo's owning session, per `~/Code/CLAUDE.md`, "Test self-audits". Newest first.

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
