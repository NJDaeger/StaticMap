package com.njdaeger.staticmap;

import org.bukkit.Bukkit;
import org.dynmap.*;
import org.dynmap.hdmap.HDMap;
import org.dynmap.hdmap.HDPerspective;
import org.dynmap.hdmap.IsoHDPerspective;
import org.dynmap.utils.Matrix3D;
import org.dynmap.utils.Vector3D;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class StaticMap extends HDMap {

    private static final BufferedImage TRANSPARENT = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

    //map tiles should be saved in this directory
    //the center tile should be named 0_0.png
    //the tile to the right of the center tile should be named 1_0.png, top right 1_1.png, etc
    private final File tileDirectory;
    private final Map<String, BufferedImage> tiles;

    //for each image, the image is expected to hold a certain number of 128x128 pixel tiles
    //note, when divided by 128, both of these values should return an ODD number, as the center
    //tile in the dynmap should be in the exact center of each tile
    private final int tilesPerSide;
    private final boolean saveRenderCameraLocations;

    public StaticMap(DynmapCore core, ConfigurationNode configuration) {
        super(core, configuration);

        this.tiles = new HashMap<>();

        var actualWorldTileDirectory = configuration.getString("tileDirectory", "");
        this.saveRenderCameraLocations = configuration.getBoolean("saveRenderCameraLocations", true);
        if (actualWorldTileDirectory.trim().isEmpty()) {
            Bukkit.getLogger().severe("No tile directory specified. Please check all maps that use a static map configuration an make sure 'tileDirectory is specified!'");
        }


        this.tileDirectory = new File(StaticMapPlugin.getPlugin(StaticMapPlugin.class).getDataFolder() + File.separator + actualWorldTileDirectory);

        this.tilesPerSide = configuration.getInteger("tilesPerSide", 9);
        if (tilesPerSide % 2 == 0) {
            throw new RuntimeException("The tilesPerSide value must be an odd number. The map this is used on will render as transparent.");
        }

        if (tilesPerSide < 3)
            throw new RuntimeException("The tilesPerSide value must be at least 3. The map this is used on will render as transparent.");

        if (!tileDirectory.exists()) {
            tileDirectory.mkdirs();
            Bukkit.getLogger().warning("The tile directory \"" + tileDirectory.getAbsolutePath() + "\" was now created. It has no tiles- the map this is used on will render as transparent.");
        } else {
            Bukkit.getLogger().info("Loading tiles from " + tileDirectory.getAbsolutePath());
            for (var file : Objects.requireNonNull(tileDirectory.listFiles())) {
                try {
                    var fileName = file.getName();
                    if (!fileName.endsWith(".png")) continue;
                    var split = fileName.split("_");
                    if (split.length != 2) continue;
                    var x = Integer.parseInt(split[0]);
                    var y = Integer.parseInt(split[1].substring(0, split[1].length() - 4));
                    Bukkit.getLogger().info("Loading tile " + x + "_" + y + ".png");
                    var loadedImage = ImageIO.read(file);
                    var intArgbImage = new BufferedImage(loadedImage.getWidth(), loadedImage.getHeight(), BufferedImage.TYPE_INT_ARGB);

                    intArgbImage.getGraphics().drawImage(loadedImage, 0, 0, null);
                    tiles.put(x + "_" + y, intArgbImage);
                } catch (IOException e) {
                    Bukkit.getLogger().warning("An error occurred while loading the tile " + file.getName() + " for the static map. The tile will render as transparent.");
                }
            }
        }
    }

    @Override
    public void addMapTiles(List<MapTile> list, DynmapWorld w, int tx, int ty) {
        MapTile tile = new StaticMapTile(w, getPerspective(), tx, ty, getBoostZoom(), getTileScale());
        list.add(tile);
    }

    @Override
    public MapTile[] getAdjecentTiles(MapTile tile) {
        return getAdjacentTilesOfTile(tile, getPerspective());
    }

    protected static MapTile[] getAdjacentTilesOfTile(MapTile tile, HDPerspective perspective) {
        StaticMapTile staticMapTile = (StaticMapTile) tile;
        DynmapWorld world = staticMapTile.getDynmapWorld();
        int x = staticMapTile.tileOrdinalX();
        int y = staticMapTile.tileOrdinalY();
        int boostZoom = staticMapTile.boostzoom;
        int scale = staticMapTile.tilescale;

        return new MapTile[]{
                new StaticMapTile(world, perspective, x - 1, y - 1, boostZoom, scale),
                new StaticMapTile(world, perspective, x, y - 1, boostZoom, scale),
                new StaticMapTile(world, perspective, x + 1, y - 1, boostZoom, scale),
                new StaticMapTile(world, perspective, x - 1, y, boostZoom, scale),
                new StaticMapTile(world, perspective, x + 1, y, boostZoom, scale),
                new StaticMapTile(world, perspective, x - 1, y + 1, boostZoom, scale),
                new StaticMapTile(world, perspective, x, y + 1, boostZoom, scale),
                new StaticMapTile(world, perspective, x + 1, y + 1, boostZoom, scale)
        };
    }

    @Override
    public List<MapType> getMapsSharingRender(DynmapWorld w) {
        List<MapType> maps = new ArrayList<>();
        for (MapType mt : w.maps) {
            if (mt instanceof StaticMap sm) {
                if (sm.getPerspective() == getPerspective() && sm.getBoostZoom() == getBoostZoom() && sm.getTileScale() == getTileScale()) {
                    maps.add(mt);
                }
            }
        }
        return maps;
    }

    @Override
    public List<String> getMapNamesSharingRender(DynmapWorld w) {
        return getMapsSharingRender(w).stream().map(MapType::getName).toList();
    }

    /**
     * Gets the tile at the given x and y dynmap tile coordinates.
     * @param x The x dynmap tile coordinate.
     * @param y The y dynmap tile coordinate.
     * @return The tile at the given x and y dynmap tile coordinates. Or transparent if no tile was found
     */
    public BufferedImage findTile(int x, int y) {
        int tileX = Math.round((x * 1.0f) / tilesPerSide);
        int tileY = Math.round((y * 1.0f) / tilesPerSide);
        var tileKey = tileX + "_" + tileY;

        var tileImage = tiles.get(tileKey);

        if (tileImage == null) {
            return TRANSPARENT;
        }
        return getSubTile(tileImage, x, y, tilesPerSide);
    }

    /**
     * Gets how many tiles per side of the parent image there are.
     * @return How many tiles per side of the parent image there are.
     */
    public int getTilesPerSide() {
        return tilesPerSide;
    }

    /**
     * Whether to save chunky camera locations rather than render out the dynmap.
     * @return True if chunky camera locations should be saved, false otherwise.
     */
    public boolean isSaveRenderCameraLocations() {
        return saveRenderCameraLocations;
    }

    /**
     * Gets the directory where all the tiles are stored for this map.
     * @return The directory where all the tiles are stored for this map.
     */
    public File getTileDirectory() {
        return tileDirectory;
    }

    /**
     * Given a parent image, find the tile at dynmap tile location x, y.
     * @param largeTile The parent image
     * @param x The x of the tile dynmap is trying to render
     * @param y The y of the tile dynmap is trying to render
     * @param tilesPerSide The number of tiles per side of the parent image
     * @return The tile at the given location as a 128x128 dynmap image
     */
    private static BufferedImage getSubTile(BufferedImage largeTile, int x, int y, int tilesPerSide) {
        int subTileSize = 128;
        int xOffsetMultiplier = calculateOffsetX(x, tilesPerSide);
        int yOffsetMultiplier = calculateOffsetY(y, tilesPerSide);
        int subTileXStart = xOffsetMultiplier * subTileSize;
        int subTileYStart = yOffsetMultiplier * subTileSize;

        return largeTile.getSubimage(subTileXStart, subTileYStart, subTileSize, subTileSize);
    }

    /**
     * Determines where a dynmap tile is within the large parent tile. eg, Given our tilesPerSide is 17, and the X value
     * of the tile dynmap is trying to render is -8, this method will return 0, since images in java are read from top
     * left, this X value indicates that it is in the leftmost side of the parent image.
     *
     * @param input The X of the tile being rendered
     * @param rangeSize How many tiles wide the parent image is
     * @return The offset of the tile within the parent image
     */
    private static int calculateOffsetX(int input, int rangeSize) {
        int floorAOver2 = (int) Math.floor(rangeSize / 2.0);
        int result;

        if (input >= -floorAOver2) {
            result = Math.floorMod(Math.abs(floorAOver2 + input), rangeSize);
        } else {
            result = -Math.floorMod(Math.abs(floorAOver2 - input), rangeSize) + rangeSize - 1;
        }

        return result;
    }

    /**
     * Determines where a dynmap tile is within the large parent tile. eg, Given our tilesPerSide is 17, and the Y value
     * of the tile dynmap is trying to render is 8, this method will return 0, since images in java are read from top
     * left, this Y value indicates that it is in the top row of the parent image.
     *
     * @param input The Y of the tile being rendered
     * @param rangeSize How many tiles wide the parent image is
     * @return The offset of the tile within the parent image
     */
    private static int calculateOffsetY(int input, int rangeSize) {
        int floorAOver2 = (int) Math.floor(rangeSize / 2.0);
        int result;

        if (input >= -floorAOver2) {
            result = -Math.floorMod(Math.abs(floorAOver2 + input), rangeSize) + rangeSize - 1;
        } else {
            result = Math.floorMod(Math.abs(floorAOver2 - input), rangeSize);
        }

        return result;
    }

    /**
     * Gets the chunky camera settings for a given tile
     *
     * @param x The X of the tile being rendered
     * @param y The Y of the tile being rendered
     * @param zoomoutLevels How many zoomout levels the dynmap has
     * @param tilesWide How many tiles wide the parent image is
     * @param perspective The perspective of the map being rendered
     * @return The camera settings to render this tile
     */
    protected static String getCameraSettingsFor(int x, int y, int zoomoutLevels, int tilesWide, IsoHDPerspective perspective) {
        var xPos = x + 0.5;
        var yPos = y + 0.5;
        var inclination = perspective.inclination;
        var azimuth = perspective.azimuth;
        var modelScale = perspective.getModelScale();
        var scale = perspective.getScale();

        var transform = new Matrix3D();
        transform.scale(1.0D / (double) modelScale,
                1.0D / (double) modelScale,
                1.0D / Math.sin(Math.toRadians(inclination)));
        transform.shearZ(0.0D, -Math.tan(Math.toRadians(90.0D - inclination)));
        transform.rotateYZ(-(90.0D - inclination));
        transform.rotateXY(-180.0D + azimuth);
        var coordSwap = new Matrix3D(0.0D, -1.0D, 0.0D, 0.0D, 0.0D, 1.0D, -1.0D, 0.0D, 0.0D);
        transform.multiply(coordSwap);

        Vector3D v = new Vector3D(xPos * (1 << zoomoutLevels) * 64 / scale,
                yPos * (1 << zoomoutLevels) * 64 / scale, 65);
        transform.transform(v);

//        return String.format("""
//                camera {
//                  projection: parallel
//                  position: x:%s y:%s z:%s
//                  view: yaw:%s pitch:%s roll:0
//                  fov: %s
//                  dof: infinity
//                  res: width:%s height:%s
//                  nameThis: %s_%s.png
//                }
//                """, v.x, v.y, v.z, azimuth - 90, inclination - 90, (128 / scale) * tilesWide, 128 * tilesWide, 128 * tilesWide, x, y);

        return String.format("""
                {
                  "width": %d,
                  "height": %d,
                  "camera": {
                    "position": {
                      "x": %f,
                      "y": %f,
                      "z": %f
                    },
                    "orientation": {
                      "roll": 0.0,
                      "pitch": %f,
                      "yaw": %f
                    },
                    "projectionMode": "PARALLEL",
                    "fov": %f,
                    "dof": "Infinity"
                  },
                  "name": "%s_%s.png"
                },
                """, 128 * tilesWide, 128 * tilesWide, v.x, v.y, v.z, inclination - 90, azimuth - 90, (128 / scale) * tilesWide, x, y);
    }
}
