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
import fr.moribus.imageonmap.map.PosterManagerException.Reason;
import fr.zcraft.quartzlib.tools.PluginLogger;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PlayerPosterStore implements ConfigurationSerializable {
    private final UUID playerUUID;
    private final ArrayList<ImagePoster> posterList = new ArrayList<>();
    private boolean modified = false;
    private int posterCount = 0;
    private FileConfiguration posterConfig = null;
    private File postersFile = null;

    public PlayerPosterStore(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    //TODO maybe usefull to merge with the other manages poster
    public synchronized boolean managesPoster(int posterID) {
        for (ImagePoster poster : posterList) {
            if (poster.managesPoster(posterID)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean managesPoster(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (item.getType() != Material.FILLED_MAP) {
            return false;
        }
        return managesPoster(PosterManager.getPosterIDFromItemStack(item));
    }

    public synchronized void addPoster(ImagePoster poster) throws PosterManagerException {
        checkPosterLimit(poster);
        insertPoster(poster);
    }

    public synchronized void insertPoster(ImagePoster poster) {
        add_Poster(poster);
        notifyModification();
    }

    private void add_Poster(ImagePoster poster) {
        posterList.add(poster);
        posterCount += poster.getPosterCount();
    }

    public synchronized void deletePoster(ImagePoster poster) throws PosterManagerException {
        delete_Poster(poster);
        notifyModification();
    }

    private void delete_Poster(ImagePoster poster) throws PosterManagerException {
        if (!posterList.remove(poster)) {
            throw new PosterManagerException(Reason.IMAGEPOSTER_DOES_NOT_EXIST);
        }
        posterCount -= poster.getPosterCount();
    }

    public synchronized boolean posterExists(String posterId) {
        return getPoster(posterId) != null;
    }

    public String getNextAvailablePosterID(String posterId) {
        //TODO check if the value is always greater than the id count
        if (!posterExists(posterId)) {
            return posterId;
        }
        int id = 0;
        do {
            id++;
        } while (posterExists(posterId + "-" + id));

        return posterId + "-" + id;
    }

    public synchronized List<ImagePoster> getPosterList() {
        return new ArrayList<>(posterList);
    }

    //TODO refactor to arraylist instead of an array
    public synchronized ImagePoster[] getPosters() {
        return posterList.toArray(new ImagePoster[0]);
    }

    public synchronized ImagePoster getPoster(String posterId) {
        for (ImagePoster poster : posterList) {
            if (poster.getId().equals(posterId)) {
                return poster;
            }
        }
        return null;
    }

    /* ===== Getters & Setters ===== */

    public void checkPosterLimit(ImagePoster poster) throws PosterManagerException {
        checkPosterLimit(poster.getPosterCount());
    }

    public void checkPosterLimit(int newPostersCount) throws PosterManagerException {
        int limit = PluginConfiguration.POSTER_PLAYER_LIMIT.get();
        if (limit <= 0) {
            return;
        }

        if (getPosterCount() + newPostersCount > limit) {
            throw new PosterManagerException(Reason.MAXIMUM_PLAYER_POSTERS_EXCEEDED, limit);
        }
    }

    public UUID getUUID() {
        return playerUUID;
    }

    public synchronized boolean isModified() {
        return modified;
    }

    public synchronized void notifyModification() {
        this.modified = true;
    }

    /* ****** Serializing ***** */

    public synchronized int getPosterCount() {
        return this.posterCount;
    }

    public synchronized int getImagesCount() {
        return this.posterList.size();
    }

    /* ****** Configuration Files management ***** */

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        ArrayList<Map> list = new ArrayList<>();
        synchronized (this) {
            for (ImagePoster tmpPoster : posterList) {
                list.add(tmpPoster.serialize());
            }
        }
        map.put("posterList", list);
        return map;
    }

    private void loadFromConfig(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        List<Map<String, Object>> list = (List<Map<String, Object>>) section.getList("posterList");
        if (list == null) {
            return;
        }

        for (Map<String, Object> tmpMap : list) {
            try {
                ImagePoster newPoster = ImagePoster.fromConfig(tmpMap, playerUUID);
                synchronized (this) {
                    add_Poster(newPoster);
                }
            } catch (InvalidConfigurationException ex) {
                PluginLogger.warning("Could not load map data : ", ex);
            }
        }

        try {
            checkPosterLimit(0);
        } catch (PosterManagerException ex) {
            PluginLogger.warning("Map limit exceeded for player {0} ({1} maps loaded)",
                    playerUUID.toString(), posterList.size());
        }
    }

    public FileConfiguration getToolConfig() {
        if (posterConfig == null) {
            load();
        }

        return posterConfig;
    }

    public void load() {
        if (postersFile == null) {
            postersFile = new File(ImageOnMap.getPlugin().getPostersDirectory(), playerUUID.toString() + ".yml");
            if (!postersFile.exists()) {
                save();
            }
        }
        posterConfig = YamlConfiguration.loadConfiguration(postersFile);
        loadFromConfig(getToolConfig().getConfigurationSection("playerPosterStore"));
    }

    public void save() {
        if (postersFile == null || posterConfig == null) {
            return;
        }
        getToolConfig().set("playerPosterStore", this.serialize());
        try {
            getToolConfig().save(postersFile);

        } catch (IOException ex) {
            PluginLogger.error("Could not save maps file for player '{0}'", ex, playerUUID.toString());
        }
        synchronized (this) {
            modified = false;
        }
    }
}
