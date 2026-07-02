package base;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import utils.ExtentReportManager;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for SauceLabs My Demo App tests.
 * Separate from BaseTest (ApiDemos) to keep configurations independent.
 *
 * App:     SauceLabs My Demo App (React Native)
 * Package: com.saucelabs.mydemoapp.rn
 */
public class MyDemoBaseTest {

    protected AndroidDriver driver;
    protected String runMode = "local";

    // ── Test credentials ──────────────────────────────────────────────────────
    public static final String VALID_USERNAME   = "bob@example.com";
    public static final String VALID_PASSWORD   = "10203040";
    public static final String INVALID_USERNAME = "invalid@example.com";
    public static final String INVALID_PASSWORD = "wrongpassword";

    // ── App identifiers ───────────────────────────────────────────────────────
    private static final String APP_PACKAGE  = "com.saucelabs.mydemoapp.android";
    private static final String APP_ACTIVITY = "com.saucelabs.mydemoapp.android.view.activities.SplashActivity";

    // ── SauceLabs App storage file ID ─────────────────────────────────────────
    private static final String SAUCELABS_APP_ID = "storage:e0e1a0ab-d0aa-462c-97ed-888868820a30";

    @BeforeClass
    @Parameters({"runMode", "saucelabsUser", "saucelabsKey", "platformVersion"})
    public void setUp(
            @Optional("local") String runMode,
            @Optional("") String saucelabsUser,
            @Optional("") String saucelabsKey,
            @Optional("12") String platformVersion
    ) throws Exception {
        this.runMode = runMode;
        if ("saucelabs".equalsIgnoreCase(runMode)) {
            Exception lastEx = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    setupSauceLabs(saucelabsUser, saucelabsKey, platformVersion);
                    lastEx = null;
                    break;
                } catch (Exception e) {
                    lastEx = e;
                    System.out.println("[MyDemoBaseTest] Attempt " + attempt + "/3 failed: " + e.getMessage());
                    try { Thread.sleep(15000); } catch (InterruptedException ignored) {}
                }
            }
            if (lastEx != null) throw lastEx;
        } else {
            setupLocal();
        }
    }

    private void setupLocal() throws Exception {
        String apkPath = "C:\\Users\\rezau\\MyDemoApp.apk";

        UiAutomator2Options options = new UiAutomator2Options();
        options.setDeviceName("emulator-5554");
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setAppPackage(APP_PACKAGE);
        // noReset: true → skip reinstall (app already installed) — much faster session start
        // App data is cleared via ADB 'pm clear' below, which is faster than fullReset reinstall
        options.setNoReset(true);
        options.setCapability("fullReset", false);
        // Wait for ANY activity — SplashActivity immediately redirects to MainActivity
        options.setCapability("appWaitActivity", "*");
        options.setCapability("appWaitDuration", 30000);
        options.setNewCommandTimeout(Duration.ofSeconds(120));
        options.setUiautomator2ServerLaunchTimeout(Duration.ofSeconds(60));
        options.setUiautomator2ServerInstallTimeout(Duration.ofSeconds(60));

        java.io.File apk = new java.io.File(apkPath);
        if (apk.exists()) {
            options.setApp(apkPath);
        }

        System.out.println("[MyDemoBaseTest] APK exists: " + apk.exists() + " — path: " + apkPath);
        System.out.println("[MyDemoBaseTest] Connecting locally — device: emulator-5554, port: 4723");

        // Clear app data via ADB before session (faster than fullReset reinstall, same effect)
        try {
            System.out.println("[MyDemoBaseTest] Clearing app data via ADB pm clear...");
            Process p = Runtime.getRuntime().exec(
                new String[]{"adb", "-s", "emulator-5554", "shell", "pm", "clear", APP_PACKAGE});
            p.waitFor();
            System.out.println("[MyDemoBaseTest] App data cleared");
        } catch (Exception e) {
            System.out.println("[MyDemoBaseTest] ADB pm clear failed (non-fatal): " + e.getMessage());
        }

        try {
            driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
            System.out.println("[MyDemoBaseTest] Local session started: " + driver.getSessionId());
        } catch (Exception e) {
            System.out.println("[MyDemoBaseTest] Local FAILED: " + e.getMessage());
            throw e;
        }
        Thread.sleep(3000);
    }

    private void setupSauceLabs(String username, String accessKey, String platformVersion) throws Exception {
        if (username == null || username.isEmpty()) username = System.getenv("SAUCE_USERNAME");
        if (accessKey == null || accessKey.isEmpty()) accessKey = System.getenv("SAUCE_ACCESS_KEY");

        Map<String, Object> sauceOpts = new HashMap<>();
        sauceOpts.put("username", username);
        sauceOpts.put("accessKey", accessKey);
        sauceOpts.put("deviceOrientation", "PORTRAIT");
        sauceOpts.put("name", "MyDemoApp E2E Suite");
        sauceOpts.put("build", "mydemo-build-" + System.currentTimeMillis());

        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setDeviceName("Android GoogleAPI Emulator");
        options.setPlatformVersion(platformVersion);
        options.setApp(SAUCELABS_APP_ID);
        options.setAppPackage(APP_PACKAGE);
        // NOTE: appActivity intentionally omitted — when 'app' is set, Appium/UiAutomator2
        // detects the launcher activity automatically from the APK manifest.
        // Setting a wrong activity causes "Configuration Failure: 1" / session never starts.
        options.setNewCommandTimeout(Duration.ofSeconds(120));
        options.setCapability("sauce:options", sauceOpts);

        if (username == null || username.isEmpty() || accessKey == null || accessKey.isEmpty()) {
            throw new IllegalStateException("[MyDemoBaseTest] SauceLabs credentials missing!");
        }

        String sauceUrl = "https://ondemand.us-west-1.saucelabs.com:443/wd/hub";
        System.out.println("[MyDemoBaseTest] Connecting to SauceLabs — user: " + username);
        System.out.println("[MyDemoBaseTest] App: " + SAUCELABS_APP_ID);
        System.out.println("[MyDemoBaseTest] Package: " + APP_PACKAGE);
        try {
            driver = new AndroidDriver(new URL(sauceUrl), options);
            System.out.println("[MyDemoBaseTest] Session started: " + driver.getSessionId());
        } catch (Exception e) {
            System.out.println("[MyDemoBaseTest] Session FAILED: " + e.getMessage());
            throw e;
        }
        Thread.sleep(3000);
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception e) {
                System.out.println("[MyDemoBaseTest] tearDown: " + e.getMessage());
            }
        }
    }

    @AfterSuite
    public void flushReport() {
        ExtentReportManager.getReportInstance().flush();
    }
}
