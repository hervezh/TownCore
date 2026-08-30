# Town Core

Town Core is a feature-rich Minecraft town and war plugin for Paper-based servers. It gives players a complete territory-management system: founding towns, claiming chunks, managing bank funds, leveling up town skills, building diplomatic relationships, and defending a town core during siege events.

The project is designed to feel like a full server-side gameplay loop rather than a simple placeholder plugin. It combines town ownership, economy integration, progression systems, GUI menus, war tracking, and admin controls into a single cohesive experience.

## What the plugin offers

### 1. Town creation and founding

Players can create a town by placing a custom Town Core item, which acts as the heart of the town.

- The town core is a crafted custom item.
- Town creation begins when a player places the core.
- The core must survive a countdown window before the town is fully established.
- If the core is destroyed during the founding phase, the town creation is cancelled.
- Once established, the Town Core becomes the home anchor for the town.

This gives the plugin a strong “base defense” mechanic and creates a clear strategic objective during early gameplay.

### 2. Land claims and protected territory

The plugin manages town territory through chunk claims.

- Players claim connected chunks to expand their town.
- Claims are tracked by town and stored in the plugin data system.
- Towns can set a home spawn, control their land, and prevent outsiders from interfering with protected areas.
- Town owners and assistants can claim, unclaim, and inspect claim state.
- Claim previews and border visual tools help players understand their territory boundaries.

This creates a stable economy around town expansion and protects town infrastructure from griefing or accidental destruction.

### 3. Town roles and member management

Town governance uses a structured role system:

- Ruler: town owner, highest authority
- Assistant: trusted co-leader or management role
- Member: regular participant

Additional features include:

- Invite and accept/deny member flow
- Kicking members
- Title assignments for players
- Public join settings and town logs
- Town member list and online status checks

This is important for both balance and administration on a multiplayer server.

### 4. Economy, bank, and upkeep

The plugin is built to work with Vault and supports an economy layer.

- Towns have a bank balance
- Players can deposit or withdraw money
- Upkeep is charged daily
- Towns can be warned when bank value is low
- Income systems allow towns to generate resources passively through progression tasks

The economy layer makes town management feel meaningful and gives players a reason to invest in their settlement over time.

### 5. Town progression and task-based leveling

Each town has progression tasks such as:

- Farming
- Cooking
- Mining
- Woodcutting
- PvP
- PvE
- Building
- Fishing
- Smithing
- Enchanting

The plugin awards XP for these activities and increases town task levels over time. Town level is then calculated from those task levels.

Benefits of progression include:

- More claim capacity
- More upgrade tokens
- Unlockable economic benefits
- Better town power and long-term growth

This is one of the main features that makes the plugin feel like a progression system instead of a static protection system.

### 6. Upgrade system and passive buffs

Town upgrades provide passive perks that players receive while inside their own town territory.

Examples include:

- Path speed boost
- No hunger penalty
- Crop growth acceleration
- Builder’s haste effect
- Night vision
- Water breathing
- Reduced fall damage
- Regeneration at low health
- Fireproof protections
- Radar upgrades
- Reinforced core during wars

Upgrades are granted by tokens and are relevant to both survival and combat play.

### 7. War system and diplomacy

The plugin includes a complete diplomacy and war model.

Players can:

- Ally with other towns
- Remove allies
- Mark enemies
- Declare war
- View relationship states
- Track hostile relationships
- Surrender or terminate war states

War mechanics include:

- Peace shield durations
- Active war sessions
- Defender Town Core health tracking
- Siege-style attack objectives
- Occupation tracking
- War broadcast messages
- Core repair and restore logic after war resolution

This creates a meaningful PvP and territorial conflict layer for servers that want competitive town gameplay.

### 8. Town chat, ally chat, and role messaging

The plugin supports different communication styles for towns:

- Town chat
- Ally chat
- Global and staff-oriented admin tools
- Player title display in notifications
- Town-based login/logout announcements

These systems keep communication organized without making the plugin feel chaotic or hard to manage.

### 9. GUIs and menus

The plugin includes a number of custom GUI systems to make interactions easier for players.

Included menu systems cover:

- Main town menu
- Staff panel
- Bank menu
- Settings menu
- Income menu
- Upgrades menu
- Town info and list screens
- Top town rankings

This improves usability and reduces the need for players to memorize long command syntax.

### 10. Admin and server tools

The plugin includes a wide range of administrative controls for staff or server operators.

Examples include:

- Admin-town claim management
- Admin money modification
- Admin war triggering
- Manual war end/termination
- Backup and restore
- Rollback tools
- Town renaming approval workflow
- Logging and audit trail for town actions

This makes it suitable for larger or more active communities that need moderation tools.

### 11. Placeholder API integration

The plugin registers PlaceholderAPI placeholders so server owners can display town data in scoreboards, tab lists, and other UI surfaces.

At minimum, the project supports a custom placeholder expansion for town name, town role, title, and related status values.

### 12. Town plots and custom property management

The system also supports plot-style subdivision and ownership records.

Features include:

- Chunk subdivision
- Plot pricing and ownership
- Plot buying and selling logic
- Plot information and clear/reset tools

This adds another layer of flexible property management beyond standard chunk claims.

### 13. Backup, save, and restore support

The project automatically saves town state and supports backup creation.

- Auto-save runs periodically
- Backup snapshots can be created
- Legacy data migration is supported
- Town data can be restored from stored backups

This helps maintain integrity over long-running server sessions.

## Core gameplay loop

The intended player experience is:

1. Craft or obtain a Town Core item.
2. Place it to begin town creation.
3. Claim nearby chunks to grow the settlement.
4. Build a town bank and maintain resources.
5. Gain town XP through activities and upgrades.
6. Manage members, permissions, and public access.
7. Build defensive structure around the core.
8. Form alliances or go to war with rival towns.
9. Upgrade the town to unlock new buffs and economic strength.

## Command overview

The plugin exposes a broad command set through the `/town` command, including:

- `/town info`
- `/town core`
- `/town spawn`
- `/town setspawn`
- `/town bank`
- `/town deposit`
- `/town withdraw`
- `/town claim`
- `/town unclaim`
- `/town claims`
- `/town members`
- `/town invite`
- `/town ally`
- `/town enemy`
- `/town declarewar`
- `/town warinfo`
- `/town list`
- `/town help`
- `/town upgrades`
- `/town rename`
- `/town backup`
- `/town restore`

Staff/admin commands extend this with town moderation, war control, rollback, and data management tools.

## Tech stack

- Java 21
- Paper API (1.21.x)
- Vault economy integration
- PlaceholderAPI support
- Bukkit scheduler, listeners, and custom recipes
- YAML-based persistent storage

## Installation

1. Build the plugin with Maven:

   ```bash
   cd "Town Core"
   mvn clean package
   ```

2. Place the generated JAR in your Paper server’s `plugins` folder.
3. Ensure Vault and PlaceholderAPI are installed if you want economy and placeholder features enabled.
4. Restart the server.

## Recommended server use cases

This plugin is a strong fit for:

- Faction-style roleplay servers
- Town-building survival servers
- Economy-driven community servers
- PvP or siege-heavy Minecraft communities
- Servers that want a deeper settlement progression system than basic claiming tools

## Summary

Town Core is a full-featured town management and warfare plugin with strong progression, economy, diplomacy, and defense gameplay. It is built around the concept of a town core, territorial claims, member governance, and strategic wars, and it gives servers the tooling needed for a deeper and more engaging settlement experience.

---

If you want, this project can also be expanded with a dedicated command reference page, example screenshots, or a feature roadmap for future releases.
