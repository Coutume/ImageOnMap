/*
 * Copyright or © or Copr. Moribus (2013)
 * Copyright or © or Copr. ProkopyL <prokopylmc@gmail.com> (2015)
 * Copyright or © or Copr. Amaury Carrade <amaury@carrade.eu> (2016 – 2020)
 * Copyright or © or Copr. Vlammar <valentin.jabre@gmail.com> (2019 – 2020)
 *
 * This software is a computer program whose purpose is to allow insertion of
 * custom images in a Minecraft world.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
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
 * knowledge of the CeCILL-B license and that you accept its terms.
 */

package fr.moribus.imageonmap.image;

import fr.moribus.imageonmap.ImageOnMap;
import fr.moribus.imageonmap.map.ImageMap;
import fr.zcraft.zlib.components.worker.Worker;
import fr.zcraft.zlib.components.worker.WorkerAttributes;
import fr.zcraft.zlib.components.worker.WorkerRunnable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;


@WorkerAttributes (name = "Image IO")
public class ImageIOExecutor extends Worker
{
    static public void loadImage(final File file, final Renderer mapRenderer) 
    {

        submitQuery(new WorkerRunnable<Void>()
        {
            @Override
            public Void run() throws Exception
            {
                BufferedImage image = ImageIO.read(file);
                mapRenderer.setImage(image);
                return null;
            }
        });
    }
    
    static public void saveImage(final File file, final BufferedImage image)
    {
        submitQuery(new WorkerRunnable<Void>()
        {
            @Override
            public Void run() throws Throwable
            {
                ImageIO.write(image, "png", file);
                return null;
            }
        });
    }
    
    static public void saveImage(int mapID, BufferedImage image)
    {
        saveImage(ImageOnMap.getPlugin().getImageFile(mapID), image);
    }
    
    static public void saveImage(int[] mapsIDs, PosterImage image)
    {
        for(int i = 0, c = mapsIDs.length; i < c; i++)
        {
            ImageIOExecutor.saveImage(ImageOnMap.getPlugin().getImageFile(mapsIDs[i]), image.getImageAt(i));
        }
    }
    
    static public void deleteImage(ImageMap map)
    {
        int[] mapsIDs = map.getMapsIDs();
        for(int i = 0, c = mapsIDs.length; i < c; i++)
        {
            deleteImage(ImageOnMap.getPlugin().getImageFile(mapsIDs[i]));
        }
    }
    
    static public void deleteImage(final File file)
    {
        submitQuery(new WorkerRunnable<Void>()
        {
            @Override
            public Void run() throws Throwable
            {
                Files.delete(file.toPath());
                return null;
            }
        });
    }
}
