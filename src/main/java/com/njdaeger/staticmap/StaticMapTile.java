package com.njdaeger.staticmap;

import org.bukkit.Bukkit;
import org.dynmap.*;
import org.dynmap.hdmap.HDMapTile;
import org.dynmap.hdmap.HDPerspective;
import org.dynmap.hdmap.IsoHDPerspective;
import org.dynmap.storage.MapStorage;
import org.dynmap.storage.MapStorageTile;
import org.dynmap.utils.MapChunkCache;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class StaticMapTile extends HDMapTile {

    public StaticMapTile(DynmapWorld world, HDPerspective perspective, int tx, int ty, int boostzoom, int tilescale) {
        super(world, perspective, tx, ty, boostzoom, tilescale);
    }

    public StaticMapTile(DynmapWorld world, String parm) throws Exception {
        super(world, parm);
    }

    @Override
    public boolean render(MapChunkCache cache, String mapname) {
        final long startTimestamp = System.currentTimeMillis();

        StaticMap map = (StaticMap) world.maps.stream()
                .filter(m -> m instanceof StaticMap
                        && (mapname == null || m.getName().equals(mapname))
                        && ((StaticMap) m).getPerspective() == perspective
                        && ((StaticMap) m).getBoostZoom() == boostzoom)
                .findFirst().orElse(null);
        if (map == null) return false;
        MapTypeState state = world.getMapState(map);
        if (state != null) state.validateTile(tx, ty);

        if (map.isSaveRenderCameraLocations()) {
            var cameraFile = new File(map.getTileDirectory().getAbsolutePath() + "\\cameras.txt");
            if (!cameraFile.exists()) {
                try {
                    cameraFile.createNewFile();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            try {
                if (tx % map.getTilesPerSide() == 0 && ty % map.getTilesPerSide() == 0) {
                    Bukkit.getLogger().info("Writing camera settings to file for " + tx + ", " + ty);
                    Files.write(cameraFile.toPath(), StaticMap.getCameraSettingsFor(tx, ty, map.getMapZoomOutLevels(), map.getTilesPerSide(), (IsoHDPerspective) perspective).getBytes(), StandardOpenOption.APPEND);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return false;
        }

        BufferedImage image = map.findTile(tx, ty);

        MapStorage storage = world.getMapStorage();
        MapStorageTile mtile = storage.getTile(world, map, tx, ty, 0, MapType.ImageVariant.STANDARD);
        MapManager mapMan = MapManager.mapman;

        boolean tileUpdated = false;
        if (mapMan != null) {
            DataBufferInt dataBuffer = (DataBufferInt) image.getRaster().getDataBuffer();
            int[] data = dataBuffer.getData();
            long crc = MapStorage.calculateImageHashCode(data, 0, data.length);
            mtile.getWriteLock();
            try {
                if (!mtile.matchesHashCode(crc)) {
                    mtile.write(crc, image, startTimestamp);
                    mapMan.pushUpdate(getDynmapWorld(), new Client.Tile(mtile.getURI()));
                    tileUpdated = true;
                }
            } finally {
                mtile.releaseWriteLock();
            }
            mapMan.updateStatistics(this, map.getPrefix(), true, true, false);
        }
        return tileUpdated;
    }

    @Override
    public MapTile[] getAdjecentTiles() {
        return StaticMap.getAdjacentTilesOfTile(this, perspective);
    }
}
