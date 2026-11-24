package org.me.gcu.filby_fraser_s2367234.data;

import android.util.Log;

import org.me.gcu.filby_fraser_s2367234.model.LocationPoint;
import org.me.gcu.filby_fraser_s2367234.model.Situation;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SituationXmlParser {

    private static final String TAG = "SituationXmlParser";
    private static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";

    private static final Set<String> IGNORED_KEYS = new HashSet<>(Arrays.asList(
            "values", "value", "comment", "descriptor", "pointCoordinates", "name"));

    public List<Situation> parse(String xml, Situation.Category category)
            throws XmlPullParserException, IOException {

        List<Situation> results = new ArrayList<>();
        if (xml == null) return results;

        // Skip anything before the XML declaration
        int start = xml.indexOf("<?");
        if (start < 0) start = xml.indexOf('<');
        if (start < 0) return results;
        xml = xml.substring(start);

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader(xml));

        Deque<String> path = new ArrayDeque<>();   // open elements, innermost first
        StringBuilder text = new StringBuilder();

        pendingDescriptor = null;
        String situationId = null;
        String confidentiality = null;
        String informationStatus = null;

        Situation current = null;          // record being built
        LocationPoint currentPoint = null; // point being built (from / to / point)

        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String name = xpp.getName();
                path.push(name);
                text.setLength(0);

                if ("situation".equals(name)) {
                    situationId = attribute(xpp, "id");
                    confidentiality = null;
                    informationStatus = null;
                } else if ("situationRecord".equals(name)) {
                    current = new Situation(category);
                    current.setSituationId(situationId);
                    current.setRecordId(attribute(xpp, "id"));
                    current.setVersion(attribute(xpp, "version"));
                    current.setRecordType(stripPrefix(xsiType(xpp)));
                    current.setConfidentiality(confidentiality);
                    current.setInformationStatus(informationStatus);
                    currentPoint = null;
                } else if (current != null) {
                    if ("groupOfLocations".equals(name)) {
                        current.setLocationType(stripPrefix(xsiType(xpp)));
                    } else if ("from".equals(name)) {
                        currentPoint = newPoint(current, LocationPoint.Role.FROM);
                    } else if ("to".equals(name)) {
                        currentPoint = newPoint(current, LocationPoint.Role.TO);
                    } else if ("locationForDisplay".equals(name)) {
                        currentPoint = newPoint(current, LocationPoint.Role.DISPLAY);
                    } else if ("point".equals(name) && currentPoint == null
                            && path.contains("groupOfLocations")) {
                        currentPoint = newPoint(current, LocationPoint.Role.POINT);
                    }
                }

            } else if (eventType == XmlPullParser.TEXT) {
                text.append(xpp.getText());

            } else if (eventType == XmlPullParser.END_TAG) {
                String name = xpp.getName();
                String value = text.toString().trim();
                text.setLength(0);
                if (!path.isEmpty()) path.pop();   // path now holds the parents

                if (!value.isEmpty()) {
                    if ("confidentiality".equals(name)) {
                        confidentiality = value;
                        if (current != null) current.setConfidentiality(value);
                    } else if ("informationStatus".equals(name)) {
                        informationStatus = value;
                        if (current != null) current.setInformationStatus(value);
                    } else if (current != null) {
                        handleValue(current, currentPoint, name, value, path);
                    }
                }

                if (current != null) {
                    if ("from".equals(name) || "to".equals(name) || "locationForDisplay".equals(name)
                            || ("point".equals(name) && currentPoint != null
                                && currentPoint.getRole() == LocationPoint.Role.POINT)) {
                        currentPoint = null;
                    } else if ("situationRecord".equals(name)) {
                        // drop points that never received coordinates
                        current.getLocations().removeIf(p -> !p.hasCoordinates() && p.getDisplayName() == null);
                        results.add(current);
                        current = null;
                        currentPoint = null;
                    }
                }
            }
            eventType = xpp.next();
        }
        Log.d(TAG, "Parsed " + results.size() + " " + category + " records");
        return results;
    }

    private String pendingDescriptor;   // last <descriptor> value, waiting for its type

    private void handleValue(Situation s, LocationPoint point, String name, String value,
                             Deque<String> parents) {
        switch (name) {
            case "value":
                if (parents.contains("generalPublicComment")) {
                    s.setComment(s.getComment() == null ? value : s.getComment() + "\n" + value);
                } else if (parents.contains("descriptor")) {
                    // what this value is depends on the *DescriptorType element that follows it
                    pendingDescriptor = value;
                } else {
                    putOther(s, firstMeaningfulParent(parents), value);
                }
                return;
            case "tpegIlcPointDescriptorType":
            case "tpegJunctionPointDescriptorType":
            case "tpegOtherPointDescriptorType":
            case "tpegPointDescriptorType":
                applyDescriptor(s, point, value);
                return;
            case "latitude":
                ensurePoint(s, point).setLatitude(parseDouble(value));
                return;
            case "longitude":
                ensurePoint(s, point).setLongitude(parseDouble(value));
                return;
            case "sourceIdentification":
                s.setSource(value);
                return;
            case "probabilityOfOccurrence":
                s.setProbabilityOfOccurrence(value);
                return;
            case "validityStatus":
                s.setValidityStatus(value);
                return;
            case "overallStartTime":
                s.setStartTime(parseTime(value));
                return;
            case "overallEndTime":
                s.setEndTime(parseTime(value));
                return;
            case "situationRecordCreationTime":
                s.setCreationTime(value);
                return;
            case "situationRecordVersionTime":
                s.setVersionTime(value);
                return;
            case "complianceOption":
                s.setComplianceOption(value);
                return;
            case "severity":
            case "overallSeverity":
                s.setSeverity(value);
                return;
            case "numberOfOperationalLanes":
                s.getImpact().setNumberOfOperationalLanes(parseInt(value));
                return;
            case "numberOfLanesRestricted":
                s.getImpact().setNumberOfLanesRestricted(parseInt(value));
                return;
            case "capacityRemaining":
                s.getImpact().setCapacityRemaining(value);
                return;
            case "delaysType":
                s.getImpact().setDelaysType(value);
                return;
            case "delayTimeValue":
                Double secs = parseDouble(value);
                if (!secs.isNaN()) s.getImpact().setDelayTimeSeconds((int) Math.round(secs));
                return;
            case "tpegDirection":
            case "directionBound":
                if (s.getDirection() == null) s.setDirection(value);
                return;
            case "roadNumber":
            case "roadName":
                s.addRoad(value);
                return;
            default:
                break;
        }

        // e.g. roadMaintenanceType - only direct children of <situationRecord>
        boolean isSubType = name.endsWith("Type")
                && "situationRecord".equals(parents.peek());
        if (isSubType && s.getSubType() == null) {
            s.setSubType(value);
        } else {
            putOther(s, name, value);
        }
    }

    private void applyDescriptor(Situation s, LocationPoint point, String type) {
        String v = pendingDescriptor;
        pendingDescriptor = null;
        if (v == null) return;
        switch (type) {
            case "tpegIlcName1":   // main road at this point
            case "linkName":       // road the link is on (roadworks feed)
                s.addRoad(v);
                return;
            case "tpegIlcName2":
            case "tpegIlcName3":
                if (point != null && !point.getCrossingRoads().contains(v)) point.getCrossingRoads().add(v);
                return;
            case "junctionName":
                if (point != null && point.getJunction() == null) point.setJunction(v);
                return;
            default:               // intersectionName, nonLinkedPointName, ...
                if (point != null && point.getName() == null) point.setName(v);
                else if (point == null) putOther(s, type, v);
        }
    }

    private static void putOther(Situation s, String key, String value) {
        if (key == null || IGNORED_KEYS.contains(key)) return;
        if (!s.getOtherFields().containsKey(key)) s.getOtherFields().put(key, value);
    }

    private static String firstMeaningfulParent(Deque<String> parents) {
        for (String p : parents) {
            if (!IGNORED_KEYS.contains(p)) return p;
        }
        return null;
    }

    private static LocationPoint newPoint(Situation s, LocationPoint.Role role) {
        LocationPoint p = new LocationPoint(role);
        s.getLocations().add(p);
        return p;
    }

    private static LocationPoint ensurePoint(Situation s, LocationPoint point) {
        if (point != null) return point;
        for (LocationPoint p : s.getLocations()) {
            if (p.getRole() == LocationPoint.Role.POINT && !p.hasCoordinates()) return p;
        }
        return newPoint(s, LocationPoint.Role.POINT);
    }

    private static String attribute(XmlPullParser xpp, String name) {
        String v = xpp.getAttributeValue(null, name);
        if (v != null) return v;
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            if (name.equals(xpp.getAttributeName(i))) return xpp.getAttributeValue(i);
        }
        return null;
    }

    private static String xsiType(XmlPullParser xpp) {
        String v = xpp.getAttributeValue(XSI_NS, "type");
        if (v != null) return v;
        for (int i = 0; i < xpp.getAttributeCount(); i++) {
            String n = xpp.getAttributeName(i);
            if ("type".equals(n) || n.endsWith(":type")) return xpp.getAttributeValue(i);
        }
        return null;
    }

    private static String stripPrefix(String value) {
        if (value == null) return null;
        int i = value.indexOf(':');
        return i >= 0 ? value.substring(i + 1) : value;
    }

    private static OffsetDateTime parseTime(String value) {
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException e) {
            Log.w(TAG, "Unparseable time: " + value);
            return null;
        }
    }

    private static Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static Integer parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Double d = parseDouble(value);
            return d.isNaN() ? null : (int) Math.round(d);
        }
    }
}
