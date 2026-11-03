package ir.dastyar.eghsat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;

/** Pay / delete flows shared by the home screen and the dashboard. */
final class InstallmentActions {
    private InstallmentActions() {}

    /** Records one more payment (no confirmation). */
    static void markPaid(Context c, Installment x) {
        x.paidCount++;
        if (x.paidCount >= x.totalCount) x.active = false;
        Store.upsert(c, x);
        PaymentLog.record(c, x.id, x.amount);
        AlarmHelper.schedule(c, x);
    }

    static void confirmPay(Activity a, Installment x, Runnable onDone) {
        int n = Math.min(x.paidCount + 1, x.totalCount);
        new AlertDialog.Builder(a).setTitle("تأیید پرداخت قسط")
                .setMessage("از پرداخت قسط " + n + " از " + x.totalCount + " برای «" + x.title + "» به مبلغ " + Ui.money(x.amount) + " مطمئنی؟")
                .setPositiveButton("بله، پرداخت شد", (d, w) -> { markPaid(a, x); onDone.run(); })
                .setNegativeButton("انصراف", null).show();
    }

    static void confirmDelete(Activity a, Installment x, Runnable onDone) {
        new AlertDialog.Builder(a)
                .setTitle("حذف قسط")
                .setMessage("از حذف «" + x.title + "» مطمئنید؟")
                .setPositiveButton("بله", (d, w) ->
                        new AlertDialog.Builder(a)
                                .setTitle("تأیید نهایی حذف")
                                .setMessage("این کار بدون بازگشت است. آیا انجام شود؟")
                                .setPositiveButton("بله، حذف کن", (d2, w2) -> {
                                    AlarmHelper.cancel(a, x.id);
                                    Store.delete(a, x.id);
                                    PaymentLog.deleteForInstallment(a, x.id);
                                    onDone.run();
                                })
                                .setNegativeButton("خیر", null)
                                .show())
                .setNegativeButton("خیر", null)
                .show();
    }
}
