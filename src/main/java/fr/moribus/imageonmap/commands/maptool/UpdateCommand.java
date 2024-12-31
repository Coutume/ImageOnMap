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
import fr.moribus.imageonmap.image.ImageRendererExecutor;
import fr.moribus.imageonmap.image.ImageUtils;
import fr.moribus.imageonmap.map.ImagePoster;
import fr.moribus.imageonmap.map.PosterManager;
import fr.zcraft.quartzlib.components.commands.CommandException;
import fr.zcraft.quartzlib.components.commands.CommandInfo;
import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.components.worker.WorkerCallback;
import fr.zcraft.quartzlib.tools.PluginLogger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandInfo(name = "update", usageParameters = "[player name]:<map name> <new url> [stretched|covered] ")
public class UpdateCommand extends IoMCommand {
    @Override
    protected void run() throws CommandException {
        //TODO fix the issue where to many quick usage of offlineNameFetch will return null
        ArrayList<String> arguments = getArgs();

        boolean isTooMany = arguments.size() > 4;
        boolean isTooFew = arguments.size() < 2;
        if (!checkArguments(isTooMany, isTooFew)) {
            return;
        }
        final String playerName;
        final String posterName;
        final String url;
        final String resize;
        final Player playerSender;
        Player playerSender1;
        try {
            playerSender1 = playerSender();
        } catch (CommandException ignored) {
            if (arguments.size() == 2) {
                throwInvalidArgument(
                        I.t("Usage: /maptool update [player name]:<map name> <new url> [stretched|covered]"));
            }
            playerSender1 = null;
        }
        playerSender = playerSender1;

        //Sent by a non player and not enough arguments
        if (arguments.size() == 2 && playerSender == null) {
            throwInvalidArgument("Usage: /maptool update [player name]:<map name> <new url> [stretched|covered]");
            return;
        }

        if (arguments.size() == 2) {
            resize = "";
            playerName = playerSender.getName();
            posterName = arguments.get(0);
            url = arguments.get(1);
        } else {
            if (arguments.size() == 4) {
                if (!Permissions.UPDATEOTHER.grantedTo(sender)) {
                    throwNotAuthorized();
                    return;
                }
                playerName = arguments.get(0);
                posterName = arguments.get(1);
                url = arguments.get(2);
                resize = arguments.get(3);
            } else {
                if (arguments.size() == 3) {
                    if (arguments.get(2).equals("covered") || arguments.get(2).equals("stretched")) {
                        playerName = playerSender.getName();
                        posterName = arguments.get(0);
                        url = arguments.get(1);
                        resize = arguments.get(2);
                    } else {
                        if (!Permissions.UPDATEOTHER.grantedTo(sender)) {
                            throwNotAuthorized();
                            return;
                        }
                        playerName = arguments.get(0);
                        posterName = arguments.get(1);
                        url = arguments.get(2);
                        resize = "";
                    }
                } else {
                    resize = "";
                    playerName = "";
                    url = "";
                    posterName = "";
                }
            }
        }

        final ImageUtils.ScalingType scaling = ImageUtils.scalingTypeFromName(resize);//TODO test if nothing broke
        // because I went from 3 to 4 by adding the none as default instead of the contained one.

        UUID uuid = getPlayerUUID(playerName);
        ImagePoster poster = PosterManager.getPoster(uuid, posterName);

        if (poster == null) {
            warning(sender, I.t("This map does not exist."));
            return;
        }

        URL url1;
        try {
            url1 = new URL(url);
            if (!Permissions.BYPASS_WHITELIST.grantedTo(playerSender) && !checkHostnameWhitelist(url1)) {
                throwInvalidArgument(I.t("This hosting website is not trusted, if you think that this is an error "
                        + " contact your server administrator"));
                return;
            }

            //TODO replace by a check of the load status.(if not loaded load the mapmanager)
            PosterManager.load(false);//we don't want to spam the console each time we reload the mapManager

            Integer[] size = {1, 1};
            if (poster.getType() == ImagePoster.Type.POSTER) {
                size = poster.getSize(poster.getUserUUID(), poster.getId());
            }

            if (size.length == 0) {
                size = new Integer[] {1, 1};
            }
            int width = size[0];
            int height = size[1];
            try {
                String msg = I.t("Updating...");
                if (playerSender != null) {
                    //TODO test if player human
                    String message = I.t("&2 Updating...");
                    TextComponent text = new TextComponent(message);
                    playerSender.spigot().sendMessage(ChatMessageType.ACTION_BAR, text);
                } else {
                    PluginLogger.info(msg);
                }
                ImageRendererExecutor
                        .update(url1, scaling, uuid, poster, width, height, new WorkerCallback<ImagePoster>() {
                            @Override
                            public void finished(ImagePoster result) {
                                String msg = I.t("The map was updated using the new image!");
                                if (playerSender != null) {
                                    String message = I.t("&2 The map was updated using the new image!");
                                    TextComponent text = new TextComponent(message);
                                    playerSender.spigot().sendMessage(ChatMessageType.ACTION_BAR, text);
                                } else {
                                    PluginLogger.info(msg);
                                }
                            }

                            @Override
                            public void errored(Throwable exception) {
                                if (playerSender != null) {
                                    String message = I.t("&C Map rendering failed: {0}", exception.getMessage());
                                    TextComponent text = new TextComponent(message);
                                    playerSender.spigot().sendMessage(ChatMessageType.CHAT, text);
                                }
                                PluginLogger.warning("Rendering from {0} failed: {1}: {2}",
                                        playerSender.getName(),
                                        exception.getClass().getCanonicalName(),
                                        exception.getMessage());
                            }
                        });
            } finally {
                if (playerSender != null) {
                    playerSender.spigot().sendMessage(ChatMessageType.ACTION_BAR,new TextComponent(""));
                }
            }
        } catch (MalformedURLException | CommandException ex) {
            warning(sender, I.t("Invalid URL."));
        }


    }

    @Override
    public boolean canExecute(CommandSender sender) {
        return Permissions.UPDATE.grantedTo(sender);
    }
}
