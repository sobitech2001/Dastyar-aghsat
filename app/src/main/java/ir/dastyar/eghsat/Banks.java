package ir.dastyar.eghsat;

import android.graphics.Color;

/**
 * A fixed list of Iranian banks for tagging an installment. The app is fully
 * offline (see README), so there's no network to fetch real bank logo
 * images — each bank instead gets a colored badge with its initial letter,
 * shown on cards and in the picker dropdown.
 */
public class Banks {

    public static class Bank {
        public final String id, name;
        public final int color;
        Bank(String id, String name, int color) { this.id = id; this.name = name; this.color = color; }
    }

    public static final Bank NONE = new Bank("", "بدون بانک", Color.rgb(150, 150, 162));

    public static final Bank[] ALL = {
            NONE,
            new Bank("meli", "بانک ملی", Color.rgb(20, 95, 55)),
            new Bank("mellat", "بانک ملت", Color.rgb(224, 58, 52)),
            new Bank("saderat", "بانک صادرات", Color.rgb(28, 102, 180)),
            new Bank("tejarat", "بانک تجارت", Color.rgb(0, 121, 165)),
            new Bank("saman", "بانک سامان", Color.rgb(132, 60, 150)),
            new Bank("pasargad", "بانک پاسارگاد", Color.rgb(0, 148, 128)),
            new Bank("parsian", "بانک پارسیان", Color.rgb(222, 140, 20)),
            new Bank("refah", "بانک رفاه", Color.rgb(40, 112, 92)),
            new Bank("resalat", "بانک رسالت", Color.rgb(62, 150, 80)),
            new Bank("sepah", "بانک سپه", Color.rgb(40, 62, 132)),
            new Bank("post", "پست بانک", Color.rgb(0, 100, 92)),
            new Bank("keshavarzi", "بانک کشاورزی", Color.rgb(62, 140, 42)),
            new Bank("maskan", "بانک مسکن", Color.rgb(150, 92, 30)),
            new Bank("shahr", "بانک شهر", Color.rgb(200, 60, 122)),
            new Bank("ayandeh", "بانک آینده", Color.rgb(22, 42, 132)),
            new Bank("dey", "بانک دی", Color.rgb(150, 40, 60)),
            new Bank("other", "سایر", Color.rgb(102, 102, 112)),
    };

    public static Bank byId(String id) {
        if (id != null) for (Bank b : ALL) if (b.id.equals(id)) return b;
        return NONE;
    }

    /** One-letter badge text — the last word of the bank name (e.g. "بانک ملی" -> "ملی"'s م). */
    public static String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        String last = parts[parts.length - 1];
        return last.isEmpty() ? "?" : last.substring(0, 1);
    }
}
