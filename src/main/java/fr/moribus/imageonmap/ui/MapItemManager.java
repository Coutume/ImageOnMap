/*
 * Copyright or © or Copr. Moribus (2013)
 * Copyright or © or Copr. ProkopyL <prokopylmc@gmail.com> (2015)
 * Copyright or © or Copr. Amaury Carrade <amaury@carrade.eu> (2016 – 2022)
 * Copyright or © or Copr. Vlammar <anais.jabre@gmail.com> (2019 – 2023)
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

package fr.moribus.imageonmap.ui;

import fr.moribus.imageonmap.Permissions;
import fr.moribus.imageonmap.map.ImageMap;
import fr.moribus.imageonmap.map.MapManager;
import fr.moribus.imageonmap.map.PosterMap;
import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.core.QuartzLib;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.items.ItemStackBuilder;
import fr.zcraft.quartzlib.tools.items.ItemUtils;
import fr.zcraft.quartzlib.tools.runners.RunTask;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;

public class MapItemManager implements Listener {
    private static HashMap<UUID, Queue<ItemStack>> mapItemCache;

    public static void init() {
        mapItemCache = new HashMap<>();
        QuartzLib.registerEvents(new MapItemManager());
    }

    public static void exit() {
        if (mapItemCache != null) {
            mapItemCache.clear();
        }
        mapItemCache = null;
    }

    public static boolean give(Player player, ImageMap map) {
        if (map instanceof PosterMap) {
            return give(player, (PosterMap) map);
        }
        return false;
    }


    public static boolean give(Player player, PosterMap map) {
        if (!map.hasColumnData()) {
            return giveParts(player, map);
        }
        return give(player, SplatterMapManager.makeSplatterMap(map));
    }

    private static boolean give(final Player player, final ItemStack item) {
        boolean given = ItemUtils.give(player, item);

        if (given) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1, 1);
        }

        return !given;
    }

    public static boolean giveParts(Player player, PosterMap map) {
        boolean inventoryFull = false;

        ItemStack mapPartItem;
        for (int i = 0, c = map.getMapCount(); i < c; i++) {
            mapPartItem = map.hasColumnData() ? createMapItem(map, map.getColumnAt(i), map.getRowAt(i)) :
                    createMapItem(map, i);
            inventoryFull = give(player, mapPartItem) || inventoryFull;
        }

        return inventoryFull;
    }

    public static int giveCache(Player player) {
        Queue<ItemStack> cache = getCache(player);
        Inventory inventory = player.getInventory();
        int givenItemsCount = 0;

        while (inventory.firstEmpty() >= 0 && !cache.isEmpty()) {
            give(player, cache.poll());
            givenItemsCount++;
        }

        return givenItemsCount;
    }

    public static ItemStack createMapItem(PosterMap map, int index) {
        return createMapItem(map.getMapIdAt(index), getMapTitle(map, index), true);
    }

    public static ItemStack createMapItem(PosterMap map, int x, int y) {
        return createMapItem(map.getMapIdAt(x, y), getMapTitle(map, y, x), true);
    }

    public static ItemStack createMapItem(int mapID, String text, boolean isMapPart) {
        return createMapItem(mapID, text, isMapPart, false);
    }

    public static ItemStack createMapItem(int mapID, String text, boolean isMapPart, boolean goldTitle) {
        ItemStack mapItem;
        if (text == "") {
            mapItem = new ItemStackBuilder(Material.FILLED_MAP)
                    .hideAllAttributes()
                    .item();
        } else {
            if (goldTitle) {
                mapItem = new ItemStackBuilder(Material.FILLED_MAP)
                        .title(ChatColor.GOLD, text)
                        .hideAllAttributes()
                        .item();
            } else {
                mapItem = new ItemStackBuilder(Material.FILLED_MAP)
                        .title(text)
                        .hideAllAttributes()
                        .item();
            }
        }

        final MapMeta meta = (MapMeta) mapItem.getItemMeta();
        meta.setMapId(mapID);
        meta.setColor(isMapPart ? Color.LIME : Color.GREEN);
        mapItem.setItemMeta(meta);
        return mapItem;
    }

    public static String getMapTitle(PosterMap map, int row, int column) {
        /// The name of a map item given to a player, if splatter maps are not used. 0 = map name; 1 = row; 2 = column.
        return I.t("{0} (row {1}, column {2})", map.getName(), row + 1, column + 1);
    }

    public static String getMapTitle(PosterMap map, int index) {
        /// The name of a map item given to a player, if splatter maps are not used. 0 = map name; 1 = index.
        return I.t("{0} (part {1})", map.getName(), index + 1);
    }

    private static String getMapTitle(ItemStack item) {
        ImageMap map = MapManager.getMap(item);
        PosterMap poster = (PosterMap) map;
        int index = poster.getIndex(MapManager.getMapIdFromItemStack(item));
        if (poster.hasColumnData()) {
            return getMapTitle(poster, poster.getRowAt(index), poster.getColumnAt(index));
        }

        return getMapTitle(poster, index);
        //}
    }

    //

    /**
     * Returns the item to place to display the (col;row) part of the given poster.
     *
     * @param map The map to take the part from.
     * @param x   The x coordinate of the part to display. Starts at 0.
     * @param y   The y coordinate of the part to display. Starts at 0.
     * @return The map.
     * @throws ArrayIndexOutOfBoundsException If x;y is not inside the map.
     */
    public static ItemStack createSubMapItem(ImageMap map, int x, int y) {
        if (map instanceof PosterMap && ((PosterMap) map).hasColumnData()) {
            return MapItemManager.createMapItem((PosterMap) map, x, y);
        } else {
            if (x != 0 || y != 0) {
                throw new ArrayIndexOutOfBoundsException(); // Coherence
            }

            return createMapItem(map.getMapsIDs()[0], map.getName(), false);
        }
    }

    public static int getCacheSize(Player player) {
        return getCache(player).size();
    }

    private static Queue<ItemStack> getCache(Player player) {
        Queue<ItemStack> cache = mapItemCache.get(player.getUniqueId());
        if (cache == null) {
            cache = new ArrayDeque<>();
            mapItemCache.put(player.getUniqueId(), cache);
        }
        return cache;
    }

    private static void onItemFramePlace(ItemFrame frame, Player player, PlayerInteractEntityEvent event) {
        final ItemStack mapItem = player.getInventory().getItemInMainHand();

        if (frame.getItem().getType() != Material.AIR) {
            return;
        }
        if (!MapManager.managesMap(mapItem)) {
            return;
        }

        if (!Permissions.PLACE_SPLATTER_MAP.grantedTo(player)) {
            player.sendMessage(I.t(ChatColor.RED + "You do not have permission to place splatter maps."));
            event.setCancelled(true);
            return;
        }

        frame.setItem(new ItemStack(Material.AIR));
        if (SplatterMapManager.hasSplatterAttributes(mapItem)) {
            if (!SplatterMapManager.placeSplatterMap(frame, player, event)) {

                event.setCancelled(true); //In case of an error allow to cancel map placement
                return;
            }
            if (frame.getFacing() != BlockFace.UP && frame.getFacing() != BlockFace.DOWN) {
                frame.setRotation(Rotation.NONE);
            }
            frame.setRotation(Rotation.NONE);

        } else {
            if (frame.getFacing() != BlockFace.UP && frame.getFacing() != BlockFace.DOWN) {
                frame.setRotation(Rotation.NONE);
            }


            final ItemStack frameItem = mapItem.clone();
            final ItemMeta meta = frameItem.getItemMeta();

            meta.setDisplayName(null);
            frameItem.setItemMeta(meta);
            RunTask.later(() -> {
                frame.setItem(frameItem);
                frame.setRotation(Rotation.NONE);
            }, 5L);

        }
        //ItemUtils.consumeItem(player, mapItem); //useless no?
    }

    private static void onItemFrameRemove(ItemFrame frame, Player player, EntityDamageByEntityEvent event) {
        ItemStack item = frame.getItem();
        if (item.getType() != Material.FILLED_MAP) {
            return;
        }

        if (Permissions.REMOVE_SPLATTER_MAP.grantedTo(player) && player.isSneaking()) {
            PluginLogger.info("Frame " + frame);
            PosterMap poster = SplatterMapManager.removeSplatterMap(frame, player);
            if (poster != null) {
                event.setCancelled(true);

                if (player.getGameMode() != GameMode.CREATIVE
                        || !SplatterMapManager.hasSplatterMap(player, poster)) {
                    poster.give(player);
                }
                return;
            }

        }

        if (!MapManager.managesMap(frame.getItem())) {
            return;
        }
        SplatterMapManager.removePropertiesFromFrames(player, frame);
        frame.setItem(new ItemStackBuilder(item)
                .title(getMapTitle(item))
                .hideAllAttributes()
                .item());

    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ItemFrame)) {
            return;
        }
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        onItemFrameRemove((ItemFrame) event.getEntity(), (Player) event.getDamager(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public static void onEntityInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame)) {
            return;
        }
        onItemFramePlace((ItemFrame) event.getRightClicked(), event.getPlayer(), event);
    }
}
