/*
 * FlyDisabler: to manage fly related things on bukkit server.
 * Copyright (C) 2013 _abc33_
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

package org.github.abc33;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AntiBlockBreaker extends JavaPlugin implements Listener{
	/* Utils */
	private static Logger log = Bukkit.getLogger();
	private List<Integer> matBlocked = new ArrayList<Integer>();
	private List<Location> tntWillBreak = new ArrayList<Location>();
	private static final String prefix = ChatColor.DARK_RED + "[" + ChatColor.GOLD + "AntiBlockBreaker" + ChatColor.DARK_RED + "] " + ChatColor.GREEN;
	
	public void onEnable() {
		log.info("[AntiBlockBreaker] Plugin author: _abc33_");
		log.info("[AntiBlockBreaker] Checking config...");
		saveDefaultConfig();
		
		/* Metrics */
		if (!getConfig().getBoolean("opt-out")){
			try {
			    Metrics metrics = new Metrics(this);
			    metrics.start();
			    log.info("[AntiBlockBreaker] Metrics integration enabled.");
			} catch (IOException e) {
			   	log.log(Level.SEVERE, "[AntiBlockBreaker] Could not reach Metrics service! Stats will not be taken.");
			}
		} else {
			log.info("[AntiBlockBreaker] Metrics is opted-out, so it will not collect stats.");
		}
		
		loadBlocks();
		
		/* Register Listeners */
		PluginManager pm = Bukkit.getServer().getPluginManager();
		pm.registerEvents(this, this);
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("antiblockbreaker") || cmd.getName().equalsIgnoreCase("abb")) {
			if (args.length > 0){
				if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("r")) {
					if (sender.hasPermission("antiblockbreaker.reload") || sender.isOp()) {
						reloadConfig();
						loadBlocks();
						sender.sendMessage(prefix + "Configuration reloaded");
					} else {
						sender.sendMessage(prefix + "You don't have permissions to do this");
					}
				} else {
					sender.sendMessage(prefix + "Command not found!");
				}
			} else {
				sender.sendMessage(prefix + "Author: _abc33_ || Version: 0.1");
			}
			return true;
		}
		return false;
	}
	
	@EventHandler
	@SuppressWarnings("deprecation")
	public void onBlockBreak(BlockBreakEvent event) {
		Player p = event.getPlayer();
		if ((this.matBlocked.contains(event.getBlock().getTypeId()) && !p.hasPermission("antiblockbreaker.exempt")) && !p.isOp()) {
			p.sendMessage(prefix + "You don't have permission to break this block!");
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	@SuppressWarnings("deprecation")
	public void onBlockExplode(EntityExplodeEvent event) {
		List<Block> brokeList = event.blockList();
		Location loc = event.getLocation();
		
		// Removes digits after point
		loc.setX(Integer.valueOf((int) loc.getX()));
		loc.setY(Integer.valueOf((int) loc.getY()));
		loc.setZ(Integer.valueOf((int) loc.getZ()));
		
		if (this.tntWillBreak.contains(loc))
			return;
		for (int i = 0; i < brokeList.size(); i++)
			if (this.matBlocked.contains(brokeList.get(i).getTypeId()))
				event.blockList().remove(brokeList.get(i--));
	}
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Block b = event.getBlock();
		Player p = event.getPlayer();
		if (b.getType() == Material.TNT && (p.hasPermission("antiblockbreaker.exempt") || p.isOp()))
			this.tntWillBreak.add(b.getLocation());
	}

	private void addBlock(int mat) {
		if (!this.matBlocked.contains(mat))
			this.matBlocked.add(mat);
	}
	
	private void loadBlocks() {
		this.matBlocked.clear();
		List<Integer> matConfig = getConfig().getIntegerList("blocked-materials");
		for (int m : matConfig)
			addBlock(Integer.valueOf(m));
	}
}
