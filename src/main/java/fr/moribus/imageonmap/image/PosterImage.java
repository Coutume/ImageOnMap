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


import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * This class represents an image split into pieces
 */
public class PosterImage {
    private static final int WIDTH = 128;
    private static final int HEIGHT = 128;

    private BufferedImage originalImage;
    private BufferedImage[] cutImages;
    private int lines;
    private int columns;
    private int cutImagesCount;
    private int remainderX;
    private int remainderY;

    /**
     * Creates a new Poster from an entire image
     *
     * @param originalImage the original image
     */
    public PosterImage(BufferedImage originalImage) {
        this.originalImage = originalImage;
        calculateDimensions();
    }

    private void calculateDimensions() {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        columns = (int) Math.ceil(originalWidth / WIDTH);
        lines = (int) Math.ceil(originalHeight / HEIGHT);

        remainderX = originalWidth % WIDTH;
        remainderY = originalHeight % HEIGHT;

        if (remainderX > 0) {
            columns++;
        }
        if (remainderY > 0) {
            lines++;
        }

        cutImagesCount = columns * lines;
    }

    public void splitImages() {
        try {
            cutImages = new BufferedImage[cutImagesCount];

            int imageX;
            int imageY = remainderY == 0 ? 0 : (remainderY - HEIGHT) / 2;
            for (int i = 0; i < lines; i++) {
                imageX = remainderX == 0 ? 0 : (remainderX - WIDTH) / 2;
                for (int j = 0; j < columns; j++) {
                    cutImages[i * columns + j] = makeSubImage(originalImage, imageX, imageY);
                    imageX += WIDTH;
                }
                imageY += HEIGHT;
            }
        } catch (final Throwable e) {
            if (cutImages != null) {
                for (BufferedImage bi : cutImages) {
                    if (bi != null) {
                        bi.flush();//Safe to free
                    }
                }
            }
            throw e;
        }

    }

    /**
     * Generates the subimage that intersects with the given poster rectangle.
     *
     * @param x X coordinate of top-left point of the poster.
     * @param y Y coordinate of top-left point of the poster.
     * @return the requested subimage.
     */
    private BufferedImage makeSubImage(BufferedImage originalImage, int x, int y) {

        BufferedImage newImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);

        Graphics graphics = newImage.getGraphics();

        graphics.drawImage(originalImage, -x, -y, null);
        return newImage;
    }

    /**
     * @return the split images
     */
    public BufferedImage[] getImages() {
        return cutImages;
    }

    public BufferedImage getImageAt(int i) {
        return cutImages[i];
    }

    public BufferedImage getImage() {
        return originalImage;
    }

    public int getColumnAt(int i) {
        return i % columns;
    }

    public int getLineAt(int i) {
        return i / columns;
    }

    /**
     * @return the number of lines of the poster
     */
    public int getLines() {
        return lines;
    }

    /**
     * @return the number of columns of the poster
     */
    public int getColumns() {
        return columns;
    }

    /**
     * @return the number of split images
     */
    public int getImagesCount() {
        return cutImagesCount;
    }

    public int getRemainderX() {
        return remainderX;
    }

    public void setRemainderX(int remainderX) {
        this.remainderX = remainderX;
    }

    public int getRemainderY() {
        return remainderY;
    }

    public void setRemainderY(int remainderY) {
        this.remainderY = remainderY;
    }
}
