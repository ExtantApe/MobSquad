# MobSquad

Coax mobs into critters for Paper: they follow you, fight only hostile mobs, and live safely in a shared Bunkhouse. No client mods. Gotta coax em all... Pokemob!

**Status:** 0.2.0. Every launch critter can be coaxed, critters fight hostile mobs only, and vittles revive them. Passed with bots on throwaway Paper 26.1.2 and 26.2 servers (111 checks each). On 26.3 the bots can't use blocks, so only the commands and the coaxing puzzles ran there (52 checks). Bots can't crouch where the server sees it, so the creeper and fox puzzles were tested with a test-only stand-in for crouching; real crouching is untested. Nothing has been seen in a real game client yet. Six of the nine puzzles and the vittles recipe are proposals waiting for the owner's say.

## What it does

- **Coaxing.** Each kind of mob has its own small puzzle, and you need its tier first. The chicken: walk it 10 blocks on a trail of seeds without running. The creeper: gunpowder starts a standoff; crouch while it swells, gunpowder when it calms, three times. The enderman: offer it a block, then stay near it for 20 seconds without looking it in the eye. `/mobsquad hints` lists all nine. Break a puzzle's rule and you start over. A mob being coaxed never turns on you, and after a failed try it leaves you alone for 10 seconds.
- **Fighting.** Critters fight hostile mobs only: the ones after you or your critters, and other hostile mobs near you that aren't neutral or named. They never hurt players, pets, villagers or farm animals. The skeleton shoots and the allay never fights.
- **Vittles.** Right-click them to perk up a tuckered-out critter on the spot. Craft two from bread, a carrot and sweet berries.
- **Bunkhouses.** An admin marks a vault block as a Bunkhouse. Anyone who right-clicks it sees only their own critters. Click one to send it out or call it back. Critters never exist as items, so they can't be dropped, traded or duplicated.
- **Following.** A critter out with you walks after you and steps beside you if it's left more than 24 blocks behind.
- **Wranglin'.** Coaxing earns Wranglin' experience. One critter can be out at once, two from level 10 and three from level 25. Mob tiers unlock at levels 1, 10, 25 and 40.
- **Safety.** A critter never targets or hurts a player, and players can't hurt it. It never changes a block, explodes, burns in the sun or wanders off by teleporting. Out of health, it's tuckered out: it drops nothing and rests in the Bunkhouse for 5 real minutes.
- **The dinnerbell.** Ring it and every critter out with you heads home.
- **Plain files.** One text file per player in `plugins/MobSquad/players`. A critter out in the world is never saved with the world, so after a restart, a crash or the player leaving, it's back in the Bunkhouse exactly once.

## Install or build

Paper 26.1 or newer, Java 25. Drop `MobSquad-<version>.jar` into `plugins` and restart. To build it yourself: `./gradlew build`, and the jar is in `build/libs`.

Settings are in `plugins/MobSquad/config.yml`: worlds where MobSquad is off, the rest time after tuckering out, and the Wranglin' experience for a coax.

## Commands

| Command | Who | What it does |
| --- | --- | --- |
| `/mobsquad` | everyone | How it works, in three lines. |
| `/mobsquad list` | everyone | Your critters and your Wranglin' level. |
| `/mobsquad hints` | everyone | How to coax each critter. |
| `/mobsquad dinnerbell` | everyone | Gives you a dinnerbell. |
| `/mobsquad bunkhouse add` and `remove` | admins | Marks or unmarks the vault you're looking at. |
| `/mobsquad give <player> <critter>` | admins | Puts a critter in a player's Bunkhouse. |
| `/mobsquad vittles <player> [amount]` | admins | Gives vittles. |
| `/mobsquad xp <player> <amount>` | admins | Sets a player's Wranglin' experience. |
| `/mobsquad reload` | admins | Rereads config.yml. |

`/ms` works too. Permissions: `mobsquad.use` (everyone) and `mobsquad.admin` (ops).

## Tests

`test/suite.mjs` builds a throwaway Paper server on 127.0.0.1 from downloaded jars and drives it with bots. See the top of the file for what it can and can't check.

## Not affiliated

The tagline is a joke. MobSquad isn't affiliated with or endorsed by the makers of any creature-collecting game, and uses none of their names, art or designs.

## License

MIT. See [LICENSE](LICENSE).
