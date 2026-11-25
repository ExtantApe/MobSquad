# MobSquad

Coax mobs into critters for Paper: they follow you, fight only hostile mobs, and live safely in a shared Bunkhouse. No client mods. Gotta coax em all... Pokemob!

**Status:** 0.12.0, for Posse 0.11.0 (formerly Mayor): the adult permission is now `posse.circle.adults`, and the Ten Winds section passed again on 26.1.2, 26.2 and 26.3 on 2026-11-25 (99, 99 and 92 checks); the rest of the suite wasn't rerun. 0.11.0: the Ten Winds story ran on throwaway Paper 26.1.2, 26.2 and 26.3 servers with BetonQuest 3.3.0-DEV-61, LuckPerms 5.5.71 and PlaceholderAPI on 2026-11-24, with bots as players (98 checks on 26.1.2 and 26.2, 91 on 26.3). They cover a kid path, an adult path with the adults' rings and the 15-kill watch, a player with no group getting the kid lines, the ring level gates, a second player standing in the ring getting nothing, the dinnerbell finale, every book read off the server, and typed words held back during a conversation. The full MobSquad suite passed on 26.1.2 and 26.2 (215 checks each) after the change. It didn't get a full rerun on 26.3, and the quest chapters section (the three tier chapters) was rerun on all three but for one count in its own check that I've since corrected and not rerun (50 of 51 passed each time). On 26.3 the bots can't place blocks, so the builder's 40-block step ran on 26.1.2 and 26.2 only. Citizens wasn't on the test server, so nobody clicked the Ranger; a console action started the conversation, and the spots were made-up places in a flat world. A tag stood in for months of play where a test needed one style or the three tier chapters done. Bots can't draw anything, so the rings, the books and the Ranger's text haven't been seen in a real game client, and the kid wording hasn't been read by a child. Before that, 0.10.0's three tier chapters ran on the same three versions (51 checks each), and 0.9.0's quest commands and placeholders and 0.8.1's glow changes passed the full bot suite on 26.2 (215 checks) and 26.3 (113; the menu sections need 26.2) on 2026-11-22. 0.8.0 passed with bots on throwaway Paper 26.1.2 and 26.2 servers (the full suite, 197 checks each) and on 26.3 (95 checks). On 26.3 the bots can't use blocks, so the Bunkhouse menu, picking a coat in it and everything sent out from it didn't run there. Bots can't crouch where the server sees it, so the creeper and fox puzzles were tested with a test-only stand-in for crouching; real crouching is untested. The daily sidekick roll is covered by unit tests only, since it needs 10 hours out. Nothing has been seen in a real game client yet, so how the glow, the particle trail and the chicken variants look is unchecked. Wolves, cats, pigs, cows and frogs aren't critters yet; only the chicken's variants ran on a server. The AuraSkills and Multiverse checks from 0.6.0 weren't rerun.

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
- **Spirit coats.** A critter earns a new coat every 10 levels, ten in all, and keeps them. Coats show only in the worlds listed under `spirit-coats.worlds` in `config.yml`, which is empty to start with, so critters look normal everywhere until an admin lists a world. Wolves, cats, pigs, cows, chickens and frogs wear a real variant. Every other critter glows in the coat's color and leaves a light particle trail, and so do pigs, cows, chickens and frogs, whose three looks repeat across coats (`glow-too` in `coats.yml`). The trail shows only for players who haven't turned particles to minimal. Worlds under `spirit-coats.no-glow-worlds` show coats without the glow outline, which shows through walls; list kids' worlds there. Right-click a critter in the Bunkhouse to pick the coat it wears from the ones it has earned; the default is the newest. The names, colors, particles and variants are in `plugins/MobSquad/coats.yml`. The journal lists each critter's coats, with the locked ones as the level that earns them.
- **The sidekick.** A critter that has spent 10 hours out with you has a 1 in 10 chance each day of becoming your sidekick. It goes everywhere with you: it comes out when you join, after a world change and after a rest, and the Bunkhouse menu, its bell and the dinnerbell leave it be. It doesn't count toward your posse. One per player.
- **Vittles.** Right-click them to perk up a tuckered-out critter on the spot. Craft two from bread, a carrot and sweet berries.
- **Bunkhouses.** An admin marks a vault block as a Bunkhouse. Anyone who right-clicks it sees only their own critters. Click one to send it out or call it back. Critters never exist as items, so they can't be dropped, traded or duplicated.
- **Following.** A critter out with you walks after you and steps beside you if it's left more than 24 blocks behind.
- **Wranglin'.** Coaxing earns Wranglin' experience. One critter can be out at once, two from level 10 and three from level 25. Mob tiers unlock at levels 1, 10, 25 and 40.
- **Quest chapters.** Three small chapters for BetonQuest open the tiers: finish one and that tier opens, whatever your Wranglin' level. Ranger Juniper gives them in order. Each has three steps and opens one critter's journal page.
  - **Holler:** pick six sweet berries, gather four seeds from tall grass, craft a note block. Opens the fox page.
  - **Yonder:** craft four arrows, a target block and a campfire. Opens the creeper page.
  - **Backcountry:** pick a poppy, smelt four iron ingots, craft shears. Opens the Iron Golem page.
  
  The level still works, so a tier opens at its level or by its chapter, whichever comes first. A chapter never changes a level, and it never opens more than its own tier. `quests.tier-unlock` in `config.yml` decides how tiers open: `either` (the default: level or chapter), `level` (level only, the way it was before quests) or `quest` (only a chapter opens Holler, Yonder and Backcountry; Yard is always open). Without BetonQuest, `either` is the same as `level`, and nothing breaks. Install steps for the chapters are under Install or build.
  
  Any quest plugin can do the same: it runs `/mobsquad unlock <player> <tier>` or `/mobsquad page <player> <critter>` from the console. With PlaceholderAPI installed, quest conditions can check the player's own progress: `%mobsquad_level%`, `%mobsquad_critters%`, `%mobsquad_pages%`, `%mobsquad_tier_<tier>%`, `%mobsquad_page_<critter>%`, `%mobsquad_has_<critter>%`, `%mobsquad_best_<critter>%` (the highest level among that kind, 0 if none) and `%mobsquad_top%` (the highest level among all the critters they own, 0 if none; it counts the critters they own, whether or not any is out). Two more are for the Ten Winds: `%mobsquad_style%` (builder, fighter, explorer, caretaker or all, from the player's own play) and `%mobsquad_holding_bell%` (true if the dinnerbell is in their main hand). Values are only for online players, and none of them name another player or a place.
- **The Ten Winds.** A story that follows the three chapters, in the same BetonQuest package. Ranger Juniper says each of ten winds once blew a spirit coat onto a critter's back, and some have gone to sleep. Five more chapters, one at a time, in this order: **Wind-Signs** (find three signs, or rebuild one with 40 blocks, or bring berries, or chase off five hostile mobs, whichever suits how you play), **the Ten Winds** (walk into three wind-rings in a magic world; each wakes for a player who owns a critter at level 10, 20 or 30), **Holding the Line** (beat hostile mobs for the Ranger's watch: 15 at dusk or night for adults, 8 at any hour for kids, and it's optional for kids), **the Far Meadow** (visit four landmarks, three for kids) and **the finale** (ring the dinnerbell in the meadow). The story picks its path from what you do most. Kids and adults get the same books, journal lines and rewards, and nothing in it changes a level, a tier or a critter. The Ranger's books (a trail book, the watch-bell, a meadow book and the finale) are written by Ranger Juniper, name no one and give no place. The rings check the level of the critters you own, whether or not one is out. Nobody types anything: the Ranger, the journal, the rings and the bell do it all.
  - **Kid or adult fails closed.** The adult wording and the adult goals show only to a player who has the permission `posse.circle.adults` (Posse's adults circle, which marks adults on the live servers). Anyone without it, with no group at all, or when the permission can't be read, gets the kid lines (12 words or fewer) and goals in the kids' own worlds.
  - **It starts switched off.** The new chapters wait until an admin sets them up (below) and turns them on.
- **AuraSkills.** If AuraSkills is installed, Wranglin' shows up as one of its skills, with MobSquad's level. MobSquad keeps the level in the player's file and copies it to AuraSkills when it changes and when the player joins, so a level set with AuraSkills' own commands is put back. AuraSkills' XP multipliers and rewards don't apply to Wranglin'. The skill's AuraSkills settings are in `plugins/MobSquad/auraskills`. Without AuraSkills, nothing changes.
- **World groups.** With Multiverse-Inventories, critters cross to another inventory group only through the Bunkhouse: crossing sends the ones out with you home, and you can send them out again from a Bunkhouse there. The sidekick comes along, one copy. A block the enderman fetches only goes into your inventory in the world it came from.
- **Safety.** A critter never targets or hurts a player, and players can't hurt it. It never changes a block on its own, explodes, burns in the sun or wanders off by teleporting. The two specials that touch blocks go through the server's own checks: the bee's crops grow through the same event as natural growth, and the enderman takes a block only where a block-break check as you passes. Out of health, it's tuckered out: it drops nothing and rests in the Bunkhouse for 5 real minutes.
- **The dinnerbell.** Ring it and every critter out with you heads home.
- **Plain files.** One text file per player in `plugins/MobSquad/players`. A critter out in the world is never saved with the world, so after a restart, a crash or the player leaving, it's back in the Bunkhouse exactly once.

## Install or build

Paper 26.1 or newer, Java 25. Drop `MobSquad-<version>.jar` into `plugins` and restart. To build it yourself: `./gradlew build`, and the jar is in `build/libs`.

Settings are in `plugins/MobSquad/config.yml`: worlds where MobSquad is off, the rest time after tuckering out, the Wranglin' experience for a coax, `spirit-coats.worlds`, the worlds where spirit coats show, and `spirit-coats.no-glow-worlds`, the ones among them where coats show without the glow outline. `coats.yml` beside it holds each coat's name, glow color, particle and variants; a line you get wrong is skipped with a warning and the built-in one stays.

### Installing the quest chapters

They need BetonQuest 3.3 and, for the Ranger, Citizens. The first time MobSquad starts it copies `package.yml` to `plugins/MobSquad/quests/mobsquad/`, and it never overwrites that copy.

1. Copy the `mobsquad` folder from `plugins/MobSquad/quests/` into `plugins/BetonQuest/QuestPackages/`.
2. In Citizens, make an NPC for the Ranger (`/npc create Ranger Juniper`) and note its number. Open `package.yml` and change `ranger: citizens 1` to that number.
   **Where the Ranger stands:** inside each world group kids play in, never only at a shared hub, so kids don't have to travel somewhere strangers could wait for them. For a second Ranger, make another NPC and add a line under each of `npcs:` and `npc_conversations:` with a new name, for example `ranger_kids: citizens 2` and `ranger_kids: ranger`. Both Rangers share one conversation and one set of chapters.
3. Run `/bq reload`. BetonQuest names anything it couldn't read in the console.
4. Players talk to the Ranger. Nobody types a command: the Ranger's conversation starts each chapter, the quest journal book is handed over when a chapter starts, and each step comes with a chat line.

**Upgrading from before 0.9.0:** check `quests.tier-unlock` in `config.yml`. A config with no `quests:` section now gets `either`; set `level` if tiers should still open by level only.

### Setting up the Ten Winds

Do this once per world group (kids' and adults'). Nothing in it is hardcoded: every place the story uses is a named location in the `constants:` section at the top of `package.yml`, shipped as `0;0;0;unset`, and an unset one can't be reached.

1. **Build the spots.** In each world group, an admin builds: three wind-signs (any sign or lantern post), three wind-rings in the magic world (any arrangement of blocks; a ring is a spot the player walks into, 6 blocks across), three or four landmarks (a hill, a lake, a river for adults only, a flower meadow) and the meadow. The kids' spots go in the kids' worlds and the adults' in the adults' worlds. Kids are never sent to the adults' world, and a kid standing at an adult spot does nothing.
2. **Protect each spot with a claim,** with GriefPrevention or whatever your server uses, so a player can't break or move a post, ring or landmark that other players need. A player's own progress is kept per player, not in the world, so nobody can undo another player's work. Put the posts in places kids can reach from their own world's spawn, not in the middle of a hub.
3. **Write each spot's place into `constants:`** as `x;y;z;world` (stand on it and read the coordinates from F3). The names say which spot each one is: `signPost1Kid`, `ringEmberAdult`, `landRiverAdult`, `meadowKid` and so on.
4. **Check the adults' permission.** The `adult` condition is `permission posse.circle.adults`. If your server marks adults with another permission, change that one line.
5. Run `/bq reload`, and read the console: BetonQuest names anything it couldn't read.
6. **Turn the chapters on** with `/bq action - mobsquad>windsOpenOn` from the console. `/bq action - mobsquad>windsOpenOff` turns them off again; a player who has started keeps their progress and finishes what they were doing, and nobody is offered a new chapter.

The Ranger gives out the books and the dinnerbell by console command (`/mobsquad book <player> <id>` and `/mobsquad dinnerbell <player>`, admins and the console only); the book ids are `trail`, `watch`, `meadow` and `finale`.

With another NPC plugin, change the `npcs:` line to match it. Without an NPC, an admin can start a chapter for a player with `/bq action <player> mobsquad>hollerStart` (or `yonderStart`, `backcountryStart`). To take a tier back, `/mobsquad lock <player> <tier>`.

## Commands

| Command | Who | What it does |
| --- | --- | --- |
| `/mobsquad` | everyone | How it works, in four lines. |
| `/mobsquad list` | everyone | Your critters and your Wranglin' level. |
| `/mobsquad hints` | everyone | How to coax each critter. |
| `/mobsquad journal` | everyone | Opens La Ménagerie. |
| `/mobsquad dinnerbell` | everyone | Gives you a dinnerbell. |
| `/mobsquad dinnerbell <player>` | admins, quests | Gives a player a dinnerbell, unless they have one. |
| `/mobsquad book <player> <id>` | admins, quests | Gives a player one of the Ranger's books: `trail`, `watch`, `meadow` or `finale`. |
| `/mobsquad bunkhouse add` and `remove` | admins | Marks or unmarks the vault you're looking at. |
| `/mobsquad give <player> <critter>` | admins | Puts a critter in a player's Bunkhouse. |
| `/mobsquad vittles <player> [amount]` | admins | Gives vittles. |
| `/mobsquad xp <player> <amount>` | admins | Sets a player's Wranglin' experience. |
| `/mobsquad level <player> <critter> <level>` | admins | Sets every critter of that kind a player has to a level. |
| `/mobsquad sidekick <player> <critter\|none>` | admins | Makes a player's critter of that kind their sidekick, or clears it. |
| `/mobsquad unlock <player> <tier>` and `lock` | admins, quests | Opens (or takes back) Holler, Yonder or Backcountry for a player through a quest. |
| `/mobsquad page <player> <critter>` | admins, quests | Opens a critter's journal page and tells the player. |
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
