package ir.dastyar.eghsat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.os.CancellationSignal;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Base class of every screen: shared view helpers + the app lock. While the app is locked it covers the screen with an opaque
 * overlay and asks for fingerprint / face / screen-lock (pattern, PIN, password).
 */
public abstract class BaseActivity extends Activity {
    private static final int AUTH = BiometricManager.Authenticators.BIOMETRIC_STRONG
            | BiometricManager.Authenticators.DEVICE_CREDENTIAL;

    private FrameLayout lockOverlay;
    private TextView lockMsg, lockBtn;
    private CancellationSignal cancel;
    private boolean needsSetup = false;

    // ---- shared view helpers (see Ui) ----
    int dp(float v) { return Ui.dp(this, v); }
    android.graphics.drawable.GradientDrawable rounded(int color, float radiusDp) { return Ui.rounded(this, color, radiusDp); }
    android.graphics.drawable.GradientDrawable roundedStroke(int color, int strokeColor, float radiusDp, float strokeDp) { return Ui.roundedStroke(this, color, strokeColor, radiusDp, strokeDp); }
    String money(long n) { return Ui.money(n); }
    int blend(int foreground, int background, float backgroundWeight) { return Ui.blend(foreground, background, backgroundWeight); }

    @Override protected void onStart() {
        super.onStart();
        if (AppLock.locked) showLock();
    }

    @Override protected void onResume() {
        super.onResume();
        if (AppLock.locked && lockOverlay != null && !AppLock.authenticating && !AppLock.userDismissed) startAuth();
    }

    @Override protected void onStop() {
        if (cancel != null) { cancel.cancel(); cancel = null; }
        AppLock.authenticating = false;
        super.onStop();
    }

    /** called right after a successful unlock */
    protected void onUnlocked() {}

    private void showLock() {
        if (lockOverlay != null) return;
        Theme th = Theme.get(this);
        lockOverlay = new FrameLayout(this);
        lockOverlay.setBackgroundColor(th.bg);
        lockOverlay.setClickable(true);
        lockOverlay.setFocusable(true);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        col.setPadding(dp(32), dp(32), dp(32), dp(32));

        TextView icon = new TextView(this); icon.setText("🔒"); icon.setTextSize(54); icon.setGravity(Gravity.CENTER);
        col.addView(icon);
        TextView title = new TextView(this); title.setText("دستیار اقساط"); title.setTextSize(22);
        title.setTextColor(th.text); title.setTypeface(null, Typeface.BOLD); title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(12), 0, dp(6)); col.addView(title);
        lockMsg = new TextView(this); lockMsg.setText("برای ورود، هویتت رو تأیید کن"); lockMsg.setTextSize(14);
        lockMsg.setTextColor(th.muted); lockMsg.setGravity(Gravity.CENTER); lockMsg.setPadding(0, 0, 0, dp(22));
        col.addView(lockMsg);
        lockBtn = new TextView(this); lockBtn.setText("باز کردن قفل"); lockBtn.setTextSize(16);
        lockBtn.setTextColor(th.primaryText); lockBtn.setTypeface(null, Typeface.BOLD); lockBtn.setGravity(Gravity.CENTER);
        GradientDrawable g = new GradientDrawable(); g.setColor(th.primary); g.setCornerRadius(dp(18));
        lockBtn.setBackground(g);
        lockBtn.setOnClickListener(v -> {
            if (needsSetup) startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            else startAuth();
        });
        col.addView(lockBtn, new LinearLayout.LayoutParams(dp(200), dp(50)));

        lockOverlay.addView(col, new FrameLayout.LayoutParams(-1, -1));
        addContentView(lockOverlay, new ViewGroup.LayoutParams(-1, -1));
        lockOverlay.bringToFront();
    }

    private void hideLock() {
        if (lockOverlay != null) {
            ViewGroup p = (ViewGroup) lockOverlay.getParent();
            if (p != null) p.removeView(lockOverlay);
            lockOverlay = null;
        }
    }

    private void startAuth() {
        if (AppLock.authenticating) return;
        BiometricManager bm = getSystemService(BiometricManager.class);
        int can = bm == null ? BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE : bm.canAuthenticate(AUTH);
        if (can == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            needsSetup = true;
            lockMsg.setText("برای استفاده از برنامه باید قفل صفحه (الگو، پین یا اثر انگشت) در تنظیمات گوشی فعال باشد.");
            lockBtn.setText("رفتن به تنظیمات");
            return;
        }
        needsSetup = false;
        lockBtn.setText("باز کردن قفل");
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            lockMsg.setText("در حال حاضر امکان تأیید هویت وجود ندارد. دوباره تلاش کن.");
            return;
        }
        BiometricPrompt prompt = new BiometricPrompt.Builder(this)
                .setTitle("ورود به دستیار اقساط")
                .setSubtitle("اثر انگشت یا الگو/پین گوشی را وارد کن")
                .setAllowedAuthenticators(AUTH)
                .build();
        cancel = new CancellationSignal();
        AppLock.authenticating = true;
        prompt.authenticate(cancel, getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) {
                AppLock.authenticating = false; AppLock.userDismissed = false; AppLock.locked = false;
                cancel = null; hideLock(); onUnlocked();
            }
            @Override public void onAuthenticationError(int code, CharSequence msg) {
                AppLock.authenticating = false; cancel = null;
                // ERROR_CANCELED (5) = system cancelled it (e.g. app went to background): prompt again next time
                AppLock.userDismissed = code != 5;
                if (lockMsg != null && code != 5) lockMsg.setText("برای ورود، هویتت رو تأیید کن");
            }
        });
    }
}
