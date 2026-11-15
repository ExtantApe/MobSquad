# Changelog

## 0.2.0

The rest of the launch puzzles, fighting and vittles.

- **Every launch critter can be coaxed.** The creeper standoff (gunpowder starts it; crouch through three swells and feed it gunpowder each time it calms) and the enderman's no-staring rule (offer it a block it could carry, then stay near it for 20 seconds without looking it in the eye) follow the brief. Proposed, waiting for the owner's say: the axolotl (show it a bucket of tropical fish while you're both in water, then swim with it for 15 seconds; the bucket isn't used), the bee (show it a flower, then stand still for 10 seconds), the fox (crouch, offer berries, and stay crouched for 15 seconds), the allay (an amethyst shard, then three notes from a note block near it), the skeleton (hand it an arrow, then hit a target block three times) and the iron golem (only one a player built: four iron ingots, then a poppy). `/mobsquad hints` lists them.
- **Coaxing is safe.** A mob being coaxed never targets you, and a creeper can't go off during the standoff. After a failed or refused attempt, the mob leaves you alone for 10 seconds, and a creeper backs away.
- **Critters fight hostile mobs only.** They go after a hostile mob near you that's after you, or has hurt you or one of your critters, or any other hostile mob near you that isn't neutral (endermen and piglins) or named. They never stray more than 16 blocks from you. The skeleton shoots a steady arrow every second and a half within 15 blocks; the allay never fights; the rest close in and hit. A critter never hurts a player, a pet, a villager, a farm animal or another player's critter, whatever its own instincts say, and its arrows never break a painting or an item frame.
- **Vittles** perk up the critter that was tuckered out most recently, and send it out beside you if there's room. Two from bread, a carrot and sweet berries (a proposal). They can't be eaten. Admins can give them with `/mobsquad vittles <player> [amount]`.
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
