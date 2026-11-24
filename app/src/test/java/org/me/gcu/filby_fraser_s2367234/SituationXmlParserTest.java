package org.me.gcu.filby_fraser_s2367234;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.me.gcu.filby_fraser_s2367234.data.SituationXmlParser;
import org.me.gcu.filby_fraser_s2367234.model.Impact;
import org.me.gcu.filby_fraser_s2367234.model.LocationPoint;
import org.me.gcu.filby_fraser_s2367234.model.Situation;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

// Checks the PullParser output against the example feeds supplied with the coursework.
public class SituationXmlParserTest {

    private SituationXmlParser parser;
    private List<Situation> incidents;
    private List<Situation> roadworks;

    @Before
    public void setUp() throws Exception {
        parser = new SituationXmlParser();
        incidents = parser.parse(read("unplannedContentExample.xml"), Situation.Category.INCIDENT);
        roadworks = parser.parse(read("roadworksContentExample.xml"), Situation.Category.ROADWORKS);
    }

    private String read(String name) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull("missing test resource " + name, in);
        try (Scanner s = new Scanner(in, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    @Test
    public void parsesEverySituationRecord() {
        assertEquals(4, incidents.size());
        assertEquals(405, roadworks.size());
    }

    @Test
    public void firstIncidentFieldsMatchFeed() {
        Situation s = incidents.get(0);
        assertEquals("I_20260810_248_S", s.getSituationId());
        assertEquals("I_20260810_248", s.getRecordId());
        assertEquals("10", s.getVersion());
        assertEquals("RoadOrCarriagewayOrLaneManagement", s.getRecordType());
        assertEquals("roadClosed", s.getSubType());
        assertEquals("Road closed", s.getTitle());
        assertEquals(Situation.Type.CLOSURE, s.getType());
        assertEquals("certain", s.getProbabilityOfOccurrence());
        assertEquals("TrafficScotland", s.getSource());
        assertEquals("noRestriction", s.getConfidentiality());
        assertEquals("real", s.getInformationStatus());
        assertEquals("mandatory", s.getComplianceOption());
        assertEquals("definedByValidityTimeSpec", s.getValidityStatus());
        assertEquals("2026-08-10T11:37:42+01:00", s.getStartTime().toString());
        assertNull(s.getEndTime());
        assertEquals(Integer.valueOf(0), s.getImpact().getNumberOfOperationalLanes());
        assertTrue(s.getComment().startsWith("The A74M(S) junction 19 (Ecclefechan)"));
        assertEquals("southBound", s.getDirection());
        assertEquals("A74(M)", s.getRoadName());
    }

    @Test
    public void linearLocationHasFromAndToPoints() {
        Situation s = incidents.get(0);
        assertEquals("Linear", s.getLocationType());
        assertTrue(s.isLinear());
        LocationPoint to = s.getLocations().get(0);
        LocationPoint from = s.getLocations().get(1);
        assertEquals(LocationPoint.Role.TO, to.getRole());
        assertEquals(55.065432, to.getLatitude(), 1e-6);
        assertEquals(-3.273135, to.getLongitude(), 1e-6);
        assertEquals("J19", to.getJunction());
        assertEquals("A74(M) J19 Eaglesfield", to.getName());
        assertEquals(LocationPoint.Role.FROM, from.getRole());
        assertEquals(55.103477, from.getLatitude(), 1e-6);
        assertEquals("A74(M) J18 Ecclefechan", from.getName());
    }

    @Test
    public void crossingRoadsAndAbnormalTraffic() {
        Situation brechin = incidents.get(1);
        assertTrue(brechin.getLocations().get(0).getCrossingRoads().contains("B957"));

        Situation queue = incidents.get(2);
        assertEquals("AbnormalTraffic", queue.getRecordType());
        assertEquals("queuingTraffic", queue.getSubType());
        assertEquals(Situation.Type.QUEUE, queue.getType());
        assertEquals(Integer.valueOf(2), queue.getImpact().getNumberOfLanesRestricted());
        assertEquals("M8", queue.getRoadName());
    }

    @Test
    public void roadUsesLocationDataNotJustComment() {
        // comment says "M8" but the location data says the slip is on the M73
        Situation s = incidents.get(3);
        assertEquals("M73", s.getRoadName());
        assertTrue(s.isOnRoad("M73"));
        assertTrue(s.isOnRoad("m8"));   // still found by the road named in the description
    }

    @Test
    public void roadworksDetails() {
        Situation s = roadworks.get(0);
        assertEquals("MaintenanceWorks", s.getRecordType());
        assertEquals("roadworks", s.getSubType());
        assertEquals("M8 WB Woodside - A804 Phoenix Rd - Closure", s.getComment());
        assertEquals("longDelays", s.getImpact().getDelaysType());
        assertEquals(Impact.DelayLevel.LONG, s.getImpact().getDelayLevel());
        assertEquals(Situation.Type.CLOSURE, s.getType());
        assertNotNull(s.getEndTime());
        assertTrue(s.getDuration().toDays() > 365);
        assertEquals("M8", s.getRoadName());
        assertEquals("M8 (Jct 18 Off Slip to Entry A82)", s.getLocations().get(0).getName());
        assertEquals("false", s.getOtherFields().get("urgentRoadworks"));
    }

    @Test
    public void everyRecordHasCommentRoadAndTwoPoints() {
        for (Situation s : roadworks) {
            assertNotNull(s.getKey(), s.getComment());
            assertNotNull(s.getKey(), s.getRoadName());
            assertEquals(s.getKey(), 2, s.getMappablePoints().size());
        }
    }

    @Test
    public void delayCountsMatchFeed() {
        int delays = 0, longDelays = 0;
        for (Situation s : roadworks) {
            String t = s.getImpact().getDelaysType();
            if ("delays".equals(t)) delays++;
            if ("longDelays".equals(t) || "veryLongDelays".equals(t)) longDelays++;
        }
        assertEquals(130, delays);
        assertEquals(27, longDelays);
    }

    @Test
    public void ignoresGarbageAndUnknownElements() throws Exception {
        String xml = "﻿junk<?xml version=\"1.0\"?><d2LogicalModel xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                + "<situation id=\"S1\"><situationRecord xsi:type=\"Future\" id=\"R1\" version=\"1\">"
                + "<brandNewElement><nested>value</nested></brandNewElement>"
                + "<generalPublicComment><comment><values><value lang=\"en\">Test on the A9</value></values></comment></generalPublicComment>"
                + "</situationRecord></situation></d2LogicalModel>";
        List<Situation> list = parser.parse(xml, Situation.Category.INCIDENT);
        assertEquals(1, list.size());
        assertEquals("Test on the A9", list.get(0).getComment());
        assertEquals("A9", list.get(0).getRoadName());
        assertEquals("value", list.get(0).getOtherFields().get("nested"));
    }

    @Test
    public void emptyInputGivesEmptyList() throws Exception {
        assertTrue(parser.parse("", Situation.Category.INCIDENT).isEmpty());
        assertTrue(parser.parse(null, Situation.Category.INCIDENT).isEmpty());
    }
}
