package ir.dastyar.eghsat;

import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Installment {
    public long id;
    public String title = "";
    public long amount;
    public int totalCount = 1;
    public int paidCount = 0;
    public int dueDay = 1;
    public long firstDueMillis;
    public boolean active = true;
    public String bank = "";
    public int color = 0; // 0 = automatic (status-based); otherwise a color from Palette.COLORS
    // Reminder: 0=off, 1=at due time, 2=two days before, 3=one week before.
    public int reminderMode = 0;
    public int reminderHour = 9;
    public int reminderMinute = 0;

    public JSONObject toJson() throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("amount", amount);
        o.put("totalCount", totalCount);
        o.put("paidCount", paidCount);
        o.put("dueDay", dueDay);
        o.put("firstDueMillis", firstDueMillis);
        o.put("active", active);
        o.put("bank", bank);
        o.put("color", color);
        o.put("reminderMode", reminderMode);
        o.put("reminderHour", reminderHour);
        o.put("reminderMinute", reminderMinute);
        return o;
    }

    public static Installment fromJson(JSONObject o) {
        Installment x = new Installment();
        x.id = o.optLong("id");
        x.title = o.optString("title");
        x.amount = o.optLong("amount");
        x.totalCount = o.optInt("totalCount", 1);
        x.paidCount = o.optInt("paidCount", 0);
        x.dueDay = o.optInt("dueDay", 1);
        x.firstDueMillis = o.optLong("firstDueMillis");
        x.active = o.optBoolean("active", true);
        x.bank = o.optString("bank", "");
        x.color = o.optInt("color", 0);
        x.reminderMode = o.optInt("reminderMode", 0);
        x.reminderHour = o.optInt("reminderHour", 9);
        x.reminderMinute = o.optInt("reminderMinute", 0);
        return x;
    }

    public String dueText() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(nextDueMillis());
        int[] j = PersianDate.toJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        return String.format(Locale.US, "%04d/%02d/%02d", j[0], j[1], j[2]);
    }

    /** Returns the due date for a zero-based payment index from the first due date. */
    public long dueMillisForIndex(int paymentIndex) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(firstDueMillis);
        int[] j = PersianDate.toJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));

        // The recurrence is Jalali, not Gregorian. This keeps the selected
        // Persian day aligned across every future month.
        int totalMonths = (j[1] - 1) + Math.max(0, paymentIndex);
        int jy = j[0] + totalMonths / 12;
        int jm = totalMonths % 12 + 1;
        int maxDay = PersianDate.daysInJalaliMonth(jy, jm);
        int jd = Math.min(dueDay, maxDay);

        int[] g = PersianDate.toGregorian(jy, jm, jd);
        Calendar out = Calendar.getInstance();
        out.set(g[0], g[1] - 1, g[2], 9, 0, 0);
        out.set(Calendar.MILLISECOND, 0);
        return out.getTimeInMillis();
    }

    /** Returns the zero-based installment index belonging to a Jalali month, or -1 when none. */
    public int paymentIndexForMonth(int jy, int jm) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(firstDueMillis);
        int[] first = PersianDate.toJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        int firstMonth = first[0] * 12 + (first[1] - 1);
        int targetMonth = jy * 12 + (jm - 1);
        int index = targetMonth - firstMonth;
        if (index < 0 || index >= totalCount) return -1;
        return index;
    }

    public long nextDueMillis() {
        return dueMillisForIndex(paidCount);
    }

    public boolean isFinished() { return paidCount >= totalCount || !active; }

    /** True if the next due date is within 3 days (or already overdue). */
    public boolean isDueSoonOrOverdue() {
        long threeDays = 3L * 24 * 60 * 60 * 1000;
        return nextDueMillis() - System.currentTimeMillis() <= threeDays;
    }

    /**
     * Show an installment on the home dashboard only when its current
     * installment belongs to the current Jalali month (or is overdue).
     *
     * After paying a month, nextDueMillis() advances to the following
     * Jalali month. That makes the paid installment disappear from the
     * dashboard until that next month begins, while the calendar and
     * management screens can still show future installments.
     */
    public boolean isVisibleOnHome() {
        if (isFinished()) return false;

        Calendar now = Calendar.getInstance();
        int[] today = PersianDate.toJalali(
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH));

        Calendar due = Calendar.getInstance();
        due.setTimeInMillis(nextDueMillis());
        int[] next = PersianDate.toJalali(
                due.get(Calendar.YEAR),
                due.get(Calendar.MONTH) + 1,
                due.get(Calendar.DAY_OF_MONTH));

        int todayMonth = today[0] * 12 + (today[1] - 1);
        int dueMonth = next[0] * 12 + (next[1] - 1);

        // Current month or an overdue installment remains visible.
        // Future-month installments stay hidden until their month starts.
        return dueMonth <= todayMonth;
    }
}
