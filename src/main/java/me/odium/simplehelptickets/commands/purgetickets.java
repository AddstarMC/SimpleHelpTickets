package me.odium.simplehelptickets.commands;

import java.sql.ResultSet;
import java.util.Objects;

import me.odium.simplehelptickets.SimpleHelpTickets;

import me.odium.simplehelptickets.utilities.Utilities;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class purgetickets implements CommandExecutor {

	private final SimpleHelpTickets plugin;

	public purgetickets(SimpleHelpTickets plugin) {
		this.plugin = plugin;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;

			if (!player.hasPermission("sht.purgetickets")) {
				sender.sendMessage(ChatColor.RED + "You do not have permission to use this command");
				return true;
			}

		}

		// Use the command name to determine if we are working with a ticket or an idea
		String targetTable = Utilities.GetTargetTableName(label);
		String itemNamePlural = Utilities.GetTargetItemName(targetTable) + "s";

		if (args.length == 0) {
			// PURGE EXPIRED TICKETS OR IDEAS

			int expiredItems;
			if (Objects.equals(targetTable, Utilities.IDEA_TABLE_NAME))
				expiredItems= plugin.expireIdeas();
			else
				expiredItems= plugin.expireTickets();

			sender.sendMessage(plugin.GRAY + "[Tickets] " + ChatColor.GOLD + expiredItems + ChatColor.WHITE + " expired " + Utilities.CheckPlural(itemNamePlural, expiredItems) + " purged");

			return true;
		} else if (args.length == 1 && args[0].equalsIgnoreCase("-c")) {
			sender.sendMessage(plugin.GRAY + "[Tickets] " + ChatColor.GOLD + "This will delete all CLOSED " + itemNamePlural + "!\n To confirm, use " + ChatColor.GREEN + "/" + itemNamePlural + " -c confirm");
			return true;
		} else if (args.length == 1 && args[0].equalsIgnoreCase("-a")) {
			sender.sendMessage(plugin.GRAY + "[Tickets] " + ChatColor.GOLD + "This will delete ALL " + itemNamePlural + "!\n To confirm, use " + ChatColor.GREEN + "/" + itemNamePlural + " -a confirm");
			return true;
		} else if (args.length == 2 && args[0].equalsIgnoreCase("-c") && args[1].equalsIgnoreCase("confirm")) {
			// PURGE CLOSED TICKETS OR IDEAS
			try {
				con = plugin.databaseService.getConnection();
				stmt = con.createStatement();
				stmt.executeUpdate("DELETE FROM " + targetTable + " WHERE status='" + "CLOSED" + "'");

				String messageName;
				if (Objects.equals(targetTable, Utilities.IDEA_TABLE_NAME))
					messageName = "AllClosedIdeasPurged";
				else
					messageName = "AllClosedTicketsPurged";
				sender.sendMessage(plugin.getMessage(messageName));

			} catch (Exception e) {
				sender.sendMessage(plugin.getMessage("Error").replace("&arg", e.toString()));
			} finally {
				closeticket.closeResources(rs, stmt);
			}
			return true;

		} else if (args.length == 2 && args[0].equalsIgnoreCase("-a") && args[1].equalsIgnoreCase("confirm")) {
			// PURGE ALL TICKETS OR IDEAS
			try {
				con = plugin.databaseService.getConnection();
				stmt = con.createStatement();
				if (plugin.getConfig().getBoolean("MySQL.USE_MYSQL")) {
					stmt.executeUpdate("TRUNCATE " + targetTable);
				} else {
					stmt.executeUpdate("DELETE FROM " + targetTable);
				}

				String messageName;
				if (Objects.equals(targetTable, Utilities.IDEA_TABLE_NAME))
					messageName = "AllIdeasPurged";
				else
					messageName = "AllTicketsPurged";
				sender.sendMessage(plugin.getMessage(messageName));

			} catch (Exception e) {
				plugin.log.info("[Tickets] " + "Error: " + e);
			} finally {
				closeticket.closeResources(rs, stmt);
			}
			return true;
		} else {
			sender.sendMessage(ChatColor.WHITE + "/purge" + itemNamePlural + " [-c|-a]");
			return true;
		}

	}
}
