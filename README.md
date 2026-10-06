# Command Palette

A keyboard-driven list of commands for RuneLite. Press a hotkey, type a few letters, press `Enter`.

<img src="docs/demo.gif" alt="Typing into the palette: finding a panel, hopping worlds, toggling a plugin, a sum, xp to a level and a wiki search" width="361">

## Using it

Press **Cmd+P** (Mac) or **Ctrl+P** (Windows/Linux) anywhere in the client, including on the login screen and
while a side panel has the keyboard. The hotkey can be changed in the plugin's settings.

<img src="docs/palette.png" alt="The command palette in the client, listing side panels to open" width="341">

| Key | Action |
| --- | --- |
| Type | Filter commands; every word typed must match |
| `Up` / `Down`, `Tab` / `Shift+Tab` | Move the selection |
| `Page Up` / `Page Down` | Move a page at a time |
| `Enter` | Run the selected command |
| `Esc`, the hotkey again, or clicking elsewhere | Cancel |

The last eight commands you ran are listed first, latest first. That can be turned off in the settings, and so
can the icons.

## Commands

| Command | What it does |
| --- | --- |
| *Panel name* - Open panel | Opens that side panel, reopening the sidebar if it is hidden. Listed in the sidebar's order. |
| Close side panel | Closes the panel that is showing |
| Hide sidebar / Show sidebar | Hides or shows the whole sidebar (needs RuneLite's custom title bar, which is on by default) |
| Take screenshot | Saves a screenshot of the game to your screenshots folder (logged in only) |
| Copy world, Copy coordinates | Copies your world number, or your tile as `x, y, plane`, to the clipboard (logged in only) |
| Session summary | Prints how long you have been logged in and the xp gained since, with the top three skills (logged in only) |
| Open my hiscores / Wise Old Man profile / collection log | Opens your page on the official hiscores, Wise Old Man or TempleOSRS in your browser (logged in only) |
| Open my group's hiscores | Opens your group's page on the group ironman hiscores (group ironmen only) |
| Open screenshots / logs / RuneLite folder | Opens that folder in your file manager |
| *Plugin name* - Open settings | Opens that plugin's settings in the Configuration panel. Type `loot settings`. |
| World *number* - Hop | Hops to that world, as the World Hopper plugin does; on the login screen it changes the selected world. Type `world 302`, or part of an activity such as `hop wintertodt`. PvP, high risk, Deadman and beta worlds aren't listed. |
| `35m / 4.2k` | Works out the sum, with `k`, `m` and `b` amounts, `+ - * x /` and brackets. `Enter` copies the answer. Start with `=` for a lone number, as in `=2.5m`. |
| `dry 1/512 600` | Shows the chance of going 600 kills without a 1/512 drop, and the kills by which 50% and 90% of players have it. `Enter` copies the line. The kills can be left out, and `dry 512 600` means the same. |
| `alch rune platebody` or `price ...` | Lists up to five matching tradeable items; running one prints its GE price and high alch value in chat (logged in only) |
| `xp 92 slayer` | Shows the xp you still need for that level, and prints it in chat. Leave the level out for your next one. (logged in only) |
| Help | Shows the keys and the commands that are turned on |
| Search the wiki for "..." | Always listed last once you have typed something; opens the OSRS Wiki's search for it in your browser |
| *Plugin name* - Turn plugin on / off | Starts or stops that plugin |

The words on the right are searchable too: `panel` lists only the panels, `plugin` only the plugins, and
`loot off` finds "Loot Tracker - Turn plugin off". Each group of commands can be turned off in the settings.

Turning a plugin on or off asks for confirmation first; `Enter` confirms and `Esc` cancels. The settings have a
switch for that too.

The **Help** command lists all of this in the client:

<img src="docs/help.png" alt="The help window, listing the keys, the commands and the symbols that narrow the list" width="466">

Start the search with a symbol to narrow the list: `#` for side panels only, `>` for plugins only, `@` for worlds only (`@302`), and `?` to
go straight to the wiki search (`?abyssal whip`).

A plugin that conflicts with one that is currently on isn't offered; turn it on from the Configuration panel,
which asks before turning the other one off.

## Adding a command

A command is a `Command`: a title, a line of detail, an optional 16x16 icon and a `Runnable`, which runs on the
Swing thread after the palette has closed. `SidebarCommands` and `PluginCommands` each add a batch of them to the
list in `CommandPalettePlugin.open()`; a new group goes alongside them, with a setting in `CommandPaletteConfig`.

## How panels are opened

RuneLite's API can open a panel only for the plugin that owns it, so this plugin works on the client window's
Swing components instead (see `Sidebar`): the sidebar is a tabbed pane with a tab per panel, and selecting a tab
is what clicking its icon does. No reflection is involved.

## Development

`./gradlew runClient` starts RuneLite with the plugin loaded. `./gradlew test` also writes a rendering of the
palette to `build/preview.png`, the help window to `build/help.png` and the animation above to
`build/demo.gif`; copy the last two into `docs/` after changing how the palette looks.
