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


import fr.zcraft.quartzlib.tools.PluginLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;

public class Argument {
    //<TODO special case with the --cancel -c type arguments
    private final String name;
    private final Type type;
    private final Status status;

    private String content;
    private String defaultValue;

    public Argument(String name, Type type, Status status) {
        this.name = name;
        this.type = type;
        this.status = status;
    }

    public Argument(String name, Type type, Status status, String defaultValue) {
        this.name = name;
        this.type = type;
        this.status = status;
        this.defaultValue = defaultValue;
    }

    public static Argument findArgument(String argumentName, List<Argument> args) {
        for (Argument a : args) {
            if (a.getName().equalsIgnoreCase(argumentName)) {
                return a;
            }
        }
        PluginLogger.error("Can't find argument");
        return null;
    }
    //TODO add error message for the console and/or player

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public Status getStatus() {
        return status;
    }

    public static <T extends Object> T findContent(String argumentName, List<Argument> args) {
        Argument argument = Argument.findArgument(argumentName, args);
        if (argument != null) {
            return argument.getContent();
        }

        PluginLogger.error("Error: argument {0} was not found", argumentName);
        for (Argument a : args) {
            PluginLogger.info("argument disponible {0}", a.getName());
        }

        return argument.getDefault();
    }

    public <T extends Object> T getDefault() {
        if (defaultValue == null) {
            return null;
        }
        switch (type) {
            case BOOLEAN:
                return (T) Boolean.valueOf(defaultValue);
            case INT:
                return (T) Integer.getInteger(defaultValue);
            case UUID:
                return (T) UUID.fromString(defaultValue);
            case DOUBLE:
                return (T) Double.valueOf(defaultValue);
            case STRING:
                return (T) defaultValue;
            case ONLINE_PLAYER:
                return (T) Bukkit.getPlayer(toUUID(defaultValue));
            case OFFLINE_PLAYER:
                return (T) Bukkit.getOfflinePlayer(toUUID(defaultValue));

            default:
                PluginLogger.info("To be implemented");
                return null;
        }
    }

    public <T extends Object> T getContent() {
        switch (type) {
            case BOOLEAN:
                return (T) Boolean.valueOf(content);
            case INT:
                return (T) Integer.getInteger(content);
            case UUID:
                return (T) UUID.fromString(content);
            case DOUBLE:
                return (T) Double.valueOf(content);
            case STRING:
                return (T) content;
            case ONLINE_PLAYER:
                return (T) Bukkit.getPlayer(toUUID(content));
            case OFFLINE_PLAYER:
                return (T) Bukkit.getOfflinePlayer(toUUID(content));

            default:
                PluginLogger.info("To be implemented");
                return null;
        }
    }

    private void setEmpty() {
        this.content = "";
    }

    private void setDefault(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    private void setContent(String content) {
        //check if added content is type compatible
        String msg = "Content wasn't parsable to type '{0}', got the following string {1}";
        switch (type) {
            case BOOLEAN:
            case ONLINE_PLAYER:
            case OFFLINE_PLAYER:
            case STRING:
                this.content = content;
                break;
            case DOUBLE:
                try {
                    Double.valueOf(content);
                } catch (NumberFormatException e) {
                    PluginLogger.warning(msg,
                            "double", content);
                    throw e;
                }
                this.content = content;
                break;
            case UUID:
                try {
                    UUID.fromString(content);
                } catch (IllegalArgumentException e) {
                    PluginLogger.warning(msg,
                            "UUID", content);
                    throw e;
                }
                this.content = content;
                break;
            case INT:
                try {
                    Integer.parseInt(content);
                } catch (NumberFormatException e) {
                    PluginLogger.warning(msg,
                            "integer", content);
                    throw e;
                }
                this.content = content;
                break;
            default:
        }


    }

    private UUID toUUID(String content) {
        try {
            return UUID.fromString(content);
        } catch (IllegalArgumentException e) {
            return Bukkit.getOfflinePlayer(content).getUniqueId();
        }
    }

    public static boolean isAmbiguous(List<Argument> prototype, boolean isPlayer) {
        //TODO add check on the possible type
        for (int i = 1; i < prototype.size(); i++) {
            Argument arg = prototype.get(i);
            Argument previous = prototype.get(i - 1);
            if ((arg.getStatus() == Status.OPTIONAL && previous.getStatus() == Status.OPTIONAL)
                    || (isPlayer
                    && ((arg.getStatus() == Status.OPTIONAL_FOR_PLAYER_ONLY
                    && previous.getStatus() == Status.OPTIONAL)
                    || (arg.getStatus() == Status.OPTIONAL
                    && previous.getStatus() == Status.OPTIONAL_FOR_PLAYER_ONLY)
                    || (arg.getStatus() == Status.OPTIONAL_FOR_PLAYER_ONLY
                    && previous.getStatus() == Status.OPTIONAL_FOR_PLAYER_ONLY)))) {
                return true;
            }
        }
        return false;
    }

    public static List<Argument> parseArguments(List<Argument> prototype, ArrayList<String> args, boolean isPlayer)
            throws Exception {
        //check if the command is not ambiguous
        if (isAmbiguous(prototype, isPlayer)) {
            throw new Exception("Parsing error, ambiguous command prototype");
        }
        // givemap Vlammar Vlammar:"carte 1" 10
        // string |string| string <int>
        List<Argument> list = new ArrayList<>();
        List<Argument> uncertain = new ArrayList<>();

        for (int i = 0; i < args.size(); i++) {
            boolean next = false;
            String arg = args.get(i);
            PluginLogger.info("Argument: {0}", arg);
            for (int j = i; j < prototype.size(); j++) {
                PluginLogger.info("j = {0}", j);
                Argument a = prototype.get(j);
                PluginLogger.info("argument name: \n{0}", a.toString());
                switch (a.status) {
                    case OBLIGATORY:
                        PluginLogger.info("OBLIGATORY");
                        if (uncertain.isEmpty()) {
                            PluginLogger.info(a.getName());
                            list.add(a);
                            a.setContent(arg);
                            PluginLogger.info("argument : \n{0}", a.toString());
                            next = true;
                        } else {
                            for (Argument l : uncertain) {
                                //if size doesnt match or
                                try {
                                    PluginLogger.info("test pour l'erreur");
                                    PluginLogger.info(a.getContent());
                                    a.setContent(a.content); //todo erreur ?
                                    PluginLogger.info("argument : \n{0}", a.toString());
                                } catch (Exception e) {
                                    //shift to the right
                                }
                            }
                            PluginLogger.info(a.getName());
                            list.add(a);
                            uncertain = new ArrayList<>();
                        }
                        break;
                    case OPTIONAL:
                        PluginLogger.info("OPTIONAL");
                        PluginLogger.info(a.getName());
                        uncertain.add(a);
                        PluginLogger.info("argument : \n{0}", a.toString());
                        break;
                    case OPTIONAL_FOR_PLAYER_ONLY:
                        PluginLogger.info("OPTIONAL_FOR_PLAYER_ONLY");
                        if (!isPlayer) {
                            PluginLogger.info(a.getName());
                            list.add(a);
                            a.setContent(arg);
                            PluginLogger.info("argument : \n{0}", a.toString());
                            next = true;
                        } else {
                            PluginLogger.info(a.getName());
                            uncertain.add(a);
                            PluginLogger.info("argument : \n{0}", a.toString());
                            break;
                            //Str Str Str Int
                            //<player name> [playerFrom] [playerFrom]:<map name> <quantity >
                        }
                        break;
                    default:
                        throw new Exception("Status does not exist");
                }
                if (next) {
                    break;
                }
            }
        }
        return list;
    }

    @Override
    public String toString() {
        return "{ name: " + name + ";\ntype: " + type + ";\nstatus: " + status + ";\ncontent: " + content + " }";
    }
}

