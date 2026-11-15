# Build order

A proposal from the plugins lead (2026-11-15), not an owner decision. Each step is a release once it passes its bot tests.

1. **0.1.0, the core loop.** The nine launch critters as data. A player's critters in a plain file of their own. Bunkhouses: an admin marks a vault block, and each player sees only their own critters in its menu. Send a critter out and it follows you; it never hurts or targets a player and never changes a block. Tuckered out instead of dying, back in the Bunkhouse, recovered after a few real minutes. The dinnerbell calls everything back. Posse size by Wranglin' level (1, 2, 3). Wranglin' as an AuraSkills skill or MobSquad's own level. The first coaxing puzzle: the chicken's seed trail.
2. **0.2.0, the rest of the launch puzzles and fighting.** Every launch critter's coaxing puzzle. Critters fight hostile mobs only. Vittles.
3. **0.3.0, levels and specials.** Levels 1 to 100 with stat growth, and each launch critter's special at its milestones.
4. **0.4.0, La Ménagerie.** The journal and the Critterbook numbers.
5. **Later:** the sidekick, spirit coats, crossing world groups through the Bunkhouse with a crash journal, quest chapters through BetonQuest, the Waystation and other hooks, then MobShowdown on MobSquad's API.

Critters never exist as items, so they can't be dropped, traded or duplicated. A critter out in the world is never saved with the world: if its chunk unloads, its owner leaves or the server stops, it's back in the Bunkhouse. Only the Bunkhouse list is saved, so a critter comes back exactly once.
