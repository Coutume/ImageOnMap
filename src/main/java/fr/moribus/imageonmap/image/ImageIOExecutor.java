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

package fr.moribus.imageonmap.image;

import fr.moribus.imageonmap.ImageOnMap;
import fr.moribus.imageonmap.map.ImagePoster;
import fr.zcraft.quartzlib.components.worker.Worker;
import fr.zcraft.quartzlib.components.worker.WorkerAttributes;
import fr.zcraft.quartzlib.components.worker.WorkerRunnable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import javax.imageio.ImageIO;


@WorkerAttributes(name = "Image IO")
public class ImageIOExecutor extends Worker {
    public static void loadImage(final File file, final Renderer posterRenderer) {

        submitQuery(new WorkerRunnable<Void>() {
            @Override
            public Void run() throws Exception {
                BufferedImage image = ImageIO.read(file);
                posterRenderer.setImage(image);
                image.flush();//Safe to free
                return null;
            }
        });
    }

    public static void saveImage(final File file, final BufferedImage image) {
        submitQuery(new WorkerRunnable<Void>() {
            @Override
            public Void run() throws Throwable {
                ImageIO.write(image, "png", file);
                return null;
            }
        });
    }

    public static void saveImage(int posterID, BufferedImage image) {
        saveImage(ImageOnMap.getPlugin().getImageFile(posterID), image);
    }

    public static void saveImage(int[] postersIDs, PosterImage image) {
        for (int i = 0, c = postersIDs.length; i < c; i++) {
            BufferedImage img = image.getImageAt(i);
            ImageIOExecutor.saveImage(ImageOnMap.getPlugin().getImageFile(postersIDs[i]), img);
            img.flush();//Safe to free
        }
    }

    public static void deleteImage(ImagePoster poster) {
        int[] postersIDs = poster.getPostersIDs();
        for (int i = 0, c = postersIDs.length; i < c; i++) {
            deleteImage(ImageOnMap.getPlugin().getImageFile(postersIDs[i]));
        }
    }

    public static void deleteImage(final File file) {
        submitQuery(new WorkerRunnable<Void>() {
            @Override
            public Void run() throws Throwable {
                Files.delete(file.toPath());
                return null;
            }
        });
    }
}
