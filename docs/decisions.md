# Decisions

Dated decisions for this repo, newest last.

- 2026-11-14: created from the ops starter kit, area `minecraft-plugins`.
- 2026-11-14: the brief is `ops/briefs/menagerie.md` (Menagerie renamed MobSquad, ops D199; the arena add-on is MobShowdown, D200). Its words, critters, tiers and safety rules are the owner's. The owner's work order (Simon, 2026-11-15): PaulBunyan, then MobSquad, then MobShowdown.
- 2026-11-15: AuraSkills' API (2.0 and up, checked on 2.4.0) registers custom skills and grants XP to them, so Wranglin' is an AuraSkills skill when AuraSkills is installed and MobSquad's own level otherwise (K604).
- 2026-11-15, proposed by the lead, not yet seen by the owner: the build order in [plan.md](plan.md).
- 2026-11-15: 0.1.0 builds the core loop from plan.md step 1. Wranglin' is MobSquad's own level in 0.1.0; the AuraSkills hook follows once it can be tested on a throwaway server with AuraSkills installed.
- 2026-11-15: critters out in the world are non-persistent mobs; only the player's Bunkhouse file is saved. A critter left in a chunk after a crash is removed when the chunk loads.
- 2026-11-15: a tier gates sending a critter out as well as coaxing it, so a critter an admin gives early waits for the level.
- 2026-11-15, 0.6.0: MobSquad keeps the Wranglin' level and copies it to AuraSkills, rather than handing AuraSkills the experience. On a test server, AuraSkills 2.4.0 dropped experience given to a skill from another plugin: `addSkillXp` gave none at all, and `addSkillXpRaw` lost it when the skill levelled up. Copying the level also keeps posse sizes and tiers on one curve. This replaces the earlier AuraSkills decision above.
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
- 2026-11-15: Critterbook numbers come from the first Java Edition entry in each mob's History section on the Minecraft Wiki, including experimental snapshots (the camel, sniffer, breeze, bogged and creaking first appeared behind experiments). A texture added before the mob doesn't count (piglin, silverfish). Mobs from the same version are ordered by first snapshot, then alphabetically by entity id. The 88 numbered are the mobs the wiki lists as current in Java Edition, without unused, removed, joke, Education or upcoming ones. The list is `src/main/resources/critterbook.txt`; numbers never change, and new mobs get the next numbers.
- 2026-11-15: a page opens when you're within 10 blocks of a living launch critter (anyone's, or wild) and have a clear line of sight, checked every 2 seconds, or when a critter is added to your Bunkhouse. Owning a critter always opens its page.
- 2026-11-15, written by the lead, for the owner's say: the eight page lines other than the creeper's, which is the owner's own (brief, "Voice").
- 2026-11-15, owner (ops D204), in the chief's chat: "Keep all eight" for the page lines, so they stand as written; and "Sidekick + later specials" as the next release.
- 2026-11-15: the sidekick's "long time" is 10 hours out together and its "small daily chance" is 1 in 10, rolled once per calendar day (server time) per player, only while an eligible critter is out, so a day without one doesn't use the roll up. The longest-serving eligible critter out is the one rolled for.
- 2026-11-15: until the Waystation hook exists, a sidekick never goes into the Bunkhouse on a player's say. It still leaves the world like any critter when its owner leaves, the server stops, or it's tuckered out, and comes back on its own when it can. A disabled world or a locked tier keeps it home.
- 2026-11-15: the brief's "later" parts of four specials unlock at level 25, the first milestone, and grow at the next ones: speedy eggs (first one 30 seconds after going out, then every 5 minutes, a minute sooner per later milestone; Speed II, III from level 75), the glowing sting (10 seconds, 5 more per later milestone), the pounce (a leap of up to 6 blocks at a mob not targeting the fox, double damage on the first hit, every 8 seconds, a second sooner per later milestone) and shared arrows (two every 2 minutes while carrying a bow or crossbow, one more and 20 seconds sooner per later milestone).
- 2026-11-15: speedy eggs can be picked up only by the chicken's owner and are used, never thrown, so they can't hatch chickens.

## 2026-11-22

- **Spirit coats (0.8.0), from the owner's brief ("Levels and spirit coats").** A coat every 10 levels, coat 1 at level 10 to coat 10 at level 100, shown only in worlds listed under `spirit-coats.worlds` (empty by default). Written by the lead for the owner's say, so they stand unless he changes them:
  - The ten names and glow colors: Ember (red), Marigold (gold), Sunbeam (yellow), Meadow (green), Pine (dark green), Lagoon (aqua), Frost (blue), Twilight (dark purple), Blossom (light purple), Starlight (white). Names are plain and always shown with the color. Dark colors are written in a lighter color in text, so every coat name reads on the dark menu.
  - The trail particles (flame, wax_on, glow, happy_villager, composter, bubble_pop, snowflake, witch, cherry_leaves, end_rod) are about 4 a second per critter.
  - Pigs, cows, chickens and frogs have three variants, so each look lasts for 3 or 4 coats in a row; wolves have nine (coat 10 repeats the pale wolf) and cats eleven. Variant critters got no glow or trail in 0.8.0, as the brief said; see the 2026-11-22 picks below for pigs, cows, chickens and frogs.
  - A pick is stored with the critter as a ninth field; 0 means "the newest earned", so older files and critters that never picked keep following new coats. A pick the critter hasn't earned (an admin lowered its level) shows the newest earned instead.
  - Choosing is a right-click on the critter in the Bunkhouse. Left-click still sends it out.
  - Glow uses scoreboard teams named `ms_coat_1` to `ms_coat_10` on the main scoreboard, removed on start and stop.
  - `/mobsquad level` can lower a critter's level, which hides coats it no longer has the level for. Normal play never lowers a level.
- **Spirit coat picks (owner, Simon chat, 2026-11-22), relayed by the chief of staff:**
  - Coat names: "Keep them (Recommended)". The ten names and colors above stand.
  - Pigs, cows, chickens and frogs: "Add glow and trail (Recommended)". They wear their variant and also glow in the coat's color with its trail, so each coat looks different even where the three looks repeat. Wolves and cats keep the variant only. It's the `glow-too` list in `coats.yml` (0.8.1), so an admin can change it.
  - Magic worlds: "We have two magic worlds…? One adult and one kid one." On Sonoran those are `magic` and `ninos_magic` (both exist there; checked 2026-11-22). Listing them in `spirit-coats.worlds` is a live change and waits for the owner's go.
- **No-glow worlds (lead, 2026-11-22, kids first).** The trust-and-safety review found the glow outline shows through walls to everyone tracking the critter in that world, and a coated critter is always near its owner, so in `ninos_magic` it could point to a kid's hidden base. 0.8.1 adds `spirit-coats.no-glow-worlds` (empty by default): coats there keep their variant and trail but get no outline. The live go request proposes `ninos_magic` there; the owner decides.

## 2026-11-15

- **API for MobShowdown (K605):** MobSquad exposes a read-only API through Bukkit's services manager, so MobShowdown builds on it as the brief says ("MobShowdown: battles, built on MobSquad's API"). Arena fights use MobShowdown's own stand-in mobs, so MobSquad's safety rules (critters fight hostile mobs only, never players) stay exactly as they are, and MobSquad stays off in the arena world. Released as 0.7.0.

