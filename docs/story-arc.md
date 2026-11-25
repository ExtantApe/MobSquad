# The Ten Winds: a MobSquad story arc (draft, revised after Trust and Safety)

Written by the narrative and systems designer, 2026-11-24, after a Trust and Safety review. The owner's picks (D289, 2026-11-24) adopted it as drafted with the safety changes, and MobSquad 0.11.0 builds it. Where the build differs from this text, `docs/decisions.md` says how. Sections 6 and 8 list things that were unverified when this was written; 0.11.0 verified them against BetonQuest 3.3.0-DEV-61, and the results are in the decisions file and the changelog.

Related decisions: D284 (players don't type commands, `ops/decisions/games.md`) and D287 (Waycave circles in Posse, `ops/decisions/minecraft-plugins.md`).

## 1. Pitch

Ranger Juniper's trails have been quiet since the Ten Winds went to sleep. Each wind once blew a spirit coat onto a critter's back, one color at a time. Lately fog has rolled into the magic worlds and some coats have gone gray. Juniper needs wranglers to follow the old wind-trails through the same Holler, Yonder and Backcountry as before, find where each wind is napping, and wake it with the right kind of trouble: a lit signal fire, a sign-post built by hand, a good scrap with hostile mobs, or a long walk to a far meadow. Every path ends in the meadow, where the whole herd comes home. A fox who is sure he is the Winds' mail carrier comes along for the ride.

## 2. The existing three chapters

Keep the mechanics, rewards and tier unlocks exactly as they are. Only the journal and Ranger lines change.

- **Holler** (opens the fox page): the finish line becomes "The first wind stirs! Juniper says there are nine more, and the fox wants to carry the news."
- **Yonder** (opens the creeper page): the campfire becomes a signal fire. "Juniper says a lit fire tells the winds you're coming."
- **Backcountry** (opens the Iron Golem page): the shears stay. The Iron Golem becomes the guard of the first wind-gate.

Finishing a chapter still never changes a level. The new chapters tie into coats through level gates, not by handing out levels.

## 3. The new chapters

Five chapters, each with a goal, a place, a tie-in and a reward. Each has builder, fighter, explorer and caretaker paths (section 4).

Shared spots: the wind-signs, landmarks and meadow sit in the world, and in `ninos_survival` they are shared with other kids. Every post and landmark is claim-protected. A player's own progress (a rebuilt sign, a landmark visit) is stored per player, so nobody can undo or take over another player's work.

**4. The Lost Wind-Signs** (survival world, after Holler)
- **Goal:** find three wind-signs the Ranger's crew stuck in the ground, and rebuild the one that's broken. Builders place the post; caretakers bring it four berries; fighters clear hostile mobs at the post; explorers walk to all three.
- **Where:** the player's own survival world. The admin builds the three posts once, claim-protected.
- **Tie-in:** finishing it opens the wind-trail (a journal entry). No tier changes.
- **Reward:** the Ranger's trail book, with a sketch of the nine winds. The book has no coordinates and isn't a map item.

**5. The Ten Winds** (magic world, after Yonder)
- **Goal:** walk into three wind-rings in the magic world. Each ring wakes for a wrangler who owns a critter at the right level.
- **Where:** `magic` for adults, `ninos_magic` for kids. Each world has its own rings. No kid is sent to the adult world.
- **Tie-in:** three gates, one per coat. Ring one needs a critter at level 10 (Ember), ring two at 20 (Marigold), ring three at 30 (Sunbeam). The gates read `%mobsquad_best_<critter>%`, which checks what the player owns, not whether a critter is out, so no coated critter has to be visible for a ring to open.
- **Reward:** each wind lights one journal line: the coat's name and a line from the wind. No items with power.
- **Effects:** the ring lighting and the wind waking are sent only to that player. Nothing is broadcast, and there are no world particles other players can see.

**6. Holding the Line** (survival world, after Backcountry)
- **Goal:** help the Ranger's watch by beating hostile mobs with a critter out.
  - Adults: 15 hostile mobs, at dusk or night.
  - Kids: 8 hostile mobs, any time of day, and the chapter is optional. Kids who skip it still get the Ranger's praise at the end of the arc, so nothing is locked.
- **How it counts:** a BetonQuest kill objective, counting hostile mob types only. Not the vanilla total-mob-kills statistic, which counts everything.
- **Where:** the player's own survival world.
- **Tie-in:** the Iron Golem's page gets a second line about the watch.
- **Reward:** the Ranger's watch-bell, a written book with a short poem about it, and a line of praise. Kids get the gentler lines.

**7. The Far Meadow** (survival world, after Yonder)
- **Goal:** visit four landmarks: a hill, a lake, a river and a flower meadow (kids: three, closer together). Each is a sign or lantern post the admin places, claim-protected.
- **Where:** the player's own survival world, reached on foot.
- **Tie-in:** the meadow is the finale's place, so this chapter teaches the way there.
- **Reward:** a meadow book with no coordinates. The meadow sign says "the wrangler", never a name.

**8. The Herd Comes Home** (finale, survival meadow)
- **Goal:** ring the dinnerbell in the meadow. The ringer's own critters head home, and the winds wake one at a time for that player.
- **Where:** the meadow in the player's own survival world. Nothing needs a visit to another group's world.
- **Tie-in:** needs the three tier chapters and at least two of the new chapters (journal tags only, no levels).
- **Reward:** a finale book from Ranger Juniper with a line for each finished chapter, and a journal entry "The Ten Winds are awake."

**Books.** Every book in the arc is written by Ranger Juniper. None of them names the player, and none has coordinates. They are books, not map items, and no reward has trade value.

## 4. How it adapts on its own

Each chapter has one story. The table changes the words, the goal size and the path. It never changes the rewards.

Kid or adult fails closed. The adult version shows only when a player is positively in an adult group. Unknown, empty or unreadable gets the kid version, and kids are never pointed at `magic`. Age never comes from the world, from stats or from play style. Test: a player with no LuckPerms group gets the kid lines.

| Signal | Where it's read | What changes |
| --- | --- | --- |
| Kid or adult | LuckPerms group, read as a placeholder or permission condition (verify it's readable on the live build). Fails closed, as above. | Kids get lines of 12 words or fewer, gentler words, and two reply choices. Kids' goals stay in the kids' own world. |
| World | The player's current world (BetonQuest world condition or PlaceholderAPI's player-world value; verify) | Survival chapters get the trail, berries, signs and meadow. Magic chapters get the rings, fog, color and wind. |
| Builder | Build count (verify BetonQuest can read a placed-block count; if not, the sign-post step is a fixed step) | Builders get the sign-post step and a line about the design. Goal: 40 blocks placed. |
| Fighter | Hostile-mob kill objective, as in chapter 6 | Fighters get "Holding the Line" and a wind-line about the fight. Adults 15 kills, kids 8. |
| Explorer | Landmark objectives (location) | Explorers get the Far Meadow and a trail-book line. Adults 4 landmarks, kids 3. |
| Caretaker | Berries or seeds handed in at a post | Caretakers get the berries step in the Wind-Signs chapter and a line about the critter's mood. |
| Level and coat | `%mobsquad_level%`, `%mobsquad_best_<critter>%` | Ring gates in chapter 5 only. Nothing else depends on level. |
| Tier | `%mobsquad_tier_<tier>%` | Chapter 8 waits for the three tiers. A player with a tier by level still does its chapter. |
| Chapters done | Journal tags (`*_done`) | Each chapter's text changes once the earlier ones are done. |
| Play style | The four signals above | The largest count wins. A tie gets the Ranger's "all of the above" line. |

The key rule: a kid's path never sends them to a shared hub or to a stranger's world. Every goal sits in the player's own group.

## 5. Sample scenes

Each is a short NPC conversation (BetonQuest `tellraw` options), with the player's choices. Kid lines are 12 words or fewer.

**Young kid (ninos_survival, Wind-Signs):**
- Juniper: "Howdy, wrangler! A sign fell over. Want to stand it up?"
- Choices: "Yes, please!" or "Maybe later."
- Player: "Yes, please!"
- Juniper: "Thanks! Bring four berries for the foxes too, if you like."

**Young kid (ninos_magic, Ten Winds, level 10 ring):**
- Wind-ring book: "Ember sleeps here. It likes a friend with a flame on its back."
- Choices: "Wake it." or "Later, maybe."
- Player: "Wake it." The light and the wind show only to this player. The journal gets: "Ember is awake."
- Below level 10: "The ring is quiet. Ember wants a critter that has grown a little."

**Builder (survival, Wind-Signs):**
- Juniper: "This post needs a sturdy base and a good top. Your call!"
- Choices: "Stone base!" or "Wood base, it's cozy."
- Either choice goes on. Stone gets "Solid as a boulder." Wood gets "Warm as a hearth."

**Fighter (survival, Holding the Line, adult):**
- Juniper: "The night gets noisy. Our watch needs help holding the line."
- Choices: "I'll hold it!" or "Show me the way first."
- Player: "I'll hold it!"
- Juniper: "Fifteen will do it. Stay near your critter and keep your wits."

**Explorer (survival, The Far Meadow, adult):**
- Juniper: "Four landmarks show the way to the meadow. Want the trail book?"
- Choices: "Give me the book!" or "I'll find my own way."
- Player: "I'll find my own way."
- Juniper: "Good! Look for a hill, a lake, a river and flowers."

**Adult in a magic world (magic, The Ten Winds):**
- Wind-ring book: "Ember sleeps here. It wakes for a critter with a flame at its back."
- Choices: "Wake it." or "Check the other rings first."
- Player: "Wake it." At level 10 the gate opens and the journal gets: "Ember is awake. Marigold is next."

## 6. D284 compliance

Nobody types a command in any part of this arc. Every step happens by:
- talking to the Ranger (Citizens NPC conversation, clickable options);
- reading the journal book, which BetonQuest hands out when a chapter starts;
- standing in a wind-ring or at a landmark (BetonQuest location conditions and objectives);
- ringing the dinnerbell.

Commands stay admin-only. The existing `/mobsquad unlock` and `page` lines run from BetonQuest's action system as console commands, never typed by a player.

**Unverified, to check on the 3.3-DEV build:**
- BetonQuest conversations work by click only. Nothing in the arc asks a player to type.
- Typed text never goes to public chat.
- The MobSquad menu offers the dinnerbell without a command. The arc depends on this, so it's unverified until a bot test shows it.

## 7. Kids' safety notes for the Trust and Safety review

- **No chat-based goals with other players.** No goal asks a player to say anything, and none asks them to find or meet a player.
- **No goals that need visiting other groups' worlds.** Each kid's path stays in `ninos_survival`, `ninos_magic` and their own meadow. The adult `magic` world is never a required step for a kid.
- **Nothing reveals other players' names or places.** The meadow sign says "the wrangler". Books carry no coordinates and no names. The journal shows only the player's own progress. MobSquad's placeholders name no one and give no place.
- **Fixed spots and privacy.** Rings, posts and landmarks are fixed and known, so they can draw kids together with coated critters. Their effects go only to the player who triggers them.
- **Glow in `ninos_magic`.** The 2026-11-22 decision is "same look everywhere". The outline shows through walls to anyone tracking a critter, and the new fixed rings mean a kid's coated critter can lead someone to a known spot. The recommendation is no-glow in `ninos_magic`. The rings don't need the glow, so either choice works.
- **Kid-safe wording:** only the game's own mobs are scary, and nothing teases a player about their choices.

## 8. Systems

**Pacing against the coats.** A coat comes every 10 levels. Level n needs 10 × (n − 1) experience squared, read here as the total to reach that level (to confirm with the plugins lead):

| Level (coat) | Total experience |
| --- | --- |
| 10 (Ember) | 810 |
| 20 (Marigold) | 3,610 |
| 25 (Yonder tier) | 5,760 |
| 30 (Sunbeam) | 8,410 |
| 40 (Backcountry tier) | 15,210 |
| 100 (Starlight) | 98,010 |

A critter earns 2 experience a minute out, plus its kills (a zombie gives 20). Level 10 takes about 40 zombie kills, or about 7 hours out. Coat 1 is a couple of good evenings. Coat 10 is months, so it stays a long-term goal.

**Chapter length.** Each old chapter takes about 10 to 15 minutes; each new one about 15 to 25. The whole arc is about 2 hours for a new wrangler.

**Rewards next to what MobSquad already gives.** MobSquad gives levels, tiers, critters, pages and coats. The arc adds story pages, journal lines and the Ranger's books. It adds no levels, no items with power, no early coats and nothing with trade value.

**Build estimate.**
- **BetonQuest only:** conversations, journal, objectives, conditions and chapter tags. A few days to write and test, plus an admin building the rings, posts and landmarks in each world.
- **With one small hook:** a `%mobsquad_style%`-type placeholder for a clean path choice. About a day or two of plugin work and a test pass.
- **To verify on the 3.3-DEV build:** the BetonQuest conditions for world, the LuckPerms group and kill objectives by mob type; whether a progress objective can count placed blocks; and whether AuraSkills' placeholders are readable. If one is missing, the hook above covers it.

## 9. Open questions for the owner

Each goes in its own question dialog, recommended option first.

1. **Path choice:** how is a player's path picked?
   - Read the largest signal automatically (Recommended): the story adapts on its own.
   - The Ranger asks once, then follows the answer.
   - Same beats for everyone, with only the words changed.
2. **Kids' rewards:** are the rewards the same for kids and adults?
   - The same books and pages (Recommended): nothing gives a kid an edge.
   - Kids get a plainer book with the same reward.
3. **Glow in `ninos_magic`:** fixed rings draw kids with coated critters to known spots. Should the glow stay there?
   - Add `ninos_magic` to `spirit-coats.no-glow-worlds` (Recommended, kids first): a one-line config change. The rings don't need the glow.
   - Keep the glow as decided on 2026-11-22. The rings still work, because their gates read what a player owns, not what is out.
4. **When the arc ships:** with the `either` tier default, or as a separate go?
   - Together with the `either` default (Recommended): one owner step.
   - Separately, after a real kid has played Holler through Backcountry.

## Safety review changes

- Kid or adult fails closed. Unknown, empty or unreadable gets the kid version. A player with no group gets the kid lines (a test).
- Posts, landmarks and the meadow are shared spots, claim-protected. Rebuilt signs and visits are per player only.
- The dinnerbell sends home only the ringer's own critters. Its availability without a command is unverified.
- Ring lighting and wind effects go only to the triggering player. Nothing is broadcast.
- Chapter 6 for kids: any time of day, 8 hostile kills, optional, gentler lines. Counted by hostile mob type with a kill objective, not the vanilla total. The adult 15 is used everywhere.
- Glow question re-asked with the new fact (fixed rings draw kids to known spots). Recommendation: no-glow in `ninos_magic`.
- Books are by Ranger Juniper, name no player, carry no coordinates, are not map items, and have no trade value.
- Kids' lines are 12 words or fewer everywhere, with kid ring lines added in chapter 5.
- Unverified: BetonQuest conversations work by click only; typed text never reaches public chat; the dinnerbell works from the menu without a command.
- D287 reference fixed to `ops/decisions/minecraft-plugins.md`.
