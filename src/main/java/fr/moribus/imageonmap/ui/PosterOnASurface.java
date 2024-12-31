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

package fr.moribus.imageonmap.ui;

import fr.moribus.imageonmap.map.ImagePoster;
import fr.moribus.imageonmap.map.PosterManager;
import fr.zcraft.quartzlib.tools.world.FlatLocation;
import fr.zcraft.quartzlib.tools.world.WorldUtils;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PosterOnASurface {
    public FlatLocation loc1;
    public FlatLocation loc2;

    public ItemFrame[] frames;

    public static ItemFrame getEmptyFrameAt(Location location, BlockFace facing) {
        Entity[] entities = location.getChunk().getEntities();

        for (Entity entity : entities) {
            boolean notItemFrame = !(entity instanceof ItemFrame);
            if (notItemFrame || !WorldUtils.blockEquals(location, entity.getLocation())) {
                continue;
            }
            ItemFrame frame = (ItemFrame) entity;
            ItemStack item = frame.getItem();
            if (frame.getFacing() != facing || item.getType() != Material.AIR) {
                continue;
            }
            return frame;
        }

        return null;
    }

    public static ItemFrame getFrameAt(Location location, BlockFace facing) {
        Entity[] entities = location.getChunk().getEntities();

        for (Entity entity : entities) {
            boolean notItemFrame = !(entity instanceof ItemFrame);
            if (notItemFrame || !WorldUtils.blockEquals(location, entity.getLocation())) {
                continue;
            }
            ItemFrame frame = (ItemFrame) entity;
            ItemStack item = frame.getItem();
            if (frame.getFacing() != facing) {
                continue;
            }
            return frame;
        }

        return null;
    }

    public boolean isValid(Player p) {
        ItemFrame curFrame;


        FlatLocation l = loc1.clone();

        BlockFace bf = WorldUtils.get4thOrientation(p.getLocation());

        l.subtract(loc2);


        int distX = Math.abs(l.getBlockX());
        int distZ = Math.abs(l.getBlockZ());

        frames = new ItemFrame[distX * distZ];
        l = loc1.clone();
        for (int x = 0; x < distX; x++) {
            for (int z = 0; z < distZ; z++) {

                curFrame = getEmptyFrameAt(l, l.getFacing());

                if (curFrame == null) {
                    return false;
                }

                frames[z * distX + x] = curFrame;

                switch (bf) {
                    case NORTH:
                    case SOUTH:
                        l.addH(0, 1, bf);
                        break;
                    case EAST:
                    case WEST:
                        l.addH(1, 0, bf);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + bf);
                }

            }

            switch (bf) {
                case NORTH:
                case SOUTH:
                    l.addH(1, -distZ, bf);
                    break;
                case EAST:
                case WEST:
                    l.addH(-distZ, 1, bf);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + bf);
            }
        }
        return true;
    }

    public static Map<Location, ItemFrame> getItemFramesLocation(Player p, Location startingLocation,
                                                                 Location selectedLocation, BlockFace facing,
                                                                 int rows,
                                                                 int columns) {
        Map<Location, ItemFrame> itemFramesLocationMap = new HashMap();
        BlockFace bf = WorldUtils.get4thOrientation(p.getLocation());
        boolean isWall =
                facing.equals(BlockFace.WEST) || facing.equals(BlockFace.EAST) || facing.equals(BlockFace.NORTH)
                        || facing.equals(BlockFace.SOUTH);
        boolean isFloor = facing.equals(BlockFace.DOWN);
        boolean isCeiling = facing.equals(BlockFace.UP);
        Location loc = startingLocation;

        ItemFrame selectedFrame = getFrameAt(selectedLocation, facing);

        for (int r = 0; r < rows; r++) {
            int x = 0;
            int z = 0;
            for (int c = 0; c < columns; c++) {
                if (test(selectedFrame, loc, facing)) {
                    itemFramesLocationMap.put(loc.clone(), getFrameAt(loc, facing));
                }
                //do a row
                if (isWall || isFloor) {
                    switch (bf) {
                        case NORTH:
                            x++;
                            loc = loc.add(1, 0, 0);
                            break;
                        case SOUTH:
                            x--;
                            loc = loc.add(-1, 0, 0);
                            break;
                        case EAST:
                            z++;
                            loc = loc.add(0, 0, 1);
                            break;
                        case WEST:
                            z--;
                            loc = loc.add(0, 0, -1);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + bf);

                    }
                } else if (isCeiling) {
                    switch (bf) {
                        case NORTH:
                            x--;
                            loc = loc.add(-1, 0, 0);
                            break;
                        case SOUTH:
                            x++;
                            loc = loc.add(1, 0, 0);
                            break;
                        case EAST:
                            z--;
                            loc = loc.add(0, 0, -1);
                            break;
                        case WEST:
                            z++;
                            loc = loc.add(0, 0, 1);
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + bf);

                    }
                }


            }
            if (isWall) {
                loc = loc.add(-x, -1, -z);
            } else if (isFloor || isCeiling) {
                switch (bf) {
                    case NORTH:
                    case SOUTH:
                        loc = loc.add(-x, 0, 1);
                        break;
                    case EAST:
                    case WEST:
                        loc = loc.add(1, 0, -z);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + bf);
                }
                x = 0;
                z = 0;
            }
        }
        return itemFramesLocationMap;
    }

    //TODO add descr and move to correct class and rename
    private static boolean test(ItemFrame selectedFrame, Location loc, BlockFace facing) {
        ImagePoster fullPoster = PosterManager.getPoster(selectedFrame.getItem());

        for (int id : fullPoster.getPostersIDs()) {
            if (getFrameAt(loc, facing) == null) {
                continue;
            }
            if (id == PosterManager.getPosterIDFromItemStack(getFrameAt(loc, facing).getItem())) {
                return true;
            }
        }
        return false;
    }
}
