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
  cacheSize: 9
```

- The class should be set to `com.njdaeger.staticmap.StaticMap`
- `tileDirectory` is where all tiles for this map are pulled from. This is within the StaticMap folder in /plugins. Above example has the tiles stored in `/plugins/StaticMap/v054`
- `tilesPerSide` is the square root of the number of dynmap tiles packed in one image in the tile directory. The above setting has it set to 33. This means that there are 33x33 dynmap tiles (that are 128px x 128px in resolution each) in each image in the tile directory. So one image in the tile directory is 4224px x 4224px in resolution. (NOTE: This __NEEDS__ to be set to an odd number greater than or equal to 3)
- `saveRenderCameraLocations` is whether or not to save the camera locations of the renders to a cameras.txt file in the specified tileDirectory. This is designed to output camera positions for chunky renders. If not defined, it is true by default. Nothing will render if this is true.
- `cacheSize` is the number of tiles to cache in memory. This is used to speed up the rendering process but can tax the server ram if it is set too high when the tilesPerSide value is also high. General rule of thumb is if your tilesPerSide is less than 9, 16-25 is a good value to set, otherwise, higher numbers I recommend doing about 9. For VERY large images, you can set this to 1, which is the minimum value.
- `disableRendering` setting to stop the task of loading tiles into memory, and will ignore all tile renders for the map. When this is true, all tiles from the larger images are assumed to have been loaded into the dynmap.
I recommend having `tilesPerSide` set to something high like 33 or higher. Higher numbers mean less renders you have to do manually, as more area is covered per large tile.

## Usage

If you have `saveRenderCameraLocations` enabled, you will want to run `/dynmap fullrender <worldName>:<mapName>`. This will save all the camera locations needed to render your dynmap in chunky to cameras.txt. Once the fullrender is complete, you can set this option to false, which will then allow your map to be rendered (aka pulled from the tiles you rendered locally).

Once you have your camera locations, you can use the information in those to render your map in chunky. See below how to read the printout of the camera locations and how to use that to render in chunky.


### Camera location usage

Your camera locations will appear like this in your cameras.txt file:
```json
{
  "width": 1152,
  "height": 1152,
  "camera": {
    "position": {
      "x": 99.99,
      "y": 99.99,
      "z": 99.99
    },
    "orientation": {
      "roll": 0.0,
      "pitch": -60,
      "yaw": 30
    },
    "projectionMode": "PARALLEL",
    "fov": 144,
    "dof": "Infinity"
  },
  "name": "0_0.png"
}
```

The above settings are the settings you will need to set in chunky in order to begin rendering tiles.

In the above case:
* The canvas width and height should be set to 1152px x 1152px.
* The camera position should be set to 99.99, 99.99, 99.99.
* The camera orientation should be set to roll: 0, pitch: -60, yaw: 30.
* The camera projection mode should be set to PARALLEL.
* The camera fov should be set to 144.
* The camera dof should be set to Infinity.

The resulting image should be saved as 0_0.png and then it should be put in the tile directory specified in the world's configuration file above.