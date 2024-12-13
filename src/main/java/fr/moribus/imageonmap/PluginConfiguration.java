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

package fr.moribus.imageonmap;

import static fr.zcraft.quartzlib.components.configuration.ConfigurationItem.item;
import static fr.zcraft.quartzlib.components.configuration.ConfigurationItem.list;

import fr.zcraft.quartzlib.components.configuration.Configuration;
import fr.zcraft.quartzlib.components.configuration.ConfigurationItem;
import fr.zcraft.quartzlib.components.configuration.ConfigurationList;
import java.util.Locale;


public final class PluginConfiguration extends Configuration {
    public static final ConfigurationItem<Locale> LANG = item("lang", Locale.class);

    public static final ConfigurationItem<Boolean> COLLECT_DATA = item("collect-data", true);

    public static final ConfigurationItem<Boolean> CHECK_FOR_UPDATES = item("check-for-updates", true);

    public static final ConfigurationItem<Integer> POSTER_GLOBAL_LIMIT =
            item("map-global-limit", 0, "Limit-map-by-server");
    public static final ConfigurationItem<Integer> POSTER_PLAYER_LIMIT =
            item("map-player-limit", 0, "Limit-map-by-player");

    public static final ConfigurationItem<Boolean> SAVE_FULL_IMAGE = item("save-full-image", true);


    public static final ConfigurationItem<Integer> LIMIT_SIZE_X = item("limit-map-size-x", 0);
    public static final ConfigurationItem<Integer> LIMIT_SIZE_Y = item("limit-map-size-y", 0);

    public static final ConfigurationList<String> IMAGES_HOSTNAMES_WHITELIST =
            list("images-hostnames-whitelist", String.class);

}
