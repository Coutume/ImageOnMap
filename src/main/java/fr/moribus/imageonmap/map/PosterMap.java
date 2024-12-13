/*
 * Copyright or © or Copr. Moribus (2013)
 * Copyright or © or Copr. ProkopyL <prokopylmc@gmail.com> (2015)
 * Copyright or © or Copr. Amaury Carrade <amaury@carrade.eu> (2016 – 2022)
 * Copyright or © or Copr. Vlammar <anais.jabre@gmail.com> (2019 – 2024)
 *
 * This software is a computer program whose purpose is to allow insertion of
 * custom images in a Minecraft world.
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package fr.moribus.imageonmap.map;

import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.world.WorldUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

public class PosterMap extends ImagePoster {
    protected final int[] postersIDs;
    protected final int columnCount;
    protected final int rowCount;

    public PosterMap(UUID userUUID, int[] postersIDs, String id, String name, int columnCount, int rowCount) {
        super(userUUID, Type.POSTER, id, name);
        this.postersIDs = postersIDs;
        this.columnCount = Math.max(columnCount, 0);
        this.rowCount = Math.max(rowCount, 0);
    }

    public PosterMap(UUID userUUID, int[] postersIDs, int columnCount, int rowCount) {
        this(userUUID, postersIDs, null, null, columnCount, rowCount);
    }

    public PosterMap(Map<String, Object> map, UUID userUUID) throws InvalidConfigurationException {
        super(map, userUUID, Type.POSTER);

        columnCount = getFieldValue(map, "columns");
        rowCount = getFieldValue(map, "rows");

        List<Integer> idList = getFieldValue(map, "postersIDs");
        postersIDs = new int[idList.size()];
        for (int i = 0, c = idList.size(); i < c; i++) {
            postersIDs[i] = idList.get(i);
        }
    }

    @Override
    public int[] getPostersIDs() {
        return postersIDs;
    }

    @Override
    public int getFirstPosterID() {
        int first = -1;
        for (int id : postersIDs) {
            if (first == -1 || first > id) {
                first = id;
            }
        }
        return first;
    }
    /* ====== Serialization methods ====== */

    @Override
    public boolean managesPoster(int posterID) {
        for (int postersID : postersIDs) {
            if (posterID == postersID) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void postSerialize(Map<String, Object> map) {
        map.put("columns", columnCount);
        map.put("rows", rowCount);
        map.put("postersIDs", postersIDs);
    }

    /* ====== Getters & Setters ====== */

    /**
     * Returns the amount of columns in the poster map
     *
     * @return The number of columns, or 0 if this data is missing
     */
    public int getColumnCount() {
        return columnCount;
    }

    /**
     * Returns the amount of rows in the poster map
     *
     * @return The number of rows, or 0 if this data is missing
     */
    public int getRowCount() {
        return rowCount;
    }

    public int getColumnAt(int i) {
        if (columnCount == 0) {
            return 0;
        }
        return (i % columnCount);
    }

    public int getRowAt(int i) {
        if (columnCount == 0) {
            return 0;
        }
        return (i / columnCount);
    }

    public int getIndexAt(int col, int row) {
        return columnCount * row + col;
    }

    /**
     * Returns the map id at the given column and line.
     *
     * @param x The x coordinate. Starts at 0.
     * @param y The y coordinate. Starts at 0.
     * @return The Minecraft map ID.
     * @throws ArrayIndexOutOfBoundsException if the given coordinates are too big (out of the poster).
     */
    public int getPosterIdAt(int x, int y) {
        return postersIDs[y * columnCount + x];
    }

    public int getPosterIdAt(int index) {
        return postersIDs[index];
    }

    public int getPosterIdAtReverseY(int index) {
        int x = index % (columnCount);
        int y = index / (columnCount);
        return getPosterIdAt(x, rowCount - y - 1);
    }


    public int getPosterIdAtReverseZ(int index, BlockFace orientation, BlockFace bf) {
        //TODO maybe a bug there why don't use orientation?
        int x = 0;
        int y = 0;
        switch (bf) {
            case UP:
                x = index % (columnCount);
                y = index / (columnCount);
                break;
            case DOWN:
                x = (columnCount - 1) - index % (columnCount);
                y = index / (columnCount);
                break;
            default:
        }

        return getPosterIdAt(x, rowCount - y - 1);

    }


    public boolean hasColumnData() {
        return rowCount != 0 && columnCount != 0;
    }

    @Override
    public int getPosterCount() {
        return postersIDs.length;
    }

    public int getIndex(int posterID) {
        for (int i = 0; i < postersIDs.length; i++) {
            if (postersIDs[i] == posterID) {
                return i;
            }
        }
        throw new IllegalArgumentException("Invalid poster ID");
    }

    public int getSortedIndex(int posterID) {
        int[] ids = postersIDs.clone();
        Arrays.sort(ids);
        for (int i : ids) {
            PluginLogger.info("" + i);
        }

        for (int i = 0; i < postersIDs.length; i++) {
            if (ids[i] == posterID) {
                return i;
            }
        }
        throw new IllegalArgumentException("Invalid poster ID");
    }

    public PosterIndexes getIndexes(int posterID) {
        int index = getSortedIndex(posterID);
        PluginLogger.info(rowCount + " " + columnCount + " " + index);
        return new PosterIndexes(index / columnCount, index % columnCount);
    }

    public Location findLocationFirstFrame(ItemFrame frame, Player player) {
        final ImagePoster iposter = PosterManager.getPoster(frame.getItem());
        if (!(iposter instanceof PosterMap)) {
            return null;
        }
        PosterMap poster = (PosterMap) iposter;
        if (!poster.hasColumnData()) {
            return null;
        }
        int posterID = PosterManager.getPosterIDFromItemStack(frame.getItem());

        BlockFace bf = WorldUtils.get4thOrientation(player.getLocation());

        PosterIndexes posterindexes = getIndexes(posterID);
        int row = posterindexes.getRowIndex();
        int column = posterindexes.getColumnIndex();
        Location loc = frame.getLocation();
        PluginLogger.info("\n\nlocalization of the initial clicked frame " + loc);
        PluginLogger.info("row " + row + " col " + column);
        switch (frame.getFacing().getOppositeFace()) {
            case UP:
            case DOWN:
                switch (bf) {
                    case NORTH:
                        loc.add(-row, 0, column);
                        break;
                    case SOUTH:
                        loc.add(row, 0, -column);
                        break;
                    case WEST:
                        loc.add(row, 0, column);
                        break;
                    case EAST:
                        loc.add(-row, 0, -column);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + bf);
                }
                break;

            case EAST:
                loc.add(0, row, -column);
                break;
            case WEST:
                loc.add(0, row, column);
                break;
            case NORTH:
                loc.add(-column, row, 0);
                break;
            case SOUTH:
                loc.add(column, row, 0);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + bf);
        }
        PluginLogger.info("\n\nlocalization of the first frame " + loc);
        return loc;
    }

}
