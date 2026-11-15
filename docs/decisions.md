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
- 2026-11-15: critter levels: level n needs 10 (n - 1)² experience, earned from hostile mobs a critter hit in the last 30 seconds before they died (their max health, 1 to 100) and 2 a minute out. Stats grow by the fraction 1 - (1 - (level - 1) / 99)²: health ×(1 + 3f), attack ×(1 + 2f), speed ×(1 + 0.2f). Specials grow at 25, 50, 75 and 100. An overpowered critter is fine (brief), since critters never hurt players.
- 2026-11-15: a critter's experience is saved when it levels up, every 5 minutes and when its owner leaves, so a crash can lose up to 5 minutes of experience short of a level.
- 2026-11-15: the brief says critters never change blocks in claims. The bee's crop growth goes through BlockGrowEvent, so whatever stops natural growth stops it; it adds or removes no block. The enderman fires a block-break check as its owner first and fetches only if nothing cancels it, so claims and protections decide, and logging plugins record it as the owner's dig. It only takes blocks endermen can carry, so never a container.
- 2026-11-15: the enderman's fetch: right-click it, then swing at the block within 10 seconds. A swing in the first quarter second is ignored, so the right-click itself doesn't count, and a block break while it's waiting is cancelled.
- 2026-11-15: the fox waits 5 seconds before fetching a dropped item, so you can still drop something for a friend, and fetches an item only once. Items count as yours when you threw them, broke the block they came from or beat the mob that dropped them.
- 2026-11-15: the creeper's blast needs no player within its radius plus 3 blocks, and hits only mobs a critter may fight. Every 6 seconds (0.75 s less per milestone); a plain hit in between.
- 2026-11-15: the four "later" parts of specials in the brief (speed eggs, glowing sting, pounce, sharing arrows) wait for a later release.
