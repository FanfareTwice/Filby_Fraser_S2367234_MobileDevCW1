package org.me.gcu.filby_fraser_s2367234.model;

public class Impact {

    public enum DelayLevel { NONE, SHORT, DELAYS, LONG }

    private Integer numberOfOperationalLanes;
    private Integer numberOfLanesRestricted;
    private String capacityRemaining;
    private String delaysType;          // e.g. "delays", "longDelays"
    private Integer delayTimeSeconds;   // <delayTimeValue>, if present

    public Integer getNumberOfOperationalLanes() { return numberOfOperationalLanes; }
    public Integer getNumberOfLanesRestricted() { return numberOfLanesRestricted; }
    public String getCapacityRemaining() { return capacityRemaining; }
    public String getDelaysType() { return delaysType; }
    public Integer getDelayTimeSeconds() { return delayTimeSeconds; }

    public void setNumberOfOperationalLanes(Integer v) { numberOfOperationalLanes = v; }
    public void setNumberOfLanesRestricted(Integer v) { numberOfLanesRestricted = v; }
    public void setCapacityRemaining(String v) { capacityRemaining = v; }
    public void setDelaysType(String v) { delaysType = v; }
    public void setDelayTimeSeconds(Integer v) { delayTimeSeconds = v; }

    public boolean isEmpty() {
        return numberOfOperationalLanes == null && numberOfLanesRestricted == null
                && capacityRemaining == null && delaysType == null && delayTimeSeconds == null;
    }

    public DelayLevel getDelayLevel() {
        if (delaysType != null) {
            String t = delaysType.toLowerCase();
            if (t.contains("verylong") || t.contains("long")) return DelayLevel.LONG;
            if (t.contains("negligible") || t.contains("short")) return DelayLevel.SHORT;
            if (t.contains("delay")) return DelayLevel.DELAYS;
        }
        if (delayTimeSeconds != null) {
            if (delayTimeSeconds >= 30 * 60) return DelayLevel.LONG;
            if (delayTimeSeconds >= 10 * 60) return DelayLevel.DELAYS;
            if (delayTimeSeconds > 0) return DelayLevel.SHORT;
        }
        return DelayLevel.NONE;
    }
}
