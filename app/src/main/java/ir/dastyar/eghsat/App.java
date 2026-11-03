package ir.dastyar.eghsat;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

/** Re-arms the app lock whenever no activity of the app is visible any more. */
public class App extends Application {
    private int started = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable arm = () -> {
        if (started == 0) { AppLock.locked = true; AppLock.userDismissed = false; }
    };

    @Override public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) { started++; handler.removeCallbacks(arm); }
            @Override public void onActivityResumed(Activity a) {}
            @Override public void onActivityPaused(Activity a) {}
            @Override public void onActivityStopped(Activity a) {
                started = Math.max(0, started - 1);
                // small delay so rotation / recreate() (theme change) doesn't trigger a lock
                if (started == 0 && !a.isChangingConfigurations()) handler.postDelayed(arm, 700);
            }
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        });
    }
}
