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


package fr.moribus.imageonmap.gui;

import fr.moribus.imageonmap.image.ImageUtils;
import fr.zcraft.quartzlib.components.gui.ActionGui;
import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.tools.items.ItemStackBuilder;
import fr.zcraft.quartzlib.tools.items.TextualBanners;
import java.net.URL;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;


public class RenderGui extends ActionGui {
    final URL url;

    boolean resize = false;
    int width = 0;
    int height = 0;

    boolean originalSizeLoaded = false;
    int originalWidth = 0;
    int originalHeight = 0;

    ImageUtils.ScalingType scaling = ImageUtils.ScalingType.NONE;

    public RenderGui(URL url) {
        this.url = url;
    }

    @Override
    protected void onUpdate() {
        setTitle(I.t("Image Editor"));
        setHeight(6);

        action("toggle_resize", 0, new ItemStackBuilder(Material.PAINTING)
                .title(ChatColor.LIGHT_PURPLE, ChatColor.BOLD + I.t("Resize image"))
                .loreLine(ChatColor.DARK_GRAY, resize ? I.t("Enabled") : I.t("Disabled"))
                .loreSeparator()
                .longLore(ChatColor.GRAY,
                        I.t("You can automatically resize the image to a certain number of blocks (or item frames)."))
                .loreSeparator()
                .loreLine(ChatColor.BLUE, I.t("Original size (in blocks)"))
                .loreLine(ChatColor.GRAY,
                        originalSizeLoaded ? I.t("{0} × {1}", originalWidth, originalHeight) : I.t("Loading..."))
                .loreSeparator()
                .longLore(resize ? I.t("{gray}» {white}Click{gray} to disable resize") :
                        I.t("{gray}» {white}Click{gray} to enable resize"))
        );

        injectSizeEditor(2, true);
        injectSizeEditor(11, false);
    }

    /**
     * Injects the size editor in the GUI.
     *
     * @param slot    The slot where the editor must start.
     * @param isWidth True to inject a width-size editor; false to inject a height-editor.
     */
    private void injectSizeEditor(int slot, final boolean isWidth) {
        final String action_key = isWidth ? "width_" : "height_";
        final String currentSize = ChatColor.DARK_GRAY + I.t("Current size: {0} × {1}", width, height);

        /*action(action_key + "_decrement_10", slot++, getBannerButton(false, true, resize)
                .title(ChatColor.RED, I.t("- 10"))
                .loreLine(currentSize)
                .loreSeparator()
                .longLore(isWidth
                        ? I.t("{gray}» {white}Click{gray} to decrease the image's width by 10 blocks")
                        : I.t("{gray}» {white}Click{gray} to decrease the image's height by 10 blocks")
                )
        );

        action(action_key + "_decrement_1", slot++, getBannerButton(false, false, resize)
                .title(ChatColor.RED, I.t("- 1"))
                .loreLine(currentSize)
                .loreSeparator()
                .longLore(isWidth
                        ? I.t("{gray}» {white}Click{gray} to decrease the image's width by one block")
                        : I.t("{gray}» {white}Click{gray} to decrease the image's height by one block")
                )
        );

        action(action_key + "_increment_1", slot++, getBannerButton(true, false, resize)
                .title(ChatColor.GREEN, I.t("+ 1"))
                .loreLine(currentSize)
                .loreSeparator()
                .longLore(isWidth
                        ? I.t("{gray}» {white}Click{gray} to increase the image's width by one block")
                        : I.t("{gray}» {white}Click{gray} to increase the image's height by one block")
                )
        );

        action(action_key + "_increment_10", slot++, getBannerButton(true, true, resize)
                .title(ChatColor.GREEN, I.t("+ 10"))
                .loreLine(currentSize)
                .loreSeparator()
                .longLore(isWidth
                        ? I.t("{gray}» {white}Click{gray} to increase the image's width by 10 blocks")
                        : I.t("{gray}» {white}Click{gray} to increase the image's height by 10 blocks")
                )
        );


        /*(action_key + "_set_values", slot++, getBannerButton(false, false, resize)
                .title(ChatColor.BLUE, I.t("set the size"))
                .loreLine(currentSize)
                .loreSeparator()
                .longLore(isWidth
                        ? I.t("{gray}» {white}Click{gray} to set the image's width")
                        : I.t("{gray}» {white}Click{gray} to set the image's height")
                )
        );*/
        slot++;
    }

    /**
     * Creates a banner for the +/- buttons.
     * + are green, - are red
     * short steps are light, long steps are dark
     * disabled banners are in grayscale
     *
     * @param positive true for a + banner
     * @param longStep true for a darker banner
     * @param disabled true for a grayscale banner
     * @return The banner in a builder.
     */
    private ItemStackBuilder getBannerButton(boolean positive, boolean longStep, boolean disabled) {
        //final char symbol = positive ? '+' : '-'; //TODO this need rework have something that work but need QL update
        final char symbol = positive ? '*' : '-';
        final DyeColor background;

        if (disabled) {
            background = longStep ? DyeColor.BLACK : DyeColor.GRAY;
        } else {
            if (positive) {
                background = longStep ? DyeColor.GREEN : DyeColor.LIME;
            } else {
                background = longStep ? DyeColor.RED : DyeColor.PINK;
            }
        }

        return new ItemStackBuilder(TextualBanners.getCharBanner(symbol, background, DyeColor.BLACK));
    }
}
