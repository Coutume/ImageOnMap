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
import fr.moribus.imageonmap.ui.PosterItemManager;
import fr.zcraft.quartzlib.components.i18n.I;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class ImagePoster implements ConfigurationSerializable {
    public static final int WIDTH = 128;
    public static final int HEIGHT = 128;
    /// The default display name of a map
    public static final String DEFAULT_NAME = I.t("Map");
    private final UUID userUUID;
    private final Type posterType;
    private String id;
    private String name;

    protected ImagePoster(UUID userUUID, Type posterType) {
        this(userUUID, posterType, null, null);
    }

    protected ImagePoster(UUID userUUID, Type posterType, String id, String name) {
        this.userUUID = userUUID;
        this.posterType = posterType;
        this.id = id;
        this.name = name;

        if (this.id == null) {
            if (this.name == null) {
                this.name = DEFAULT_NAME;
            }
            this.id = PosterManager.getNextAvailablePosterID(this.name, userUUID);
        }
    }

    protected ImagePoster(Map<String, Object> map, UUID userUUID, Type posterType)
            throws InvalidConfigurationException {
        this(userUUID, posterType,
                (String) getNullableFieldValue(map, "id"),
                (String) getNullableFieldValue(map, "name"));

    }

    public static File getFullImageFile(int posterIDstart, int posterIDend) {
        return new File(ImageOnMap.getPlugin().getImagesDirectory(), "_" + posterIDstart
                + "-" + posterIDend + ".png");
    }

    public static ImagePoster fromConfig(Map<String, Object> map, UUID userUUID) throws InvalidConfigurationException {
        Type posterType;//TODO refactor this
        try {
            posterType = Type.valueOf((String) map.get("type"));
        } catch (ClassCastException ex) {
            throw new InvalidConfigurationException(ex);
        }

        switch (posterType) {
            case SINGLE:
            case POSTER:
                return new PosterMap(map, userUUID);
            default:
                throw new IllegalArgumentException("Unhandled map type given");
        }
    }

    public static Integer[] getSize(UUID playerUUID, String id) {

        ConfigurationSection section =
                PosterManager.getplayerPosterStore(playerUUID).getToolConfig()
                        .getConfigurationSection("playerPosterStore");

        if (section == null) {
            return new Integer[0];
        }
        List<Map<String, Object>> list = (List<Map<String, Object>>) section.getList("mapList");
        if (list == null) {
            return new Integer[0];
        }

        for (Map<String, Object> tmpMap : list) {
            if (tmpMap.get("id").equals(id)) {
                return new Integer[] {(Integer) tmpMap.get("columns"), (Integer) tmpMap.get("rows")};
            }
        }
        return new Integer[0];
    }

    protected static <T> T getFieldValue(Map<String, Object> map, String fieldName)
            throws InvalidConfigurationException {
        T value = getNullableFieldValue(map, fieldName);
        if (value == null) {
            throw new InvalidConfigurationException("Field value not found for \"" + fieldName + "\"");
        }
        return value;
    }

    protected static <T> T getNullableFieldValue(Map<String, Object> map, String fieldName)
            throws InvalidConfigurationException {
        try {
            return (T) map.get(fieldName);
        } catch (ClassCastException ex) {
            throw new InvalidConfigurationException("Invalid field \"" + fieldName + "\"", ex);
        }
    }

    public abstract int[] getPostersIDs();

    public abstract int getFirstPosterID();
    /* ====== Serialization methods ====== */

    public abstract boolean managesPoster(int posterID);

    public boolean managesPoster(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (item.getType() != Material.FILLED_MAP) {
            return false;
        }
        return managesPoster(PosterManager.getPosterIDFromItemStack(item));
    }

    //
    public abstract int getPosterCount();

    //

    public boolean give(Player player) {
        return PosterItemManager.give(player, this);
    }

    public boolean give(Player player, int quantity) {
        boolean bool = true;
        for (int i = 0; i < quantity; i++) {
            bool = bool && PosterItemManager.give(player, this);
        }
        return bool;
    }

    protected abstract void postSerialize(Map<String, Object> map);

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", getId());
        map.put("type", posterType.toString());
        map.put("name", getName());
        this.postSerialize(map);
        return map;
    }

    public UUID getUserUUID() {
        return userUUID;
    }


    /* ====== Getters & Setters ====== */

    public synchronized String getName() {
        return name;
    }

    public synchronized String getId() {
        return id;
    }

    public synchronized Type getType() {
        return posterType;
    }

    public synchronized void rename(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void rename(String name) {
        if (getName().equals(name)) {
            return;
        }
        rename(PosterManager.getNextAvailablePosterID(name, getUserUUID()), name);
    }

    public enum Type {
        SINGLE, POSTER
    }
}
