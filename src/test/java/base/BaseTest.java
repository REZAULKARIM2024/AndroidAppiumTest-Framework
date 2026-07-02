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

public class BaseTest {

    protected AndroidDriver driver;
    protected String deviceName = "emulator-5554";
    protected String platformVersion = "";
    protected String runMode = "local";

    @BeforeClass
    @Parameters({"runMode", "saucelabsUser", "saucelabsKey", "platformVersion"})
    public void setUp(
            @Optional("local") String runMode,
            @Optional("") String saucelabsUser,
            @Optional("") String saucelabsKey,
            @Optional("12") String platformVersion
    ) throws Exception {
        this.runMode = runMode;
        this.platformVersion = platformVersion;
        if ("saucelabs".equalsIgnoreCase(runMode)) {
            // SauceLabs session slot free না হলে retry করি
            Exception lastEx = null;
            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    setupSauceLabs(saucelabsUser, saucelabsKey, platformVersion);
                    lastEx = null;
                    break;
                } catch (Exception e) {
                    lastEx = e;
                    System.out.println("[BaseTest] SauceLabs attempt " + attempt + "/3 failed — retrying in 15s...");
                    try { Thread.sleep(15000); } catch (InterruptedException ignored) {}
                }
            }
            if (lastEx != null) throw lastEx;
        } else {
            setupLocal();
        }
    }

    private void setupLocal() throws Exception {
        String apkPath = "C:\\Users\\rezau\\ApiDemos-debug.apk";
        java.io.File apkFile = new java.io.File(apkPath);

        UiAutomator2Options options = new UiAutomator2Options();
        options.setDeviceName(this.deviceName);
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setAppPackage("io.appium.android.apis");
        options.setAppActivity("io.appium.android.apis.ApiDemos");
        options.setNewCommandTimeout(Duration.ofSeconds(120));
        options.setUiautomator2ServerLaunchTimeout(Duration.ofSeconds(60));
        options.setUiautomator2ServerInstallTimeout(Duration.ofSeconds(60));

        if (apkFile.exists()) {
            System.out.println("[BaseTest] APK found — installing: " + apkPath);
            options.setApp(apkPath);
            options.setNoReset(false);
        } else {
            System.out.println("[BaseTest] APK not found — launching installed app");
            options.setNoReset(true);
        }

        System.out.println("[BaseTest] Connecting to Appium — device: " + this.deviceName);
        driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
        System.out.println("[BaseTest] Driver created successfully");
        Thread.sleep(2000);
    }

    private void setupSauceLabs(String username, String accessKey, String platformVersion) throws Exception {
        if (username == null || username.isEmpty()) username = System.getenv("SAUCE_USERNAME");
        if (accessKey == null || accessKey.isEmpty()) accessKey = System.getenv("SAUCE_ACCESS_KEY");
        if (username == null || username.isEmpty() || accessKey == null || accessKey.isEmpty()) {
            throw new IllegalStateException(
                "[BaseTest] SauceLabs credentials missing. " +
                "Set saucelabsUser/saucelabsKey in testng-saucelabs.xml " +
                "or export SAUCE_USERNAME / SAUCE_ACCESS_KEY.");
        }

        Map<String, Object> sauceOpts = new HashMap<>();
        sauceOpts.put("username", username);
        sauceOpts.put("accessKey", accessKey);
        sauceOpts.put("deviceOrientation", "PORTRAIT");

        sauceOpts.put("name", "ApiDemos Regression Suite");
        sauceOpts.put("build", "build-" + System.currentTimeMillis());

        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setDeviceName("Android GoogleAPI Emulator");
        options.setPlatformVersion(platformVersion);
        options.setApp("storage:22cf786b-cedf-4fac-82bd-aa3361162dcf");
        options.setAppPackage("io.appium.android.apis");
        options.setAppActivity("io.appium.android.apis.ApiDemos");
        options.setNewCommandTimeout(Duration.ofSeconds(120));
        options.setCapability("sauce:options", sauceOpts);

        String sauceUrl = "https://ondemand.us-west-1.saucelabs.com:443/wd/hub";
        System.out.println("[BaseTest] Connecting to SauceLabs — user: " + username);
        try {
            driver = new AndroidDriver(new URL(sauceUrl), options);
            System.out.println("[BaseTest] SauceLabs session: " + driver.getSessionId());
        } catch (Exception e) {
            System.out.println("[BaseTest] SauceLabs connection FAILED: " + e.getMessage());
            throw e;
        }
        Thread.sleep(3000);
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception e) {
                System.out.println("[BaseTest] tearDown quit failed (session may already be dead): " + e.getMessage());
            }
        }
    }

    @AfterSuite
    public void flushReport() {
        ExtentReportManager.getReportInstance().flush();
    }
}
