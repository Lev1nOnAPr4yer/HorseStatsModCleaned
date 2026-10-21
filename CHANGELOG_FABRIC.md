## Version 3.11.0 Fabric

_supports Minecraft 26.3.x_

Changes:
- The message shown when you click a horse now also shows its breeding timer, when there is one: the cooldown ("On
  cooldown 4:59"), the love time ("Ready 0:29") or the time before a foal grows up ("Foal 19:58"). It counts down while
  the message is on screen, and it starts with "~" when it is an estimate (on servers). Nothing is added for a horse
  without a timer. It follows the `displayBreedingStatus` option.

## Version 3.10.0 Fabric

_supports Minecraft 26.3.x_

Changes:
- The breeding status and cooldown now also work on servers, as an **estimate** (the times start with "~"). The game
  doesn't tell clients about the cooldown, but the mod sees what leads to it:
  - Hearts above a horse that is fed: it was off cooldown, and is in love for 30 seconds ("Ready", "In love").
  - Hearts above a horse and a foal appearing next to it: two horses just bred. Both parents get a 5 minute
    cooldown. The partner is found among the adults within 3 blocks, and if it can't be told which one it was, only the
    mother gets the cooldown.
  - It only knows what happened in front of you. A horse that you haven't seen fed or bred is "Unknown", and everything
    is forgotten when you leave the server. The server stops the cooldown of a horse in an unloaded area, and a horse
    that was hurt isn't in love any more, which the mod can't see.
  - In singleplayer and when hosting a LAN world nothing changes: the exact values are still read from the game.
- This fork only supports Fabric now: Forge and NeoForge, their builds and their changelogs are removed.

## Version 3.9.0 Fabric

_supports Minecraft 26.3.x_

Changes:
- New option `displayBreedingStatus` (on by default) to show whether a horse can breed, or how long its breeding
  cooldown lasts, in the **Stats** panel of the horse inventory and, when the statistics above mounts are on, above
  the horses' heads:
  - **Foal**: it can't breed yet, and when it grows up.
  - **On cooldown**: the time left before it can breed again (5 minutes after a foal is born).
  - **Off cooldown**: it can be fed a golden apple or a golden carrot to breed.
  - **In love** and **Ready**: it was fed less than 30 seconds ago, with the time left. "Ready" means nothing is in
    the way; otherwise the panel tells why (not at full health, being ridden).
- The game keeps the cooldown and the love time on the server only. They are exact in singleplayer and when hosting a
  LAN world. On someone else's server they are shown as unknown, while foals and untamed horses are still recognized.

## Version 3.8.0 Fabric

_supports Minecraft 26.3.x_

Changes:
- The horse inventory has its own panels now, next to the inventory window, instead of cramming the stats into its
  title line:
  - A **Stats** panel (on the right) with, for each stat, its value and its percentage at the same time, and the
    overall percentage. It follows the options for colors, decimals and `displayMinMax`. Both panels are shown when
    `displayStatsInInventory` is on; when it is off, the stats only appear in a tooltip over the mount's name.
  - A **Compare** panel (on the left) to find out which two mounts to breed.
- New buttons in the inventory of horses and donkeys: **Add to compare 1**, **Add to compare 2** and **Run compare**.
  The selected mounts are kept when you close the inventory, so the second one can be added from another inventory.
  The result is the chance, for each of the health, the jump height, the speed and the overall percentage, that their
  foal ends up higher than both parents. It is calculated exactly from the game's breeding formula, with the parents'
  base stats (potion effects aren't inherited). Horse with horse, donkey with donkey, and horse with donkey (their foal
  is a mule) are supported; mules can't have foals.
- The panels move to the other side of the inventory, or stack, when the window is too narrow for both. Below about 424
  GUI units of width (a 1280x720 window at GUI scale 4, for example), they overlap the inventory: lower the GUI scale.

## Version 3.7.0 Fabric

_supports Minecraft 26.3.x_

Changes:
- Percentages are now rounded to the nearest value instead of being cut off: 65.6% is displayed as 66% instead of 65%.
  A mount with the minimum or maximum possible stat now shows exactly 0% or 100%.
- New option `percentageWithDecimal` to display the percentages with one decimal (65.6%).
- New options `healthDecimals`, `jumpHeightDecimals` and `speedDecimals` to choose how many decimals are displayed
  for each stat, from 0 to 5. Bred horses have a fractional health, set `healthDecimals` to see it.
- Fixed the jump height: it was up to almost one block too low (3.12 blocks were displayed as 2.84). It is now
  computed the same way the game moves a jumping horse.
- Fixed the minimum jump height of skeleton horses, which have the same 0.4 - 1.0 jump strength range as horses.
- The minimum and maximum speeds now match the game's exact values.
- Removed the player's walking speed, sprinting speed, jump height and health from the inventory tooltips.
- The stats in the horse inventory are now spaced according to their width, so that more decimals don't overlap.

## Version 3.6.0 Fabric

_supports Minecraft 26.3.x_

Tested loader:
- Fabric Loader `0.19.5` (client and dedicated server).

Requirements:
- Fabric Loader `>=0.19.5`
- Fabric API `>=0.162.0+26.3`
- Cloth Config API `>=26.3.159`

Optional:
- Mod Menu `21.0.0`, to edit the configuration from the Mods screen

Changes:
- New option `displayStatsAboveHead` to display the statistics above the mounts' heads, like a name tag:
  - `WHEN_LOOKING`: only above the mount you are looking at, up to 64 blocks away.
  - `ALWAYS`: above every mount within 32 blocks.
  - `DISABLED` (default): never.
- Fixed the configuration screen crashing the game when saving, which prevented any setting from being saved.
- Updated Fabric API, Cloth Config API, and Mod Menu.
