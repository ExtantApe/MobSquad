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
