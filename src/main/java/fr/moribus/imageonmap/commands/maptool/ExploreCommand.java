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
import fr.moribus.imageonmap.gui.PosterListGui;
import fr.zcraft.quartzlib.components.commands.CommandException;
import fr.zcraft.quartzlib.components.commands.CommandInfo;
import fr.zcraft.quartzlib.components.gui.Gui;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


@CommandInfo(name = "explore", usageParameters = "[player name]")
public class ExploreCommand extends IoMCommand {
    @Override
    protected void run() throws CommandException {
        ArrayList<String> arguments = getArgs();
        boolean isTooMany = arguments.size() > 1;
        boolean isTooFew = false;
        if (!checkArguments(isTooMany, isTooFew)) {
            return;
        }
        final String playerName;
        final Player sender = playerSender();

        if (arguments.size() == 1) {
            if (!Permissions.LISTOTHER.grantedTo(sender)) {
                throwNotAuthorized();
                return;
            }
            playerName = arguments.get(0);
        } else {
            playerName = sender.getName();
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        //TODO replace with homemade solution using https://api.mojang.com/users/profiles/minecraft/playername
        // to get the UUId
        if (sender.isOnline()) {
            Gui.open(sender, new PosterListGui(offlinePlayer, playerName));
        }

    }

    @Override
    public boolean canExecute(CommandSender sender) {
        return Permissions.LIST.grantedTo(sender);
    }
}
