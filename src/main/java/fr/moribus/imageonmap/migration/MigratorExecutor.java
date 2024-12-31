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

package fr.moribus.imageonmap.migration;

import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.tools.PluginLogger;

//TODO need to support new render folder and write on maps automatically
public class MigratorExecutor {
    private static Thread migratorThread;

    public static void migrate() {
        if (isRunning()) {
            PluginLogger.error(I.t("Migration is already running."));
            return;
        }
        //migratorThread = new Thread(new V3Migrator(ImageOnMap.getPlugin()), "ImageOnMap-Migration");
        migratorThread.start();
    }

    public static boolean isRunning() {
        return migratorThread != null && migratorThread.isAlive();
    }

    public static void waitForMigration() {
        if (isRunning()) {
            PluginLogger.info(I.t("Waiting for migration to finish..."));

            try {
                migratorThread.join();
            } catch (InterruptedException ex) {
                PluginLogger.error(I.t(
                        "Migration thread has been interrupted while waiting to finish."
                                + " It may not have ended correctly."));
            }
        }
    }
}
