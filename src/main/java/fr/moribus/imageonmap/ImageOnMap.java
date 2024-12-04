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

package fr.moribus.imageonmap;


import fr.moribus.imageonmap.commands.maptool.DeleteCommand;
import fr.moribus.imageonmap.commands.maptool.ExploreCommand;
import fr.moribus.imageonmap.commands.maptool.GetCommand;
import fr.moribus.imageonmap.commands.maptool.GetRemainingCommand;
import fr.moribus.imageonmap.commands.maptool.GiveCommand;
import fr.moribus.imageonmap.commands.maptool.ListCommand;
import fr.moribus.imageonmap.commands.maptool.NewCommand;
import fr.moribus.imageonmap.commands.maptool.RemotePlacingCommand;
import fr.moribus.imageonmap.commands.maptool.RenameCommand;
import fr.moribus.imageonmap.commands.maptool.UpdateCommand;
import fr.moribus.imageonmap.image.ImageIOExecutor;
import fr.moribus.imageonmap.image.ImageRendererExecutor;
import fr.moribus.imageonmap.image.MapInitEvent;
import fr.moribus.imageonmap.map.MapManager;
import fr.moribus.imageonmap.ui.MapItemManager;
import fr.zcraft.quartzlib.components.commands.CommandWorkers;
import fr.zcraft.quartzlib.components.commands.Commands;
import fr.zcraft.quartzlib.components.gui.Gui;
import fr.zcraft.quartzlib.components.i18n.I18n;
import fr.zcraft.quartzlib.core.QuartzPlugin;
import fr.zcraft.quartzlib.tools.PluginLogger;
import fr.zcraft.quartzlib.tools.UpdateChecker;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.bstats.bukkit.Metrics;

//TODO ERROR when loading plugin org.bukkit.plugin.InvalidPluginException: java.lang.NoClassDefFoundError:
// fr/zcraft/imageonmap/quartzlib/core/QuartzPlugin
//TODO list: bug sur supprimer map si coin manquant, passer en position au lieu de frame;
// repair fonction permet avec la carte en main si clic droit de réparer (permission);
// replace IG si clic droit alors remplacer le dessin si meme taille sinon mettre un message erreur;
// bug: pas de message d'erreur si pas assez de place;
// finir déploiement plus efficace des cartes;
// bug rotation des frames au sol et plafond (faire un fix mieux que juste mettre x rotation);
// continuer rework des commandes;
// GUI permettre de changer objets utilisés et ajouter ressource pack;
// rework des GUI (map part utile ???);
// Pouvoir déployer carte à distance;
// carte interractive (5.1?);
// bug: hover text ne marche plus;
// glow effect a refaire (vraiment utile ???);
// Miniature des maps en 1x1 (migration?);
// ecrire directement sur les cartes pour ne pas tout perdre(migration);
// réduire dépendance QL;
// retirer les prints de QL et IoM => verbose ou debug pour avoir une trace de ce qui est fait ?;
// bug: suppression image chat (inconnue ?) laisse item frame invisible;
// passer invisibiliter et glow dans le GUI si permission sinon solution RP avec le nombre qu'il faut de glowsac
// avec un clic droit sur les frames et fenetre en verre pour la protection contre le grief des mobs et potion
// invisibilité pour rendre frame invisible (peut etre que l'item utilisé pour le craft); proposer craft RP des cartes
// soit avec un item map vide+ un item de peintre ? soit avec un atelier?
// soit avec certains objets simple dans inventaire
public final class ImageOnMap extends QuartzPlugin {
    private static final String IMAGES_DIRECTORY_NAME = "images";
    private static final String RENDERS_DIRECTORY_NAME = "renders";
    private static final String MAPS_DIRECTORY_NAME = "maps";
    private static ImageOnMap plugin;
    private File imagesDirectory;

    private File rendersDirectory;
    private File mapsDirectory;
    private CommandWorkers commandWorker;

    public ImageOnMap() {
        imagesDirectory = new File(this.getDataFolder(), IMAGES_DIRECTORY_NAME);
        rendersDirectory = new File(this.getDataFolder(), RENDERS_DIRECTORY_NAME);
        mapsDirectory = new File(this.getDataFolder(), MAPS_DIRECTORY_NAME);
        plugin = this;
    }

    public static ImageOnMap getPlugin() {
        return plugin;
    }

    public File getImagesDirectory() {
        return imagesDirectory;
    }

    public File getRendersDirectory() {
        return rendersDirectory;
    }

    public File getMapsDirectory() {
        return mapsDirectory;
    }

    public File getImageFile(int mapID) {
        return new File(imagesDirectory, "map" + mapID + ".png");
    }

    public File getRenderFile(int mapID) {
        return new File(rendersDirectory, "render" + mapID + ".png");
    }

    public CommandWorkers getCommandWorker() {
        return commandWorker;
    }

    private Map<String, File> checkDirs() throws IOException {
        Map<String, File> dirs = new HashMap<>();
        dirs.put("mapsDirectory", checkPluginDirectory(mapsDirectory));
        dirs.put("rendersDirectory", checkPluginDirectory(rendersDirectory));
        dirs.put("imagesDirectory", checkPluginDirectory(imagesDirectory));
        return dirs;
    }

    @Override
    public void onEnable() {
        // Creating the images and maps directories if necessary
        try {
            Map<String, File> directories = checkDirs();
            mapsDirectory = directories.get("mapsDirectory");
            rendersDirectory = directories.get("rendersDirectory");
            imagesDirectory = directories.get("imagesDirectory");
        } catch (final IOException ex) {
            PluginLogger.error("FATAL: " + ex.getMessage());
            //disable the plugin
            this.setEnabled(false);
            return;
        }


        saveDefaultConfig();
        commandWorker = loadComponent(CommandWorkers.class);
        loadComponents(I18n.class, Gui.class, Commands.class, PluginConfiguration.class, ImageIOExecutor.class,
                ImageRendererExecutor.class);

        //Init all the things !
        I18n.setPrimaryLocale(PluginConfiguration.LANG.get());

        MapManager.init();
        MapInitEvent.init();
        MapItemManager.init();

        String commandGroupName = "maptool";
        Commands.register(
                commandGroupName,
                NewCommand.class,
                ListCommand.class,
                GetCommand.class,
                RenameCommand.class,
                DeleteCommand.class,
                GiveCommand.class,
                GetRemainingCommand.class,
                ExploreCommand.class,
                //MigrateCommand.class,//Removed for now doesn't work nor is useful, maybe useful later on
                UpdateCommand.class,
                RemotePlacingCommand.class
        );

        Commands.registerShortcut(commandGroupName, NewCommand.class, "tomap");
        Commands.registerShortcut(commandGroupName, ExploreCommand.class, "maps");
        Commands.registerShortcut(commandGroupName, GiveCommand.class, "givemap");
        Commands.registerShortcut(commandGroupName, RemotePlacingCommand.class, "placemap");

        if (PluginConfiguration.CHECK_FOR_UPDATES.get()) {
            UpdateChecker.boot("imageonmap.26585");
        }

        if (PluginConfiguration.COLLECT_DATA.get()) {
            final Metrics metrics = new Metrics(this, 5920);
            metrics.addCustomChart(new Metrics.SingleLineChart("rendered-images", MapManager::getImagesCount));
            metrics.addCustomChart(new Metrics.SingleLineChart("used-minecraft-maps", MapManager::getMapCount));
        } else {
            PluginLogger.warning("Collect data disabled");
        }
    }

    @Override
    public void onDisable() {
        MapManager.exit();
        MapItemManager.exit();
        //MigratorExecutor.waitForMigration();//Removed for now doesn't work nor is useful, maybe useful later on

        super.onDisable();
    }

    private File checkPluginDirectory(File primaryFile, File... alternateFiles) throws IOException {
        if (primaryFile.exists()) {
            return primaryFile;
        }
        for (File file : alternateFiles) {
            if (file.exists()) {
                return file;
            }
        }
        if (!primaryFile.mkdirs()) {
            throw new IOException("Could not create '" + primaryFile.getName() + "' plugin directory.");
        }
        return primaryFile;
    }
}
