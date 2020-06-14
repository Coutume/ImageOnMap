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

import fr.moribus.imageonmap.PluginConfiguration;
import fr.moribus.imageonmap.map.ImageMap;
import fr.moribus.imageonmap.map.MapManager;
import fr.zcraft.zlib.components.i18n.I;
import fr.zcraft.zlib.components.worker.Worker;
import fr.zcraft.zlib.components.worker.WorkerAttributes;
import fr.zcraft.zlib.components.worker.WorkerCallback;
import fr.zcraft.zlib.components.worker.WorkerRunnable;
import org.bukkit.Bukkit;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@WorkerAttributes (name = "Image Renderer", queriesMainThread = true)
public class ImageRendererExecutor extends Worker
{
    static public void render(final URL url, final ImageUtils.ScalingType scaling, final UUID playerUUID, final int width, final int height, WorkerCallback<ImageMap> callback)
    {
        submitQuery(new WorkerRunnable<ImageMap>()
        {
            @Override
            public ImageMap run() throws Throwable
            {
                final URLConnection connection = url.openConnection();
                connection.connect();
                if(connection instanceof HttpURLConnection)
                {
                    final HttpURLConnection  httpConnection = (HttpURLConnection) connection;
                    final int httpCode = httpConnection.getResponseCode();
                    if((httpCode / 100) != 2)
                    {
                        throw new IOException(I.t("HTTP error: {0} {1}", httpCode, httpConnection.getResponseMessage()));
                    }
                }
                final InputStream stream = connection.getInputStream();
                final BufferedImage image = ImageIO.read(stream);
                
                if (image == null) throw new IOException(I.t("The given URL is not a valid image"));
                
                
                //Limits are in place and the player does NOT have rights to avoid them.
                if((PluginConfiguration.LIMIT_SIZE_X.get() > 0 || PluginConfiguration.LIMIT_SIZE_Y.get() > 0) && !Bukkit.getPlayer(playerUUID).hasPermission("imageonmap.bypasssize")) {
                	if(PluginConfiguration.LIMIT_SIZE_X.get() > 0) {
                		if(image.getWidth() > PluginConfiguration.LIMIT_SIZE_X.get()) throw new IOException(I.t("The image is too wide!"));
                	}
                	if(PluginConfiguration.LIMIT_SIZE_Y.get() > 0) {
                		if(image.getHeight() > PluginConfiguration.LIMIT_SIZE_Y.get()) throw new IOException(I.t("The image is too tall!"));
                	}
                }
                
                if(scaling != ImageUtils.ScalingType.NONE && height <= 1 && width <= 1) {
                    return renderSingle(scaling.resize(image, ImageMap.WIDTH, ImageMap.HEIGHT), playerUUID);
                }

                final BufferedImage resizedImage = scaling.resize(image, ImageMap.WIDTH * width, ImageMap.HEIGHT * height);
                return renderPoster(resizedImage, playerUUID);
                //return RenderPoster(image, playerUUID);
            }
        }, callback);
    }

    static private ImageMap renderSingle(final BufferedImage image, final UUID playerUUID) throws Throwable
    {
        MapManager.checkMapLimit(1, playerUUID);
        final Future<Integer> futureMapID = submitToMainThread(new Callable<Integer>()
        {
            @Override
            public Integer call() throws Exception
            {
                return MapManager.getNewMapsIds(1)[0];
            }
        });

        final int mapID = futureMapID.get();
        ImageIOExecutor.saveImage(mapID, image);
        
        submitToMainThread(new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                Renderer.installRenderer(image, mapID);
                return null;
            }
        });
        
        return MapManager.createMap(playerUUID, mapID);
    }

    static private ImageMap renderPoster(final BufferedImage image, final UUID playerUUID) throws Throwable
    {
        final PosterImage poster = new PosterImage(image);
        final int mapCount = poster.getImagesCount();
        
        MapManager.checkMapLimit(mapCount, playerUUID);
        final Future<int[]> futureMapsIds = submitToMainThread(new Callable<int[]>()
        {
            @Override
            public int[] call() throws Exception
            {
                return MapManager.getNewMapsIds(mapCount);
            }
        });

        poster.splitImages();

        final int[] mapsIDs = futureMapsIds.get();
        
        ImageIOExecutor.saveImage(mapsIDs, poster);
        
        if(PluginConfiguration.SAVE_FULL_IMAGE.get()) {
        	ImageIOExecutor.saveImage(ImageMap.getFullImageFile(mapsIDs[0], mapsIDs[mapsIDs.length - 1]), image);
        }
        
        submitToMainThread(new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                Renderer.installRenderer(poster, mapsIDs);
                return null;
            }

        });
        
        return MapManager.createMap(poster, playerUUID, mapsIDs);
    }
}