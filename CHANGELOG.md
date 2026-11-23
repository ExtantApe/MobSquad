# Changelog

## 0.8.1

- **Pigs, cows, chickens and frogs also glow in their coat.** They have only three looks each, so a look repeats for three or four coats; now they also glow in the coat's color and leave its trail, so every coat looks different. Wolves and cats keep the variant only. The list is `glow-too` in `coats.yml`. A copy of `coats.yml` already in `plugins/MobSquad` without that line gets the built-in list. A name that isn't a variant critter is skipped with a warning.
- **No-glow worlds.** A glow outline shows through walls, so in a kids' world it could lead others to where a kid is building. Worlds listed under `spirit-coats.no-glow-worlds` in `config.yml` show coats without the outline: critters keep their variant and their coat's trail, and particles don't show through blocks. Empty by default.
- Tested: unit tests (46). The bot suite's coat checks were updated for the chicken's glow and no-glow worlds but haven't been rerun yet; that run comes before any live install. Not seen in a real game client.

## 0.8.0

Spirit coats.

- **A new coat every 10 levels**, ten per critter: coat 1 at level 10 up to coat 10 at level 100. Coats are earned by level and a critter keeps them. Leveling past a multiple of 10 tells the owner the coat's name.
- **Shown only in magic worlds.** `spirit-coats.worlds` in `config.yml` lists them, by folder name, and it's empty by default, so nobody sees a coat until an admin lists a world. Elsewhere critters look as they always did. Unlisting a world (`/mobsquad reload`) or leaving one takes the look off at once, and entering one puts it on.
- **What a coat looks like** is in the new `coats.yml` (copied to `plugins/MobSquad` the first time; edit it there). Each coat has a plain name, a glow color and a particle: Ember, Marigold, Sunbeam, Meadow, Pine, Lagoon, Frost, Twilight, Blossom and Starlight. Wolves, cats, pigs, cows, chickens and frogs wear a real variant, set per coat in the same file. Every other critter glows in the coat's color and leaves a light particle trail. A bad line in the file is skipped with a warning in the log, and the built-in value stays.
- **Cheap particles.** About 4 a second per glowing critter, sent only to players inside their own view distance, and never to a player who set particles to minimal in their game options. Players on decreased get half.
- **Choosing a coat.** Right-click a critter in the Bunkhouse for its coats: earned ones by name, locked ones as "Level 30", the one it wears marked in words, and "Newest coat", which is the default and follows each new coat. Left-click still sends it out. The choice is saved with the critter as a ninth field on its line in the player's file; older files read as "newest", so every existing save loads unchanged.
- **La Ménagerie** has a fourth page for each critter that lists its coats (earned ones by name, the rest as the level that earns them), in place of "Spirit coats come later."
- **API version 2.** `CritterInfo` has `coat` (0 before level 10, otherwise 1 to 10) and `coatName`, for MobShowdown. It's the coat the owner picked or the newest earned, whether or not the world shows it. The old seven-argument constructor still works.
- Colors never carry the meaning: every menu line, chat message and journal line has the coat's name, and the menu's dark glow colors are written in a lighter color so they read on the dark background.
- Tested: unit tests (earning at levels 9, 10, 99 and 100, the choice and the migration, magic-world gating, the data file with bad entries, the journal page, the API) and the bot suite (see the README for versions and counts). Not seen in a real game client: how the glow, the trail and the variants look. Wolves, cats, pigs, cows and frogs aren't critters yet, so their variants are covered by the data file's tests only; the chicken's variants ran for real.

## 0.7.1

- Menu and chat hints are easier to read (K681). Lore lines in the Bunkhouse, Critterbook and vittles menus, and the locked lines in the coaxing list, move from dark gray (under 3:1 on the dark tooltip) to gray (about 9:1). Unlocked coaxing hints are now white so they still stand apart. Critterbook book pages keep dark gray, since they sit on a light page.
- Tested: unit tests. Not seen in a real game client.

## 0.7.0

An API for other plugins, for MobShowdown.

- **MobSquadApi**, from Bukkit's services manager: a player's critters (read-only), whether they have one out, sending them home, whether MobSquad is on in a world, keeping it off in another plugin's private world until the server stops, and a critter's strength at a level. Nothing in it lets another plugin add, remove or change a critter.
- Nothing changes for players.

## 0.6.0

Wranglin' in AuraSkills.

- **With AuraSkills installed**, Wranglin' is one of its skills, with a lead as its icon and MobSquad's level. MobSquad still keeps the level in the player's file and copies it to AuraSkills when it changes, when the player joins and when an admin uses `/mobsquad xp`. A level set with AuraSkills' commands is put back at the next change or join. AuraSkills' XP multipliers and rewards don't apply.
- The skill's AuraSkills settings are written to `plugins/MobSquad/auraskills` the first time. MobSquad declares `auraskills.skill.wranglin` (default true), which AuraSkills checks for each skill.
- Without AuraSkills nothing changes. Built against AuraSkills 2.4.0.
- **World groups.** Tested with Multiverse-Inventories 5.3.6: crossing to another inventory group sends critters home, the sidekick comes along as one copy, and nothing turns into an item.
- **Fixed:** a block the enderman fetched went into your inventory in whatever world you were in a second later, so it could cross world groups, and it was lost if the enderman went home in that second. It now goes to you only in the world it came from, and otherwise drops where the block was.

## 0.5.0

The sidekick and the later parts of four specials.

- **The sidekick.** A critter with 10 hours out with you has a 1 in 10 chance each day of becoming your sidekick, rolled once a day while it's out. It comes out on its own when you join, change worlds or it's rested, doesn't count toward your posse, and stays when you click it in the Bunkhouse menu, use the menu's bell or ring the dinnerbell. One per player. Admins can set or clear it with `/mobsquad sidekick <player> <critter|none>`.
- **Time together** is counted per critter and saved as an eighth field on its line in the player's file; older files read as 0.
- **From level 25:** the chicken lays speedy eggs, the bee's sting makes hostile mobs glow, the fox pounces for a double first hit, and the skeleton shares arrows when you carry a bow or crossbow. Each gets stronger at the later milestones; see the README.
- Not yet: travelling with your sidekick through a gate (that waits for the Waystation hook) and the small model of your sidekick above the Bunkhouse.

## 0.4.0

La Ménagerie and the Critterbook.

- **La Ménagerie**, the journal: its own menu, opened with `/mobsquad journal` or from the book in the Bunkhouse menu. Each launch critter has a page with its Critterbook number, a half-ridiculous line about it, how to coax it, its special and your best one's level. Locked pages show only their number.
- **Pages open when you meet a critter**: within 10 blocks with a clear line of sight, or when you coax or are given one. You're told, with a link to the journal. Pages are saved in the player's file as `page` lines; files from before 0.4.0 open pages for the critters already in them.
- **Critterbook numbers** for all 88 mobs in Java Edition, in the order each first appeared, from the Minecraft Wiki's history sections. Same-version ties go by first snapshot, then alphabetically. The launch critters: creeper 1, skeleton 3, chicken 9, enderman 15, iron golem 25, fox 62, bee 63, axolotl 69, allay 75. The list ships in the jar as `critterbook.txt`.
- Not yet: spirit coats (the page says they come later) and quest chapters opening pages.

## 0.3.0

Levels and specials.

- **Critter levels, 1 to 100.** A critter earns experience for each hostile mob it helps beat (the mob's max health, up to 100) and 2 for each minute out. Level n needs 10 (n - 1)² experience: level 10 at 810, level 50 at 24,010, level 100 at 98,010. You're told when a critter levels up, and again when its special grows.
- **Stats grow with the level** and slow near the top: health up to four times, attack up to three times, speed up to a fifth more at level 100.
- **Every launch critter's special**, growing at levels 25, 50, 75 and 100: the chicken's slow falling, the axolotl's water breathing and regeneration (and playing dead), the bee's crop growth, the fox's fetching, the allay's ore finding, the creeper's blast that hurts only hostile mobs, the enderman's block fetching and its teleport out of trouble, the skeleton's longer, faster covering fire, and the iron golem's guard and throw. See the README for the numbers.
- The Bunkhouse menu shows each critter's experience to the next level and its special.
- `/mobsquad level <player> <critter> <level>` for admins.
- Not yet: the brief's later parts of four specials (the chicken's speed eggs, the bee's glowing sting, the fox's pounce, the skeleton sharing arrows).

## 0.2.0

The rest of the launch puzzles, fighting and vittles.

- **Every launch critter can be coaxed.** The creeper standoff (gunpowder starts it; crouch through three swells and feed it gunpowder each time it calms) and the enderman's no-staring rule (offer it a block it could carry, then stay near it for 20 seconds without looking it in the eye) follow the brief. The owner kept the other six as drafted (2026-11-15): the axolotl (show it a bucket of tropical fish while you're both in water, then swim with it for 15 seconds; the bucket isn't used), the bee (show it a flower, then stand still for 10 seconds), the fox (crouch, offer berries, and stay crouched for 15 seconds), the allay (an amethyst shard, then three notes from a note block near it), the skeleton (hand it an arrow, then hit a target block three times) and the iron golem (only one a player built: four iron ingots, then a poppy). `/mobsquad hints` lists them.
- **Coaxing is safe.** A mob being coaxed never targets you, and a creeper can't go off during the standoff. After a failed or refused attempt, the mob leaves you alone for 10 seconds, and a creeper backs away.
- **Critters fight hostile mobs only.** They go after a hostile mob near you that's after you, or has hurt you or one of your critters, or any other hostile mob near you that isn't neutral (endermen and piglins) or named. They never stray more than 16 blocks from you. The skeleton shoots a steady arrow every second and a half within 15 blocks; the allay never fights; the rest close in and hit. A critter never hurts a player, a pet, a villager, a farm animal or another player's critter, whatever its own instincts say, and its arrows never break a painting or an item frame.
- **Vittles** perk up the critter that was tuckered out most recently, and send it out beside you if there's room. Two from bread, a carrot and sweet berries. They can't be eaten. Admins can give them with `/mobsquad vittles <player> [amount]`.
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
