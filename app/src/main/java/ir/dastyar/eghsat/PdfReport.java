package ir.dastyar.eghsat;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Builds the printable A4 report with the platform PdfDocument API, so the app keeps
 * its zero-dependency build. Text is laid out through StaticLayout with an RTL
 * paragraph direction, which gives correct Persian shaping and wrapping.
 *
 * The report is print-oriented (white paper, dark ink) and deliberately independent
 * of the app theme. It contains every field the app stores for an installment plus
 * the recorded payment history.
 */
final class PdfReport {

    private static final int PAGE_W = 595;   // A4 at 72 dpi
    private static final int PAGE_H = 842;
    private static final float MARGIN = 34f;
    private static final float CONTENT_W = PAGE_W - 2 * MARGIN;

    private static final int INK = Color.rgb(24, 30, 40);
    private static final int SOFT = Color.rgb(96, 108, 124);
    private static final int ACCENT = Color.rgb(45, 91, 153);
    private static final int RULE = Color.rgb(214, 222, 232);

    private static final Paint LINE = new Paint();
    static {
        LINE.setColor(RULE);
        LINE.setStrokeWidth(0.8f);
    }

    private PdfReport() {}

    /** Internal drawing state: current page, cursor and the shared text paints. */
    private static final class Ctx {
        PdfDocument doc;
        PdfDocument.Page page;
        Canvas canvas;
        int pageNo;
        float y;
        final float bottom = PAGE_H - MARGIN - 26f;
        final TextPaint h1 = textPaint(15.5f, true, INK);
        final TextPaint h2 = textPaint(11.5f, true, ACCENT);
        final TextPaint body = textPaint(9.5f, false, INK);
        final TextPaint strong = textPaint(9.5f, true, INK);
        final TextPaint small = textPaint(8.5f, false, SOFT);
    }

    private static TextPaint textPaint(float size, boolean bold, int color) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(size);
        p.setColor(color);
        p.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return p;
    }

    /**
     * @param all      every installment, active or finished
     * @param payments the recorded payment log
     * @param year     Jalali year of the month being reported
     * @param month    Jalali month (1-12) of the month being reported
     */
    static PdfDocument build(List<Installment> all, List<PaymentLog.Event> payments,
                             int year, int month, long now) {
        Ctx c = new Ctx();
        c.doc = new PdfDocument();
        startPage(c);

        para(c, "گزارش اقساط", c.h1, 0f, 2f);
        para(c, "دستیار اقساط  •  تاریخ گزارش: " + date(now) + " ساعت " + clock(now)
                + "  •  ماه: " + monthLabel(year, month), c.small, 0f, 5f);
        drawRule(c);

        // ---- monthly summary ----
        int monthCount = 0, monthPaidCount = 0, monthOverdue = 0, openCount = 0, finishedCount = 0;
        long monthTotal = 0, monthPaidAmount = 0, monthRemaining = 0, totalDebt = 0;
        for (Installment x : all) {
            if (x.isFinished()) finishedCount++;
            else {
                openCount++;
                totalDebt += x.amount * Math.max(0, x.totalCount - x.paidCount);
            }
            int idx = x.paymentIndexForMonth(year, month);
            if (idx < 0) continue;
            monthCount++;
            monthTotal += x.amount;
            if (idx < x.paidCount) {
                monthPaidCount++;
                monthPaidAmount += x.amount;
            } else {
                monthRemaining += x.amount;
                if (x.dueMillisForIndex(idx) < now) monthOverdue++;
            }
        }

        section(c, "خلاصهٔ " + monthLabel(year, month));
        kv(c, "تعداد اقساط این ماه", fa(monthCount) + " قسط");
        kv(c, "جمع مبلغ اقساط این ماه", money(monthTotal));
        kv(c, "پرداخت‌شده در این ماه", fa(monthPaidCount) + " قسط  •  " + money(monthPaidAmount));
        kv(c, "باقی‌ماندهٔ این ماه", money(monthRemaining));
        kv(c, "اقساط معوق این ماه", fa(monthOverdue) + " قسط");
        kv(c, "بدهی کل (همهٔ اقساط فعال)", money(totalDebt));
        kv(c, "اقساط فعال / تکمیل‌شده", fa(openCount) + " فعال  •  " + fa(finishedCount) + " تکمیل‌شده");
        kv(c, "رکوردهای ثبت‌شده", fa(all.size()) + " قسط  •  " + fa(payments.size()) + " پرداخت");
        c.y += 5f;

        // ---- installments due this month ----
        section(c, "اقساط سررسید این ماه");
        final List<Installment> monthItems = new ArrayList<>();
        for (Installment x : all) if (x.paymentIndexForMonth(year, month) >= 0) monthItems.add(x);
        if (monthItems.isEmpty()) {
            para(c, "در این ماه قسطی برنامه‌ریزی نشده است.", c.small, 1f, 9f);
        } else {
            Collections.sort(monthItems, (a, b) -> Long.compare(
                    a.dueMillisForIndex(a.paymentIndexForMonth(year, month)),
                    b.dueMillisForIndex(b.paymentIndexForMonth(year, month))));
            for (Installment x : monthItems) installmentBlock(c, x, x.paymentIndexForMonth(year, month), now);
        }

        // ---- every installment, all fields ----
        section(c, "همهٔ اقساط");
        final List<Installment> sorted = new ArrayList<>(all);
        Collections.sort(sorted, (a, b) -> Long.compare(a.firstDueMillis, b.firstDueMillis));
        if (sorted.isEmpty()) {
            para(c, "هنوز قسطی ثبت نشده است.", c.small, 1f, 9f);
        } else {
            for (Installment x : sorted) installmentBlock(c, x, x.isFinished() ? x.totalCount : x.paidCount, now);
        }

        // ---- payment history ----
        section(c, "سابقهٔ پرداخت‌ها");
        if (payments.isEmpty()) {
            para(c, "هنوز پرداختی ثبت نشده است.", c.small, 1f, 9f);
        } else {
            final List<PaymentLog.Event> events = new ArrayList<>(payments);
            Collections.sort(events, (a, b) -> Long.compare(a.timestamp, b.timestamp));
            long sum = 0;
            for (PaymentLog.Event e : events) {
                sum += e.amount;
                para(c, date(e.timestamp) + "   •   " + money(e.amount) + "   •   " + titleOf(all, e.installmentId),
                        c.body, 0f, 2f);
            }
            para(c, "جمع " + fa(events.size()) + " پرداخت ثبت‌شده: " + money(sum), c.strong, 5f, 8f);
        }

        para(c, "پایان گزارش  •  ساخته‌شده در " + date(now) + " ساعت " + clock(now), c.small, 6f, 0f);

        finishPage(c);
        return c.doc;
    }

    private static void installmentBlock(Ctx c, Installment x, int index, long now) {
        String bank = Banks.byId(x.bank).name;
        boolean hasIndex = index >= 0 && index < x.totalCount;
        long paidAmount = x.amount * Math.max(0, Math.min(x.paidCount, x.totalCount));
        long remaining = x.amount * Math.max(0, x.totalCount - x.paidCount);

        ensure(c, 96f); // keep the heading and the first lines of a block on one page

        para(c, "«" + x.title + "»  —  " + bank, c.strong, 6f, 2f);
        if (hasIndex) {
            para(c, "قسط " + fa(index + 1) + " از " + fa(x.totalCount)
                    + "   |   مبلغ هر قسط: " + money(x.amount)
                    + "   |   سررسید: " + date(x.dueMillisForIndex(index)), c.body, 0f, 1.5f);
            para(c, "وضعیت این قسط: " + statusText(x, index, now), c.body, 0f, 1.5f);
        } else {
            para(c, "همهٔ " + fa(x.totalCount) + " قسط پرداخت شده"
                    + "   |   مبلغ هر قسط: " + money(x.amount), c.body, 0f, 1.5f);
        }
        para(c, "پرداخت‌شده: " + fa(x.paidCount) + " از " + fa(x.totalCount) + " قسط (" + money(paidAmount) + ")"
                + "   |   ماندهٔ پرداخت: " + money(remaining), c.body, 0f, 1.5f);
        para(c, "مبلغ کل قرارداد: " + money(x.amount * Math.max(1, x.totalCount))
                + "   |   روز سررسید ماهانه: " + fa(x.dueDay)
                + "   |   اولین سررسید: " + date(x.firstDueMillis), c.body, 0f, 1.5f);
        para(c, "یادآور: " + reminderText(x) + "   |   رنگ قسط: " + colorText(x.color), c.body, 0f, 1.5f);
        para(c, "وضعیت قسط: " + (x.isFinished() ? "تکمیل‌شده / غیرفعال" : "فعال")
                + "   |   شناسه: " + fa(x.id), c.small, 0f, 4f);
        drawRule(c);
        c.y += 3f;
    }

    private static String statusText(Installment x, int index, long now) {
        if (index < x.paidCount) return "پرداخت شده";
        if (index >= x.totalCount) return "تکمیل‌شده";
        long due = x.dueMillisForIndex(index);
        long days = (due - now) / 86400000L;
        if (due < now) return "سررسید گذشته (" + fa(Math.abs(days)) + " روز)";
        if (days == 0) return "سررسید امروز";
        return "در انتظار پرداخت (" + fa(days) + " روز مانده)";
    }

    private static String reminderText(Installment x) {
        if (x.reminderMode == 0) return "خاموش";
        String when;
        switch (x.reminderMode) {
            case 1: when = "در روز سررسید"; break;
            case 2: when = "دو روز قبل"; break;
            case 3: when = "یک هفته قبل"; break;
            default: when = "نامشخص"; break;
        }
        return when + "، ساعت " + fa(String.format(Locale.US, "%02d:%02d", x.reminderHour, x.reminderMinute));
    }

    private static String colorText(int color) {
        if (color == 0) return "خودکار (بر اساس وضعیت)";
        return "#" + Integer.toHexString(color & 0xFFFFFF).toUpperCase(Locale.US);
    }

    private static String titleOf(List<Installment> all, long id) {
        for (Installment x : all) if (x.id == id) return "«" + x.title + "»";
        return "قسط حذف‌شده";
    }

    // ---- layout helpers ---------------------------------------------------

    private static void startPage(Ctx c) {
        c.page = c.doc.startPage(new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, c.pageNo + 1).create());
        c.pageNo++;
        c.canvas = c.page.getCanvas();
        c.canvas.drawColor(Color.WHITE);
        c.y = MARGIN;
        if (c.pageNo > 1) {
            StaticLayout head = layout("گزارش اقساط — ادامه", c.small);
            drawAt(c, head, MARGIN, c.y);
            c.y += head.getHeight() + 4f;
            c.canvas.drawRect(MARGIN, c.y, PAGE_W - MARGIN, c.y + 0.8f, LINE);
            c.y += 10f;
        }
        StaticLayout foot = layout("دستیار اقساط  •  صفحهٔ " + fa(c.pageNo), c.small);
        drawAt(c, foot, MARGIN, PAGE_H - MARGIN + 4f);
    }

    private static void finishPage(Ctx c) {
        if (c.page != null) {
            c.doc.finishPage(c.page);
            c.page = null;
        }
    }

    private static void ensure(Ctx c, float height) {
        if (c.page == null || c.y + height > c.bottom) {
            finishPage(c);
            startPage(c);
        }
    }

    private static StaticLayout layout(String text, TextPaint p) {
        return StaticLayout.Builder.obtain(text, 0, text.length(), p, (int) CONTENT_W)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setIncludePad(false)
                .setLineSpacing(0f, 1.22f)
                .build();
    }

    private static void drawAt(Ctx c, StaticLayout sl, float x, float y) {
        c.canvas.save();
        c.canvas.translate(x, y);
        sl.draw(c.canvas);
        c.canvas.restore();
    }

    /** Draws a paragraph at the cursor, continuing on a new page when it does not fit. */
    private static void para(Ctx c, String text, TextPaint p, float gapBefore, float gapAfter) {
        StaticLayout sl = layout(text, p);
        ensure(c, gapBefore + sl.getHeight());
        c.y += gapBefore;
        drawAt(c, sl, MARGIN, c.y);
        c.y += sl.getHeight() + gapAfter;
    }

    private static void drawRule(Ctx c) {
        ensure(c, 8f);
        c.canvas.drawRect(MARGIN, c.y, PAGE_W - MARGIN, c.y + 0.8f, LINE);
        c.y += 7f;
    }

    private static void section(Ctx c, String text) {
        para(c, text, c.h2, 4f, 2f);
        drawRule(c);
    }

    private static void kv(Ctx c, String label, String value) {
        para(c, label + ": " + value, c.body, 0f, 2.5f);
    }

    // ---- formatting -------------------------------------------------------

    private static String fa(long n) { return Ui.persianDigits(String.valueOf(n)); }

    private static String fa(String s) { return Ui.persianDigits(s); }

    private static String money(long n) { return Ui.moneyFa(n); }

    private static String date(long millis) { return Ui.jalaliFa(millis); }

    private static String clock(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return fa(String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
    }

    private static String monthLabel(int year, int month) {
        String name = (month >= 1 && month <= 12) ? PersianDate.MONTH_NAMES[month - 1] : String.valueOf(month);
        return name + " " + fa(year);
    }
}
