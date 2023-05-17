# StaticMap

StaticMap is a plugin for Bukkit servers that allows static images to be sent to, and saved by, dynmap.

The original use case for this plugin was a want to have a dynmap rendered by chunky, but have the ability to use any chunky version I want- that way I wasnt locked into the chunky version that ChunkyMap used. In addition to that, I didn't want to have my ChunkyMap rendering at all after the initial render (as it was a static map), so this allows me to render all the tiles on my personal PC, and then send the tiles to the server to be saved by dynmap.

## Dynmap Configuration
Your dynmap world configuration should look like this example below:
```yaml
- class: com.njdaeger.staticmap.StaticMap
  name: static
  title: Surface Static
  prefix: static
  perspective: #whatever perspective you want
  tileDirectory: "/v054" 
  tilesPerSide: 33
  saveRenderCameraLocations: false
```

- The class should be set to `com.njdaeger.staticmap.StaticMap`
- `tileDirectory` is where all tiles for this map are pulled from. This is within the StaticMap folder in /plugins. Above example has the tiles stored in `/plugins/StaticMap/v054`
- `tilesPerSide` is the square root of the number of dynmap tiles packed in one image in the tile directory. The above setting has it set to 33. This means that there are 33x33 dynmap tiles (that are 128px x 128px in resolution each) in each image in the tile directory. So one image in the tile directory is 4224px x 4224px in resolution. (NOTE: This __NEEDS__ to be set to an odd number greater than or equal to 3)
- `saveRenderCameraLocations` is whether or not to save the camera locations of the renders to a cameras.txt file in the specified tileDirectory. This is designed to output camera positions for chunky renders. If not defined, it is true by default. Nothing will render if this is true.

I recommend having `tilesPerSide` set to something high like 33 or higher. Higher numbers mean less renders you have to do manually, as more area is covered per large tile.

## Usage

If you have `saveRenderCameraLocations` enabled, you will want to run `/dynmap fullrender <worldName>:<mapName>`. This will save all the camera locations needed to render your dynmap in chunky to cameras.txt. Once the fullrender is complete, you can set this option to false, which will then allow your map to be rendered (aka pulled from the tiles you rendered locally).

Once you have your camera locations, you can use the information in those to render your map in chunky. See below how to read the printout of the camera locations and how to use that to render in chunky.


### Camera location usage

Your camera locations will appear like this in your cameras.txt file:
```
camera {
  projection: parallel
  position: x:73.95156239096093 y:65.00000000000003 z:62.637853891976164
  view: yaw:135.0 pitch:-60.0 roll:0
  fov: 1040.0
  dof infinity
  res: width:8320 height:8320
  nameThis: 0_0.png
}
```
