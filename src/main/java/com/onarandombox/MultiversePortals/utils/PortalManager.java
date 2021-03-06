package com.onarandombox.MultiversePortals.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.util.config.Configuration;

import com.onarandombox.MultiverseCore.MVWorld;
import com.onarandombox.MultiversePortals.MVPortal;
import com.onarandombox.MultiversePortals.MultiversePortals;
import com.onarandombox.MultiversePortals.PortalLocation;

/**
 * Manages all portals for all worlds.
 * 
 * @author fernferret
 */
public class PortalManager {
    private MultiversePortals plugin;
    private Map<String, MVPortal> portals;

    public PortalManager(MultiversePortals plugin) {
        this.plugin = plugin;
        this.portals = new HashMap<String, MVPortal>();
    }

    public MVPortal isPortal(CommandSender sender, Location l) {
        if (!this.plugin.getCore().isMVWorld(l.getWorld().getName())) {
            return null;
        }
        MVWorld world = this.plugin.getCore().getMVWorld(l.getWorld().getName());
        List<MVPortal> portalList = this.getPortals(sender, world);
        if (portalList == null || portalList.size() == 0) {
            return null;
        }
        for (MVPortal portal : portalList) {
            PortalLocation portalLoc = portal.getLocation();
            if (portalLoc.isValidLocation() && portalLoc.getRegion().containsVector(l)) {
                return portal;
            }
        }
        return null;
    }

    public boolean addPortal(MVPortal portal) {
        if (!this.portals.containsKey(portal.getName())) {
            this.portals.put(portal.getName(), portal);
            return true;
        }
        return false;
    }

    public boolean addPortal(MVWorld world, String name, String owner, PortalLocation location) {
        if (!this.portals.containsKey(name)) {
            this.portals.put(name, new MVPortal(this.plugin, name, owner, location));
            return true;
        }
        return false;
    }

    public MVPortal removePortal(String portalName, boolean removeFromConfigs) {
        if (!isPortal(portalName)) {
            return null;
        }
        if (removeFromConfigs) {
            Configuration config = this.plugin.getPortalsConfig();
            config.removeProperty("portals." + portalName);
            config.save();
        }

        MVPortal removed = this.portals.remove(portalName);
        removed.removePermission();
        Permission portalAccess = this.plugin.getServer().getPluginManager().getPermission("multiverse.portal.access.*");
        Permission exemptAccess = this.plugin.getServer().getPluginManager().getPermission("multiverse.portal.exempt.*");
        if (exemptAccess != null) {
            exemptAccess.getChildren().remove(removed.getExempt().getName());
            this.plugin.getServer().getPluginManager().recalculatePermissionDefaults(exemptAccess);
        }
        if (portalAccess != null) {
            portalAccess.getChildren().remove(removed.getPermission().getName());
            this.plugin.getServer().getPluginManager().recalculatePermissionDefaults(portalAccess);
        }
        this.plugin.getServer().getPluginManager().removePermission(removed.getPermission());
        this.plugin.getServer().getPluginManager().removePermission(removed.getExempt());
        return removed;
    }

    public List<MVPortal> getAllPortals() {
        return new ArrayList<MVPortal>(this.portals.values());
    }

    public List<MVPortal> getPortals(CommandSender sender) {
        if (!(sender instanceof Player)) {
            return this.getAllPortals();
        }
        List<MVPortal> all = this.getAllPortals();
        List<MVPortal> validItems = new ArrayList<MVPortal>();
        for (MVPortal p : all) {
            if (p.playerCanEnterPortal((Player) sender)) {
                validItems.add(p);
            }
        }
        return validItems;
    }

    private List<MVPortal> getPortals(MVWorld world) {
        List<MVPortal> all = this.getAllPortals();
        List<MVPortal> validItems = new ArrayList<MVPortal>();
        for (MVPortal p : all) {
            MVWorld portalworld = p.getLocation().getMVWorld();
            if (portalworld != null && portalworld.equals(world)) {
                validItems.add(p);
            }
        }
        return validItems;
    }

    public List<MVPortal> getPortals(CommandSender sender, MVWorld world) {
        if (!(sender instanceof Player)) {
            return this.getPortals(world);
        }
        List<MVPortal> all = this.getAllPortals();
        List<MVPortal> validItems = new ArrayList<MVPortal>();
        for (MVPortal p : all) {
            if (p.getLocation().isValidLocation() && p.getLocation().getMVWorld().equals(world) &&
                    p.playerCanEnterPortal((Player) sender)) {
                validItems.add(p);
            }
        }
        return validItems;
    }

    public MVPortal getPortal(String portalName) {
        if (this.portals.containsKey(portalName)) {
            return this.portals.get(portalName);
        }
        return null;
    }

    public MVPortal getPortal(String portalName, CommandSender sender) {
        if (!this.plugin.getCore().getPermissions().hasPermission(sender, "multiverse.portal.access." + portalName, true)) {
            return null;
        }
        return this.getPortal(portalName);
    }

    public boolean isPortal(String portalName) {
        return this.portals.containsKey(portalName);
    }

    public void removeAll(boolean removeFromConfigs) {
        List<String> iterList = new ArrayList<String>(this.portals.keySet());
        for (String s : iterList) {
            this.removePortal(s, removeFromConfigs);
        }
    }

}
