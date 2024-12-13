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

import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.world.FlatLocation;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;

public class PosterWall {

    public FlatLocation loc1;
    public FlatLocation loc2;

    public ItemFrame[] frames;

    public static ItemFrame getEmptyFrameAt(Location location, BlockFace facing) {
        return PosterOnASurface.getEmptyFrameAt(location, facing);
    }

    public boolean isValid() {
        ItemFrame curFrame;

        FlatLocation bottomLeft = FlatLocation.minMerged(loc1, loc2);
        FlatLocation loc = bottomLeft.clone();
        int distX = FlatLocation.flatBlockDistanceX(loc1, loc2);
        int distY = FlatLocation.flatBlockDistanceY(loc1, loc2);

        frames = new ItemFrame[distX * distY];
        for (int x = 0; x < distX; x++) {
            for (int y = 0; y < distY; y++) {
                curFrame = getEmptyFrameAt(loc, loc.getFacing());
                if (curFrame == null) {
                    return false;
                }
                frames[y * distX + x] = curFrame;
                loc.add(0, 1);
            }
            loc.add(1, 0);
            loc.setY(bottomLeft.getY());
        }

        return true;
    }

}
