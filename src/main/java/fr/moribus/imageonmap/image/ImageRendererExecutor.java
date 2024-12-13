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


import fr.moribus.imageonmap.Permissions;
import fr.moribus.imageonmap.PluginConfiguration;
import fr.moribus.imageonmap.map.ImagePoster;
import fr.moribus.imageonmap.map.PosterData;
import fr.moribus.imageonmap.map.PosterManager;
import fr.moribus.imageonmap.map.PosterMap;
import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.components.worker.Worker;
import fr.zcraft.quartzlib.components.worker.WorkerAttributes;
import fr.zcraft.quartzlib.components.worker.WorkerCallback;
import fr.zcraft.quartzlib.components.worker.WorkerRunnable;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.text.ActionBar;
import fr.zcraft.quartzlib.tools.text.MessageSender;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import javax.imageio.ImageIO;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;


@WorkerAttributes(name = "Image Renderer", queriesMainThread = true)
public class ImageRendererExecutor extends Worker {
    public static void renderAndNotify(final URL url, final ImageUtils.ScalingType scaling, final UUID playerUUID,
                                       final int width, final int height) {
        final Player player = Bukkit.getPlayer(playerUUID);
        if (player == null) {
            return;
        }

        ActionBar.sendPermanentMessage(player, ChatColor.DARK_GREEN + I.t("Rendering..."));

        render(url, scaling, player.getUniqueId(), width, height, new WorkerCallback<ImagePoster>() {
            @Override
            public void finished(ImagePoster result) {
                ActionBar.removeMessage(player);
                MessageSender.sendActionBarMessage(player, ChatColor.DARK_GREEN + I.t("Rendering finished!"));

                if (result.give(player) && (result instanceof PosterMap && !((PosterMap) result).hasColumnData())) {
                    player.sendMessage(ChatColor.GRAY + I.t("The rendered map was too big to fit in your inventory."));
                    player.sendMessage(ChatColor.GRAY + I.t("Use '/maptool getremaining' to get the remaining maps."));
                }
            }

            @Override
            public void errored(Throwable exception) {
                ActionBar.removeMessage(player);
                player.sendMessage(I.t("{ce}Map rendering failed: {0}", exception.getMessage()));

                PluginLogger.warning("Rendering from {0} failed: {1}: {2}",
                        player.getName(),
                        exception.getClass().getCanonicalName(),
                        exception.getMessage());
            }
        });
    }

    private static URLConnection connecting(URL url) throws IOException {
        final URLConnection connection = url.openConnection();
        connection.addRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
        connection.connect();

        if (connection instanceof HttpURLConnection) {
            final HttpURLConnection httpConnection = (HttpURLConnection) connection;
            final int httpCode = httpConnection.getResponseCode();
            if ((httpCode / 100) != 2) {
                throw new IOException(I.t("HTTP error: {0} {1}", httpCode, httpConnection.getResponseMessage()));
            }
        }
        return connection;
    }

    private static void checkSizeLimit(final UUID playerUUID, final BufferedImage image) throws IOException {
        if ((PluginConfiguration.LIMIT_SIZE_X.get() > 0 || PluginConfiguration.LIMIT_SIZE_Y.get() > 0)
                && !Permissions.BYPASS_SIZE.grantedTo(Bukkit.getPlayer(playerUUID))) {
            if (PluginConfiguration.LIMIT_SIZE_X.get() > 0) {
                if (image.getWidth() > PluginConfiguration.LIMIT_SIZE_X.get()) {
                    throw new IOException(I.t("The image is too wide!"));
                }
            }
            if (PluginConfiguration.LIMIT_SIZE_Y.get() > 0) {
                if (image.getHeight() > PluginConfiguration.LIMIT_SIZE_Y.get()) {
                    throw new IOException(I.t("The image is too tall!"));
                }
            }
        }
    }

    public static void render(final PosterData posterData, final UUID playerUUID,
                              WorkerCallback<ImagePoster> callback) {
        render(posterData.getURL(), posterData.getScaling(), playerUUID, posterData.getWidth(), posterData.getHeight(),
                callback);
    }

    public static void render(final URL url, final ImageUtils.ScalingType scaling, final UUID playerUUID,
                              final int width, final int height, WorkerCallback<ImagePoster> callback) {
        submitQuery(new WorkerRunnable<ImagePoster>() {

            @Override
            public ImagePoster run() throws Throwable {

                BufferedImage image = null;

                //If the link is an imgur one
                if (url.toString().toLowerCase().startsWith("https://imgur.com/")) {

                    //Not handled, can't with the hash only access the image in i.imgur.com/<hash>.<extension>

                    if (url.toString().contains("gallery/")) {
                        throw new IOException(
                                "We do not support imgur gallery yet, please use direct link to image instead."
                                        + " Right click on the picture you want "
                                        + "to use then select copy picture link:) ");
                    }

                    for (Extension ext : Extension.values()) {
                        String newLink = "https://i.imgur.com/" + url.toString().split("https://imgur.com/")[1]
                                + "."
                                + ext.toString();
                        URL url2 = new URL(newLink);

                        //Try connecting
                        URLConnection connection = connecting(url2);

                        final InputStream stream = connection.getInputStream();

                        image = ImageIO.read(stream);

                        //valid image
                        if (image != null) {
                            break;
                        }

                    }

                } else {
                    //Try connecting
                    URLConnection connection = connecting(url);

                    final InputStream stream = connection.getInputStream();

                    image = ImageIO.read(stream);
                }
                if (image == null) {
                    throw new IOException(I.t("The given URL is not a valid image"));
                }
                // Limits are in place and the player does NOT have rights to avoid them.
                checkSizeLimit(playerUUID, image);
                final BufferedImage resizedImage;

                resizedImage = scaling.resize(image, ImagePoster.WIDTH * width, ImagePoster.HEIGHT * height);
                image.flush();//Safe to free

                return renderPoster(resizedImage, playerUUID);
            }
        }, callback);
    }

    public static void update(final URL url, final ImageUtils.ScalingType scaling, final UUID playerUUID,
                              final ImagePoster poster, final int width, final int height,
                              WorkerCallback<ImagePoster> callback) {
        submitQuery(new WorkerRunnable<ImagePoster>() {
            @Override
            public ImagePoster run() throws Throwable {
                final URLConnection connection = connecting(url);

                final InputStream stream = connection.getInputStream();
                final BufferedImage image = ImageIO.read(stream);
                stream.close();

                if (image == null) {
                    throw new IOException(I.t("The given URL is not a valid image"));
                }

                // Limits are in place and the player does NOT have rights to avoid them.
                checkSizeLimit(playerUUID, image);

                updatePoster(scaling.resize(image, width * 128, height * 128),
                        playerUUID, poster.getPostersIDs());
                return poster;

            }
        }, callback);

    }

    private static void updatePoster(final BufferedImage image, final UUID playerUUID, int[] postersIDs)
            throws Throwable {

        final PosterImage poster = new PosterImage(image);
        poster.splitImages();

        ImageIOExecutor.saveImage(postersIDs, poster);

        if (PluginConfiguration.SAVE_FULL_IMAGE.get()) {
            ImageIOExecutor.saveImage(ImagePoster.getFullImageFile(postersIDs[0], postersIDs[postersIDs.length - 1]),
                    image);
        }

        submitToMainThread(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Renderer.installRenderer(poster, postersIDs);
                return null;
            }
        });
    }

    private static ImagePoster renderPoster(final BufferedImage image, final UUID playerUUID) throws Throwable {
        final PosterImage poster = new PosterImage(image);
        final int posterCount = poster.getImagesCount();
        PosterManager.checkPosterLimit(posterCount, playerUUID);
        final Future<int[]> futurePostersIds = submitToMainThread(new Callable<int[]>() {
            @Override
            public int[] call() throws Exception {
                return PosterManager.getNewPostersIds(posterCount);
            }
        });
        poster.splitImages();
        final int[] postersIDs = futurePostersIds.get();
        ImageIOExecutor.saveImage(postersIDs, poster);


        ImageIOExecutor.saveImage(postersIDs, poster);
        if (PluginConfiguration.SAVE_FULL_IMAGE.get()) {
            ImageIOExecutor.saveImage(ImagePoster.getFullImageFile(postersIDs[0], postersIDs[postersIDs.length - 1]),
                    image);
        }

        submitToMainThread(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Renderer.installRenderer(poster, postersIDs);
                return null;
            }

        });
        poster.getImage().flush();//Safe to free
        return PosterManager.createPoster(poster, playerUUID, postersIDs);
    }

    private enum Extension {
        png, jpg, jpeg, gif
    }

}
