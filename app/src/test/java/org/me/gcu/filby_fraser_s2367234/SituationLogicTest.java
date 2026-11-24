package org.me.gcu.filby_fraser_s2367234;

import static org.junit.Assert.*;

import org.junit.Test;
import org.me.gcu.filby_fraser_s2367234.model.Impact;
import org.me.gcu.filby_fraser_s2367234.model.LocationPoint;
import org.me.gcu.filby_fraser_s2367234.model.Situation;
import org.me.gcu.filby_fraser_s2367234.util.GeoUtils;

public class SituationLogicTest {

    private static Situation roadworks(String comment) {
        Situation s = new Situation(Situation.Category.ROADWORKS);
        s.setRecordType("MaintenanceWorks");
        s.setSubType("roadworks");
        s.setComment(comment);
        return s;
    }

    @Test
    public void roadNameMatchingIgnoresFormatting() {
        Situation s = new Situation(Situation.Category.INCIDENT);
        s.addRoad("A74(M)");
        assertTrue(s.isOnRoad("A74(M)"));
        assertTrue(s.isOnRoad("a74m"));
        assertTrue(s.isOnRoad(" A74 (M) "));
        assertFalse(s.isOnRoad("A7"));
        assertFalse(s.isOnRoad(""));
    }

    @Test
    public void m8DoesNotMatchM80() {
        Situation s = roadworks("M80 J4 - Lane Closure");
        assertTrue(s.isOnRoad("M80"));
        assertFalse(s.isOnRoad("M8"));
    }

    @Test
    public void delayColourLevels() {
        Impact i = new Impact();
        assertEquals(Impact.DelayLevel.NONE, i.getDelayLevel());
        i.setDelaysType("delays");
        assertEquals(Impact.DelayLevel.DELAYS, i.getDelayLevel());
        i.setDelaysType("longDelays");
        assertEquals(Impact.DelayLevel.LONG, i.getDelayLevel());
        i.setDelaysType("veryLongDelays");
        assertEquals(Impact.DelayLevel.LONG, i.getDelayLevel());
    }

    @Test
    public void roadworksTypeFromDescription() {
        assertEquals(Situation.Type.SIGNALS, roadworks("A83 Tarbet - TTL").getType());
        assertEquals(Situation.Type.SIGNALS, roadworks("A9 Dalwhinnie - Stop and Go").getType());
        assertEquals(Situation.Type.LANES, roadworks("M8 J15 - Lane Closure").getType());
        assertEquals(Situation.Type.LANES, roadworks("M9 - Hard Shoulder Closure").getType());
        assertEquals(Situation.Type.CLOSURE, roadworks("M8 WB Woodside - Closure").getType());
        assertEquals(Situation.Type.HAZARD, roadworks("A77 - Speed Restriction").getType());
        assertEquals(Situation.Type.OTHER, roadworks("A82 - Signage Only").getType());
        assertEquals(Situation.Type.ROADWORKS, roadworks("A87 - Vergeworks").getType());
    }

    @Test
    public void distanceGlasgowToEdinburgh() {
        double km = GeoUtils.distanceKm(55.8642, -4.2518, 55.9533, -3.1883);
        assertEquals(67, km, 2);
    }

    @Test
    public void distanceToLinearSituationUsesNearestPoint() {
        Situation s = new Situation(Situation.Category.INCIDENT);
        LocationPoint a = new LocationPoint(LocationPoint.Role.FROM);
        a.setLatitude(55.8631); a.setLongitude(-4.2706);
        LocationPoint b = new LocationPoint(LocationPoint.Role.TO);
        b.setLatitude(56.7065); b.setLongitude(-2.8248);
        s.getLocations().add(a);
        s.getLocations().add(b);
        assertTrue(GeoUtils.distanceToSituationKm(55.8666, -4.2506, s) < 2);
        assertEquals(Double.MAX_VALUE,
                GeoUtils.distanceToSituationKm(55.8, -4.2, new Situation(Situation.Category.INCIDENT)), 0);
    }
}
