package tests;

import base.BaseTest;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import io.appium.java_client.android.connection.ConnectionState;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.MainScreenPage;
import utils.ExtentReportManager;

public class InterruptSimulationTests extends BaseTest {

    ExtentTest test;
    MainScreenPage mainPage;

    @BeforeMethod
    public void setupReport(java.lang.reflect.Method method) {
        if (driver == null) throw new SkipException("Driver not initialized — skipping test");
        test = ExtentReportManager.createTest(method.getName());
        mainPage = new MainScreenPage(driver);
        mainPage.ensureOnMainScreen(driver);
    }

    // 1. Incoming Call সিমুলেট করে app এর প্রতিক্রিয়া যাচাই (emulator console কমান্ড দিয়ে)
    @Test(priority = 1)
    public void interrupt_IncomingCallDoesNotCrashApp() {
        try {
            Runtime.getRuntime().exec("adb -s emulator-5554 emu gsm call 5551234567");
            Thread.sleep(2000);

            // Call শেষ করি (cancel) যাতে test আটকে না থাকে
            Runtime.getRuntime().exec("adb -s emulator-5554 emu gsm cancel 5551234567");
            Thread.sleep(1000);
        } catch (Exception e) {
            test.log(Status.WARNING, "Could not simulate call (emulator console may be unavailable): " + e.getMessage());
        }

        int count = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Menu items visible after call interrupt: " + count);
        Assert.assertTrue(count > 0, "App became unresponsive after simulated incoming call");
    }

    // 2. Incoming SMS সিমুলেট করে app এর প্রতিক্রিয়া যাচাই
    @Test(priority = 2)
    public void interrupt_IncomingSmsDoesNotCrashApp() {
        try {
            Runtime.getRuntime().exec("adb -s emulator-5554 emu sms send 5551234567 \"Test SMS interrupt\"");
            Thread.sleep(2000);
        } catch (Exception e) {
            test.log(Status.WARNING, "Could not simulate SMS: " + e.getMessage());
        }

        int count = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Menu items visible after SMS interrupt: " + count);
        Assert.assertTrue(count > 0, "App became unresponsive after simulated SMS");
    }

    // 3. Network Loss (Airplane mode সিমুলেশন) এর পরেও app crash করে না
    @Test(priority = 3)
    public void interrupt_NetworkLossDoesNotCrashApp() {
        io.appium.java_client.android.connection.ConnectionStateBuilder builder =
                new io.appium.java_client.android.connection.ConnectionStateBuilder();

        // সব connection বন্ধ করি (airplane mode এর কাছাকাছি)
        driver.setConnection(builder.withWiFiDisabled().withDataDisabled().withAirplaneModeDisabled().build());
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        int countOffline = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Menu items visible while offline: " + countOffline);

        // আগের মতো connection ফিরিয়ে আনি (WiFi + Data চালু)
        driver.setConnection(builder.withWiFiEnabled().withDataEnabled().build());
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        Assert.assertTrue(countOffline > 0, "App crashed or became unresponsive during network loss");
    }

    // 4. App Background এ থাকার সময় Low Battery সিমুলেশনের পরেও resume ঠিক থাকে কিনা
    @Test(priority = 4)
    public void interrupt_LowBatterySimulationDoesNotAffectApp() {
        try {
            Runtime.getRuntime().exec("adb -s emulator-5554 emu power capacity 5");
            Thread.sleep(1500);
        } catch (Exception e) {
            test.log(Status.WARNING, "Could not simulate low battery: " + e.getMessage());
        }

        int count = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Menu items visible under low battery simulation: " + count);
        Assert.assertTrue(count > 0, "App became unresponsive under low battery condition");

        // battery normal এ ফিরিয়ে আনা (cleanup)
        try {
            Runtime.getRuntime().exec("adb -s emulator-5554 emu power capacity 100");
        } catch (Exception ignored) {}
    }
}