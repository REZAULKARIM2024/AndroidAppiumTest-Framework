package tests;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import utils.ExtentReportManager;

import java.net.URL;
import java.time.Duration;

/**
 * এই Test Class গুলো App এর Install/Uninstall/Reinstall lifecycle যাচাই করে।
 * এখানে driver session সরাসরি app launch ছাড়াই তৈরি করা হয় (noReset/app না দিয়ে),
 * কারণ আমরা নিজেরাই install/uninstall নিয়ন্ত্রণ করব।
 */
public class InstallLifecycleTests {

    AndroidDriver driver;
    ExtentTest test;
    private static final String APP_PACKAGE = "io.appium.android.apis";
    private static final String APP_PATH = "C:\\Users\\rezau\\ApiDemos-debug.apk";

    @BeforeClass
    public void setUp() throws Exception {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setDeviceName("emulator-5554");
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setNewCommandTimeout(Duration.ofSeconds(120));
        // লক্ষ্য করুন: এখানে setApp() ব্যবহার করা হয়নি — আমরা নিজেরা install নিয়ন্ত্রণ করব

        driver = new AndroidDriver(new URL("http://127.0.0.1:4723"), options);
    }

    @BeforeMethod
    public void setupReport(java.lang.reflect.Method method) {
        test = ExtentReportManager.createTest(method.getName());
    }

    // 1. Fresh Install: App ইনস্টল করা এবং সফলভাবে ইনস্টল হয়েছে কিনা যাচাই
    @Test(priority = 1)
    public void install_FreshInstallSucceeds() {
        // নিশ্চিত করি আগে কোনো পুরোনো ভার্সন নেই
        if (driver.isAppInstalled(APP_PACKAGE)) {
            driver.removeApp(APP_PACKAGE);
        }

        driver.installApp(APP_PATH);
        boolean installed = driver.isAppInstalled(APP_PACKAGE);

        test.log(Status.INFO, "App installed after fresh install: " + installed);
        Assert.assertTrue(installed, "Fresh install failed — app not found after installApp()");
    }

    // 2. App চালু করে দেখা ইনস্টল হওয়া App actually কাজ করে কিনা
    @Test(priority = 2, dependsOnMethods = "install_FreshInstallSucceeds")
    public void install_FreshlyInstalledAppLaunchesSuccessfully() {
        driver.activateApp(APP_PACKAGE);
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        String currentPackage = driver.getCurrentPackage();
        test.log(Status.INFO, "Current package after launch: " + currentPackage);
        Assert.assertEquals(currentPackage, APP_PACKAGE, "App did not launch after fresh install");
    }

    // 3. Re-install (Upgrade simulation): একই APK পুনরায় ইনস্টল করলে app data থাকে কিনা
    @Test(priority = 3, dependsOnMethods = "install_FreshlyInstalledAppLaunchesSuccessfully")
    public void install_ReinstallOverExistingAppSucceeds() {
        driver.installApp(APP_PATH); // পুনরায় install — উপস্থিত app এর উপর "upgrade" সিমুলেশন
        boolean stillInstalled = driver.isAppInstalled(APP_PACKAGE);

        test.log(Status.INFO, "App still installed after reinstall: " + stillInstalled);
        Assert.assertTrue(stillInstalled, "App missing after reinstall/upgrade simulation");
    }

    // 4. Uninstall: App সফলভাবে আনইনস্টল হচ্ছে কিনা
    @Test(priority = 4, dependsOnMethods = "install_ReinstallOverExistingAppSucceeds")
    public void uninstall_AppRemovesSuccessfully() {
        driver.removeApp(APP_PACKAGE);
        boolean stillInstalled = driver.isAppInstalled(APP_PACKAGE);

        test.log(Status.INFO, "App still installed after uninstall: " + stillInstalled);
        Assert.assertFalse(stillInstalled, "App was not properly uninstalled");
    }

    // 5. পুনরায় Install (uninstall এর পর) — clean reinstall flow সম্পূর্ণ চক্র যাচাই
    @Test(priority = 5, dependsOnMethods = "uninstall_AppRemovesSuccessfully")
    public void install_ReinstallAfterUninstallSucceeds() {
        driver.installApp(APP_PATH);
        boolean installed = driver.isAppInstalled(APP_PACKAGE);

        test.log(Status.INFO, "App installed after post-uninstall reinstall: " + installed);
        Assert.assertTrue(installed, "Could not reinstall app after uninstall");

        driver.activateApp(APP_PACKAGE);
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}