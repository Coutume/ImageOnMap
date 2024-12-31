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

package fr.moribus.imageonmap.image;

import fr.moribus.imageonmap.ImageOnMap;
import fr.moribus.imageonmap.map.PosterManager;
import fr.zcraft.quartzlib.components.events.FutureEventHandler;
import fr.zcraft.quartzlib.components.events.FutureEvents;
import fr.zcraft.quartzlib.components.events.WrappedEvent;
import fr.zcraft.quartzlib.core.QuartzLib;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.reflection.Reflection;
import fr.zcraft.quartzlib.tools.runners.RunTask;
import java.io.File;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapView;

public class PosterInitEvent implements Listener {

    public static void init() {

        QuartzLib.registerEvents(new PosterInitEvent());
        FutureEvents.registerFutureEvents(new EntitiesLoadListener());

        for (World world : Bukkit.getWorlds()) {
            for (ItemFrame frame : world.getEntitiesByClass(ItemFrame.class)) {
                initPoster(frame.getItem());
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            initPoster(player.getInventory().getItemInMainHand());
        }
    }

    public static void initPoster(ItemStack item) {
        if (item != null && item.getType() == Material.FILLED_MAP) {
            initPoster(PosterManager.getPosterIDFromItemStack(item));
        }
    }

    @Deprecated
    public static void initPoster(int id) {
        initPoster(Bukkit.getServer().getMap(id));
    }

    public static void initPoster(MapView view) {
        if (view == null) {
            return;
        }
        if (Renderer.isHandled(view)) {
            return;
        }

        File imageFile = ImageOnMap.getPlugin().getImageFile(view.getId());
        if (imageFile.isFile()) {
            ImageIOExecutor.loadImage(imageFile, Renderer.installRenderer(view));
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        RunTask.later(() -> {
            for (Entity entity : event.getChunk().getEntities()) {
                if (entity instanceof ItemFrame) {
                    initPoster(((ItemFrame) entity).getItem());
                }
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerInv(PlayerItemHeldEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        initPoster(item);
    }

    @EventHandler
    public void onPlayerPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof HumanEntity)) {
            return;
        }
        initPoster(event.getItem().getItemStack());
    }

    @EventHandler
    public void onPlayerInventoryPlace(InventoryClickEvent event) {
        switch (event.getAction()) {
            case PLACE_ALL:
            case PLACE_ONE:
            case PLACE_SOME:
            case SWAP_WITH_CURSOR:
                initPoster(event.getCursor());
                break;
            default:

        }
    }

    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        //Negate entity interaction with item frame containing IoM posters.

        Entity entity = event.getEntity();
        if (!(entity instanceof ItemFrame)) {
            return;
        }
        Entity damager = event.getDamager();
        if (damager instanceof Player) {
            //can solve the dup with the poster here by doing a better handling
            return;
        }
        ItemStack item = ((ItemFrame) entity).getItem();
        if (item.getType() == Material.FILLED_MAP) {
            //if the poster exist we canceled the event
            if (PosterManager.getPoster(item) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByBlockEvent(EntityDamageByBlockEvent event) {
        //Negate damage done to IoM posters by some blocks

        Entity entity = event.getEntity();
        if (!(entity instanceof ItemFrame)) {
            return;
        }
        ItemStack item = ((ItemFrame) entity).getItem();
        if (item.getType() == Material.FILLED_MAP) {
            //if the poster exist we canceled the event
            if (PosterManager.getPoster(item) != null) {
                switch (event.getCause()) {
                    case MAGIC:
                    case ENTITY_EXPLOSION:
                    case FIRE_TICK:
                    case LIGHTNING:
                    case CRAMMING:
                    case WITHER:
                    case SUFFOCATION:
                    case DROWNING:
                    case BLOCK_EXPLOSION:
                        event.setCancelled(true);
                        break;
                    default:
                }
            }
        }
    }


    @EventHandler
    public void onHangingBreakEvent(HangingBreakEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ItemFrame)) {
            return;
        }

        ItemStack item = ((ItemFrame) entity).getItem();
        if (item.getType() == Material.FILLED_MAP) {
            //if the poster exist we canceled the event
            if (PosterManager.getPoster(item) != null) {
                if (event.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION) {
                    //creeper goes boom
                    event.setCancelled(true);
                }
            }
        }
    }


    protected static final class EntitiesLoadListener implements Listener {
        @FutureEventHandler(event = "world.EntitiesLoadEvent")
        public void onEntitiesLoad(WrappedEvent event) {
            //New in 1.17
            //Used to make sure poster are really loaded in 1.17 on Paper (else some won't render or update properly)
            RunTask.later(() -> {
                try {
                    List<Entity> entities = (List) Reflection.call(event.getEvent(), "getEntities");
                    for (Entity entity : entities) {
                        if (entity instanceof ItemFrame) {
                            initPoster(((ItemFrame) entity).getItem());
                        }
                    }
                } catch (Exception e) {
                    PluginLogger.error(e.toString());
                    return;
                }

            }, 5L);
        }

    }
}
