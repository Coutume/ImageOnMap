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

import fr.moribus.imageonmap.Permissions;
import fr.moribus.imageonmap.PluginConfiguration;
import fr.moribus.imageonmap.map.ImageMap;
import fr.moribus.imageonmap.map.MapManager;
import fr.moribus.imageonmap.map.PosterMap;
import fr.moribus.imageonmap.ui.MapItemManager;
import fr.moribus.imageonmap.ui.SplatterMapManager;
import fr.zcraft.quartzlib.components.gui.ExplorerGui;
import fr.zcraft.quartzlib.components.gui.Gui;
import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.items.ItemStackBuilder;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;


public class MapListGui extends ExplorerGui<ImageMap> {
    private OfflinePlayer offplayer;
    private String name;

    public MapListGui(OfflinePlayer sender) {
        this.offplayer = sender;
        this.name = sender.getName();
    }

    public MapListGui(OfflinePlayer p, String name) {
        this.offplayer = p;
        this.name = name;
    }

    @Override
    protected ItemStack getViewItem(ImageMap map) {
        String mapDescription;
        PosterMap poster = (PosterMap) map;
        if (poster.hasColumnData()) {
            /// Displayed subtitle description of a poster map on the list GUI (columns × rows in english)
            mapDescription = I.tl(getPlayerLocale(), "{white}Poster map ({0} × {1})", poster.getColumnCount(),
                    poster.getRowCount());
        } else {
            /// Displayed subtitle description of a poster map without column data on the list GUI
            mapDescription = I.tl(getPlayerLocale(), "{white}Poster map ({0} parts)", poster.getMapCount());
        }
        ItemStackBuilder builder =
                new ItemStackBuilder(Material.FILLED_MAP);
        ItemStack item = builder.craftItem();
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setCustomModelData(1);
        item.setItemMeta(itemMeta);
        PluginLogger.info(item.getItemMeta().toString());
        builder = new ItemStackBuilder(item);
        builder = builder
                /// Displayed title of a map on the list GUI
                .title(I.tl(getPlayerLocale(), "{green}{bold}{0}", map.getName()))

                .lore(mapDescription)
                .loreLine()
                /// Map ID displayed in the tooltip of a map on the list GUI
                .lore(I.tl(getPlayerLocale(), "{gray}Map ID: {0}", map.getId()))
                .loreLine();

        if (Permissions.GET.grantedTo(getPlayer())) {
            builder.lore(I.tl(getPlayerLocale(), "{gray}» {white}Left-click{gray} to get this map"));
        }

        builder.lore(I.tl(getPlayerLocale(), "{gray}» {white}Right-click{gray} for details and options"));

        final ItemStack mapItem = builder.item();

        final MapMeta meta = (MapMeta) mapItem.getItemMeta();
        meta.setColor(Color.GREEN);
        mapItem.setItemMeta(meta);

        return mapItem;
    }

    @Override
    protected ItemStack getEmptyViewItem() {
        ItemStackBuilder builder = new ItemStackBuilder(Material.BARRIER);
        if (offplayer.getUniqueId().equals(getPlayer().getUniqueId())) {

            builder.title(I.tl(getPlayerLocale(), "{red}You don't have any map."));

            if (Permissions.NEW.grantedTo(getPlayer())) {
                builder.longLore(I.tl(getPlayerLocale(),
                        "{gray}Get started by creating a new one using {white}/tomap <URL> [resize]{gray}!"));
            } else {
                builder.longLore(I.tl(getPlayerLocale(), "{gray}Unfortunately, you are not allowed to create one."));
            }
        } else {
            builder.title(I.tl(getPlayerLocale(), "{red}{0} doesn't have any map.", name));
        }
        return builder.item();
    }

    @Override
    protected void onRightClick(ImageMap data) {
        Gui.open(getPlayer(), new MapDetailGui(data, getPlayer(), name), this);
    }

    @Override
    protected void onClose() {
        super.onClose();
    }

    @Override
    protected ItemStack getPickedUpItem(ImageMap map) {
        if (!Permissions.GET.grantedTo(getPlayer())) {
            return null;
        }

        if (map instanceof PosterMap) {
            PosterMap poster = (PosterMap) map;

            if (poster.hasColumnData()) {
                return SplatterMapManager.makeSplatterMap((PosterMap) map);
            }

            MapItemManager.giveParts(getPlayer(), poster);
            return null;
        }

        MapItemManager.give(getPlayer(), map);
        return null;
    }

    @Override
    protected void onUpdate() {
        ImageMap[] maps = MapManager.getMaps(offplayer.getUniqueId());
        setData(maps);
        /// The maps list GUI title
        //Equal if the person who send the command is the owner of the mapList
        if (offplayer.getUniqueId().equals(getPlayer().getUniqueId())) {
            setTitle(I.tl(getPlayerLocale(), "{black}Your maps {reset}({0})", maps.length));
        } else {
            setTitle(I.tl(getPlayerLocale(), "{black}{1}'s maps {reset}({0})", maps.length, name));
        }

        setKeepHorizontalScrollingSpace(true);


        /* ** Statistics ** */
        int imagesCount = MapManager.getMapList(offplayer.getUniqueId()).size();
        int mapPartCount = MapManager.getMapPartCount(offplayer.getUniqueId());

        int mapGlobalLimit = PluginConfiguration.MAP_GLOBAL_LIMIT.get();
        int mapPersonalLimit = PluginConfiguration.MAP_PLAYER_LIMIT.get();

        int mapPartGloballyLeft = mapGlobalLimit - MapManager.getMapCount();
        int mapPartPersonallyLeft = mapPersonalLimit - mapPartCount;

        int mapPartLeft;
        if (mapGlobalLimit <= 0 && mapPersonalLimit <= 0) {
            mapPartLeft = -1;
        } else if (mapGlobalLimit <= 0) {
            mapPartLeft = mapPartPersonallyLeft;
        } else if (mapPersonalLimit <= 0) {
            mapPartLeft = mapPartGloballyLeft;
        } else {
            mapPartLeft = Math.min(mapPartGloballyLeft, mapPartPersonallyLeft);
        }

        double percentageUsed =
                mapPartLeft < 0 ? 0 : ((double) mapPartCount) / ((double) (mapPartCount + mapPartLeft)) * 100;

        ItemStackBuilder statistics = new ItemStackBuilder(Material.ENCHANTED_BOOK)
                .title(I.t(getPlayerLocale(), "{blue}Usage statistics"))
                .loreLine()
                .lore(I.tn(getPlayerLocale(), "{white}{0}{gray} image rendered", "{white}{0}{gray} images rendered",
                        imagesCount))
                .lore(I.tn(getPlayerLocale(), "{white}{0}{gray} Minecraft map used",
                        "{white}{0}{gray} Minecraft maps used", mapPartCount));

        if (mapPartLeft >= 0) {
            statistics
                    .lore("", I.t(getPlayerLocale(), "{blue}Minecraft maps limits"), "")
                    .lore(mapGlobalLimit == 0
                            ? I.t(getPlayerLocale(), "{gray}Server-wide limit: {white}unlimited")
                            : I.t(getPlayerLocale(), "{gray}Server-wide limit: {white}{0}", mapGlobalLimit))
                    .lore(mapPersonalLimit == 0
                            ? I.t(getPlayerLocale(), "{gray}Per-player limit: {white}unlimited")
                            : I.t(getPlayerLocale(), "{gray}Per-player limit: {white}{0}", mapPersonalLimit))
                    .loreLine()
                    .lore(I.t(getPlayerLocale(), "{white}{0} %{gray} of your quota used",
                            (int) Math.rint(percentageUsed)))
                    .lore(I.tn(getPlayerLocale(), "{white}{0}{gray} map left", "{white}{0}{gray} maps left",
                            mapPartLeft));
        }

        statistics.hideAllAttributes();

        action("", getSize() - 5, statistics);
    }
}
