# MobSquad

Coax mobs into critters for Paper: they follow you, fight only hostile mobs, and live safely in a shared Bunkhouse. No client mods. Gotta coax em all... Pokemob!

**Status:** 0.8.0. Spirit coats. Passed with bots on throwaway Paper 26.1.2 and 26.2 servers (the full suite, 197 checks each) and on 26.3 (95 checks), then the Bunkhouse and coats parts again on all three after a last small change (43 checks on 26.1.2 and 26.2, 26 on 26.3). On 26.3 the bots can't use blocks, so the Bunkhouse menu, picking a coat in it and everything sent out from it didn't run there; the coats themselves ran, with the sidekick getting critters out. Bots can't crouch where the server sees it, so the creeper and fox puzzles were tested with a test-only stand-in for crouching; real crouching is untested. The daily sidekick roll is covered by unit tests only, since it needs 10 hours out. Nothing has been seen in a real game client yet, so how the glow, the particle trail and the chicken variants look is unchecked. Wolves, cats, pigs, cows and frogs aren't critters yet; only the chicken's variants ran on a server. The AuraSkills and Multiverse checks from 0.6.0 weren't rerun.

## What it does

- **Coaxing.** Each kind of mob has its own small puzzle, and you need its tier first. The chicken: walk it 10 blocks on a trail of seeds without running. The creeper: gunpowder starts a standoff; crouch while it swells, gunpowder when it calms, three times. The enderman: offer it a block, then stay near it for 20 seconds without looking it in the eye. `/mobsquad hints` lists all nine. Break a puzzle's rule and you start over. A mob being coaxed never turns on you, and after a failed try it leaves you alone for 10 seconds.
- **Fighting.** Critters fight hostile mobs only: the ones after you or your critters, and other hostile mobs near you that aren't neutral or named. They never hurt players, pets, villagers or farm animals. The skeleton shoots and the allay never fights.
- **Levels.** Each critter has its own level, 1 to 100. It earns experience for every hostile mob it helps beat (the mob's max health, so 20 for a zombie) and 2 for every minute out with you. Health, attack and speed grow a little each level and slow near the top: at level 100 a critter has four times its health, three times its attack and a fifth more speed. The Bunkhouse menu shows each critter's level, how far it is from the next and its special.
- **Specials.** Every critter has one, and it grows at levels 25, 50, 75 and 100.
  - Chicken: you fall slowly while it's out. From level 25 it lays a speedy egg at your feet every 5 minutes (a minute sooner at each later milestone); right-click it for Speed II for 10 seconds, Speed III from level 75.
  - Axolotl: water breathing and regeneration while you're within 6 blocks of it. It goes after drowned and guardians first, and plays dead and heals when it's badly hurt.
  - Bee: crops near it grow faster, one crop a second. From level 25 its sting makes a hostile mob glow, so you can see it through walls.
  - Fox: fetches items you drop or leave behind (drops from blocks you break and mobs you beat), after they've lain 5 seconds. It never touches anyone else's. From level 25 it pounces on a hostile mob that isn't after it, and the hit it lands with the pounce does double damage.
  - Allay: right-click it holding an ore, a raw ore or a gem, and it flies to the nearest matching ore within 8 blocks of you.
  - Creeper: a blast that hurts only the hostile mobs around its target and breaks nothing. It never goes off with a player within 6 blocks.
  - Enderman: right-click it, then swing at a block within 8 blocks, and it fetches it for you. Only blocks an enderman can carry, and only where you could dig yourself, so claims say no. When it's badly hurt, it teleports to you.
  - Skeleton: covering fire from 15 blocks, reaching further and shooting faster at each milestone. From level 25 it shares two arrows every 2 minutes while you carry a bow or crossbow.
  - Iron golem: takes a quarter of any hostile mob's hit on you within 8 blocks, and throws the mobs it hits back.
- **La Ménagerie.** Your journal, with a page for each critter: its Critterbook number, a line about it, how to coax it and its special. A page opens the first time you're within 10 blocks of that critter and can see it, or when you get one. `/mobsquad journal` or the book in the Bunkhouse menu opens it. Critterbook numbers follow the order each mob first appeared in Minecraft Java Edition, so the creeper is No. 1 and the allay No. 75. Every mob in the game has a number already, and the numbers never change.
- **Spirit coats.** A critter earns a new coat every 10 levels, ten in all, and keeps them. Coats show only in the worlds listed under `spirit-coats.worlds` in `config.yml`, which is empty to start with, so critters look normal everywhere until an admin lists a world. Wolves, cats, pigs, cows, chickens and frogs wear a real variant; every other critter glows in the coat's color and leaves a light particle trail, only for players who haven't turned particles to minimal. Right-click a critter in the Bunkhouse to pick the coat it wears from the ones it has earned; the default is the newest. The names, colors, particles and variants are in `plugins/MobSquad/coats.yml`. The journal lists each critter's coats, with the locked ones as the level that earns them.
- **The sidekick.** A critter that has spent 10 hours out with you has a 1 in 10 chance each day of becoming your sidekick. It goes everywhere with you: it comes out when you join, after a world change and after a rest, and the Bunkhouse menu, its bell and the dinnerbell leave it be. It doesn't count toward your posse. One per player.
- **Vittles.** Right-click them to perk up a tuckered-out critter on the spot. Craft two from bread, a carrot and sweet berries.
- **Bunkhouses.** An admin marks a vault block as a Bunkhouse. Anyone who right-clicks it sees only their own critters. Click one to send it out or call it back. Critters never exist as items, so they can't be dropped, traded or duplicated.
- **Following.** A critter out with you walks after you and steps beside you if it's left more than 24 blocks behind.
- **Wranglin'.** Coaxing earns Wranglin' experience. One critter can be out at once, two from level 10 and three from level 25. Mob tiers unlock at levels 1, 10, 25 and 40.
- **AuraSkills.** If AuraSkills is installed, Wranglin' shows up as one of its skills, with MobSquad's level. MobSquad keeps the level in the player's file and copies it to AuraSkills when it changes and when the player joins, so a level set with AuraSkills' own commands is put back. AuraSkills' XP multipliers and rewards don't apply to Wranglin'. The skill's AuraSkills settings are in `plugins/MobSquad/auraskills`. Without AuraSkills, nothing changes.
- **World groups.** With Multiverse-Inventories, critters cross to another inventory group only through the Bunkhouse: crossing sends the ones out with you home, and you can send them out again from a Bunkhouse there. The sidekick comes along, one copy. A block the enderman fetches only goes into your inventory in the world it came from.
- **Safety.** A critter never targets or hurts a player, and players can't hurt it. It never changes a block on its own, explodes, burns in the sun or wanders off by teleporting. The two specials that touch blocks go through the server's own checks: the bee's crops grow through the same event as natural growth, and the enderman takes a block only where a block-break check as you passes. Out of health, it's tuckered out: it drops nothing and rests in the Bunkhouse for 5 real minutes.
- **The dinnerbell.** Ring it and every critter out with you heads home.
- **Plain files.** One text file per player in `plugins/MobSquad/players`. A critter out in the world is never saved with the world, so after a restart, a crash or the player leaving, it's back in the Bunkhouse exactly once.

## Install or build

Paper 26.1 or newer, Java 25. Drop `MobSquad-<version>.jar` into `plugins` and restart. To build it yourself: `./gradlew build`, and the jar is in `build/libs`.

Settings are in `plugins/MobSquad/config.yml`: worlds where MobSquad is off, the rest time after tuckering out, the Wranglin' experience for a coax, and `spirit-coats.worlds`, the worlds where spirit coats show. `coats.yml` beside it holds each coat's name, glow color, particle and variants; a line you get wrong is skipped with a warning and the built-in one stays.

## Commands

| Command | Who | What it does |
| --- | --- | --- |
| `/mobsquad` | everyone | How it works, in four lines. |
| `/mobsquad list` | everyone | Your critters and your Wranglin' level. |
| `/mobsquad hints` | everyone | How to coax each critter. |
| `/mobsquad journal` | everyone | Opens La Ménagerie. |
| `/mobsquad dinnerbell` | everyone | Gives you a dinnerbell. |
| `/mobsquad bunkhouse add` and `remove` | admins | Marks or unmarks the vault you're looking at. |
| `/mobsquad give <player> <critter>` | admins | Puts a critter in a player's Bunkhouse. |
| `/mobsquad vittles <player> [amount]` | admins | Gives vittles. |
| `/mobsquad xp <player> <amount>` | admins | Sets a player's Wranglin' experience. |
| `/mobsquad level <player> <critter> <level>` | admins | Sets every critter of that kind a player has to a level. |
| `/mobsquad sidekick <player> <critter\|none>` | admins | Makes a player's critter of that kind their sidekick, or clears it. |
| `/mobsquad reload` | admins | Rereads config.yml. |

`/ms` works too. Permissions: `mobsquad.use` (everyone) and `mobsquad.admin` (ops).

## For other plugins

MobSquad registers `com.extantape.mobsquad.api.MobSquadApi` with Bukkit's services manager. Another plugin can read a player's critters, check whether one is out, send them home, ask whether MobSquad is on in a world, keep it off in its own private world, and get a critter's health, damage and speed at a level. It can't add, remove or change critters. MobShowdown uses it for its battles. Call it on the main thread, and check `version()` before using anything newer than version 1. Version 2 adds each critter's spirit coat (`coat` and `coatName`) to `CritterInfo`.

## Tests

`test/suite.mjs` builds a throwaway Paper server on 127.0.0.1 from downloaded jars and drives it with bots. See the top of the file for what it can and can't check.

## Not affiliated

The tagline is a joke. MobSquad isn't affiliated with or endorsed by the makers of any creature-collecting game, and uses none of their names, art or designs.

## License

MIT. See [LICENSE](LICENSE).
