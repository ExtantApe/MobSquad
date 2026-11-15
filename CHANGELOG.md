# Changelog

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
