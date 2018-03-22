/*
 * Copyright (C) 2013 Moribus
 * Copyright (C) 2015 ProkopyL <prokopylmc@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.moribus.imageonmap.commands.maptool;


import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import fr.moribus.imageonmap.commands.IoMCommand;
import fr.moribus.imageonmap.map.ImageMap;
import fr.moribus.imageonmap.map.MapManager;
import fr.moribus.imageonmap.map.PosterMap;
import fr.zcraft.zlib.components.commands.CommandException;
import fr.zcraft.zlib.components.commands.CommandInfo;
import fr.zcraft.zlib.components.i18n.I;
import fr.zcraft.zlib.components.rawtext.RawText;
import fr.zcraft.zlib.components.rawtext.RawTextPart;
import fr.zcraft.zlib.tools.items.ItemStackBuilder;
import fr.zcraft.zlib.tools.text.RawMessage;


@CommandInfo (name = "listother", usageParameters = "<PlayerName>")
public class ListOtherCommand extends IoMCommand
{
	@Override
    protected void run() throws CommandException
    {
		if(!playerSender().hasPermission("imageonmap.list.other")) {
    		warning(I.t("You do not have permission for this command. (imageonmap.list.other)"));
    		return;
    	}
		
		Player player = null;
		UUID uuid = null;
        player = Bukkit.getPlayer(args[0]);
        if(player == null){
        	OfflinePlayer op = Bukkit.getOfflinePlayer(args[0]);
			if(op.hasPlayedBefore()) {
				uuid = op.getUniqueId();
			}
			else {
				warning(I.t("We've never seen that player before!"));
				
			}
        }
        else{
        	 uuid = player.getUniqueId();
        }
       
        List<ImageMap> mapList = null;
        try{
        	mapList = MapManager.getMapList(uuid);
        }
        catch(Exception e){
        	
        }
        if(mapList.isEmpty())
        {
            info(I.t("No map found."));
            return;
        }
        
        info(I.tn("{white}{bold}{0} map found.", "{white}{bold}{0} maps found.", mapList.size()));

        RawTextPart rawText = new RawText("");
        rawText = addMap(rawText, mapList.get(0));

        for(int i = 1, c = mapList.size(); i < c; i++)
        {
            rawText = rawText.then(", ").color(ChatColor.GRAY);
            rawText = addMap(rawText, mapList.get(i));
        }

        RawMessage.send(playerSender(), rawText.build());
    }

    private RawTextPart<?> addMap(RawTextPart<?> rawText, ImageMap map)
    {
        final String size = map.getType() == ImageMap.Type.SINGLE ? "1 × 1" : ((PosterMap) map).getColumnCount() + " × " + ((PosterMap) map).getRowCount();

        return rawText
                .then(map.getId())
                .color(ChatColor.WHITE)
                .command(GetCommand.class, map.getId())
                .hover(new ItemStackBuilder(Material.MAP)
                                .title(ChatColor.GREEN + "" + ChatColor.BOLD + map.getName())
                                .lore(ChatColor.GRAY + map.getId() + ", " + size)
                                .lore("")
                                .lore(I.t("{white}Click{gray} to get this map"))
                                .hideAttributes()
                                .item()
                );
    }
}
