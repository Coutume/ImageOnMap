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

import fr.moribus.imageonmap.ImageOnMap;
import fr.moribus.imageonmap.PluginConfiguration;
import fr.moribus.imageonmap.image.ImageIOExecutor;
import fr.moribus.imageonmap.image.PosterImage;
import fr.moribus.imageonmap.map.PosterManagerException.Reason;
import fr.zcraft.quartzlib.tools.PluginLogger;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.scheduler.BukkitTask;

public abstract class PosterManager {
    private static final long SAVE_DELAY = 200;
    private static final ArrayList<PlayerPosterStore> playerPosters = new ArrayList<PlayerPosterStore>();
    private static BukkitTask autosaveTask;

    public static void init() {
        load();
    }

    public static void exit() {
        save();
        playerPosters.clear();
        if (autosaveTask != null) {
            autosaveTask.cancel();
        }
    }

    @Deprecated
    public static boolean managesPoster(int posterID) {
        synchronized (playerPosters) {
            for (PlayerPosterStore posterStore : playerPosters) {
                if (posterStore.managesPoster(posterID)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean managesPoster(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (item.getType() != Material.FILLED_MAP) {
            return false;
        }

        synchronized (playerPosters) {
            for (PlayerPosterStore posterStore : playerPosters) {
                if (posterStore.managesPoster(item)) {
                    //duplicated ? maybe to remove from posterstore
                    return true;
                }
            }
        }
        return false;
    }

    public static ImagePoster createPoster(UUID playerUUID, int posterID) throws PosterManagerException {
        //ImagePoster newPoster = new SinglePoster(playerUUID, posterID);
        int[] ids = new int[] {posterID};
        ImagePoster newPoster = new PosterMap(playerUUID, ids, 1, 1);
        addPoster(newPoster);//TODO refactor this
        return newPoster;
    }

    public static ImagePoster createPoster(PosterImage image, UUID playerUUID, int[] postersIDs)
            throws PosterManagerException {
        ImagePoster newPoster;

        if (image.getImagesCount() == 1) {
            newPoster = new PosterMap(playerUUID, postersIDs, 1, 1);//TODO refactor this
            //newPoster = new SinglePoster(playerUUID, postersIDs[0]);
        } else {
            PluginLogger.info("pb ici ? " + image.getColumns() + " " + image.getLines());
            newPoster = new PosterMap(playerUUID, postersIDs, image.getColumns(), image.getLines());
        }
        addPoster(newPoster);
        return newPoster;
    }

    public static int[] getNewPostersIds(int amount) {
        int[] postersIDs = new int[amount];
        for (int i = 0; i < amount; i++) {
            postersIDs[i] = Bukkit.createMap(Bukkit.getWorlds().get(0)).getId();
        }
        return postersIDs;
    }

    /**
     * Returns the map ID from an ItemStack
     *
     * @param item The item stack
     * @return The map ID, or 0 if invalid.
     */
    public static int getPosterIDFromItemStack(final ItemStack item) {
        final ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof MapMeta)) {
            return 0;
        }

        return ((MapMeta) meta).hasMapId() ? ((MapMeta) meta).getMapId() : 0;
    }

    public static void addPoster(ImagePoster poster) throws PosterManagerException {
        getplayerPosterStore(poster.getUserUUID()).addPoster(poster);
    }

    public static void insertPoster(ImagePoster poster) {
        getplayerPosterStore(poster.getUserUUID()).insertPoster(poster);
    }

    public static void deletePoster(ImagePoster poster) throws PosterManagerException {
        getplayerPosterStore(poster.getUserUUID()).deletePoster(poster);
        ImageIOExecutor.deleteImage(poster);
    }

    public static void notifyModification(UUID playerUUID) {
        getplayerPosterStore(playerUUID).notifyModification();
        if (autosaveTask == null) {
            Bukkit.getScheduler().runTaskLater(ImageOnMap.getPlugin(), new AutoSaveRunnable(), SAVE_DELAY);
        }
    }

    public static String getNextAvailablePosterID(String posterID, UUID playerUUID) {
        return getplayerPosterStore(playerUUID).getNextAvailablePosterID(posterID);
    }

    public static List<ImagePoster> getPosterList(UUID playerUUID) {
        return getplayerPosterStore(playerUUID).getPosterList();
    }

    public static ImagePoster[] getPosters(UUID playerUUID) {
        return getplayerPosterStore(playerUUID).getPosters();
    }

    /**
     * Returns the number of minecraft maps used by the images rendered by the given player.
     *
     * @param playerUUID The player's UUID.
     * @return The count.
     */
    public static int getPosterPartCount(UUID playerUUID) {
        return getplayerPosterStore(playerUUID).getPosterCount();
    }

    public static ImagePoster getPoster(UUID playerUUID, String posterID) {
        return getplayerPosterStore(playerUUID).getPoster(posterID);
    }

    /**
     * Returns the {@link ImagePoster} this map belongs to.
     *
     * @param posterID The ID of the Minecraft map.
     * @return The {@link ImagePoster}.
     */
    public static ImagePoster getPoster(int posterID) {
        synchronized (playerPosters) {
            for (PlayerPosterStore posterStore : playerPosters) {
                if (posterStore.managesPoster(posterID)) {
                    for (ImagePoster poster : posterStore.getPosterList()) {
                        if (poster.managesPoster(posterID)) {
                            return poster;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Returns the {@link ImagePoster} this map belongs to.
     *
     * @param item The map, as an {@link ItemStack}.
     * @return The {@link ImagePoster}.
     */
    public static ImagePoster getPoster(ItemStack item) {
        if (item == null) {
            return null;
        }
        if (item.getType() != Material.FILLED_MAP) {
            return null;
        }
        return getPoster(getPosterIDFromItemStack(item));
    }

    public static void clear(Inventory inventory) {
        for (int i = 0, c = inventory.getSize(); i < c; i++) {
            if (managesPoster(inventory.getItem(i))) {
                inventory.setItem(i, new ItemStack(Material.AIR));
            }
        }
    }

    public static void clear(Inventory inventory, ImagePoster poster) {
        for (int i = 0, c = inventory.getSize(); i < c; i++) {
            if (poster.managesPoster(inventory.getItem(i))) {
                inventory.setItem(i, new ItemStack(Material.AIR));
            }
        }
    }

    private static UUID getUUIDFromFile(File file) {
        String fileName = file.getName();
        int fileExtPos = fileName.lastIndexOf('.');
        if (fileExtPos <= 0) {
            return null;
        }

        String fileExt = fileName.substring(fileExtPos + 1);
        if (!fileExt.equals("yml")) {
            return null;
        }

        try {
            return UUID.fromString(fileName.substring(0, fileExtPos));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    //Silent load
    public static void load() {
        load(true);
    }

    //Loading
    public static void load(boolean verbose) {
        int loadedFilesCount = 0;
        for (File file : Objects.requireNonNull(ImageOnMap.getPlugin().getPostersDirectory().listFiles())) {
            UUID uuid = getUUIDFromFile(file);
            if (uuid == null) {
                continue;
            }
            getplayerPosterStore(uuid);
            ++loadedFilesCount;
        }

        if (verbose) {
            PluginLogger.info("Loaded {0} player map files.", loadedFilesCount);
        }
    }

    public static void save() {
        synchronized (playerPosters) {
            for (PlayerPosterStore tmpStore : playerPosters) {
                tmpStore.save();
            }
        }
    }

    public static void checkPosterLimit(ImagePoster poster) throws PosterManagerException {
        checkPosterLimit(poster.getPosterCount(), poster.getUserUUID());
    }

    public static void checkPosterLimit(int newPostersCount, UUID userUUID) throws PosterManagerException {
        int limit = PluginConfiguration.POSTER_GLOBAL_LIMIT.get();

        if (limit > 0 && getPosterCount() + newPostersCount > limit) {
            throw new PosterManagerException(Reason.MAXIMUM_SERVER_POSTERS_EXCEEDED);
        }

        getplayerPosterStore(userUUID).checkPosterLimit(newPostersCount);
    }

    /**
     * Returns the total number of minecraft maps used by ImageOnMap images.
     *
     * @return The count.
     */
    public static int getPosterCount() {
        int posterCount = 0;
        synchronized (playerPosters) {
            for (PlayerPosterStore tmpStore : playerPosters) {
                posterCount += tmpStore.getPosterCount();
            }
        }
        return posterCount;
    }

    /**
     * Returns the total number of images rendered by ImageOnMap.
     *
     * @return The count.
     */
    public static int getImagesCount() {
        int imagesCount = 0;
        synchronized (playerPosters) {
            for (PlayerPosterStore tmpStore : playerPosters) {
                imagesCount += tmpStore.getImagesCount();
            }
        }
        return imagesCount;
    }

    /**
     * Returns if the given map ID is valid and exists in the current save.
     *
     * @param posterID the map ID.
     * @return true if the given map ID is valid and exists in the current save, false otherwise.
     */
    public static boolean posterIDExists(int posterID) {
        try {
            return Bukkit.getMap(posterID) != null;
        } catch (Throwable ex) {
            return false;
        }
    }

    public static PlayerPosterStore getplayerPosterStore(UUID playerUUID) {
        PlayerPosterStore store;
        synchronized (playerPosters) {
            store = getExistingplayerPosterStore(playerUUID);
            if (store == null) {
                store = new PlayerPosterStore(playerUUID);

                playerPosters.add(store);
                store.load();
            }
        }
        return store;
    }

    private static PlayerPosterStore getExistingplayerPosterStore(UUID playerUUID) {
        synchronized (playerPosters) {
            for (PlayerPosterStore posterStore : playerPosters) {
                if (posterStore.getUUID().equals(playerUUID)) {
                    return posterStore;
                }
            }
        }
        return null;
    }

    private static class AutoSaveRunnable implements Runnable {
        @Override
        public void run() {
            synchronized (playerPosters) {
                for (PlayerPosterStore toolStore : playerPosters) {
                    if (toolStore.isModified()) {
                        toolStore.save();
                    }
                }
                autosaveTask = null;
            }
        }

    }
}
