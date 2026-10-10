# Command Palette

A keyboard-driven list of commands for RuneLite. Press a hotkey, type a few letters, press `Enter`.

<img src="docs/demo.gif" alt="Typing into the palette: finding a panel, hopping worlds, a plugin's settings, a sum, the tax on a sale, a timer, xp to a level and a wiki search" width="361">

## Using it

Press **Cmd+P** (Mac) or **Ctrl+P** (Windows/Linux) anywhere in the client, including on the login screen and
while a side panel has the keyboard.

The plugin's settings have the key, **Open hotkey**, and whether Cmd or Ctrl has to be held with it, **Require
Ctrl/Cmd**. Only the key of the hotkey is used, so the same settings work on a Mac and on Windows. If the
Platform Keys plugin is on, its key profile decides which of Cmd and Ctrl it is. With Require Ctrl/Cmd off the
key opens the palette on its own, except while you are typing in a text field.

<img src="docs/palette.png" alt="The command palette in the client, listing side panels to open" width="341">

| Key | Action |
| --- | --- |
| Type | Filter commands; every word typed must match |
| `Up` / `Down`, `Tab` / `Shift+Tab` | Move the selection |
| `Page Up` / `Page Down` | Move a page at a time |
| `Enter` | Run the selected command |
| `Esc`, the hotkey again, or clicking elsewhere | Cancel |

The last eight commands you ran are listed first, latest first, in a section of their own: a line sets them
apart from the rest and each has a clock at the end of its row. **Recently used count** in the settings changes
how many, from 1 to 20. The section can be turned off there, and so can the icons. Once you type, the best
matches come first whichever section they were in, and the recently used ones keep their clock.

## Commands

Each group below has a switch in the plugin's settings. All of them start on except **Plugins on/off**.

### Side panels

| Command | What it does |
| --- | --- |
| *Panel name* - Open panel | Opens that side panel, reopening the sidebar if it is hidden. Listed in the sidebar's order. |
| Close side panel | Closes the panel that is showing |
| Hide sidebar / Show sidebar | Hides or shows the whole sidebar (needs RuneLite's custom title bar, which is on by default) |

### Client utilities

| Command | What it does |
| --- | --- |
| Take screenshot | Saves a screenshot of the game to your screenshots folder (logged in only) |
| Copy world, Copy coordinates | Copies your world number, or your tile as `x, y, plane`, to the clipboard (logged in only) |
| Session summary | Prints how long you have been logged in and the xp gained since, with the top three skills (logged in only) |
| Open my hiscores / Wise Old Man profile / collection log | Opens your page on the official hiscores, Wise Old Man or TempleOSRS in your browser (logged in only) |
| Open my group's hiscores | Opens your group's page on the group ironman hiscores (group ironmen only) |
| Open screenshots / logs / RuneLite folder | Opens that folder in your file manager |

### Lookups and sums

These are made from what you type, so they aren't in the list until you do.

| Type | What it does |
| --- | --- |
| `35m / 4.2k` | Works out the sum, with `k`, `m` and `b` amounts, `+ - * x /` and brackets. `Enter` copies the answer. Start with `=` for a lone number, as in `=2.5m`. |
| `dry 1/512 600` | Shows the chance of going 600 kills without a 1/512 drop, and the kills by which 50% and 90% of players have it. `Enter` copies the line. The kills can be left out, and `dry 512 600` means the same. |
| `tax 12m` | Shows the Grand Exchange's tax on selling one item at that price, 2% rounded down and at most 5m, and what is left. `Enter` copies the line. The price can be a sum, as in `tax 1.2b / 3`. The few items the Grand Exchange doesn't tax aren't known about. |
| `ticks 250` or `ticks 2m30s` | Converts game ticks of 0.6 seconds to time, or time to ticks. `Enter` copies the line. Time is written `90s`, `2m30s`, `1h 5m` or `1:30`. |
| `alch rune platebody` or `price ...` | Lists up to five matching tradeable items; running one prints its GE price and high alch value in chat (logged in only) |
| `xp 92 slayer` | Shows the xp you still need for that level, and prints it in chat. Leave the level out for your next one. (logged in only) |

### Timers

| Type | What it does |
| --- | --- |
| `timer 5m herb run` | Starts a timer that says "Timer done: herb run" in chat when it runs out, and sends a RuneLite notification. The words after the time can be left out, and a number on its own is in minutes. |
| Cancel 5m timer: herb run | Listed while that timer is running, with the time left |

Timers go up to a day and are lost when the client closes. RuneLite's notifications are, unless you have changed
its settings, only sent while the client isn't the window in use; the chat message is there either way when you
are logged in.

### World hopping

| Command | What it does |
| --- | --- |
| World *number* - Hop | Hops to that world, as the World Hopper plugin does; on the login screen it changes the selected world. Type `world 302`, or part of an activity such as `hop wintertodt`. |

The worlds you have made favourites in the World Hopper plugin's panel come first, with a star, and `favourite`
finds just those. PvP, high risk, Deadman and beta worlds aren't listed.

### Plugins

| Command | What it does |
| --- | --- |
| *Plugin name* - Open settings | Opens that plugin's settings in the Configuration panel. Type `loot settings`. |
| *Plugin name* - Turn plugin on / off | Starts or stops that plugin. Off until **Plugins on/off** is turned on in the settings. |

Turning a plugin on or off asks for confirmation first; `Enter` confirms and `Esc` cancels. **Confirm plugins
on/off** in the settings turns that off. A plugin that conflicts with one that is currently on isn't offered;
turn it on from the Configuration panel, which asks before turning the other one off.

### Wiki search and help

| Command | What it does |
| --- | --- |
| Search the wiki for "..." | Always listed last once you have typed something; opens the OSRS Wiki's search for it in your browser |
| Help | Shows the commands that are turned on, with an example of each. The **Help** section of the settings points to it. |

<img src="docs/help.png" alt="The help window, listing the commands and the symbols that narrow the list" width="467">

## Finding a command

The words on the right of each row are searchable too: `panel` lists only the panels, and `loot settings` finds
"Loot Tracker - Open settings".

Start the search with a symbol to narrow the list:

| Symbol | Lists only |
| --- | --- |
| `#` | Side panels |
| `>` | Plugins |
| `@` | Worlds, as in `@302` |
| `?` | The wiki search, as in `?abyssal whip` |

## Adding a command

A command is a `Command`: a title, a line of detail, an optional 16x16 icon and a `Runnable`, which runs on the
Swing thread after the palette has closed. `SidebarCommands` and `PluginCommands` each add a batch of them to the
list in `CommandPalettePlugin.open()`; a new group goes alongside them, with a setting in `CommandPaletteConfig`.
A command made from what is typed, like `tax 12m`, goes in `TypedCommands.forQuery()` instead. Either way, add a
row for it to `HelpCommand`.

## How panels are opened

RuneLite's API can open a panel only for the plugin that owns it, so this plugin works on the client window's
Swing components instead (see `Sidebar`): the sidebar is a tabbed pane with a tab per panel, and selecting a tab
is what clicking its icon does. No reflection is involved.

## Development

`./gradlew runClient` starts RuneLite with the plugin loaded. `./gradlew test` also writes a rendering of the
palette to `build/preview.png`, the help window to `build/help.png` and the animation above to
`build/demo.gif`; copy the last two into `docs/` after changing how the palette looks.
