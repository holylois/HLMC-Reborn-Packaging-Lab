# Holy Lois: Reborn launcher and add-ons

Launcher 1.6.0 installs and updates the signed Holy Lois pack for Minecraft 26.3. Pick how you play (I own Minecraft, or Play with a name), install, then Play. One button follows the state (Install, Update, Repair, Play), the account card shows your skin head, and the main screen keeps the most used keys and commands in view, with gold kept for the main action. Built-in pack: 2.2.0 (the launcher still downloads the newest signed pack).

Not an official Minecraft product. Not approved by or associated with Mojang or Microsoft.

[Download the launcher](https://github.com/holylois/HLMC-Reborn-Packaging-Lab/releases/latest/download/HolyLoisReborn.exe). [Player guide](https://github.com/holylois/HLMC-Reborn/blob/main/PLAYER-GUIDE.md). [Admin guide](https://github.com/holylois/HLMC-Reborn/blob/main/ADMIN-GUIDE.md).

While you type a name, the launcher checks it (green free, orange bought account's name, red already playing here). Existing Holy Lois accounts require their server password. Admin-granted rename/recovery forms preserve the verified UUID; they do not grant access to someone else's account. Old names remain protected. /logout and account changes wait for combat to end.

R over an item shows its recipe; R over an empty slot sorts without opening a recipe as the slot fills. Matching stones can merge after chest acquisition; rolled variants keep their own components.

## Build

Use the .NET10 SDK on Windows and run `./build.ps1 -Version 1.6.0`. Add `-Package` for the release EXE. Sources under `addons/` target the pinned Fabric26.3 APIs; their server build scripts compile against the installed libraries and run the relevant checks. Do not ship private preview/probe mods.

For a quiet pack patch, append `--silent` to the publisher's `prepare-pack` command. It signs the new version while
retaining the previous public history. Omit the flag for an announced release. The launcher derives the quiet policy
from that signed history, so older launchers keep reading the same schema. The Update button still follows the pack
version; a quiet patch does not highlight the previous announcement as new. Mutable pack/app metadata requests use
fresh paired identities and no-cache headers; verified immutable downloads retain their normal cache.

## Release integrity

The app/catalog and pack manifests use independent RSA-PSS verification keys. Private signing keys, worlds, account data, tokens and personal settings never belong in this repository. Keep signed `assets/` bytes exact; run `scripts/check_public_data.py` before publishing. Updates preserve personal settings/keybinds and use the reviewed pack defaults.

The Windows app has no Authenticode certificate; SmartScreen may show Unknown publisher. A local antivirus result is not a guarantee for every computer.

## Later work

Slipped items (/played, party HUD default, login before the world and more) are tracked for the next batch. Release history: [CHANGELOG](https://github.com/holylois/HLMC-Reborn/blob/main/CHANGELOG.md).
