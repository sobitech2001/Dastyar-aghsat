package ir.dastyar.eghsat;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Installments live in SharedPreferences as a JSON array. The storage keys and the
 * per-field JSON names are kept exactly as before, because an installed copy of the
 * app must keep reading its existing data after an update.
 */
public class Store {
    private static final String PREF = "dastyar_store";
    private static final String KEY = "installments";
    /** Copy of a payload that could not be parsed, so a later save never destroys it. */
    private static final String KEY_CORRUPT = "installments_corrupt";
    private static final String KEY_BEFORE_RESTORE = "installments_before_restore";
    private static final String KEY_BEFORE_RESTORE_PAYMENTS = "payments_before_restore";

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static List<Installment> all(Context c) {
        ArrayList<Installment> list = new ArrayList<>();
        String raw = null;
        try {
            raw = prefs(c).getString(KEY, "[]");
            JSONArray a = new JSONArray(raw);
            for (int i=0;i<a.length();i++) list.add(Installment.fromJson(a.getJSONObject(i)));
        } catch (Exception e) {
            // Never let a corrupt payload disappear silently: keep a copy first.
            try {
                if (raw != null && !raw.trim().isEmpty() && !raw.trim().equals("[]"))
                    prefs(c).edit().putString(KEY_CORRUPT, raw).commit();
            } catch (Exception ignored) {}
        }
        return list;
    }

    public static void saveAll(Context c, List<Installment> list) {
        try {
            JSONArray a = new JSONArray();
            for (Installment x : list) a.put(x.toJson());
            prefs(c).edit().putString(KEY, a.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static void upsert(Context c, Installment item) {
        List<Installment> list = all(c);
        boolean found = false;
        for (int i=0;i<list.size();i++) if (list.get(i).id == item.id) {
            list.set(i, item); found = true; break;
        }
        if (!found) list.add(item);
        saveAll(c, list);
    }

    public static Installment find(Context c, long id) {
        for (Installment x : all(c)) if (x.id == id) return x;
        return null;
    }

    public static void delete(Context c, long id) {
        List<Installment> list = all(c);
        for (int i = 0; i < list.size(); i++) if (list.get(i).id == id) { list.remove(i); break; }
        saveAll(c, list);
    }

    // ---- backup / restore -------------------------------------------------
    // Purely additive: nothing here changes how the app reads its own data.

    /** Full backup as one JSON document (installments + payment log). */
    public static String backupJson(Context c) throws Exception {
        JSONObject o = new JSONObject();
        o.put("app", "DastyarEghsat");
        o.put("format", 1);
        o.put("savedAt", System.currentTimeMillis());
        o.put("installments", new JSONArray(prefs(c).getString(KEY, "[]")));
        o.put("payments", new JSONArray(PaymentLog.raw(c)));
        return o.toString(2);
    }

    /**
     * Replaces the current data with a backup document. Returns how many installments
     * were restored. The previous data is kept in memory first, so a wrong file cannot
     * wipe the app.
     */
    public static int restore(Context c, String json) throws Exception {
        JSONObject o = new JSONObject(json);
        JSONArray items = o.optJSONArray("installments");
        if (items == null) throw new Exception("no installments array");
        JSONArray pays = o.optJSONArray("payments");

        ArrayList<Installment> parsed = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) parsed.add(Installment.fromJson(items.getJSONObject(i)));

        JSONArray out = new JSONArray();
        for (Installment x : parsed) out.put(x.toJson());

        prefs(c).edit()
                .putString(KEY_BEFORE_RESTORE, prefs(c).getString(KEY, "[]"))
                .putString(KEY_BEFORE_RESTORE_PAYMENTS, PaymentLog.raw(c))
                .commit();
        prefs(c).edit().putString(KEY, out.toString()).commit();
        if (pays != null) PaymentLog.writeRaw(c, pays.toString());
        return parsed.size();
    }

}
