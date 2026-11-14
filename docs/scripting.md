# Scripting (ModJS)

BarelyStorage adds a global `BarelyStorage` to ModJS server scripts when ModJS is installed. Without ModJS nothing is loaded. Client scripts do not get the global, because the network lives on the server.

## Events

`BarelyStorage.on(name, fn)` calls `fn(event)` on the server thread. The event has `type()`, `get(key)`, `string(key)`, `number(key)` and `keys()`. A listener that throws is logged on one line and skipped.

| Event | Keys |
|---|---|
| `rack_online`, `rack_offline` | `dimension`, `x`, `y`, `z` |
| `craft_queued` | `item`, `amount`, `player`, `steps` |
| `craft_failed` | `item`, `amount`, `player`, `message` |
| `craft_halted` | `dimension`, `x`, `y`, `z`, `message` (the rack that runs the job) |
| `pattern_encoded` | `kind` (`crafting` or `processing`), `output`, `outputCount`, `inputs` |
| `items_taken` | `item`, `count`, `player` |

`BarelyStorage.events()` lists the names.

## Reading a network

`BarelyStorage.network(dimension, x, y, z)` returns a read-only snapshot of the network that contains a rack, cable, node or terminal at that position, or `null` when there is none (or the server has not started). Fields: `online()`, `racks()`, `energy()`, `maxEnergy()`, `upkeep()`, `threads()`, `ram()`, `depth()` (-1 means no limit), `used()`, `capacity()`, `jobs()`, `lastError()`, `itemTypes()`, `count(itemId)`, `items()` (item id to amount).

## Rules and settings

- `BarelyStorage.addInsertFilter(fn)`: when `fn(itemId)` returns `false`, no network takes that item.
- `BarelyStorage.settingKeys()`, `setting(key)` and `setSetting(key, value)` read and change the values of `config/barelystorage-common.toml` until the game closes or the file is reloaded (the file is not changed).

## Example

```js
BarelyStorage.on('rack_offline', e => {
  console.log('Rack at ' + e.number('x') + ' ' + e.number('y') + ' ' + e.number('z') + ' lost power');
});

BarelyStorage.on('craft_halted', e => {
  console.log('Craft halted: ' + e.string('message'));
});

BarelyStorage.addInsertFilter(id => id !== 'minecraft:dirt');

BarelyStorage.setSetting('power.base_per_tick', '4');
```

Left for later (needs larger changes): adding or removing drive tiers and upgrade cards from scripts, and creating patterns from scripts.
