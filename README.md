# SC Client mod

SC Client's in-game Fabric mod. The [SC Client launcher](https://github.com/baksyaaaaa/SCClient) downloads it by itself when you press Play.

- **Cosmetics:** the hats, capes, wings and cape-patterned elytras picked in the launcher's Wardrobe, on you and on other SC Client players. Picks changed in the launcher show up in game within 2 seconds.
- **Pay timer:** counts down and watches chat for payments ("Steve paid you $500"), keeping the biggest one. Any "outbid" line puts the clock back to 10 seconds.
  - **K** starts / stops it (change the key in Controls).
  - `/paytimer start [5m]`, `/paytimer stop`, `/paytimer clear`, `/paytimer length 1m30s`
  - Different payment wording on your server: put a pattern in `config/sc-mod.json` as `payRegex` with the named groups `amount` (and optionally `player`).

Built by GitHub Actions for Minecraft 1.21.11 and 26.x; the jars are on the [latest release](https://github.com/baksyaaaaa/SC-Mod/releases/latest).

<sub>Licensed under GPL-3.0.</sub>
