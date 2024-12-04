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

package fr.moribus.imageonmap.gui;

import static fr.zcraft.quartzlib.tools.reflection.Reflection.getFieldValue;

import fr.moribus.imageonmap.Permissions;
import fr.moribus.imageonmap.map.ImageMap;
import fr.moribus.imageonmap.map.MapManager;
import fr.moribus.imageonmap.map.PosterMap;
import fr.moribus.imageonmap.ui.MapItemManager;
import fr.moribus.imageonmap.ui.SplatterMapManager;
import fr.zcraft.quartzlib.components.gui.ExplorerGui;
import fr.zcraft.quartzlib.components.gui.Gui;
import fr.zcraft.quartzlib.components.gui.GuiAction;
import fr.zcraft.quartzlib.components.gui.PromptGui;
import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.items.ItemStackBuilder;
import fr.zcraft.quartzlib.tools.runners.RunTask;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class MapDetailGui extends ExplorerGui<Integer> {
    private final ImageMap map;
    private final OfflinePlayer offplayer;
    private final String name;

    public MapDetailGui(ImageMap map, OfflinePlayer p, String name) {
        super();
        this.map = map;
        this.offplayer = p;
        this.name = name;
    }

    private void setMapPartCustomModelData(ItemStack item, int x, int y) {
        int id = 1000;
        ItemMeta meta = item.getItemMeta();


        String mapId = map.getId();
        //TODO make it prettier and fix issues with scrolling
        if (map instanceof PosterMap) {
            PosterMap poster = ((PosterMap) map);
            int columnCount = poster.getColumnCount();
            int rowCount = poster.getRowCount();
            if (columnCount == 0 && rowCount == 0) {
                id = 1008;
            }
            if (x == 0 && y == 0 && columnCount > 0 && rowCount > 0) {
                id = 1001;
            }




            meta.setCustomModelData(1000);
            item.setItemMeta(meta);
            PluginLogger.info(item.getItemMeta().toString());
        }
    }

    @Override
    protected ItemStack getViewItem(int x, int y) {
        final Material partMaterial = y % 2 == x % 2 ? Material.MAP : Material.PAPER;
        ItemStackBuilder builder = new ItemStackBuilder(partMaterial);
        ItemStack itemStack = builder.craftItem();
        PluginLogger.info("before setmappart");

        setMapPartCustomModelData(itemStack, x, y);
        builder = new ItemStackBuilder(itemStack);
        builder = builder
                .title(I.t(getPlayerLocale(), "{green}Map part"))
                .lore(I.t(getPlayerLocale(), "{gray}Row: {white}{0}", y + 1))
                .lore(I.t(getPlayerLocale(), "{gray}Column: {white}{0}", x + 1));

        if (Permissions.GET.grantedTo(getPlayer())) {
            builder.loreLine().lore(I.t(getPlayerLocale(), "{gray}» {white}Click{gray} to get only this part"));
        }

        return builder.item();
    }

    @Override
    protected ItemStack getViewItem(Integer mapId) {
        final int index = ((PosterMap) map).getIndex(mapId);
        final Material partMaterial = index % 2 == 0 ? Material.MAP : Material.PAPER;

        final ItemStackBuilder builder = new ItemStackBuilder(partMaterial)
                .title(I.t(getPlayerLocale(), "{green}Map part"))
                .lore(I.t(getPlayerLocale(), "{gray}Part: {white}{0}", index + 1));

        if (Permissions.GET.grantedTo(getPlayer())) {
            builder.loreLine().lore(I.t(getPlayerLocale(), "{gray}» {white}Click{gray} to get only this part"));
        }

        return builder.item();
    }

    @Override
    protected ItemStack getPickedUpItem(int x, int y) {
        if (!Permissions.GET.grantedTo(getPlayer())) {
            return null;
        }

        if (map instanceof PosterMap) {
            return MapItemManager.createMapItem((PosterMap) map, x, y);
        }

        throw new IllegalStateException("Unsupported map type: " + map.getType());
    }

    @Override
    protected ItemStack getPickedUpItem(Integer mapId) {
        if (!Permissions.GET.grantedTo(getPlayer())) {
            return null;
        }

        final PosterMap poster = (PosterMap) map;
        return MapItemManager.createMapItem(poster, poster.getIndex(mapId));
    }

    @Override
    protected ItemStack getEmptyViewItem() {
        return super.getEmptyViewItem();
    }

    @Override
    protected void onUpdate() {
        /// Title of the map details GUI
        if (offplayer.getUniqueId().equals(getPlayer().getUniqueId())) {
            setTitle(I.t(getPlayerLocale(), "Your maps » {black}{0}", map.getName()));
        } else {
            setTitle(I.t(getPlayerLocale(), "{1}'s maps » {black}{0}", map.getName(), name));
        }
        setKeepHorizontalScrollingSpace(true);

        if (map instanceof PosterMap) {
            PosterMap poster = (PosterMap) map;
            if (poster.hasColumnData()) {
                setDataShape(poster.getColumnCount(), poster.getRowCount());
            } else {
                setData(ArrayUtils.toObject(poster.getMapsIDs()));
            }
        } else {
            setDataShape(1, 1);
        }

        final boolean canRename = Permissions.RENAME.grantedTo(getPlayer());
        final boolean canDelete = Permissions.DELETE.grantedTo(getPlayer());

        int renameSlot = getSize() - 7;
        int deleteSlot = getSize() - 6;

        if (!canRename) {
            deleteSlot--;
        }

        if (canRename) {
            action("rename", renameSlot, new ItemStackBuilder(Material.WRITABLE_BOOK)
                    .title(I.t(getPlayerLocale(), "{blue}Rename this image"))
                    .longLore(I.t(getPlayerLocale(),
                            "{gray}Click here to rename this image; this is used for your own organization."))
            );
        }

        if (canDelete) {
            action("delete", deleteSlot, new ItemStackBuilder(Material.BARRIER)
                    .title(I.t(getPlayerLocale(), "{red}Delete this image"))
                    .longLore(I.t(getPlayerLocale(),
                            "{gray}Deletes this map {white}forever{gray}. This action cannot be undone!"))
                    .loreLine()
                    .longLore(
                            I.t(getPlayerLocale(), "{gray}You will be asked to confirm your choice if you click here."))
            );
        }


        // To keep the controls centered, the back button is shifted to the right when the
        // arrow isn't displayed, so when the map fit on the grid without sliders.
        int backSlot = getSize() - 4;

        if (!canRename && !canDelete) {
            backSlot = getSize() - 5;
        } else if (map instanceof PosterMap && ((PosterMap) map).getColumnCount() <= INVENTORY_ROW_SIZE) {
            backSlot++;
        }

        action("back", backSlot, new ItemStackBuilder(Material.EMERALD)
                .title(I.t(getPlayerLocale(), "{green}« Back"))
                .lore(I.t(getPlayerLocale(), "{gray}Go back to the list."))
        );
    }


    @GuiAction("rename")
    public void rename() {
        if (!Permissions.RENAME.grantedTo(getPlayer())) {
            I.sendT(getPlayer(), "{ce}You are no longer allowed to do that.");
            update();
            return;
        }

        try {
            PromptGui.prompt(getPlayer(), newName -> {
                if (!Permissions.RENAME.grantedTo(getPlayer())) {
                    I.sendT(getPlayer(), "{ce}You are no longer allowed to do that.");
                    return;
                }

                if (newName == null || newName.isEmpty()) {
                    I.sendT(getPlayer(), "{ce}Map names can't be empty.");
                    return;
                }
                if (newName.equals(map.getName())) {
                    return;
                }

                map.rename(newName);
                I.sendT(getPlayer(), "{cs}Map successfully renamed.");

                if (getParent() != null) {
                    RunTask.later(() -> Gui.open(getPlayer(), this), 1L);

                } else {
                    close();
                }
            }, map.getName(), this);

        } catch (IllegalStateException e) {
            PluginLogger.error("Error while renaming map: ", e);
        }
    }

    @GuiAction("delete")
    public void delete() {
        if (!Permissions.DELETE.grantedTo(getPlayer())) {
            I.sendT(getPlayer(), "{ce}You are no longer allowed to do that.");
            update();
            return;
        }
        Gui.open(getPlayer(), new ConfirmDeleteMapGui(map), this);
    }

    @GuiAction("back")
    public void back() {
        close();
    }
}
