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
import fr.moribus.imageonmap.image.PosterInitEvent;
import fr.moribus.imageonmap.map.ImagePoster;
import fr.moribus.imageonmap.map.PosterManager;
import fr.moribus.imageonmap.map.PosterMap;
import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.items.ItemStackBuilder;
import fr.zcraft.quartzlib.tools.world.FlatLocation;
import fr.zcraft.quartzlib.tools.world.WorldUtils;
import java.util.Map;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.scheduler.BukkitScheduler;

// TODO update when adding small picture snapshot.
public abstract class SplatterPosterManager {
    private SplatterPosterManager() {
    }

    public static ItemStack makeSplatterPoster(PosterMap poster) {


        final ItemStack splatter = new ItemStackBuilder(Material.FILLED_MAP).title(ChatColor.GOLD, poster.getName())
                .title(ChatColor.DARK_GRAY, " - ").title(ChatColor.GRAY, I.t("Splatter Map"))
                .title(ChatColor.DARK_GRAY, " - ")
                .title(ChatColor.GRAY, I.t("{0} × {1}", poster.getColumnCount(), poster.getRowCount()))
                .loreLine(ChatColor.GRAY, poster.getId()).loreLine()
                /// Title in a splatter map tooltip
                .loreLine(ChatColor.BLUE, I.t("Item frames needed"))
                /// Size of a map stored in a splatter map
                .loreLine(ChatColor.GRAY,
                        I.t("{0} × {1} (total {2} frames)", poster.getColumnCount(), poster.getRowCount(),
                                poster.getColumnCount() * poster.getRowCount()))
                .loreLine()
                /// Title in a splatter map tooltip
                .loreLine(ChatColor.BLUE, I.t("How to use this?"))
                .longLore(
                        ChatColor.GRAY
                                +
                                I.t("Place empty item frames on a wall, enough to host the whole map."
                                        + " Then, right-click on the bottom-left frame with this map."),
                        40)
                .loreLine()
                .longLore(ChatColor.GRAY
                        + I.t("Shift-click one of the placed maps to remove the whole poster in one shot."), 40)
                .hideAllAttributes()
                .craftItem();

        final MapMeta meta = (MapMeta) splatter.getItemMeta();
        meta.setMapId(poster.getPosterIdAt(0));
        meta.setColor(Color.GREEN);
        splatter.setItemMeta(meta);

        return addSplatterAttribute(splatter);
    }

    /**
     * To identify image on maps for the auto-splattering to work, we mark the
     * items using an enchantment maps are not supposed to have (Mending).
     *
     * <p>
     * Then we check if the map is enchanted at all to know if it's a splatter
     * map. This ensure compatibility with old splatter maps from 3.x, where
     * zLib's glow effect was used.
     * </p>
     * An AttributeModifier (using zLib's attributes system) is not used,
     * because Minecraft (or Spigot) removes them from maps in 1.14+, so that
     * wasn't stable enough (and the glowing effect of enchantments is
     * prettier).
     *
     * @param itemStack The item stack to mark as a splatter map.
     * @return The modified item stack. The instance may be different if the passed item stack is not a craft itemstack.
     */
    public static ItemStack addSplatterAttribute(final ItemStack itemStack) {
        itemStack.addUnsafeEnchantment(Enchantment.LURE, 1);
        //TODO ADD event to forbid xp duplication and usage in crafting table
        return itemStack;
    }

    /**
     * Checks if an item have the splatter attribute set (i.e. if the item is
     * enchanted in any way).
     *
     * @param itemStack The item to check.
     * @return True if the attribute was detected.
     */
    public static boolean hasSplatterAttributes(ItemStack itemStack) {
        return PosterManager.managesPoster(itemStack);
    }

    /**
     * Return true if it has a specified splatter map
     *
     * @param player The player to check.
     * @param poster The map to check.
     * @return True if the player has this map
     */
    public static boolean hasSplatterPoster(Player player, PosterMap poster) {
        Inventory playerInventory = player.getInventory();

        for (int i = 0; i < playerInventory.getSize(); ++i) {
            ItemStack item = playerInventory.getItem(i);
            if (hasSplatterAttributes(item) && poster.managesPoster(item)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Place a splatter map
     *
     * @param startFrame Frame clicked by the player
     * @param player     Player placing map
     * @return true if the map was correctly placed
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public static boolean placeSplatterPoster(ItemFrame startFrame, Player player) {
        ImagePoster iposter = PosterManager.getPoster(player.getInventory().getItemInMainHand());
        if (!(iposter instanceof PosterMap)) {
            return false;
        }

        PosterMap poster = (PosterMap) iposter;
        PosterWall wall = new PosterWall();

        if (startFrame.getFacing().equals(BlockFace.DOWN) || startFrame.getFacing().equals(BlockFace.UP)) {
            // If it is on floor or ceiling
            PosterOnASurface surface = new PosterOnASurface();
            FlatLocation startLocation = new FlatLocation(startFrame.getLocation(), startFrame.getFacing());
            FlatLocation endLocation = startLocation.clone().addH(poster.getColumnCount(), poster.getRowCount(),
                    WorldUtils.get4thOrientation(player.getLocation()));

            surface.loc1 = startLocation;
            surface.loc2 = endLocation;

            if (!surface.isValid(player)) {
                String message =
                        I.t("§c There is not enough space to place this map ({0} × {1}).", poster.getColumnCount(),
                                poster.getRowCount());
                TextComponent text = new TextComponent(message);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, text);
                return false;
            }
            Rotation r = Rotation.NONE;
            BlockFace bf = WorldUtils.get4thOrientation(player.getLocation());
            PluginLogger.info("YAW calc {0} YAW {1} ", Math.abs(player.getLocation().getYaw()) - 180f,
                    player.getLocation().getYaw());
            switch (bf) {
                case NORTH:
                    break;
                case EAST:
                    r = r.rotateClockwise();
                    break;
                case WEST:
                    r = r.rotateCounterClockwise();
                    break;
                case SOUTH:
                    r = r.rotateClockwise();
                    r = r.rotateClockwise();
                    break;

                default:
                    throw new IllegalStateException("Unexpected value: " + bf);
            }

            int i = 0;
            for (ItemFrame frame : surface.frames) {

                bf = WorldUtils.get4thOrientation(player.getLocation());
                int id = poster.getPosterIdAtReverseZ(i, bf, startFrame.getFacing());
                switch (frame.getFacing()) {
                    case UP:
                        break;
                    case DOWN:
                        r = r.rotateClockwise().rotateClockwise(); //Invert
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + frame.getFacing());
                }
                frame.setRotation(r);
                /*if (i == 0) {
                    //First map need to be rotate one time CounterClockwise
                    switch (bf) {
                        case EAST:
                            frame.setRotation(r.rotateClockwise());
                            break;
                        case WEST:
                            frame.setRotation(r.rotateCounterClockwise());
                            break;
                        case SOUTH:
                            frame.setRotation(r.rotateClockwise().rotateClockwise());
                            break;
                        case NORTH:
                            break;
                        default:
                            throw new IllegalStateException("Unexpected value: " + frame.getFacing());
                    }
                }*/

                PosterInitEvent.initPoster(id);
                i++;
            }
        } else {
            // If it is on a wall NSEW
            FlatLocation startLocation = new FlatLocation(startFrame.getLocation(), startFrame.getFacing());
            FlatLocation endLocation = startLocation.clone().add(poster.getColumnCount(), poster.getRowCount());

            wall.loc1 = startLocation;
            wall.loc2 = endLocation;

            if (!wall.isValid()) {

                String message =
                        I.t("§c There is not enough space to place this map ({0} × {1}).", poster.getColumnCount(),
                                poster.getRowCount());
                TextComponent text = new TextComponent(message);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, text);
                return false;
            }

            int i = 0;
            for (ItemFrame frame : wall.frames) {

                int id = poster.getPosterIdAtReverseY(i);


                setupPoster(player, frame, id);


                //Force reset of rotation
                frame.setRotation(Rotation.NONE);
                PosterInitEvent.initPoster(id);
                ++i;
            }
        }
        return true;
    }

    private static void setupPoster(Player player, ItemFrame frame, int id) {
        BukkitScheduler scheduler = ImageOnMap.getPlugin().getServer().getScheduler();
        scheduler.scheduleSyncDelayedTask(ImageOnMap.getPlugin(), new Runnable() {
            @Override
            public void run() {
                addPropertiesToFrames(player, frame);
                ItemStack item = PosterItemManager.createPosterItem(id, "", true, false);
                frame.setItem(item);
            }
        }, 5L);
    }

    /**
     * Remove splattermap
     *
     * @param startFrame Frame clicked by the player
     * @param player     The player removing the map
     * @return
     **/
    public static PosterMap removeSplatterPoster(ItemFrame startFrame, Player player) {
        final ImagePoster iposter = PosterManager.getPoster(startFrame.getItem());
        if (!(iposter instanceof PosterMap)) {
            return null;
        }
        PosterMap poster = (PosterMap) iposter;
        if (!poster.hasColumnData()) {
            return null;
        }
        //We search for the map on the top left corner
        Location startingLocation = poster.findLocationFirstFrame(startFrame, player);
        Map<Location, ItemFrame>
                itemFrameLocations =
                PosterOnASurface.getItemFramesLocation(player, startingLocation, startFrame.getLocation(),
                        startFrame.getFacing(),
                        poster.getRowCount(), poster.getColumnCount());
        //TODO check if it is the correct map id and check the why it delete more than it should and out of place
        for (Map.Entry<Location, ItemFrame> entry : itemFrameLocations.entrySet()) {
            ItemFrame frame = itemFrameLocations.get(entry.getKey());
            if (frame != null) {
                removePropertiesFromFrames(frame);
                frame.setItem(null);
            }
        }

        return poster;
    }

    public static void addPropertiesToFrames(Player player, ItemFrame frame) {
        if (Permissions.PLACE_INVISIBLE_SPLATTER_POSTER.grantedTo(player)) {
            frame.setVisible(false);
        }
    }

    public static void removePropertiesFromFrames(ItemFrame frame) {
        frame.setVisible(true);
    }
}
