package com.gmail.falistos.KillRewards;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class EventListener implements Listener {
	
	private KillRewards plugin;

	public EventListener(KillRewards plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler(ignoreCancelled=true)
	public void onCreatureEntitySpawn(CreatureSpawnEvent event) {
		if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
			this.plugin.spawnerEntities.add(event.getEntity().getUniqueId());
		}
	}
	
	@EventHandler(ignoreCancelled=true)
	public void onEntityDeath(EntityDeathEvent event) {
		Player killer = event.getEntity().getKiller();
		if (killer == null) return;
		if (!(killer instanceof Player)) return;
		
		LivingEntity victim = event.getEntity();
		String section = victim.getType().toString();

		if (!killer.hasPermission("killrewards.earn")) return;
		if (!this.plugin.getConfig().getBoolean("rewards." + section + ".enabled")) return;
		
		if (this.plugin.isFromSpawner(victim.getUniqueId())) {
			this.plugin.spawnerEntities.remove(victim.getUniqueId());
			if (!this.plugin.getConfig().getBoolean("spawners.enable")) return;
		}
		
		double reward = 0;
  
		String victimName = null;
		String message = null;
		
		if ((victim instanceof Player)) {
			Player victimPlayer = (Player)victim;
    
			victimName = victimPlayer.getDisplayName();
    
			double percentageTransfert = this.plugin.getConfig().getDouble("rewards.player.percentageTransfert");
			reward = this.plugin.economy.getBalance(victimPlayer) * percentageTransfert;
			reward = MathUtils.round(reward, 2);
			
			if (reward > this.plugin.getConfig().getDouble("rewards.player.maximumTransfert")) {
				reward = this.plugin.getConfig().getDouble("rewards.player.maximumTransfert");
			}
			this.plugin.economy.withdrawPlayer(victimPlayer, reward);
			
			String victimMessage = this.plugin.getConfig().getString("rewards.player.victimMessage");
			if (victimMessage != null && !victimMessage.isEmpty()) {
				this.plugin.sendMessage(victimPlayer, killer.getDisplayName(), victimName, victimMessage, Double.valueOf(reward));
			}
		}
		else {
			victimName = this.plugin.getConfig().getString("rewards." + section + ".name");
			
			if ((victim.getCustomName() != null) && (this.plugin.getConfig().isConfigurationSection("rewards." + ChatColor.stripColor(victim.getCustomName())))) {
				victimName = victim.getCustomName();
				section = ChatColor.stripColor(victim.getCustomName());
			}
			
			double minReward = this.plugin.getConfig().getDouble("rewards." + section + ".minReward");
			double maxReward = this.plugin.getConfig().getDouble("rewards." + section + ".maxReward");
    
			double random = new Random().nextDouble();
			reward = minReward + random * (maxReward - minReward);
			
			if (this.plugin.getConfig().getDouble("multipliers.global") != 0.0D) {
				reward *= this.plugin.getConfig().getDouble("multipliers.global");
			}
			
			reward *= this.plugin.getPermissionMultiplier(killer);
			
			if ((this.plugin.isFromSpawner(victim.getUniqueId())) && (this.plugin.getConfig().getBoolean("multipliers.spawner.enable"))) {
				reward *= this.plugin.getConfig().getDouble("multipliers.spawner.multiplier");
      
				int droppedExp = (int)(event.getDroppedExp() * this.plugin.getConfig().getDouble("multipliers.spawner.experience-multiplier"));
				if (droppedExp <= 0) {
					droppedExp = 1;
				}
				event.setDroppedExp(droppedExp);
			}
			reward = MathUtils.round(reward, 2);
		}
		
		if (this.plugin.getConfig().getString("rewards." + section + ".killerMessage") != null) {
			message = this.plugin.getConfig().getString("rewards." + section + ".killerMessage");
		}
		else message = this.plugin.getConfig().getString("rewards.default.killerMessage");
		
		this.plugin.economy.depositPlayer(killer, reward);
		
		if (message != null && !message.isEmpty()) {
			this.plugin.sendMessage(killer, killer.getDisplayName(), victimName, message, Double.valueOf(reward));
		}
	}
	
}