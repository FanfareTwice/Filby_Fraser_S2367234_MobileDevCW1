package org.me.gcu.filby_fraser_s2367234.model;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Situation {

    public enum Category { INCIDENT, ROADWORKS }

    public enum Type { CLOSURE, LANES, SIGNALS, QUEUE, VEHICLE, WEATHER, ROADWORKS, HAZARD, OTHER }

    private static final Pattern ROAD_PATTERN =
            Pattern.compile("\\b([AMB][0-9]{1,4}(?:\\(M\\)|M)?)(?![0-9])");

    private final Category category;

    private String situationId;
    private String recordId;
    private String version;
    private String confidentiality;
    private String informationStatus;
    private String creationTime;
    private String versionTime;

    private String recordType;          // xsi:type of situationRecord, e.g. MaintenanceWorks
    private String subType;             // roadMaintenanceType / abnormalTrafficType / ...
    private String source;
    private String probabilityOfOccurrence;
    private String severity;
    private String complianceOption;
    private String comment;             // generalPublicComment

    private String validityStatus;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    private final Impact impact = new Impact();
    private String locationType;        // xsi:type of groupOfLocations (Linear / Point)
    private String direction;
    private String roadNumber;          // main road from the feed (ilc name 1 / linkName)
    private final Set<String> roads = new LinkedHashSet<>();   // every road this situation is on
    private final List<LocationPoint> locations = new ArrayList<>();

    // Elements not handled above, keyed by element name
    private final Map<String, String> otherFields = new LinkedHashMap<>();

    public Situation(Category category) {
        this.category = category;
    }


    // Prefer the road from the location data; fall back to the description
    public String getRoadName() {
        if (roadNumber != null && !roadNumber.isEmpty()) return roadNumber.toUpperCase(Locale.UK);
        if (comment != null) {
            Matcher m = ROAD_PATTERN.matcher(comment);
            if (m.find()) return m.group(1).toUpperCase(Locale.UK);
        }
        return null;
    }

    public Set<String> getRoads() {
        Set<String> out = new LinkedHashSet<>(roads);
        Set<String> seen = new java.util.HashSet<>();
        for (String r : roads) seen.add(normaliseRoad(r));
        if (comment != null) {
            Matcher m = ROAD_PATTERN.matcher(comment);
            while (m.find()) {
                String r = m.group(1).toUpperCase(Locale.UK);
                if (seen.add(normaliseRoad(r))) out.add(r);
            }
        }
        return out;
    }

    // "A74(M)", "a74m" and "A74 (M)" all match
    public boolean isOnRoad(String query) {
        String q = normaliseRoad(query);
        if (q.isEmpty()) return false;
        for (String r : getRoads()) {
            if (normaliseRoad(r).equals(q)) return true;
        }
        return false;
    }

    public static String normaliseRoad(String road) {
        return road == null ? "" : road.toUpperCase(Locale.UK).replaceAll("[^A-Z0-9]", "");
    }

    public void addRoad(String road) {
        if (road == null || road.trim().isEmpty()) return;
        String r = road.trim().toUpperCase(Locale.UK);
        roads.add(r);
        if (roadNumber == null) roadNumber = r;
    }

    public String getTitle() {
        if (category == Category.ROADWORKS) {
            Type t = getType();
            return t == Type.ROADWORKS ? "Roadworks" : "Roadworks – " + getTypeLabel().toLowerCase(Locale.UK);
        }
        if (subType != null && !subType.isEmpty()) return humanise(subType);
        if (recordType != null && !recordType.isEmpty()) return humanise(recordType);
        return category == Category.ROADWORKS ? "Roadworks" : "Incident";
    }

    public Type getType() {
        String rt = recordType == null ? "" : recordType.toLowerCase(Locale.UK);
        String st = subType == null ? "" : subType.toLowerCase(Locale.UK);
        String text = comment == null ? "" : comment.toLowerCase(Locale.UK);

        if (category == Category.ROADWORKS || rt.contains("maintenance") || rt.contains("construction")) {
            if (text.matches("(?s).*\\b(ttls?|ttl's|temp(orary)? (traffic )?lights|traffic signals|stop ?(and|/|&) ?go|convoy)\\b.*"))
                return Type.SIGNALS;
            if (text.matches("(?s).*\\b(lane closures?|mlc'?s?|hard shoulder|narrow lanes|contraflow|lane restriction)\\b.*"))
                return Type.LANES;
            if (text.matches("(?s).*\\b(road closure|slip closure|closure|closed|diversion)\\b.*")
                    && !text.contains("layby"))
                return Type.CLOSURE;
            if (text.contains("speed restriction") || text.contains("speed limit"))
                return Type.HAZARD;
            if (text.contains("signage") || text.contains("mvms"))
                return Type.OTHER;
            return Type.ROADWORKS;
        }

        if (st.contains("closed") || st.contains("closure") || st.contains("blocked")) return Type.CLOSURE;
        if (st.contains("lane") || st.contains("carriageway") || st.contains("contraflow")
                || st.contains("narrow")) return Type.LANES;
        if (rt.contains("abnormaltraffic") || st.contains("queu") || st.contains("slow")
                || st.contains("stationary") || st.contains("heavy")) return Type.QUEUE;
        if (rt.contains("vehicle") || rt.contains("accident") || st.contains("vehicle")
                || st.contains("accident") || st.contains("collision")) return Type.VEHICLE;
        if (rt.contains("weather") || rt.contains("environment") || st.contains("snow")
                || st.contains("ice") || st.contains("flood") || st.contains("wind")
                || st.contains("fog")) return Type.WEATHER;
        if (rt.contains("obstruction") || rt.contains("animal") || rt.contains("hazard")
                || st.contains("obstruction") || st.contains("debris")) return Type.HAZARD;
        if (rt.contains("signal")) return Type.SIGNALS;
        if (rt.contains("roadorcarriagewayorlanemanagement")) return Type.LANES;
        return Type.OTHER;
    }

    public String getTypeLabel() {
        switch (getType()) {
            case CLOSURE: return "Closure";
            case LANES: return "Lane restriction";
            case SIGNALS: return "Temporary lights";
            case QUEUE: return "Queues";
            case VEHICLE: return "Vehicle incident";
            case WEATHER: return "Weather";
            case ROADWORKS: return "Roadworks";
            case HAZARD: return "Hazard / speed limit";
            default: return "Information";
        }
    }

    public Duration getDuration() {
        if (startTime == null || endTime == null) return null;
        return Duration.between(startTime, endTime);
    }

    public List<LocationPoint> getMappablePoints() {
        List<LocationPoint> out = new ArrayList<>();
        for (LocationPoint p : locations) {
            if (p.hasCoordinates()) out.add(p);
        }
        return out;
    }

    public boolean isLinear() {
        int count = 0;
        for (LocationPoint p : getMappablePoints()) {
            if (p.getRole() == LocationPoint.Role.FROM || p.getRole() == LocationPoint.Role.TO) count++;
        }
        return count >= 2;
    }

    public LocationPoint getCentre() {
        List<LocationPoint> pts = getMappablePoints();
        if (pts.isEmpty()) return null;
        double lat = 0, lon = 0;
        for (LocationPoint p : pts) { lat += p.getLatitude(); lon += p.getLongitude(); }
        LocationPoint c = new LocationPoint(LocationPoint.Role.POINT);
        c.setLatitude(lat / pts.size());
        c.setLongitude(lon / pts.size());
        return c;
    }

    public String getKey() {
        return category + ":" + (recordId != null ? recordId : situationId);
    }

    // "roadClosed" -> "Road closed"
    public static String humanise(String value) {
        if (value == null || value.isEmpty()) return "";
        String spaced = value.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("([A-Za-z])([0-9])", "$1 $2")
                .replace('_', ' ')
                .toLowerCase()
                .trim();
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }


    public Category getCategory() { return category; }
    public String getSituationId() { return situationId; }
    public void setSituationId(String v) { situationId = v; }
    public String getRecordId() { return recordId; }
    public void setRecordId(String v) { recordId = v; }
    public String getVersion() { return version; }
    public void setVersion(String v) { version = v; }
    public String getConfidentiality() { return confidentiality; }
    public void setConfidentiality(String v) { confidentiality = v; }
    public String getInformationStatus() { return informationStatus; }
    public void setInformationStatus(String v) { informationStatus = v; }
    public String getCreationTime() { return creationTime; }
    public void setCreationTime(String v) { creationTime = v; }
    public String getVersionTime() { return versionTime; }
    public void setVersionTime(String v) { versionTime = v; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String v) { recordType = v; }
    public String getSubType() { return subType; }
    public void setSubType(String v) { subType = v; }
    public String getSource() { return source; }
    public void setSource(String v) { source = v; }
    public String getProbabilityOfOccurrence() { return probabilityOfOccurrence; }
    public void setProbabilityOfOccurrence(String v) { probabilityOfOccurrence = v; }
    public String getSeverity() { return severity; }
    public void setSeverity(String v) { severity = v; }
    public String getComplianceOption() { return complianceOption; }
    public void setComplianceOption(String v) { complianceOption = v; }
    public String getComment() { return comment; }
    public void setComment(String v) { comment = v; }
    public String getValidityStatus() { return validityStatus; }
    public void setValidityStatus(String v) { validityStatus = v; }
    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime v) { startTime = v; }
    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime v) { endTime = v; }
    public Impact getImpact() { return impact; }
    public String getLocationType() { return locationType; }
    public void setLocationType(String v) { locationType = v; }
    public String getDirection() { return direction; }
    public void setDirection(String v) { direction = v; }
    public String getRoadNumber() { return roadNumber; }
    public void setRoadNumber(String v) { roadNumber = v; }
    public List<LocationPoint> getLocations() { return locations; }
    public Set<String> getRoadsFromFeed() { return roads; }
    public Map<String, String> getOtherFields() { return otherFields; }
}
