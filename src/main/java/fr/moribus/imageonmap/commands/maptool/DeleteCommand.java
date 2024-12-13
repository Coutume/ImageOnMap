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

package fr.moribus.imageonmap.commands.maptool;

import fr.moribus.imageonmap.Permissions;
import fr.moribus.imageonmap.commands.IoMCommand;
import fr.moribus.imageonmap.map.ImagePoster;
import fr.moribus.imageonmap.map.PosterManager;
import fr.moribus.imageonmap.map.PosterManagerException;
import fr.zcraft.quartzlib.components.commands.CommandException;
import fr.zcraft.quartzlib.components.commands.CommandInfo;
import fr.zcraft.quartzlib.components.commands.WithFlags;
import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.components.rawtext.RawText;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.text.RawMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandInfo(name = "delete", usageParameters = "[player name]:<poster name> [--confirm]")
@WithFlags({"confirm"})
public class DeleteCommand extends IoMCommand {

    private static RawText deleteMsg(Class klass, String playerName, ImagePoster poster) {
        return new RawText(I.t("You are going to delete") + " ")
                .then(poster.getId())
                .color(ChatColor.GOLD)
                .then(". " + I.t("Are you sure ? "))
                .color(ChatColor.WHITE)
                .then(I.t("[Confirm]"))
                .color(ChatColor.GREEN)
                .hover(new RawText(I.t("{red}This map will be deleted {bold}forever{red}!")))
                .command(klass, playerName + ":" + "\"" + poster.getId() + "\"", "--confirm")
                .build();
    }

    @Override
    protected void run() throws CommandException {
        ArrayList<String> arguments = getArgs();
        final boolean confirm = hasFlag("confirm");

        boolean isTooMany = arguments.size() > 3 || (arguments.size() > 2 && !confirm);
        boolean isTooFew = arguments.isEmpty();
        if (!checkArguments(isTooMany, isTooFew)) {
            return;
        }

        final String playerName;
        final String posterName;
        final Player sender;
        Player playerSender;
        try {
            playerSender = playerSender();
        } catch (CommandException ignored) {
            if (arguments.size() != 2) {
                throwInvalidArgument(I.t("Player name is required from the console"));
            }
            playerSender = null;
        }

        sender = playerSender;
        boolean notPlayer = sender == null;
        if (arguments.size() == 2 || arguments.size() == 3) {
            if (!Permissions.DELETEOTHER.grantedTo(sender)) {
                throwNotAuthorized();
                return;
            }
            playerName = arguments.get(0);
            posterName = arguments.get(1);
        } else {
            playerName = sender.getName();
            posterName = arguments.get(0);
        }
        UUID uuid = getPlayerUUID(playerName);
        ImagePoster poster = PosterManager.getPoster(uuid, posterName);
        if (poster == null) {
            final String msg = "This map does not exist.";
            if (notPlayer) {
                PluginLogger.warning("" + msg);
            } else {
                warning(sender, I.t(msg));
            }

            return;
        }

        if (!confirm && !notPlayer) {
            RawText msg = deleteMsg(getClass(), playerName, poster);

            if (notPlayer) {
                PluginLogger.info("" + msg.toFormattedText());
            } else {
                RawMessage.send(sender, msg);
            }
        } else {
            if (sender != null && sender.isOnline()) {
                PosterManager.clear(sender.getInventory(), poster);
            }
            try {
                PosterManager.deletePoster(poster);
                String msg = I.t("Map successfully deleted.");
                if (sender != null) {
                    success(sender, msg);
                } else {
                    PluginLogger.info(msg);
                }
            } catch (PosterManagerException ex) {
                PluginLogger.warning(I.t("A non-existent map was requested to be deleted", ex));
                warning(sender, I.t("This map does not exist."));
            }
        }


    }

    @Override
    protected List<String> complete() throws CommandException {
        if (args.length == 1) {
            return getMatchingPosterNames(playerSender(), args[0]);
        }

        return null;
    }

    @Override
    public boolean canExecute(CommandSender sender) {
        return Permissions.DELETE.grantedTo(sender);
    }
}
