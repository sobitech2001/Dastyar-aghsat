package ir.dastyar.eghsat;

/** Process-wide lock state. The app starts locked and is re-locked every time it leaves the foreground. */
public class AppLock {
    public static volatile boolean locked = true;
    /** true while the system biometric / screen-lock prompt is showing */
    public static volatile boolean authenticating = false;
    /** true when the user dismissed the prompt; we then wait for the unlock button instead of re-prompting */
    public static volatile boolean userDismissed = false;
}
