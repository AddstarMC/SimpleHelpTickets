package me.odium.simplehelptickets.manager;

import me.odium.simplehelptickets.SimpleHelpTickets;
import me.odium.simplehelptickets.database.Database;
import me.odium.simplehelptickets.database.Table;
import me.odium.simplehelptickets.objects.Pair;
import me.odium.simplehelptickets.objects.Ticket;
import me.odium.simplehelptickets.objects.TicketLocation;
import me.odium.simplehelptickets.utilities.Utilities;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.sql.Date;
import java.util.*;

import java.sql.*;
import java.text.SimpleDateFormat;

import static java.sql.Types.TIMESTAMP;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 18/07/2017.
 */
public class TicketManager {
    
    public static Map<String, String> tableNames = new HashMap<>();
    
    static {
        tableNames.put("idea", "SHT_Ideas");
        tableNames.put("ticket", "SHT_Tickets");
    }
    
    public static Table getTargetItemName(String targetTable) {
        return Table.matchTableName(targetTable);
    }
    
    public static Table getTableFromCommandString(String commandString) {
        if (commandString.toLowerCase().contains("idea"))
            return Table.matchIdentifier("idea");
        else
            return Table.matchIdentifier("ticket");
    }
    
    public void ShowAdminTickets(Player player) {
        int total = getTickets(getTableName("ticket").tableName, null, Ticket.Status.OPEN).size();
        if (total > 0) {
            player.sendMessage(plugin.getMessage("AdminJoin").replace("&arg", total + ""));
        }
        int ideas = getTickets(getTableName("idea").tableName, null, Ticket.Status.OPEN).size();
        if (ideas > 0) {
            player.sendMessage(plugin.getMessage("AdminJoinIdeas").replace("&arg", ideas + ""));
        }
    }
    
    private final SimpleHelpTickets plugin;
    
    public TicketManager(SimpleHelpTickets plugin) {
        this.plugin = plugin;
    }
    
    public static Table getTableName(String identifier) {
        return Table.matchIdentifier(identifier);
    }

    private List<Ticket> getTickets(String table, Player player, Ticket.Status status) {
        String sql;
        if (player != null) {
            sql = "SELECT * FROM " + table + " WHERE uuid='" + player.getUniqueId().toString() + "' and status = '" + status.name() + "'";
        } else {
            sql = "SELECT * FROM " + table + " WHERE status = '" + status.name() + "'";
        }
        List<Ticket> tickets = new ArrayList<>();
        Database db = plugin.databaseService;
        Connection con = db.getConnection();
        if (con != null) {
            try (Statement statement = con.createStatement();
                 ResultSet result = statement.executeQuery(sql)) {
                while (result.next()) {
                    tickets.add(getFromResultRow(result));
                }
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            plugin.log.warning("Unable to get Database Connection");
        }
        return tickets;
    }
    
    public boolean saveTicket(Ticket ticket, String table) {
        List<Ticket> t = new ArrayList<>();
        t.add(ticket);
        return saveTickets(t, table);
    }

    /**
     * @param table
     * @param status
     * @return num rows deleted
     */
    public int deleteTickets(String table, Ticket.Status status) {
        String sql = "DELETE FROM " + table + " WHERE status=?";
        try (PreparedStatement statement = plugin.databaseService.getConnection().prepareStatement(sql)) {
            statement.setString(1, status.name());
            int integer = statement.executeUpdate();
            return integer;
        } catch (SQLException e) {
            plugin.log.warning("[DELETE TICKETS ERROR]:" + e.getMessage());
        }
        return 0;
    }

    public int deleteTicketbyId(String table, Integer id) {
        String sql = "DELETE FROM " + table + " WHERE id=?";
        try (PreparedStatement statement = plugin.databaseService.getConnection().prepareStatement(sql)) {
            statement.setInt(1, id);
            int integer = statement.executeUpdate();
            return integer;
        } catch (SQLException e) {
            plugin.log.warning("[DELETE TICKETS ERROR]:" + e.getMessage());
        }
        return 0;
    }
    
    /**
     * This will always return true but will log an error to the log if saving was unsuccessful.
     *
     * @param tickets
     * @param table
     *
     * @return
     */
    public boolean saveTicketsAsync(List<Ticket> tickets, String table) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    if (!saveTickets(tickets, table))
                        plugin.log.warning("[SHT] Error Saving Tickets");
                }
        );
        return true;
    }
    
    /**
     * Best to run this async - as if there is any delay or a lot of tickets to save then it could
     * lock up the main thread too long.
     *
     * @param tickets
     * @param table
     *
     * @return true if saved.
     */
    public boolean saveTickets(List<Ticket> tickets, String table) {
        Connection con = plugin.databaseService.getConnection();
        PreparedStatement updateSQL;
        PreparedStatement insertSQL;
        try {
            updateSQL = con.prepareStatement("UPDATE " + table + " SET description = ?,adminreply = ?,admin=?,userreply=?,status=?,owner=?  WHERE id = ?");
            String sql = "INSERT INTO " + table + "(description,date,uuid,owner,world,x,y,z,p,f,adminreply,userreply,status,admin,expiration) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            insertSQL = con.prepareStatement(sql);
            insertSQL.getParameterMetaData().getParameterCount();
            int done = 0;
            
            for (Ticket ticket : tickets) {
                if (ticket.getId() == null) {
                    insertSQL.setString(1, ticket.getDescription());
                    insertSQL.setDate(2, ticket.getCreatedDate());
                    insertSQL.setString(3, ticket.getOwner().toString());
                    insertSQL.setString(4, ticket.getOwnerName());
                    TicketLocation loc = ticket.getLocation();
                    if (loc != null) {
                        insertSQL.setString(5, loc.getWorld());
                        insertSQL.setDouble(6, loc.getX());
                        insertSQL.setDouble(7, loc.getY());
                        insertSQL.setDouble(8, loc.getZ());
                        insertSQL.setFloat(9, loc.getPitch());
                        insertSQL.setFloat(10, loc.getYaw());
                    } else {
                        insertSQL.setString(5, "NONE");
                        insertSQL.setDouble(6, 0D);
                        insertSQL.setDouble(7, 0D);
                        insertSQL.setDouble(8, 0D);
                        
                        insertSQL.setFloat(9, 0F);
                        insertSQL.setFloat(10, 0F);
                    }
                    insertSQL.setString(11, ticket.getAdminReply());
                    insertSQL.setString(12, ticket.getUserReply());
                    insertSQL.setString(13, ticket.getStatus().name());
                    insertSQL.setString(14, ticket.getAdmin());
                    Timestamp expirationDate = ticket.getExpirationDate();
                    if (expirationDate == null) {
                        insertSQL.setNull(15, TIMESTAMP);
                    } else {
                        insertSQL.setTimestamp(15, expirationDate);
                    }
                    int r = insertSQL.executeUpdate();
                    if (r == 1) {
                        done++;
                    }
                } else {
                    updateSQL.setString(1, ticket.getDescription());
                    updateSQL.setString(2, ticket.getAdminReply());
                    updateSQL.setString(3, ticket.getAdmin());
                    updateSQL.setString(4, ticket.getUserReply());
                    updateSQL.setString(5, ticket.getStatus().name());
                    updateSQL.setString(6, ticket.getOwnerName());
                    updateSQL.setInt(7, ticket.getId());
                    int r = updateSQL.executeUpdate();
                    if (r == 1) {
                        done++;
                    }
                }
                
            }
            if (tickets.size() != done) {
                return false;
            }
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Pair<Integer, Timestamp> getTicketCount(Player player, String targetTable, Ticket.Status status, Integer ticketId) {
        String where = "";
        int param = 1;
        int playerIndex = 0;
        int statusIndex = 0;
        int idIndex = 0;
        if (player != null) {
            where += "uuid like ?";
            playerIndex = param;
            param++;

        }
        if (status != null) {
            if (param > 1) where += " AND ";
            where += "status like ?";
            statusIndex = param;
            param++;

        }
        if (ticketId != null) {
            if (param > 1) where += " AND ";
            where += "id = ?";
            idIndex = param;
        }

        String sql = "SELECT COUNT(uuid) AS itemTotal, MAX(UNIX_TIMESTAMP(date)) AS newestItem FROM " + targetTable +
                " WHERE " + where;
        String uuidString = null;
        String statusString = null;
        if (player != null)
            uuidString = player.getUniqueId().toString();
        if (status != null)
            statusString = status.name();
        try (PreparedStatement statement = plugin.databaseService.getConnection().prepareStatement(sql)) {
            if (playerIndex > 0) statement.setString(playerIndex, uuidString);
            if (statusIndex > 0) statement.setString(statusIndex, statusString);
            if (idIndex > 0) statement.setInt(idIndex, ticketId);
            ResultSet rs = statement.executeQuery();
            rs.next();
            return new Pair<>(rs.getInt(1), rs.getTimestamp(2));
        } catch (SQLException e) {
            plugin.log.warning(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private Ticket getFromResultRow(ResultSet result) throws SQLException {
        int id = result.getInt("id");
        UUID uuid = UUID.fromString(result.getString("uuid"));
        World world = Bukkit.getWorld(result.getString("world"));
        String server;
        try {
            server = result.getString("server");
        } catch (SQLException e) {
            server = null;
        }
        Location location;
        TicketLocation tL;
        if (world == null) {
            tL = new TicketLocation(result.getDouble("x"),
                    result.getDouble("y"),
                    result.getDouble("z"),
                    result.getString("world"),
                    result.getFloat("p"),
                    result.getFloat("f"),
                    server);
        } else {
            location = new Location(world, result.getDouble("x"), result.getDouble("y"), result.getDouble("z"), result.getFloat("p"), result.getFloat("f"));
            tL = new TicketLocation(location, server);
        }
        String details = result.getString("description");
        Date date = result.getDate("date");
        Ticket ticket = new Ticket(id, uuid, details, date, tL);
        String ownerName = result.getString("owner");
        ticket.setOwnerName(ownerName);
        ticket.setAdminReply(result.getString("adminreply"));
        ticket.setUserReply(result.getString("userreply"));
        Ticket.Status s;
        try {
            s = Ticket.Status.valueOf(result.getString("status"));
        } catch (IllegalArgumentException e) {
            s = Ticket.Status.OPEN;
        }
        ticket.setStatus(s);
        return ticket;
    }

    private void showPlayerOpenTickets(Player player) {
        int ticket = getTickets(TicketManager.getTableName("ticket"), player, Ticket.Status.OPEN).size();
        if (ticket > 0) {
            player.sendMessage(plugin.getMessage("UserJoin").replace("&arg", ticket + ""));
        }
    }

    public void runOnJoin(Player player){
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (player.hasPermission("sht.admin")) {
                boolean DisplayTicketAdmin = plugin.getConfig().getBoolean("OnJoin.DisplayTicketAdmin");
                if (DisplayTicketAdmin) {
                    ShowAdminTickets(player);
                }
                // IF PLAYER IS USER
            } else {
                boolean DisplayTicketUser = plugin.getConfig().getBoolean("OnJoin.DisplayTicketUser");

                if (DisplayTicketUser) {
                    showPlayerOpenTickets(player);
                }
            }
        });

    }

    public List<Ticket> getTickets(String targetTable, String where, int maxRecords) {
        Connection con = plugin.databaseService.getConnection();
        List<Ticket> results = new ArrayList<>();
        try {
            PreparedStatement s = con.prepareStatement(GetItemSelectQuery(targetTable, where, maxRecords));
            ResultSet rs = s.executeQuery();
            while (rs.next()) {
                results.add(getFromResultRow(rs));
            }
            s.close();
            con.close();
        } catch (SQLException e) {
            plugin.log.warning(plugin.getMessage("Error").replace("&arg", e.getMessage()));
            e.printStackTrace();
            return results;
        }
        return results;
    }

    private String GetItemSelectQuery(String targetTable, String whereClause, int maxRecordsToReturn) {
        String innerQuery = "SELECT * FROM " + targetTable;
        if (!whereClause.isEmpty())
            innerQuery += " WHERE " + whereClause;

        innerQuery += " ORDER BY id DESC LIMIT " + maxRecordsToReturn;

        return "SELECT * FROM (" + innerQuery + ") AS SelectQ ORDER BY id ASC";
    }

    public int expireItems(String targetTable) {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        PreparedStatement stmt2 = null;
        Connection con;
        int expirations = 0;
        Date dateNEW;
        Date expirationNEW;
        try {
            con = plugin.databaseService.getConnection();
            stmt = con.prepareStatement("SELECT * FROM " + targetTable);
            rs = stmt.executeQuery();
            stmt2 = con.prepareStatement("DELETE FROM " + targetTable + " WHERE id=?");
            while (rs.next()) {
                Ticket ticket = getFromResultRow(rs);
                Date exp = null;
                if (ticket.getExpirationDate() != null) {
                    exp = new Date(ticket.getExpirationDate().getTime());
                }
                Integer id = ticket.getId();
                // IF AN EXPIRATION HAS BEEN APPLIED
                if (exp != null) {
                    // CONVERT DATE-STRINGS FROM DB TO DATES
                    Date date = ticket.getCreatedDate();
                    Date expiration = new Date(ticket.getExpirationDate().getTime());
                    dateNEW = date;
                    expirationNEW = expiration;

                    // COMPARE STRINGS
                    int HasExpired = dateNEW.compareTo(expirationNEW);
                    if (HasExpired >= 0) {
                        expirations++;
                        stmt2.setInt(1, id);
                        stmt2.executeUpdate();
                    }
                }
            }
            // return expirations;
        } catch (Exception e) {
            plugin.getLogger().info("[SimpleHelpTickets] " + "Error: " + e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (stmt2 != null) {
                    stmt2.close();
                }
            } catch (SQLException e) {
                System.out.println("ERROR: Failed to close PreparedStatement or ResultSet!");
                e.printStackTrace();
            }
        }
        return expirations;
    }

    public List<Ticket> findTickets(String targetTable,
                                    String ticketOwner,
                                    String staffName,
                                    boolean mostRecentTimeDefined,
                                    boolean dateRangeDefined,
                                    SimpleDateFormat dateFormatter,
                                    String startDate,
                                    String endDate,
                                    Calendar cal,
                                    String searchPhrase,
                                    String sortDirection,
                                    Integer ticketsToShow,
                                    Integer ticketIDStart,
                                    Integer ticketIDEnd

    ) {
        List<Ticket> results = new ArrayList<>();
        long recentTimeStartMillisec = System.currentTimeMillis() - 86400 * 1000;
        String sqlStatement = "SELECT * FROM " + targetTable +
                " WHERE id >= ? AND id <= ? AND owner LIKE ? AND admin LIKE ? AND " +
                " (description LIKE ? OR userreply LIKE ? OR adminreply LIKE ?) AND ";
        String recentTimeStartDate = Utilities.DateToString(recentTimeStartMillisec, dateFormatter);

        if (dateRangeDefined) {


            if ((startDate.contains("00:00:00") && endDate.contains("00:00:00"))) {
                // Start and end dates do not contain a time component
                // Add 24 hours to the end date

                java.util.Date parsedDate = Utilities.parseDate(endDate);

                cal.setTime(parsedDate);
                cal.add(Calendar.DATE, 1);

                endDate = Utilities.DateToString(cal, dateFormatter);
            }

            if (dateRangeDefined && mostRecentTimeDefined)
                sqlStatement += "(date BETWEEN '" + startDate + "' AND '" + endDate + "' AND " +
                        " date >= '" + recentTimeStartDate + "') ";
            else
                sqlStatement += "date BETWEEN '" + startDate + "' AND '" + endDate + "' ";
        } else {
            sqlStatement += "date >= '" + recentTimeStartDate + "' ";
        }

        sqlStatement += "ORDER BY id " + sortDirection + " LIMIT " + ticketsToShow + ";";
        try {
            PreparedStatement statement = plugin.databaseService.getConnection().prepareStatement(sqlStatement);
            statement.setString(1, Utilities.NumToString(ticketIDStart));
            statement.setString(2, Utilities.NumToString(ticketIDEnd));
            statement.setString(3, ticketOwner);
            statement.setString(4, staffName);
            statement.setString(5, searchPhrase);
            statement.setString(6, searchPhrase);
            statement.setString(7, searchPhrase);
            ResultSet set = statement.executeQuery();
            while (set.next()) {
                results.add(getFromResultRow(set));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;

    }
}
