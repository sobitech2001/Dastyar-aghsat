package ir.dastyar.eghsat;

import android.content.Context;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public class Store {
    private static final String PREF = "dastyar_store";
    private static final String KEY = "installments";

    public static List<Installment> all(Context c) {
        ArrayList<Installment> list = new ArrayList<>();
        try {
            String raw = c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]");
            JSONArray a = new JSONArray(raw);
            for (int i=0;i<a.length();i++) list.add(Installment.fromJson(a.getJSONObject(i)));
        } catch (Exception ignored) {}
        return list;
    }

    public static void saveAll(Context c, List<Installment> list) {
        try {
            JSONArray a = new JSONArray();
            for (Installment x : list) a.put(x.toJson());
            c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply();
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
}
