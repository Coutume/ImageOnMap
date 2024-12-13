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

package fr.moribus.imageonmap.ui;

import fr.moribus.imageonmap.ImageOnMap;
import fr.moribus.imageonmap.Permissions;
import fr.moribus.imageonmap.map.ImagePoster;
import fr.moribus.imageonmap.map.PosterManager;
import fr.moribus.imageonmap.map.PosterMap;
import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.core.QuartzLib;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.items.ItemStackBuilder;
import fr.zcraft.quartzlib.tools.items.ItemUtils;
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
import org.bukkit.scheduler.BukkitScheduler;

public class PosterItemManager implements Listener {
    private static HashMap<UUID, Queue<ItemStack>> mapItemCache;

    public static void init() {
        mapItemCache = new HashMap<>();
        QuartzLib.registerEvents(new PosterItemManager());
    }

    public static void exit() {
        if (mapItemCache != null) {
            mapItemCache.clear();
        }
        mapItemCache = null;
    }

    public static boolean give(Player player, ImagePoster poster) {
        if (poster instanceof PosterMap) {
            return give(player, (PosterMap) poster);
        }
        return false;
    }


    public static boolean give(Player player, PosterMap poster) {
        if (!poster.hasColumnData()) {
            return giveParts(player, poster);
        }
        return give(player, SplatterPosterManager.makeSplatterPoster(poster));
    }

    private static boolean give(final Player player, final ItemStack item) {
        boolean given = ItemUtils.give(player, item);
        //TODO simplify this
        if (given) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 1, 1);
        }

        return !given;
    }

    public static boolean giveParts(Player player, PosterMap poster) {
        boolean inventoryFull = false;

        ItemStack posterPartItem;
        for (int i = 0, c = poster.getPosterCount(); i < c; i++) {
            posterPartItem = poster.hasColumnData() ? createPosterItem(poster, poster.getColumnAt(i),
                    poster.getRowAt(i)) :
                    createPosterItem(poster, i);
            inventoryFull = give(player, posterPartItem) || inventoryFull;
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

    public static ItemStack createPosterItem(PosterMap poster, int index) {
        return createPosterItem(poster.getPosterIdAt(index), getPosterTitle(poster, index), true);
    }

    public static ItemStack createPosterItem(PosterMap poster, int x, int y) {
        return createPosterItem(poster.getPosterIdAt(x, y), getPosterTitle(poster, y, x), true);
    }

    public static ItemStack createPosterItem(int posterID, String text, boolean isPosterPart) {
        return createPosterItem(posterID, text, isPosterPart, false);
    }

    public static ItemStack createPosterItem(int posterID, String text, boolean isPosterPart, boolean goldTitle) {
        ItemStack posterItem;
        if (text == "") {
            posterItem = new ItemStackBuilder(Material.FILLED_MAP)
                    .hideAllAttributes()
                    .item();
        } else {
            if (goldTitle) {
                posterItem = new ItemStackBuilder(Material.FILLED_MAP)
                        .title(ChatColor.GOLD, text)
                        .hideAllAttributes()
                        .item();
            } else {
                posterItem = new ItemStackBuilder(Material.FILLED_MAP)
                        .title(text)
                        .hideAllAttributes()
                        .item();
            }
        }

        final MapMeta meta = (MapMeta) posterItem.getItemMeta();
        meta.setMapId(posterID);
        meta.setColor(isPosterPart ? Color.LIME : Color.GREEN);
        posterItem.setItemMeta(meta);
        return posterItem;
    }

    public static String getPosterTitle(PosterMap poster, int row, int column) {
        // The name of a poster item given to a player, if splatter posters are not used.
        // 0 = poster name; 1 = row; 2 = column.
        return I.t("{0} (row {1}, column {2})", poster.getName(), row + 1, column + 1);
    }

    public static String getPosterTitle(PosterMap poster, int index) {
        // The name of a poster item given to a player, if splatter posters are not used. 0 = poster name; 1 = index.
        return I.t("{0} (part {1})", poster.getName(), index + 1);
    }

    private static String getPosterTitle(ItemStack item) {
        ImagePoster iposter = PosterManager.getPoster(item);
        PosterMap poster = (PosterMap) iposter;
        int index = poster.getIndex(PosterManager.getPosterIDFromItemStack(item));
        if (poster.hasColumnData()) {
            return getPosterTitle(poster, poster.getRowAt(index), poster.getColumnAt(index));
        }

        return getPosterTitle(poster, index);
        //}
    }

    //

    /**
     * Returns the item to place to display the (col;row) part of the given poster.
     *
     * @param poster The map to take the part from.
     * @param x   The x coordinate of the part to display. Starts at 0.
     * @param y   The y coordinate of the part to display. Starts at 0.
     * @return The map.
     * @throws ArrayIndexOutOfBoundsException If x;y is not inside the map.
     */
    public static ItemStack createSubPosterItem(ImagePoster poster, int x, int y) {
        if (poster instanceof PosterMap && ((PosterMap) poster).hasColumnData()) {
            return PosterItemManager.createPosterItem((PosterMap) poster, x, y);
        } else {
            if (x != 0 || y != 0) {
                throw new ArrayIndexOutOfBoundsException(); // Coherence
            }

            return createPosterItem(poster.getPostersIDs()[0], poster.getName(), false);
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
        final ItemStack posterItem = player.getInventory().getItemInMainHand();

        if (frame.getItem().getType() != Material.AIR) {
            return;
        }
        if (!PosterManager.managesPoster(posterItem)) {
            return;
        }

        if (!Permissions.PLACE_SPLATTER_POSTER.grantedTo(player)) {
            player.sendMessage(I.t(ChatColor.RED + "You do not have permission to place splatter maps."));
            event.setCancelled(true);
            return;
        }

        frame.setItem(new ItemStack(Material.AIR));
        if (SplatterPosterManager.hasSplatterAttributes(posterItem)) {
            if (!SplatterPosterManager.placeSplatterPoster(frame, player, event)) {

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


            final ItemStack frameItem = posterItem.clone();
            final ItemMeta meta = frameItem.getItemMeta();

            meta.setDisplayName(null);
            frameItem.setItemMeta(meta);
            BukkitScheduler scheduler = ImageOnMap.getPlugin().getServer().getScheduler();
            scheduler.scheduleSyncDelayedTask(ImageOnMap.getPlugin(), () -> {
                frame.setItem(frameItem);
                frame.setRotation(Rotation.NONE);
            }, 5L);
        }
        //ItemUtils.consumeItem(player, mapItem); //todo useless ?
    }

    private static void onItemFrameRemove(ItemFrame frame, Player player, EntityDamageByEntityEvent event) {
        ItemStack item = frame.getItem();
        if (item.getType() != Material.FILLED_MAP) {
            return;
        }

        if (Permissions.REMOVE_SPLATTER_POSTER.grantedTo(player) && player.isSneaking()) {
            PluginLogger.info("Frame " + frame);
            PosterMap poster = SplatterPosterManager.removeSplatterPoster(frame, player);
            if (poster != null) {
                event.setCancelled(true);

                if (player.getGameMode() != GameMode.CREATIVE
                        || !SplatterPosterManager.hasSplatterPoster(player, poster)) {
                    poster.give(player);
                }
                return;
            }

        }

        if (!PosterManager.managesPoster(frame.getItem())) {
            return;
        }
        SplatterPosterManager.removePropertiesFromFrames(player, frame);
        frame.setItem(new ItemStackBuilder(item)
                .title(getPosterTitle(item))
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
