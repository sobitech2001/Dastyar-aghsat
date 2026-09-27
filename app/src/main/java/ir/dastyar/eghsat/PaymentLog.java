package ir.dastyar.eghsat;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class PaymentLog {
    private static final String PREF = "dastyar_store";
    private static final String KEY = "payments";

    public static class Event {
        public long installmentId, amount, timestamp;
    }

    public static List<Event> all(Context c) {
        List<Event> list = new ArrayList<>();
        try {
            String raw = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]");
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Event e = new Event();
                e.installmentId = o.optLong("installmentId");
                e.amount = o.optLong("amount");
                e.timestamp = o.optLong("timestamp");
                list.add(e);
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static void record(Context c, long installmentId, long amount) {
        try {
            JSONArray a = new JSONArray();
            for (Event e : all(c)) {
                JSONObject o = new JSONObject();
                o.put("installmentId", e.installmentId);
                o.put("amount", e.amount);
                o.put("timestamp", e.timestamp);
                a.put(o);
            }
            JSONObject ne = new JSONObject();
            ne.put("installmentId", installmentId);
            ne.put("amount", amount);
            ne.put("timestamp", System.currentTimeMillis());
            a.put(ne);
            c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply();
        } catch (Exception ignored) {}
    }

    /** Removes every logged payment for an installment — called when the installment itself is deleted,
     *  so a deleted installment stops affecting "paid this month" / monthly totals. */
    public static void deleteForInstallment(Context c, long installmentId) {
        try {
            JSONArray a = new JSONArray();
            for (Event e : all(c)) {
                if (e.installmentId == installmentId) continue;
                JSONObject o = new JSONObject();
                o.put("installmentId", e.installmentId);
                o.put("amount", e.amount);
                o.put("timestamp", e.timestamp);
                a.put(o);
            }
            c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply();
        } catch (Exception ignored) {}
    }
}
