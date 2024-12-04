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


import fr.zcraft.quartzlib.components.i18n.I;
import fr.zcraft.quartzlib.tools.PluginLogger;
import java.util.Set;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;

public enum Permissions {
    NEW("imageonmap.new", "imageonmap.userender"),
    LIST("imageonmap.list"),
    LISTOTHER("imageonmap.listother"),
    GET("imageonmap.get"),
    GETOTHER("imageonmap.getother"),
    RENAME("imageonmap.rename"),
    PLACE_SPLATTER_MAP("imageonmap.placesplattermap"),
    PLACE_INVISIBLE_SPLATTER_MAP("imageonmap.placeinvisiblesplattermap"),
    REMOVE_SPLATTER_MAP("imageonmap.removesplattermap"),
    DELETE("imageonmap.delete"),
    DELETEOTHER("imageonmap.deleteother"),
    UPDATE("imageonmap.update"),
    UPDATEOTHER("imageonmap.updateother"),
    ADMINISTRATIVE("imageonmap.administrative"),
    BYPASS_SIZE("imageonmap.bypasssize"),
    BYPASS_IMAGE_LIMIT("imageonmap.bypassimagelimit"),
    BYPASS_MAP_LIMIT("imageonmap.bypassmaplimit"),
    GIVE("imageonmap.give"),
    BYPASS_WHITELIST("imageonmap.bypasswhitelist"),
    REMOTE_PLACING("imageonmap.remoteplacing"),
    REPAIR_MAP("imageonmap.repairmap");

    private final String permission;
    private final String[] aliases;

    Permissions(String permission, String... aliases) {
        this.permission = permission;
        this.aliases = aliases;
    }

    /**
     * Checks if this permission is granted to the given permissible.
     *
     * @param permissible The permissible to check.
     * @return {@code true} if this permission is granted to the permissible.
     */
    public boolean grantedTo(Permissible permissible) {
        //true only if not a player. If the console or a command block as send the command we can assume that it has
        //enough privilege
        if (permissible == null || permissible.isOp()) {
            return true;
        }

        if (permissible.hasPermission(permission)) {
            return true;
        }

        for (String alias : aliases) {
            if (permissible.hasPermission(alias)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Return the limit of map the user is allowed to make
     *
     * @param permissible The permissible to check.
     * @return the limit
     */
    public int getLimitPermission(Permissible permissible, LimitType type) {
        Set<PermissionAttachmentInfo> perms = permissible.getEffectivePermissions();
        String prefix = String.format("imageonmap.%slimit.", type.name());
        for (PermissionAttachmentInfo pai : perms) {
            String permString = pai.getPermission().toLowerCase();
            if (permString.startsWith(prefix) && pai.getValue()) {
                try {
                    return Integer.parseInt(permString.split(prefix)[1].trim());
                } catch (Exception e) {
                    PluginLogger.warning(
                            I.t("The correct syntax for setting map limit node is: ImageOnMap.mapLimit.X "
                                    + "where you can replace X with the limit of map a player is allowed to have"));
                }
            }
        }
        return 2147483647; //Virtually no limit
    }

    public enum LimitType {
        map,
        image
    }
}