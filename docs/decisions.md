# Decisions

Dated decisions for this repo, newest last.

- 2026-11-14: created from the ops starter kit, area `minecraft-plugins`.
- 2026-11-14: the brief is `ops/briefs/menagerie.md` (Menagerie renamed MobSquad, ops D199; the arena add-on is MobShowdown, D200). Its words, critters, tiers and safety rules are the owner's. The owner's work order (Simon, 2026-11-15): PaulBunyan, then MobSquad, then MobShowdown.
- 2026-11-15: AuraSkills' API (2.0 and up, checked on 2.4.0) registers custom skills and grants XP to them, so Wranglin' is an AuraSkills skill when AuraSkills is installed and MobSquad's own level otherwise (K604).
- 2026-11-15, proposed by the lead, not yet seen by the owner: the build order in [plan.md](plan.md).
- 2026-11-15: 0.1.0 builds the core loop from plan.md step 1. Wranglin' is MobSquad's own level in 0.1.0; the AuraSkills hook follows once it can be tested on a throwaway server with AuraSkills installed.
- 2026-11-15: critters out in the world are non-persistent mobs; only the player's Bunkhouse file is saved. A critter left in a chunk after a crash is removed when the chunk loads.
- 2026-11-15: a tier gates sending a critter out as well as coaxing it, so a critter an admin gives early waits for the level.
- 2026-11-15: running on a seed trail means sprinting, riding, gliding, flying or crossing more than 1.35 blocks in a quarter second, about 1.25 times walking pace. A longer jump counts as a teleport, which loses the trail instead.
- 2026-11-15: bot tests use port 25631.
- 2026-11-15: public at the first release under the owner's standing yes of 2026-11-07, relayed by Simon: MIT, Copyright (c) 2026 EXTANT APE, after the first-public-push audit (no secrets, addresses, player data or build output; no private-term hits in any file, commit or message).
- 2026-11-15, proposed by the lead: the six puzzles the brief doesn't spell out (axolotl, bee, fox, allay, skeleton, iron golem; see CHANGELOG 0.2.0) and the vittles recipe (two from bread, a carrot and sweet berries). Only a golem a player built can be coaxed, so village golems stay with their village.
- 2026-11-15, owner (ops D203), in the chief's chat: "Keep all as drafted". The six puzzles and the vittles recipe above are settled as written.
- 2026-11-15: after a failed or refused coaxing attempt, the mob leaves that player alone for 10 seconds and a creeper can't go off in that time, because a bot test showed a refused creeper blowing up at once.
- 2026-11-15: a mob "started it" when it targets the owner, or really hurt the owner or one of their critters in the last 30 seconds. Targeting a critter isn't enough, because a critter's blocked swing can provoke a named mob.
- 2026-11-15: critter damage follows the same rules as picking a fight, so a golem's own instincts can't hurt a named or neutral mob either.
- 2026-11-15: bots can't crouch where the server sees it. Test servers start with `-Dmobsquad.test.sneak-tag=true`, under which the scoreboard tag `mobsquad-test-sneak` counts as crouching. Without the flag the tag does nothing.
- 2026-11-15: the skeleton critter shoots its own arrows (every 1.5 seconds, within 15 blocks, with a clear line, 2 damage, never picked up) because the game's bow AI fired too rarely for a critter in bot tests.
