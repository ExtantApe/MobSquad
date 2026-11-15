# Test audit

Kept by this repo's owning session, per `~/Code/CLAUDE.md`, "Test self-audits". Newest first.

## 2026-11-15: 0.1.0

- Promises: README "What it does", CHANGELOG 0.1.0, the brief's safety rules.
- Covered by unit tests (ModelTest): Wranglin' levels, posse size, tier unlocks, the player file round trip, bad lines skipped, a new player empty.
- Covered by bots (test/suite.mjs, 60 checks on 26.1.2 and 26.2, 16 on 26.3): marking Bunkhouses and refusing non-admins; the menu shows only your own critters, gives no vault loot, and its icons can't be taken; the chicken trail starts, uses one seed, coaxes after 10 blocks, saves to the player's file and earns experience; named chickens, putting seeds away, leaving the chicken, running and someone else's GriefPrevention claim all refuse or end it; sending out, posse size at levels 1 and 10, the Yonder tier lock; following and catching up; the menu bell, the dinnerbell and /mobsquad list; creeper and skeleton critters never explode, hurt either player, burn or change blocks; players can't hurt critters; tuckering out drops nothing, saves the rest time and refuses a resting critter; leaving, restarting and kill -9 leave no critter in the world and no copy in the list; a disabled world refuses the Bunkhouse.
- Gaps skipped: block-use checks on 26.3 (bots can't use blocks there; covered on 26.1.2 and 26.2). Breaking a Bunkhouse by hand (vault hardness makes it slow for a bot; the unmark path is the same code as `/mobsquad bunkhouse remove`). Reload of every setting (only the two used are exercised).
- Gaps for later: endermen and iron golems out as critters, when fighting lands in 0.2.0.

## 2026-11-14: first audit (starter kit)

- Promises: none yet.
- Covered: none yet.
- Gaps added: none.
- Gaps skipped (and why): none.
