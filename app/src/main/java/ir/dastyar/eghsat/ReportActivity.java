package ir.dastyar.eghsat;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Export screen: builds the PDF report of the current Jalali month (plus the full
 * list and payment history) and lets the user keep a JSON backup of everything.
 *
 * Files are written through MediaStore into Download/DastyarAghsat, which needs no
 * runtime permission on Android 12+ and keeps the app free of FileProvider
 * dependencies.
 */
public class ReportActivity extends BaseActivity {
    private static final int PICK_BACKUP = 411;

    Theme th;
    TextView dataSummary;

    TextView tv(String s, float size, int color) { return Ui.tv(this, s, size, color); }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        th = Theme.get(this);
        getWindow().setStatusBarColor(th.bg);
        getWindow().setNavigationBarColor(th.bg);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(th.bg);
        scroll.setClipToPadding(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = tv("‹", 22, th.text);
        back.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("بازگشت");
        back.setBackground(roundedStroke(blend(th.text, th.card, 0.85f), th.border, 15, 1));
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(dp(40), dp(40)));
        TextView title = tv("گزارش و خروجی", 21, th.text);
        title.setTypeface(null, 1);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1);
        titleLp.setMarginStart(dp(10));
        top.addView(title, titleLp);
        root.addView(top, new LinearLayout.LayoutParams(-1, -2));

        dataSummary = tv("", 12, th.muted);
        dataSummary.setPadding(dp(2), dp(12), dp(2), dp(14));
        root.addView(dataSummary);

        LinearLayout pdfCard = card("📄", "خروجی PDF این ماه",
                "خلاصهٔ ماه جاری، اقساط سررسید این ماه، همهٔ اقساط با تمام فیلدها و سابقهٔ پرداخت‌ها");
        pdfCard.setOnClickListener(v -> exportPdf());
        root.addView(pdfCard, cardLp());

        LinearLayout backupCard = card("🗄", "پشتیبان‌گیری از داده‌ها",
                "همهٔ اقساط و پرداخت‌ها در یک فایل JSON ذخیره می‌شود");
        backupCard.setOnClickListener(v -> backup());
        root.addView(backupCard, cardLp());

        LinearLayout restoreCard = card("♻", "بازگردانی از پشتیبان",
                "داده‌های فعلی جایگزین می‌شوند؛ نسخهٔ فعلی هم نگه داشته می‌شود");
        restoreCard.setOnClickListener(v -> pickBackup());
        root.addView(restoreCard, cardLp());

        TextView note = tv("فایل‌های ساخته‌شده در پوشهٔ Download/DastyarAghsat گوشی ذخیره می‌شوند و "
                + "بدون نیاز به اینترنت ساخته می‌شوند.", 11, th.muted);
        note.setPadding(dp(2), dp(14), dp(2), 0);
        root.addView(note);

        scroll.addView(root);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            root.setPadding(dp(18), bars.top + dp(18), dp(18), bars.bottom + dp(24));
            return insets;
        });
        setContentView(scroll);
        refreshDataSummary();
    }

    @Override protected void onResume() {
        super.onResume();
        if (dataSummary != null) refreshDataSummary();
    }

    void refreshDataSummary() {
        int installments = Store.all(this).size();
        int payments = PaymentLog.all(this).size();
        dataSummary.setText(Ui.persianDigits(String.valueOf(installments)) + " قسط و "
                + Ui.persianDigits(String.valueOf(payments)) + " پرداخت ثبت‌شده در برنامه");
    }

    LinearLayout card(String icon, String heading, String sub) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER_VERTICAL);
        box.setPadding(dp(14), dp(13), dp(14), dp(13));
        box.setBackground(roundedStroke(th.card, th.border, 18, 1));
        TextView ic = tv(icon, 20, th.primary);
        ic.setGravity(Gravity.CENTER);
        box.addView(ic, new LinearLayout.LayoutParams(dp(34), dp(34)));
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView t = tv(heading, 15, th.text);
        t.setTypeface(null, 1);
        col.addView(t);
        TextView s = tv(sub, 11, th.muted);
        s.setPadding(0, dp(3), 0, 0);
        col.addView(s);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, -2, 1);
        colLp.setMarginStart(dp(10));
        box.addView(col, colLp);
        return box;
    }

    LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(10));
        return lp;
    }

    // ---- PDF --------------------------------------------------------------

    void exportPdf() {
        try {
            List<Installment> all = Store.all(this);
            List<PaymentLog.Event> payments = PaymentLog.all(this);
            Calendar now = Calendar.getInstance();
            int[] today = PersianDate.toJalali(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1,
                    now.get(Calendar.DAY_OF_MONTH));
            long stamp = System.currentTimeMillis();

            PdfDocument doc = PdfReport.build(all, payments, today[0], today[1], stamp);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try {
                doc.writeTo(buffer);
            } finally {
                doc.close();
            }

            String name = "Dastyar-" + today[0] + "-" + String.format(Locale.US, "%02d", today[1])
                    + "-" + stamp + ".pdf";
            Uri uri = saveToDownloads(name, "application/pdf", buffer.toByteArray());
            showResult("PDF ساخته شد",
                    "فایل در پوشهٔ Download/DastyarAghsat ذخیره شد:\n" + name,
                    uri, "application/pdf");
        } catch (Exception e) {
            toast("ساخت PDF ناموفق بود (" + e.getClass().getSimpleName() + ")");
        }
    }

    // ---- backup / restore -------------------------------------------------

    void backup() {
        try {
            String json = Store.backupJson(this);
            String name = "Dastyar-backup-" + System.currentTimeMillis() + ".json";
            Uri uri = saveToDownloads(name, "application/json", json.getBytes(StandardCharsets.UTF_8));
            showResult("پشتیبان ساخته شد",
                    "فایل در پوشهٔ Download/DastyarAghsat ذخیره شد:\n" + name
                            + "\n\nاین فایل را در جای امنی (مثل گوگل‌درایو) نگه دار.",
                    uri, "application/json");
        } catch (Exception e) {
            toast("ساخت پشتیبان ناموفق بود (" + e.getClass().getSimpleName() + ")");
        }
    }

    void pickBackup() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, PICK_BACKUP);
        } catch (Exception e) {
            toast("انتخاب فایل ممکن نشد.");
        }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_BACKUP || result != RESULT_OK || data == null || data.getData() == null) return;
        String json;
        try {
            json = readAll(data.getData());
        } catch (Exception e) {
            toast("خواندن فایل پشتیبان ممکن نشد.");
            return;
        }
        final String payload = json;
        new AlertDialog.Builder(this)
                .setTitle("بازگردانی پشتیبان")
                .setMessage("داده‌های فعلی با محتوای این فایل جایگزین می‌شوند. نسخهٔ فعلی هم به‌صورت خودکار نگه داشته می‌شود.\n\nادامه می‌دهی؟")
                .setPositiveButton("بله، بازگردان", (d, w) -> doRestore(payload))
                .setNegativeButton("انصراف", null)
                .show();
    }

    void doRestore(String json) {
        try {
            int restored = Store.restore(this, json);
            AlarmHelper.rescheduleAll(this);
            refreshDataSummary();
            toast(Ui.persianDigits(String.valueOf(restored)) + " قسط بازگردانی شد.");
        } catch (Exception e) {
            toast("فایل پشتیبان معتبر نیست یا بازگردانی ناموفق بود.");
        }
    }

    String readAll(Uri uri) throws Exception {
        InputStream in = getContentResolver().openInputStream(uri);
        if (in == null) throw new Exception("no stream");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    // ---- file helpers -----------------------------------------------------

    Uri saveToDownloads(String name, String mime, byte[] data) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, name);
        values.put(MediaStore.Downloads.MIME_TYPE, mime);
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DastyarAghsat");
        values.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new Exception("insert failed");
        OutputStream os = getContentResolver().openOutputStream(uri);
        if (os == null) throw new Exception("open failed");
        try {
            os.write(data);
        } finally {
            os.close();
        }
        ContentValues done = new ContentValues();
        done.put(MediaStore.Downloads.IS_PENDING, 0);
        getContentResolver().update(uri, done, null, null);
        return uri;
    }

    void showResult(String heading, String message, Uri uri, String mime) {
        new AlertDialog.Builder(this)
                .setTitle(heading)
                .setMessage(message)
                .setPositiveButton("باز کردن", (d, w) -> openFile(uri, mime))
                .setNeutralButton("اشتراک‌گذاری", (d, w) -> shareFile(uri, mime))
                .setNegativeButton("بستن", null)
                .show();
    }

    void openFile(Uri uri, String mime) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, mime)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch (Exception e) {
            toast("برنامه‌ای برای باز کردن این فایل روی گوشی پیدا نشد.");
        }
    }

    void shareFile(Uri uri, String mime) {
        try {
            Intent i = new Intent(Intent.ACTION_SEND)
                    .setType(mime)
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "ارسال فایل"));
        } catch (Exception e) {
            toast("ارسال فایل ممکن نشد.");
        }
    }

    void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
