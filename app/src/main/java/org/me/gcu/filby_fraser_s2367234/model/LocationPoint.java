package org.me.gcu.filby_fraser_s2367234.model;

import java.util.ArrayList;
import java.util.List;

public class LocationPoint {

    public enum Role { FROM, TO, POINT, DISPLAY }

    private final Role role;
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    private String name;            // descriptive name, e.g. "M8 J18 Charing Cross"
    private String junction;        // junction number, e.g. "J18"
    private final List<String> crossingRoads = new ArrayList<>();  // other roads at this point

    public LocationPoint(Role role) {
        this.role = role;
    }

    public Role getRole() { return role; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getName() { return name; }
    public String getJunction() { return junction; }
    public List<String> getCrossingRoads() { return crossingRoads; }

    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setName(String name) { this.name = name; }
    public void setJunction(String junction) { this.junction = junction; }

    public boolean hasCoordinates() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    public String getDisplayName() {
        if (name != null) return name;
        if (junction != null) return "Junction " + junction;
        return null;
    }

    public String getRoleLabel() {
        switch (role) {
            case FROM: return "From";
            case TO: return "To";
            case DISPLAY: return "Display point";
            default: return "Location";
        }
    }

    @Override
    public String toString() {
        return getRoleLabel() + (getDisplayName() != null ? " " + getDisplayName() : "")
                + " (" + latitude + ", " + longitude + ")";
    }
}
