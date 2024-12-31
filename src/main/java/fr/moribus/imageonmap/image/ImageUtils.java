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

import fr.zcraft.quartzlib.tools.PluginLogger;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

/**
 * Various image-related utilities
 */
public class ImageUtils {

    /**
     * Generates a resized buffer of the given source
     *
     * @param source       The source buffer to draw
     * @param destinationW resize width
     * @param destinationH resize height
     * @return The new buffer, with the source buffer drawn on it
     */
    private static BufferedImage resize(BufferedImage source, int destinationW, int destinationH, boolean covered) {
        float ratioW = (float) destinationW / (float) source.getWidth();
        float ratioH = (float) destinationH / (float) source.getHeight();
        int finalW;
        int finalH;
        int x;
        int y;

        if (covered ? ratioW > ratioH : ratioW < ratioH) {
            finalW = destinationW;
            finalH = (int) (source.getHeight() * ratioW);
        } else {
            finalW = (int) (source.getWidth() * ratioH);
            finalH = destinationH;
        }

        x = (destinationW - finalW) / 2;
        y = (destinationH - finalH) / 2;

        return drawImage(source,
                destinationW, destinationH,
                x, y, finalW, finalH);
    }

    /**
     * @param source       The source buffer to draw
     * @param destinationW resize width
     * @param destinationH resize height
     * @return The new buffer, with the source buffer drawn on it
     */
    private static BufferedImage resizeStretched(BufferedImage source, int destinationW, int destinationH) {
        return drawImage(source,
                destinationW, destinationH,
                0, 0, destinationW, destinationH);
    }

    /**
     * Draws the source image on a new buffer, and returns it.
     * The source buffer can be drawn at any size and position in the new buffer.
     *
     * @param source  The source buffer to draw
     * @param bufferW The width of the new buffer
     * @param bufferH The height of the new buffer
     * @param posX    The X position of the source buffer
     * @param posY    The Y position of the source buffer
     * @param sourceW The width of the source buffer
     * @param sourceH The height of the source buffer
     * @return The new buffer, with the source buffer drawn on it
     */
    private static BufferedImage drawImage(BufferedImage source,
                                           int bufferW, int bufferH,
                                           int posX, int posY,
                                           int sourceW, int sourceH) {
        Graphics graphics;
        BufferedImage newImage = null;
        try {
            newImage = new BufferedImage(bufferW, bufferH, BufferedImage.TYPE_INT_ARGB);

            graphics = newImage.getGraphics();
            graphics.drawImage(source, posX, posY, sourceW, sourceH, null);

            return newImage;
        } catch (final Throwable e) {
            PluginLogger.warning("Exception/error at drawImage");
            if (newImage != null) {
                newImage.flush();//Safe to free
            }
            throw e;
        }

    }

    public static ScalingType scalingTypeFromName(String resize) {
        switch (resize) {
            case "stretch":
            case "stretched":
            case "resize-stretched":
                return ScalingType.STRETCHED;

            case "cover":
            case "covered":
            case "resize-covered":
                return ScalingType.COVERED;

            case "contain":
            case "contained":
            case "resize-contained":
            case "resize":
                return ScalingType.CONTAINED;

            default:
                return ScalingType.NONE;
        }
    }

    public enum ScalingType {
        NONE,
        CONTAINED,
        COVERED,
        STRETCHED,
        ;

        public BufferedImage resize(BufferedImage source, int destinationW, int destinationH) {
            switch (this) {
                case CONTAINED:
                    return ImageUtils.resize(source, destinationW, destinationH, false);
                case COVERED:
                    return ImageUtils.resize(source, destinationW, destinationH, true);
                case STRETCHED:
                    return resizeStretched(source, destinationW, destinationH);
                default:
                    return source;

            }
        }
    }
}