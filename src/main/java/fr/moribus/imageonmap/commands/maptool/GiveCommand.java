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

import fr.moribus.imageonmap.Argument;
import fr.moribus.imageonmap.Permissions;
import fr.moribus.imageonmap.Status;
import fr.moribus.imageonmap.Type;
import fr.moribus.imageonmap.commands.IoMCommand;
import fr.moribus.imageonmap.map.ImagePoster;
import fr.moribus.imageonmap.map.PosterManager;
import fr.zcraft.quartzlib.components.commands.CommandException;
import fr.zcraft.quartzlib.components.commands.CommandInfo;
import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.tools.PluginLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


@CommandInfo(name = "give", usageParameters = "<player name> [playerFrom]:<map name> [quantity]")
public class GiveCommand extends IoMCommand {
    protected List<Argument> commandPrototype() {
        Argument target = new Argument("target", Type.STRING, Status.OBLIGATORY);
        Argument from = new Argument("from", Type.STRING, Status.OPTIONAL_FOR_PLAYER_ONLY);
        Argument posterName = new Argument("posterName", Type.STRING, Status.OBLIGATORY);
        Argument quantity = new Argument("quantity", Type.INT, Status.OPTIONAL, "1");

        List<Argument> list = new ArrayList<>();
        list.add(target);
        list.add(from);
        list.add(posterName);
        list.add(quantity);

        return list;
    }
    // /givemap Vlammar "Carte 1" 10
    // /maptool give Vlammar Vlammar:"Carte"

    @Override
    protected void run() throws CommandException {

        List<Argument> cmdProto = commandPrototype();
        ArrayList<String> arguments = getArgs();
        Boolean isPlayer = sender != null;
        List<Argument> args;
        try {
            args = Argument.parseArguments(cmdProto, arguments, isPlayer);
        } catch (Exception e) {
            PluginLogger.error("Error while parsing command");
            return;
        }


        boolean isTooMany = arguments.size() > 4;
        boolean isTooFew = arguments.size() < 2;
        if (!checkArguments(isTooMany, isTooFew)) {
            return;
        }

        final String posterName = Argument.findContent("posterName", args);
        final String from = Argument.findContent("from", args);
        final String playerName = Argument.findContent("target", args);
        final int quantity = Argument.findContent("quantity", args);

        PluginLogger.info("quantity {0}", quantity);
        if (posterName == null || from == null || playerName == null) {
            PluginLogger.error("Error one argument is null, check argument names");
            return;
        }
        final Player playerSender;
        Player playerSender1;
        try {
            playerSender1 = playerSender();
        } catch (CommandException ignored) {
            if (arguments.size() == 2) {
                throwInvalidArgument(I.t("Player name is required for the console"));
            }
            playerSender1 = null;
        }
        playerSender = playerSender1;
        /*if (arguments.size() == 2) {
            from = playerSender.getName();
            playerName = arguments.get(0);
            posterName = arguments.get(1);
        } else {
            if (arguments.size() == 3) {
                from = arguments.get(1);
                playerName = arguments.get(0);
                posterName = arguments.get(2);
            } else {
                from = "";
                playerName = "";
                posterName = "";
            }
        }*/

        final Player sender = playerSender();
        UUID uuid = getPlayerUUID(from);
        UUID uuid2 = getPlayerUUID(playerName);

        final ImagePoster poster = PosterManager.getPoster(uuid, posterName);

        if (poster == null) {
            warning(sender, I.t("This map does not exist."));
            return;
        }


        if (Bukkit.getPlayer((uuid2)) != null && Bukkit.getPlayer((uuid2)).isOnline()
                && poster.give(Bukkit.getPlayer(uuid2), quantity)) {
            info(I.t("The requested map was too big to fit in your inventory."));
            info(I.t("Use '/maptool getremaining' to get the remaining maps."));
        }

    }

    @Override
    public boolean canExecute(CommandSender sender) {
        return Permissions.GIVE.grantedTo(sender);
    }
}
