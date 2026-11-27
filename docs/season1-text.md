# Season 1 text, for the owner's approval

Every player-facing line that 0.13.0 adds. No existing line changed its words. None of these is approved yet. Everyone sees the same words, kids and adults, so each is kid wording: no sentence is over 12 words, and nobody types anything.

## Ranger Juniper

| When | Juniper says | The player can click |
| --- | --- | --- |
| Backcountry is done and the finale isn't started | You did all three! Ready for the Bunkhouse Round-up? | Yes, please! / Maybe later. |
| After yes | Coax a new critter. Then bring it to me. | |
| Started, nothing coaxed yet | Coax a new critter. Your journal says what's next. | Thanks, Ranger! |
| Coaxed, not sent out yet | Now send your new critter out from a Bunkhouse. | Thanks, Ranger! |
| Sent out before, not out now | Send your new critter out again. Then come back. | Thanks, Ranger! |
| The new critter is out with them | You brought it! The first wind is lit! | |
| Season 1 is done and nothing else is open | Season 1 is done! More adventures are coming soon. | |

"Yes, please!", "Maybe later." and "Thanks, Ranger!" are the same words as before. "Maybe later." here ends in the goodbye line that exists today.

## Chat lines

| When | Line |
| --- | --- |
| Holler is finished (after its own "Chapter done!" line) | You kept the fox's message. Here are two vittles. |
| Yonder is finished | You kept the signal fire. Here are two vittles. |
| Backcountry is finished | You kept the gate-guard's poppy. Here are two vittles. |
| The finale starts | Step 1 of 3. Coax a new critter. |
| A new critter is coaxed | Step 2 of 3. Send it out from a Bunkhouse. |
| It's sent out from a Bunkhouse | Step 3 of 3. Bring your new critter to Juniper. |
| 15 minutes on step 1 or step 2 | Juniper left a tip in your journal. |
| The finale is finished | Season 1 is done! A mail fox waits in your Bunkhouse. |
| Right after that | You lit the first wind. Here are two vittles. |

## Quest journal

| When | Entry |
| --- | --- |
| Holler is finished | Keepsake from the Holler. The fox's message. |
| Yonder is finished | Keepsake from Yonder. The signal fire. |
| Backcountry is finished | Keepsake from the Backcountry. The gate-guard's poppy. |
| Step 1 | The Round-up begins. I'll coax a new critter. |
| Tip on step 1 | Juniper's tip. Chickens love seeds. Walk slowly with one. |
| Step 2 | I coaxed one! I'll send it out from a Bunkhouse. |
| Tip on step 2 | Juniper's tip. A Bunkhouse is a vault. Right-click it. |
| Step 3 | My new critter is ready. I'll bring it to Juniper. |
| The finale is finished | Season 1 is done! A mail-carrier fox joined my Bunkhouse. |
| The finale is finished | Keepsake from the Round-up. The first wind is lit. |

## Things to check while reading

- **The fox arrives as an ordinary fox.** The give command can't name it, so the Bunkhouse menu calls it "Fox". The words call it a mail fox or mail-carrier fox. It's a new level 1 critter, even for a player who already has a fox.
- **"A Bunkhouse is a vault."** The plugin is public, so no line says where a Bunkhouse stands. On a server where each one is beside a gate, a line could say so ("Look by the gate where you arrive.").
- **An old line is now mostly out of reach.** "You've opened the Holler, Yonder and the Backcountry. I'm proud of you, wrangler!" used to follow Backcountry. The finale offer and the "Season 1 is done!" line now come first.
- **Not retroactive.** Someone who finished a chapter before 0.13.0 is offered the finale but gets no keepsake or vittles for the earlier chapter.

## Admin and console lines (not seen by kids)

`The vault at X Y Z in W is a Bunkhouse now.`, `... is already a Bunkhouse.`, `... is a Bunkhouse.`, `... isn't a Bunkhouse.`, `... is a plain vault again.`, `There's no vault at X Y Z in W.` (with `That height is outside the world.` or `That part of the world hasn't been made yet.`), `There's no world called W loaded.` and `[MobSquad] <player>'s Round-up is on.` or `off.`
