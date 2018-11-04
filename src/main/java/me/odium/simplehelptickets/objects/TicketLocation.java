package me.odium.simplehelptickets.objects;


import org.bukkit.Location;

/**
 * Created for use for the Add5tar MC Minecraft server
 * Created by benjamincharlton on 4/11/2018.
 */
public class TicketLocation {
    private final Location location;
    private double x;
    private double y;
    private double z;
    private String world;
    private float pitch;
    private float yaw;
    private String server;

    public TicketLocation(Location location, String server) {
        this.location = location;
        x = location.getX();
        y = location.getY();
        z = location.getZ();
        pitch = location.getPitch();
        yaw = location.getYaw();
        world = location.getWorld().getName();
        server = null;
    }

    public TicketLocation(Double x, Double y, Double z, String world, Float pitch, Float yaw, String server) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.server = server;
        location = null;
    }

    public Location getLocation() {
        return location;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getWorld() {
        return world;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public String getServer() {
        return server;
    }
}
