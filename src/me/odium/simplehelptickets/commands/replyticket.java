package me.odium.simplehelptickets.commands;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import me.odium.simplehelptickets.DBConnection;
import me.odium.simplehelptickets.SimpleHelpTickets;

import me.odium.simplehelptickets.Utilities;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class replyticket implements CommandExecutor {

	public SimpleHelpTickets plugin;

	public replyticket(SimpleHelpTickets plugin) {
		this.plugin = plugin;
	}

	DBConnection service = DBConnection.getInstance();
	ResultSet rs = null;
	java.sql.Statement stmt = null;
	Connection con = null;

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
		}

		// Use the command name to determine if we are working with a ticket or an idea
		String targetTable = Utilities.GetTargetTableName(label, Arrays.asList("replyidea", "ridea"));
		String itemName = Utilities.GetTargetItemName(targetTable);

		if (args.length <= 1) {
			sender.sendMessage("/reply" + itemName + " <#> <reply>");
			return true;
		} else if (args.length > 1) {

			String messageName;
			String notExistMessageName;
			if (targetTable == Utilities.IDEA_TABLE_NAME) {
				messageName = "InvalidIdeaNumber";
				notExistMessageName = "IdeaNotExist";
			} else {
				messageName = "InvalidTicketNumber";
				notExistMessageName = "TicketNotExist";
			}

			for (char c : args[0].toCharArray()) {
				if (!Character.isDigit(c)) {
					sender.sendMessage(plugin.getMessage(messageName).replace("&arg", args[0]));
					return true;
				}
			}

			StringBuilder sb = new StringBuilder();
			for (String arg : args)
				sb.append(arg + " ");
			String[] temp = sb.toString().split(" ");
			String[] temp2 = Arrays.copyOfRange(temp, 1, temp.length);
			sb.delete(0, sb.length());
			for (String details : temp2) {
				sb.append(details);
				sb.append(" ");
			}
			String details = sb.toString().replace("'", "''");
			String id = args[0];

			try {
				if (plugin.getConfig().getBoolean("MySQL.USE_MYSQL")) {
					con = plugin.mysql.getConnection();
				} else {
					con = service.getConnection();
				}
				stmt = con.createStatement();

				if (player == null) {
					// CONSOLE COMMANDS
					String admin = "CONSOLE";
					// CHECK IF TICKET EXISTS
					rs = stmt.executeQuery("SELECT COUNT(id) AS ticketTotal FROM " + targetTable + " WHERE id='" + id + "'");
					if (plugin.getConfig().getBoolean("MySQL.USE_MYSQL")) {
						rs.next(); // sets pointer to first record in result set
					}
					if (rs.getInt("ticketTotal") == 0) {
						sender.sendMessage(plugin.getMessage(notExistMessageName).replace("&arg", args[0]));
						return true;
					}
					stmt.executeUpdate("UPDATE " + targetTable + " SET adminreply='" + admin + ": " + details + "', admin='" + admin + "' WHERE id='" + id + "'");

					NotifyReplied(sender, id, targetTable);

					try {
						if (plugin.getConfig().getBoolean("MySQL.USE_MYSQL")) {
							con = plugin.mysql.getConnection();
						} else {
							con = service.getConnection();
						}
						stmt = con.createStatement();

						rs = stmt.executeQuery("SELECT * FROM " + targetTable + " WHERE id='" + id + "'");
						if (plugin.getConfig().getBoolean("MySQL.USE_MYSQL")) {
							rs.next(); // sets pointer to first record in result
										// set
						}

						NotifyOwnerOfReply(sender, targetTable, id, admin, rs.getString("owner"));

					} catch (Exception e) {
						if (e.toString().contains("ResultSet closed")) {
							sender.sendMessage(plugin.getMessage(notExistMessageName).replace("&arg", args[0]));
						} else {
							sender.sendMessage(plugin.getMessage("Error").replace("&arg", e.toString()));
						}
					} finally {
						try {
							if (rs != null) { rs.close(); rs = null; }
							if (stmt != null) { stmt.close(); stmt = null; }
						} catch (SQLException e) {
							System.out.println("ERROR: Failed to close PreparedStatement or ResultSet!");
							e.printStackTrace();
						}
					}
					return true;
				} else {
					// PLAYER COMMANDS
					String admin = player.getName();
					// CHECK IF PLAYE HAS TICKET PERMS OR ADMIN PERMS
					if (!player.hasPermission("sht.ticket") && !player.hasPermission("sht.admin")) {
						sender.sendMessage(plugin.getMessage("NoPermission"));
						return true;
					}

					// CHECK IF TICKET EXISTS
					rs = stmt.executeQuery("SELECT COUNT(id) AS ticketTotal FROM " + targetTable + " WHERE id='" + id + "'");
					if (plugin.getConfig().getBoolean("MySQL.USE_MYSQL")) {
						rs.next(); // sets pointer to first record in result set
					}
					if (rs.getInt("ticketTotal") == 0) {
						sender.sendMessage(plugin.getMessage(notExistMessageName).replace("&arg", args[0]));
						return true;
					}

					rs = stmt.executeQuery("SELECT * FROM " + targetTable + " WHERE id='" + id + "'");
					if (plugin.getConfig().getBoolean("MySQL.USE_MYSQL")) {
						rs.next(); // sets pointer to first record in result set
					}
					if (player.getUniqueId().toString().equals(rs.getString("uuid"))) {
						// PLAYER IS THE TICKET OWNER

						stmt.executeUpdate("UPDATE " + targetTable + " SET userreply='" + details + "' WHERE id='" + id + "'");

						NotifyReplied(sender, id, targetTable);

						try {
							if (plugin.getConfig().getBoolean("MySQL.USE_MYSQL")) {
								con = plugin.mysql.getConnection();
							} else {
								con = service.getConnection();
							}
							stmt = con.createStatement();
							rs = stmt.executeQuery("SELECT * FROM " + targetTable + " WHERE id='" + id + "'");
							if (plugin.getConfig().getBoolean("MySQL.USE_MYSQL")) {
								rs.next(); // sets pointer to first record in
											// result set
							}

							if (targetTable == Utilities.IDEA_TABLE_NAME)
								messageName = "UserRepliedToIdea";
							else
								messageName = "UserRepliedToTicket";

							String msg = plugin.getMessage(messageName).replace("%player", player.getName()).replace("&arg", id);
							plugin.notifyAdmins(msg, sender);
						} catch (Exception e) {
							if (e.toString().contains("ResultSet closed")) {
								sender.sendMessage(plugin.getMessage(notExistMessageName).replace("&arg", args[0]));
								return true;
							} else {
								sender.sendMessage(plugin.getMessage("Error").replace("&arg", e.toString()));
								return true;
							}
						} finally {
							try {
								if (rs != null) { rs.close(); rs = null; }
								if (stmt != null) { stmt.close(); stmt = null; }
							} catch (SQLException e) {
								System.out.println("ERROR: Failed to close PreparedStatement or ResultSet!");
								e.printStackTrace();
							}
						}
					} else {
						// PLAYER ISN'T THE TICKET OWNER

						if (!player.hasPermission("sht.admin")) {
							sender.sendMessage(plugin.getMessage("NoPermission"));
							return true;
						}

						stmt.executeUpdate("UPDATE " + targetTable + " SET adminreply='" + admin + ": " + details + "', admin='" + admin + "' WHERE id='" + id + "'");

						// INFORM OTHER ADMINS THAT AN ADMIN REPLIED TO TICKET

						if (targetTable == Utilities.IDEA_TABLE_NAME)
							messageName = "AdminRepliedToIdea";
						else
							messageName = "AdminRepliedToTicket";

						String msg = plugin.getMessage(messageName).replace("&arg", id);
						plugin.notifyAdmins(msg, null);

						try {
							if (plugin.getConfig().getBoolean("MySQL.USE_MYSQL")) {
								con = plugin.mysql.getConnection();
							} else {
								con = service.getConnection();
							}
							stmt = con.createStatement();
							rs = stmt.executeQuery("SELECT * FROM " + targetTable + " WHERE id='" + id + "'");
							if (plugin.getConfig().getBoolean("MySQL.USE_MYSQL")) {
								rs.next(); // sets pointer to first record in
											// result set
							}
							// INFORM TICKET-OWNER THAT AN ADMIN REPLIED TO
							// THEIR TICKET
							NotifyOwnerOfReply(sender, targetTable, id, admin, rs.getString("owner"));
						} catch (Exception e) {
							if (e.toString().contains("ResultSet closed")) {
								sender.sendMessage(plugin.getMessage(notExistMessageName).replace("&arg", args[0]));
							} else {
								sender.sendMessage(plugin.getMessage("Error").replace("&arg", e.toString()));
							}
						} finally {
							try {
								if (rs != null) { rs.close(); rs = null; }
								if (stmt != null) { stmt.close(); stmt = null; }
							} catch (SQLException e) {
								System.out.println("ERROR: Failed to close PreparedStatement or ResultSet!");
								e.printStackTrace();
							}
						}
						return true;
					}
				}
			} catch (Exception e) {
				if (e.toString().contains("ResultSet closed")) {
					sender.sendMessage(plugin.getMessage(notExistMessageName).replace("&arg", args[0]));
					return true;
				} else {
					sender.sendMessage(plugin.getMessage("Error").replace("&arg", e.toString()));
					return true;
				}
			} finally {
				try {
					if (rs != null) { rs.close(); rs = null; }
					if (stmt != null) { stmt.close(); stmt = null; }
				} catch (SQLException e) {
					System.out.println("ERROR: Failed to close PreparedStatement or ResultSet!");
					e.printStackTrace();
				}
			}
			return true;
		}
		return false;
	}

	private void NotifyOwnerOfReply(CommandSender sender, String targetTable, String id, String admin, String owner) {
		String messageName;
		if (targetTable == Utilities.IDEA_TABLE_NAME)
			messageName = "AdminRepliedToIdeaOWNER";
		else
			messageName = "AdminRepliedToTicketOWNER";

		plugin.notifyUser(plugin.getMessage(messageName).replace("&arg", id).replace("&admin", admin), owner);

	}

	private void NotifyReplied(CommandSender sender, String id, String targetTable) {

		String messageName;
		if (targetTable == Utilities.IDEA_TABLE_NAME)
			messageName = "AdminRepliedToIdea";
		else
			messageName = "AdminRepliedToTicket";

		sender.sendMessage(plugin.getMessage(messageName).replace("&arg", id));

	}
}