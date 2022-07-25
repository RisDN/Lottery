package hu.ris.lottery;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;
import net.milkbowl.vault.economy.Economy;

public class Main extends JavaPlugin implements Listener {

	Economy econ = null;
	Random random;
	FileManager fs;
	public void onEnable() {
		fs = new FileManager(this, "lottery.yml");
		random = new Random();
		
		saveDefaultConfig();
		getLogger().info("Lottery indul v" + getDescription().getVersion());
		getServer().getPluginManager().registerEvents(this, this);
		
		if (!setupEconomy() ) {
			getLogger().warning("Vault nem található!!!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		
	}
	
	
	public void onDisable() {
		getLogger().info("Lottery leáll v" + getDescription().getVersion());
	}

	public void checkForRoll() {
		String currentTime =  String.valueOf(LocalDateTime.now().toLocalTime().getHour()) + ":" + String.valueOf(LocalDateTime.now().toLocalTime().getMinute());
		List<String> rollDates = getConfig().getStringList("idopontok");
		for(String date : rollDates) {
			if(date.equals(currentTime)) {
				rollWinner();
			}
		}
	}
	
	public void rollWinner() {
		List<String> joinedPlayers = fs.getConfig("lottery.yml").getStringList("beleptek");
		String winnerUUID = joinedPlayers.get(random.nextInt(joinedPlayers.size()));
		Integer prize = Integer.valueOf(getAllPrize());
		Player winner = getServer().getPlayer(UUID.fromString(winnerUUID));
		for(Player p : getServer().getOnlinePlayers()) {
			p.sendMessage(msgForm(getConfig().getString("uzenetek.nyert").replace("%nyertes%", winner.getName()).replace("%nyeremeny%", String.valueOf(prize))));
		}
		econ.depositPlayer(winner, prize);
		fs.getConfig("lottery.yml").set("beleptek", null);
		fs.saveConfig("lottery.yml");
	}
    
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!(sender instanceof Player)) {
			return true;
		}
		
		Player player = (Player) sender;
		
		
		if(args.length == 0) {
			player.sendMessage(msgForm("&d---------- &5&lLottó  &d--------"));
			player.sendMessage(msgForm("&e Parancsok: "));
			player.sendMessage(msgForm("&7 - &f/lotto"));
			player.sendMessage(msgForm("&7 - &f/lotto buy"));
			player.sendMessage(msgForm("&7 - &f/lotto info"));
			player.sendMessage(msgForm("&d-------------------------"));
			return true;
		}
		
		if(args[0].equalsIgnoreCase("info")) {
			checkForRoll();
			player.sendMessage(msgForm("&d-------------------------"));
			player.sendMessage(msgForm(getConfig().getString("uzenetek.info_varhato").replace("%varhato_fny%", getAllPrize())));
			player.sendMessage(msgForm(getConfig().getString("uzenetek.info_sorsolas").replace("%sorsolas%", getRollTime())));
			player.sendMessage(msgForm("&d-------------------------"));
			return true;
		}
		
		if(args[0].equalsIgnoreCase("buy")) {
			List<String> joinedPlayers = fs.getConfig("lottery.yml").getStringList("beleptek");
			int numberOfPlayersTickets = Collections.frequency(joinedPlayers, String.valueOf(player.getUniqueId()));
			
			if(numberOfPlayersTickets >= getConfig().getInt("max")) {
				player.sendMessage(msgForm(getConfig().getString("uzenetek.max")));
				return true;
			}
			double balance = econ.getBalance(player);
			double price = getConfig().getInt("price");
			
			if(balance < price) {
				double diff = price - balance;
				player.sendMessage(msgForm(getConfig().getString("uzenetek.nincselegendo").replace("%szukseges%", String.valueOf(diff))));
				return true;
			}
			
			econ.withdrawPlayer(player, price);


			joinedPlayers.add(String.valueOf(player.getUniqueId()));
			fs.getConfig("lottery.yml").set("beleptek", joinedPlayers);
			
			fs.saveConfig("lottery.yml");
			
			player.sendMessage(msgForm(getConfig().getString("uzenetek.sikeres").replace("%ar%", String.valueOf(price))));
			for(Player p : getServer().getOnlinePlayers()) {
				p.sendMessage(msgForm(getConfig().getString("uzenetek.vasarolt").replace("%kicsoda%", player.getName())));
			}
			
		}
		
	
		
		return true;
	}
	
	public String msgForm(String message) {
		return ChatColor.translateAlternateColorCodes('&', message
		.replace("%max%", String.valueOf(getConfig().getInt("max")))
		.replace("%prefix%", getConfig().getString("prefix")));
	}
	
	public String getRollTime() {
		
		return "test";
	}
	
	public String getAllPrize() {
		List<String> joinedPlayers = fs.getConfig("lottery.yml").getStringList("beleptek");
		return String.valueOf(joinedPlayers.size()*getConfig().getInt("price"));
	}
	
	private boolean setupEconomy() {
		if (getServer().getPluginManager().getPlugin("Vault") == null) {
			return false;
		}
		RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
		if (rsp == null) {
			return false;
		}
		econ = rsp.getProvider();
			return econ != null;
		}
	

}
