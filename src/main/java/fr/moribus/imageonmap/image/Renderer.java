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
import java.awt.image.BufferedImage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class Renderer extends MapRenderer {
    private BufferedImage image;

    protected Renderer() {
        this(null);
    }

    protected Renderer(BufferedImage image) {
        this.image = image;
    }

    public static boolean isHandled(MapView poster) {
        if (poster == null) {
            return false;
        }
        for (MapRenderer renderer : poster.getRenderers()) {
            if (renderer instanceof Renderer) {
                return true;
            }
        }
        return false;
    }

    public static void installRenderer(PosterImage image, int[] postersIds) {
        for (int i = 0; i < postersIds.length; i++) {
            installRenderer(image.getImageAt(i), postersIds[i]);
        }
    }

    public static void installRenderer(BufferedImage image, int posterID) {
        MapView poster = Bukkit.getMap(posterID);
        if (poster == null) {
            PluginLogger.warning("Could not install renderer for map {0}: the Minecraft map does not exist",
                    posterID);
        } else {
            installRenderer(poster).setImage(image);
        }
    }

    public static Renderer installRenderer(MapView poster) {
        Renderer renderer = new Renderer();
        removeRenderers(poster);
        poster.addRenderer(renderer);
        return renderer;
    }

    public static void removeRenderers(MapView poster) {
        for (MapRenderer renderer : poster.getRenderers()) {
            poster.removeRenderer(renderer);
        }
    }

    @Override
    public void render(MapView v, final MapCanvas canvas, Player p) {
        //Render only once to avoid overloading the server
        if (image == null) {
            return;
        }
        canvas.drawImage(0, 0, image);
        image = null;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }
}
