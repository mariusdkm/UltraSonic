# *¡WIP!* UltraSonic

[![Platform](https://img.shields.io/badge/platform-fabric-yellow)](https://fabricmc.net)
[![Minecraft version](https://img.shields.io/badge/minecraft_version-1.17.1-informational)](https://www.minecraft.net/en-us/article/caves---cliffs--part-i-out-today-java)
[![GitHub issues](https://img.shields.io/github/issues/mariusdkm/UltraSonic)](https://github.com/mariusdkm/UltraSonic/issues)
[![GitHub license](https://img.shields.io/github/license/mariusdkm/UltraSonic)](https://github.com/mariusdkm/UltraSonic/blob/main/LICENSE.md)

UltraSonic is going to be a pathing extension for the macros mod [JsMacros](https://github.com/wagyourtail/JsMacros).  
3d pathing is now possible;

[<img src="https://i.imgur.com/8aLJBvT.mp4" width="50%">](https://i.imgur.com/8aLJBvT.mp4)

![vid](https://user-images.githubusercontent.com/43829743/131727352-fe2afd61-3fbd-45fe-9527-a7145e9df2fb.mp4)

<img src="https://user-images.githubusercontent.com/43829743/131728909-00ed89ea-62a1-41ff-864d-3e390978b53e.mp4"/>

https://user-images.githubusercontent.com/43829743/131728909-00ed89ea-62a1-41ff-864d-3e390978b53e.mp4


## Installation

1. Download and run the [Fabric installer](https://fabricmc.net/use).
   - Click the "vanilla" button, leave the other settings as they are,
     and click "download installer".
   - Note: this step may vary if you aren't using the vanilla launcher
     or an old version of Minecraft.
2. Download the [Fabric API](https://minecraft.curseforge.com/projects/fabric)
   and move it to the mods folder (`.minecraft/mods`).
3. Download the newest [JsMacros release](https://github.com/wagyourtail/JsMacros/releases)
   and move it to the mods folder (`.minecraft/mods`).
4. Download UltraSonic from the [releases page](https://github.com/mariusdkm/UltraSonic/releases)
   and move it to the mods folder (`.minecraft/mods`).

## Releases

None yet... come back in a couple of months if you want a working version.

## Beta builds  [![beta builds badge](https://github.com/mariusdkm/UltraSonic/actions/workflows/beta.yml/badge.svg?branch=main)](https://github.com/mariusdkm/UltraSonic/actions?query=branch%3Amain)

For (possibly unstable) beta builds [click here](https://github.com/mariusdkm/UltraSonic/actions?query=branch%3Amain), go to the most recent action and download the Zip-file at the bottom.  
The use the jar without `dev` or `sources` in the filename your mod.  
(Note: You have to be logged in, to be able to download the Zip-file.)

## Usage

This mod currently adds the `Pathing` object to use in macros.  
It can be used in any language just like for example the `Player` object.

### Example

1st script, pathfind
```js
Player.setDrawPredictions(true);
const goal = Player.rayTraceBlock(20, false).getBlockPos(); 
Chat.log("Goal: " + goal);

if (Pathing.pathTo(goal.getX(), goal.getY(), goal.getZ(), true)) {
    Pathing.visualizePath();
    var inputs = Pathing.getInputs();
    //Player.predictInputs(inputs, true);
    //Player.addInputs(inputs);
    const file = FS.open("inputs.csv");
    file.write("movementForward,movementSideways,yaw,pitch,jumping,sneaking,sprinting\n")
    Chat.log("Saving inputs")
    for (const input of inputs) {  
        file.append(input.toString(false) + "\n")
    }
} else {
    Chat.log("No path found")
}
```

2nd script, execute Inputs
```js
const PlayerInput = Java.type("xyz.wagyourtail.jsmacros.client.api.classes.PlayerInput")

Chat.log("Reading inputs from file")
let csv = FS.open("inputs.csv").read()
let inputs = PlayerInput.fromCsv(csv)
Player.addInputs(inputs)
```

## Contributing and Issues

If you expierence any issues or have a question or a feature request, please open an [issue](https://github.com/mariusdkm/UltraSonic/issues).

Contribution are also very welcome, feel free to open a [pull request](https://github.com/mariusdkm/UltraSonic/pulls).

## FAQ

### Does it work yet?

**NO**

### Why is it called UltraSonic?

It's the opposite of [baritone](https://github.com/cabaletta/baritone).  
And this mod's goal is it to be **Ultra** fast like **Sonic** from the video games.
