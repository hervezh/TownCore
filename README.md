# Town Core

please note: before you use this plugin, please acknowledge that there is still many bugs to it, and yeah that's basically it. thanks for reading and planning to use my plugin!

Town Core is a town-building and war plugin for minecraft servers. It gives players a proper setup for founding towns, claiming land, managing a bank, building up a town economy, and fighting over territory.

Since I'm too lazy to like write a whole new category about the economy, the town plugin only supports the Vault Economy. (and also placeholderapi if anybody cares)

## What the plugin does

- Lets players create a town by placing a custom Town Core
- Tracks chunks and claims for each town
- Lets rulers and assistants manage members, invites, and settings
- Adds a town bank with deposits, withdrawals, and town upkeep
- Gives towns progression through task XP and town levels
- Includes upgrades and passive perks for players inside their own town
- Supports diplomacy between towns through allies, enemies, and wars (still a w.i.p)
- Adds a siege-style war system where the defender core is the main objective (still a w.i.p)
- Includes admin tools for rollback, backups, war management, and moderation (still a w.i.p)
- Works with PlaceholderAPI and Vault when installed

## How to craft a Town Core

The Town Core is a custom recipe added in the plugin. The recipe is:

```text
GGG
GDG
OOO
```

Where:
- G = Gold Block
- D = Diamond
- O = Obsidian

So this is:
- 9 Gold Blocks
- 1 Diamond
- 9 Obsidian

Place the item in a crafting table using that pattern.

## How town founding works

Once you craft the Town Core:

1. Place the Town Core in a valid location.
2. The plugin checks that the placement is legal.
3. The core starts a 5-minute founding countdown.
4. The town is only created if the core remains intact for the full timer.
5. If the core is destroyed during that time, the town creation cancels and the core is lost.

That means the early game is not just about claiming land. It is also about defending the heart of the town.

## Town features

### Claims and territory

Towns can claim connected chunks and build around that territory. This gives each town a protected area to build in, protect, and defend.

### Town management

Each town has a role system:

- Ruler
- Assistant
- Member

The plugin supports invites, promotions, demotions, titles, and town logs.

### Economy and upkeep

Towns have a bank, can collect income, and must pay upkeep over time. This adds a real survival and economy loop to the plugin rather than just static land control.

### Growth and upgrades

Town progression is based on task XP like farming, mining, PvE, PvP, fishing, building, and more. As the town grows, it unlocks new bonus perks and higher-level mechanics.

### Wars and diplomacy

Towns can form alliances, mark enemies, and declare war. War sessions focus on attacking the enemy Town Core and controlling the battle around it.

### Admin tools

There are tools for backups, restoring data, rollback actions, and staff war management if you want to run a larger server.

## Useful commands

Most of the plugin is accessed through `/town`.

Common commands include:

- `/town help`
- `/town claim`
- `/town unclaim`
- `/town bank`
- `/town deposit`
- `/town withdraw`
- `/town invite`
- `/town members`
- `/town ally`
- `/town enemy`
- `/town declarewar`
- `/town info`
- `/town upgrades`
- `/town rename`
- `/town backup`
- `/town restore`

## Installation

1. Build the plugin with Maven:

   ```bash
   cd "Town Core"
   mvn clean package
   ```

2. Put the generated jar in your Paper server’s `plugins` folder.
3. Install Vault if you want the economy systems to work.
4. Install PlaceholderAPI if you want placeholders enabled.
5. Restart the server.

## Best fit

This plugin is a good fit for servers that want:

- town-building gameplay
- territorial control
- economy and progression
- PvP and wars
- a stronger sense of ownership and defense

## Summary

Town Core is meant to feel like a real town system, not just a tiny utility plugin. The goal is simple: players craft a core, found a town, expand their land, manage the economy, recruit members, and defend their base when other towns decide to fight.
