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
import fr.moribus.imageonmap.map.PlayerPosterStore;
import fr.moribus.imageonmap.map.PosterManager;
import fr.moribus.imageonmap.map.PosterMap;
import fr.zcraft.quartzlib.components.commands.CommandException;
import fr.zcraft.quartzlib.components.commands.CommandInfo;
import fr.zcraft.quartzlib.components.i18n.I;
import java.util.ArrayList;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ItemFrame;

@CommandInfo(name = "RemotePlacing", usageParameters = "[player name]:mapName worldname x y z [N|W|S|E]")
public class RemotePlacingCommand extends IoMCommand {
    @Override
    protected void run() throws CommandException {
        ArrayList<String> arguments = getArgs();
        String playerName;
        String mapName;
        if (arguments.get(1).contains(":")) {
            playerName = arguments.get(1).split(":")[0];
            mapName = arguments.get(1).split(":")[1];
        } else {
            playerName = playerSender().getName();
            mapName = arguments.get(1);
        }
        UUID uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        ImagePoster poster = PosterManager.getplayerPosterStore(uuid).getPoster(mapName);

        String worldName = arguments.get(1);
        World world = Bukkit.getWorld(worldName);
        Location loc = new Location(world,
                Integer.parseInt(arguments.get(2)),
                Integer.parseInt(arguments.get(3)),
                Integer.parseInt(arguments.get(4)));
        BlockFace bf = null;
        //TODO add ground placement and bf for ground ceilling and wall
        switch (arguments.get(5)) {
            case "N":
            case "n":
            case "North":
            case "north":
                bf = BlockFace.NORTH;
                break;
            case "E":
            case "e":
            case "East":
            case "east":
                bf = BlockFace.EAST;
                break;
            case "W":
            case "w":
            case "West":
            case "west":
                bf = BlockFace.WEST;
                break;
            case "S":
            case "s":
            case "South":
            case "south":
                bf = BlockFace.SOUTH;
                break;
            default:
                //or messagesender
                throwInvalidArgument(I.t("Must specify a valid rotation N|W|S|E|"));
                break;
        }
        ItemFrame i = world.spawn(loc, ItemFrame.class);
        i.setFacingDirection(bf);
        world.getBlockAt(loc);

        //summon item frame(location, rotation)
        //if wall => need position and direction N/S/E/W
        //else if floor or ceiling => same + rotation
    }

    @Override
    public boolean canExecute(CommandSender sender) {
        return Permissions.REMOTE_PLACING.grantedTo(sender);
    }

}
