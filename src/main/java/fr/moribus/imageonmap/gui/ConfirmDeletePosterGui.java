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

import fr.moribus.imageonmap.Permissions;
import fr.moribus.imageonmap.map.ImagePoster;
import fr.moribus.imageonmap.map.PosterManager;
import fr.moribus.imageonmap.map.PosterManagerException;
import fr.zcraft.quartzlib.components.gui.ActionGui;
import fr.zcraft.quartzlib.components.gui.Gui;
import fr.zcraft.quartzlib.components.gui.GuiAction;
import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.items.ItemStackBuilder;
import java.util.Random;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;


public class ConfirmDeletePosterGui extends ActionGui {
    private static final int BUTTONS_WIDTH = 4;

    private static final int FIRST_SLOT_DELETE_BUTTON = 27;
    private static final int SHIFT_CANCEL_BUTTON = 5;

    /**
     * The messages randomly displayed in the lore of the “delete” buttons.
     */
    private static final String[] DELETE_MESSAGES = new String[] {
            "Please", "I'm still alive", "Don't do that", "I'm still loving you", "I want to live",
            "Please please", "Please please please", "What are you doing?!", "Nooooo!",
            "Click and I'll be dead", "Why?", "Please don't do that", "Think about my family",
            "Click, I don't like you anymore.", "I don't hate you.", "Click, I'm ready.",
            "I'm a green button.", "I'm different.", "Thanks anyway.", "Excuse me.", "Get mad.",
            "Sorry!", "My fault!", "I don't blame you.", "No hard feelings.",
            "But I need to protect the humans!", "Noooo!", "I'm scared!", "What are you doing?",
            "It burns.", "This is not good.", "Can't breathe.", "Thanks anyway.", "These things happen.",
            "That was nobody's fault.", "I probably deserved it.", "I blame myself."
    };

    /**
     * The messages randomly displayed in the lore of the “cancel” buttons.
     */
    private static final String[] CANCEL_MESSAGES = new String[] {
            "Yay!", "Still aliiiive!", "Click click click", "Yes do that", "I'm a red button.",
            "Please click here", "The other button is ugly", "Save me", "This is the good choice",
            "Click, I want to live!", "I'll be dead another day anyway", "Are you sure?",
            "So you're still loving me?", "Please save me", "Take me with you.",
            "Excuse me.", "Don't make lemonade.", "Sleep mode activated.", "Hybernating.",
            "Your business is appreciated.", "Hey! It's me! Don't shoot!", "Wheee!", "Hurray!",
            "You have excellent aim!", "Please please please"
    };

    /**
     * The map being deleted.
     */
    private final ImagePoster posterToDelete;

    /**
     * A source of randomness.
     * Yes, this javadoc comment is REALLY useful. Trust me.
     */
    private final Random random = new Random();


    /**
     * @param posterToDelete The poster being deleted.
     */
    public ConfirmDeletePosterGui(ImagePoster posterToDelete) {
        this.posterToDelete = posterToDelete;
    }

    @Override
    protected void onUpdate() {
        /// The title of the poster deletion GUI. {0}: poster name.
        setTitle(I.t(getPlayerLocale(), "{0} » {black}Confirm deletion", posterToDelete.getName()));
        setSize(6 * 9);


        /* ** Item representation of the image being deleted ** */

        action("", 13, new ItemStackBuilder(Material.FILLED_MAP)
                /// The title of the map deletion item
                .title(I.t(getPlayerLocale(), "{red}You're about to destroy this map..."))
                /// The end, in the lore, of a title starting with “You're about to destroy this map...”.
                .lore(I.t(getPlayerLocale(), "{red}...{italic}forever{red}."))
                .loreLine()
                .lore(I.t(getPlayerLocale(), "{gray}Name: {white}{0}", posterToDelete.getName()))
                .lore(I.t(getPlayerLocale(), "{gray}Map ID: {white}{0}", posterToDelete.getId()))
                .lore(I.t(getPlayerLocale(), "{gray}Maps inside: {white}{0}", posterToDelete.getPostersIDs().length))
                .hideAllAttributes()
        );

        /* ** Buttons ** */

        int slot = FIRST_SLOT_DELETE_BUTTON;
        for (; slot < getSize() - (9 - BUTTONS_WIDTH); slot++) {
            action("delete", slot, createDeleteSubButton());
            action("cancel", slot + SHIFT_CANCEL_BUTTON, createCancelSubButton());

            if ((slot + 1) % 9 == (9 - BUTTONS_WIDTH - 1)) {
                slot += 5;
            }
        }
    }

    private ItemStack createDeleteSubButton() {
        return createSubButton(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Delete the map", DELETE_MESSAGES);
    }

    private ItemStack createCancelSubButton() {
        return createSubButton(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Cancel", CANCEL_MESSAGES);
    }

    private ItemStack createSubButton(Material color, String title, String[] messages) {
        return new ItemStackBuilder(color)
                .title(title)
                .loreSeparator()
                .longLore(ChatColor.GRAY + messages[random.nextInt(messages.length)])
                .item();
    }

    @GuiAction("cancel")
    protected void cancel() {
        close();
    }

    @GuiAction("delete")
    protected void delete() {

        // Does the player still have the permission to delete a poster?
        if (!Permissions.DELETE.grantedTo(getPlayer())) {
            I.sendT(getPlayer(), "{ce}You are no longer allowed to do that.");
            close();
            return;
        }

        PosterManager.clear(getPlayer().getInventory(), posterToDelete);

        try {
            PosterManager.deletePoster(posterToDelete);
            getPlayer().sendMessage(I.t("{gray}Map successfully deleted."));
        } catch (PosterManagerException ex) {
            PluginLogger.warning("Error while deleting map", ex);
            getPlayer().sendMessage(ChatColor.RED + ex.getMessage());
        }


        // We try to open the poster list GUI, if the poster was deleted, before the details GUI
        // (so the grandparent GUI).
        if (getParent() != null && getParent().getParent() != null) {
            Gui.open(getPlayer(), getParent().getParent());
        } else {
            close();
        }
    }
}
