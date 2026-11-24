package org.me.gcu.filby_fraser_s2367234.ui;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import org.me.gcu.filby_fraser_s2367234.R;
import org.me.gcu.filby_fraser_s2367234.model.Impact;
import org.me.gcu.filby_fraser_s2367234.model.Situation;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Icons, colours and formatting shared by the different screens
public final class SituationStyle {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.UK);

    private SituationStyle() { }

    @DrawableRes
    public static int iconFor(Situation.Type type) {
        switch (type) {
            case CLOSURE: return R.drawable.ic_type_closure;
            case LANES: return R.drawable.ic_type_lanes;
            case SIGNALS: return R.drawable.ic_type_signals;
            case QUEUE: return R.drawable.ic_type_queue;
            case VEHICLE: return R.drawable.ic_type_vehicle;
            case WEATHER: return R.drawable.ic_type_weather;
            case ROADWORKS: return R.drawable.ic_type_roadworks;
            case HAZARD: return R.drawable.ic_type_hazard;
            default: return R.drawable.ic_type_other;
        }
    }

    @ColorInt
    public static int colourFor(Context c, Situation.Type type) {
        int res;
        switch (type) {
            case CLOSURE: res = R.color.type_closure; break;
            case LANES: res = R.color.type_lanes; break;
            case SIGNALS: res = R.color.type_signals; break;
            case QUEUE: res = R.color.type_queue; break;
            case VEHICLE: res = R.color.type_vehicle; break;
            case WEATHER: res = R.color.type_weather; break;
            case ROADWORKS: res = R.color.type_roadworks; break;
            case HAZARD: res = R.color.type_hazard; break;
            default: res = R.color.type_other; break;
        }
        return ContextCompat.getColor(c, res);
    }

    @ColorInt
    public static int delayColour(Context c, Impact.DelayLevel level) {
        switch (level) {
            case LONG: return ContextCompat.getColor(c, R.color.delay_long);
            case DELAYS: return ContextCompat.getColor(c, R.color.delay_medium);
            case SHORT: return ContextCompat.getColor(c, R.color.delay_short);
            default: return ContextCompat.getColor(c, R.color.delay_none);
        }
    }

    public static String delayLabel(Impact.DelayLevel level) {
        switch (level) {
            case LONG: return "Long delays";
            case DELAYS: return "Delays";
            case SHORT: return "Short delays";
            default: return "No delays reported";
        }
    }

    // Roadworks are coloured by delay; incidents by their type
    @ColorInt
    public static int stripColour(Context c, Situation s) {
        if (s.getCategory() == Situation.Category.ROADWORKS) {
            return delayColour(c, s.getImpact().getDelayLevel());
        }
        return colourFor(c, s.getType());
    }

    public static String formatTime(OffsetDateTime t) {
        if (t == null) return "Not given";
        return t.atZoneSameInstant(ZoneId.of("Europe/London")).format(DATE_TIME);
    }

    public static String formatDuration(Duration d) {
        if (d == null) return null;
        if (d.isNegative()) return "Ended";
        long days = d.toDays();
        long hours = d.minusDays(days).toHours();
        long mins = d.minusDays(days).minusHours(hours).toMinutes();
        if (days > 0) return days + (days == 1 ? " day" : " days") + (hours > 0 ? " " + hours + " h" : "");
        if (hours > 0) return hours + " h" + (mins > 0 ? " " + mins + " min" : "");
        return mins + " min";
    }
}
