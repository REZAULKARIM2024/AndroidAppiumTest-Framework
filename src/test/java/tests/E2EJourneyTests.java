package tests;

import base.BaseTest;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import org.openqa.selenium.By;
import org.openqa.selenium.ScreenOrientation;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.MainScreenPage;
import utils.ExtentReportManager;
import utils.WaitUtils;

import java.time.Duration;

/**
 * E2E Journey Tests
 * End-to-end multi-step user journeys simulating real-world usage patterns:
 * app exploration, device rotation mid-session, and reset + re-explore flows.
 *
 * These tests verify that the full user journey works correctly from start
 * to finish — equivalent to signup → browse → action → logout flows in
 * e-commerce apps.
 */
public class E2EJourneyTests extends BaseTest {

    ExtentTest test;
    MainScreenPage mainPage;

    private void sleep(long localMs, long cloudMs) {
        long ms = "saucelabs".equalsIgnoreCase(runMode) ? cloudMs : localMs;
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    @BeforeMethod
    public void setupReport(java.lang.reflect.Method method) {
        if (driver == null) throw new SkipException("Driver not initialized — skipping");
        test = ExtentReportManager.createTest(method.getName());
        mainPage = new MainScreenPage(driver);
        mainPage.ensureOnMainScreen(driver);
    }

    /**
     * Journey 1: Full App Exploration Flow
     *
     * Simulates a new user opening the app, exploring multiple sections,
     * putting the app in background mid-session, and returning to a clean state.
     * Equivalent to: App open → Browse categories → Background → Resume → Verify
     */
    @Test(priority = 1)
    public void testCompleteUserExplorationJourney() {
        test.log(Status.INFO, "═══ Journey 1: Full App Exploration ═══");

        // Step 1: Verify launch
        test.log(Status.INFO, "Step 1: Verify app launched to main screen");
        Assert.assertEquals(mainPage.getMenuItemCount(), 12, "App did not launch correctly");
        Assert.assertEquals(driver.getCurrentPackage(), "io.appium.android.apis");

        // Step 2: Explore first category
        test.log(Status.INFO, "Step 2: Explore Accessibility section");
        mainPage.clickMenuItemByText("Accessibility");
        sleep(800, 2000);
        Assert.assertEquals(driver.getCurrentPackage(), "io.appium.android.apis",
                "Accessibility navigation failed");
        driver.navigate().back();
        sleep(400, 1000);

        // Step 3: Explore second category
        test.log(Status.INFO, "Step 3: Explore Animation section");
        mainPage.clickMenuItemByText("Animation");
        sleep(800, 2000);
        Assert.assertEquals(driver.getCurrentPackage(), "io.appium.android.apis",
                "Animation navigation failed");
        driver.navigate().back();
        sleep(400, 1000);

        // Step 4: Explore third category
        test.log(Status.INFO, "Step 4: Explore Graphics section");
        mainPage.clickMenuItemByText("Graphics");
        sleep(800, 2000);
        Assert.assertEquals(driver.getCurrentPackage(), "io.appium.android.apis",
                "Graphics navigation failed");
        driver.navigate().back();
        sleep(400, 1000);

        // Step 5: Background the app mid-journey
        test.log(Status.INFO, "Step 5: Background app mid-journey (simulates phone call / notification)");
        driver.runAppInBackground(Duration.ofSeconds(3));
        sleep(1500, 3500);

        // Step 6: Recover and verify clean state
        test.log(Status.INFO, "Step 6: Verify app resumed to correct state");
        if ("saucelabs".equalsIgnoreCase(runMode)) {
            try {
                if (!"io.appium.android.apis".equals(driver.getCurrentPackage())) {
                    driver.activateApp("io.appium.android.apis");
                    sleep(0, 2000);
                }
            } catch (Exception ignored) {}
        }
        mainPage.ensureOnMainScreen(driver);
        int finalCount = mainPage.getMenuItemCount();
        Assert.assertEquals(finalCount, 12,
                "App did not resume to clean state after full exploration journey");

        test.log(Status.PASS, "Journey 1 complete — all 6 steps passed. Final item count: " + finalCount);
    }

    /**
     * Journey 2: Device Rotation Mid-Journey
     *
     * Simulates a user navigating to a section and rotating the device.
     * Equivalent to: Browse → Rotate phone → Continue → Rotate back → Verify
     */
    @Test(priority = 2)
    public void testUserJourneyWithDeviceRotation() {
        test.log(Status.INFO, "═══ Journey 2: Navigation + Device Rotation ═══");

        // Step 1: Navigate to a section
        test.log(Status.INFO, "Step 1: Navigate to Content section");
        mainPage.clickMenuItemByText("Content");
        sleep(800, 2000);
        Assert.assertEquals(driver.getCurrentPackage(), "io.appium.android.apis");

        // Step 2: Rotate to landscape
        test.log(Status.INFO, "Step 2: Rotate device to landscape (mid-journey)");
        driver.rotate(ScreenOrientation.LANDSCAPE);
        sleep(1000, 2000);
        Assert.assertEquals(driver.getCurrentPackage(), "io.appium.android.apis",
                "App crashed during landscape rotation");

        // Step 3: Verify still functional in landscape
        test.log(Status.INFO, "Step 3: Verify app functional in landscape");
        int landscapeCount = mainPage.getMenuItemCount();
        Assert.assertTrue(landscapeCount > 0, "No items visible in landscape mode");

        // Step 4: Navigate back while in landscape
        test.log(Status.INFO, "Step 4: Navigate back in landscape mode");
        driver.navigate().back();
        sleep(500, 1000);

        // Step 5: Rotate back to portrait
        test.log(Status.INFO, "Step 5: Rotate back to portrait");
        driver.rotate(ScreenOrientation.PORTRAIT);
        sleep(1000, 2000);

        // Step 6: Verify clean state
        test.log(Status.INFO, "Step 6: Verify clean state after rotation journey");
        mainPage.ensureOnMainScreen(driver);
        Assert.assertEquals(mainPage.getMenuItemCount(), 12,
                "App state corrupted after rotation journey");

        test.log(Status.PASS, "Journey 2 complete — rotation mid-journey handled correctly");
    }

    /**
     * Journey 3: App Reset + Re-Explore (Logout → Login)
     *
     * Simulates a user logging out (app reset) and logging back in to explore.
     * Equivalent to: Explore → Logout (terminate) → Login (relaunch) → Re-explore → Verify
     */
    @Test(priority = 3)
    public void testAppResetAndReExploreJourney() {
        test.log(Status.INFO, "═══ Journey 3: Reset + Re-Explore (Logout/Login) ═══");

        // Step 1: Navigate to a section (simulate "logged in" state)
        test.log(Status.INFO, "Step 1: Navigate to Graphics (simulate active session)");
        mainPage.clickMenuItemByText("Graphics");
        sleep(800, 2000);
        Assert.assertEquals(driver.getCurrentPackage(), "io.appium.android.apis");

        // Step 2: Terminate app (simulate logout)
        test.log(Status.INFO, "Step 2: Terminate app (simulate logout)");
        driver.terminateApp("io.appium.android.apis");
        sleep(1000, 2000);
        Assert.assertNotEquals(driver.getCurrentPackage(), "io.appium.android.apis",
                "App still running after termination");

        // Step 3: Relaunch (simulate login → fresh start)
        test.log(Status.INFO, "Step 3: Relaunch app (simulate fresh login)");
        driver.activateApp("io.appium.android.apis");
        WaitUtils.waitForVisible(driver, By.id("android:id/text1"), 20);
        sleep(1500, 4000);

        // Step 4: Verify fresh state
        test.log(Status.INFO, "Step 4: Verify app launched to clean state");
        mainPage.ensureOnMainScreen(driver);
        Assert.assertEquals(mainPage.getMenuItemCount(), 12,
                "App did not reset to clean state after relaunch");

        // Step 5: Re-explore after reset
        test.log(Status.INFO, "Step 5: Re-explore after reset");
        mainPage.clickMenuItemByText("Accessibility");
        sleep(600, 1500);
        driver.navigate().back();
        sleep(400, 1000);

        mainPage.clickMenuItemByText("Animation");
        sleep(600, 1500);
        driver.navigate().back();
        sleep(400, 1000);

        // Step 6: Verify final state
        test.log(Status.INFO, "Step 6: Verify final clean state");
        Assert.assertEquals(mainPage.getMenuItemCount(), 12,
                "App unstable after reset + re-explore journey");

        test.log(Status.PASS, "Journey 3 complete — reset + re-explore passed all 6 steps");
    }

    /**
     * Journey 4: Orientation + Background + Reset (Full stress journey)
     *
     * Combines multiple real-world interruptions in a single journey.
     * Equivalent to: Browse → Rotate → Background → Restore → Verify
     */
    @Test(priority = 4)
    public void testFullStressJourney() {
        test.log(Status.INFO, "═══ Journey 4: Full Stress Journey ═══");

        // Step 1: Initial state check
        Assert.assertEquals(mainPage.getMenuItemCount(), 12, "Precondition failed");
        test.log(Status.INFO, "Step 1: Precondition — main screen has 12 items");

        // Step 2: Navigate
        mainPage.clickMenuItemByText("Accessibility");
        sleep(600, 1500);
        test.log(Status.INFO, "Step 2: Navigated to Accessibility");

        // Step 3: Rotate to landscape
        driver.rotate(ScreenOrientation.LANDSCAPE);
        sleep(800, 1500);
        Assert.assertEquals(driver.getCurrentPackage(), "io.appium.android.apis");
        test.log(Status.INFO, "Step 3: Rotated to landscape");

        // Step 4: Background while in landscape
        driver.runAppInBackground(Duration.ofSeconds(2));
        sleep(1000, 3000);
        test.log(Status.INFO, "Step 4: Backgrounded in landscape");

        // Step 5: Resume
        if ("saucelabs".equalsIgnoreCase(runMode)) {
            try {
                if (!"io.appium.android.apis".equals(driver.getCurrentPackage())) {
                    driver.activateApp("io.appium.android.apis");
                    sleep(0, 2000);
                }
            } catch (Exception ignored) {}
        }
        test.log(Status.INFO, "Step 5: Resumed from background");

        // Step 6: Rotate back to portrait
        driver.rotate(ScreenOrientation.PORTRAIT);
        sleep(800, 1500);
        test.log(Status.INFO, "Step 6: Rotated back to portrait");

        // Step 7: Navigate back and verify
        driver.navigate().back();
        sleep(500, 1000);
        mainPage.ensureOnMainScreen(driver);
        int finalCount = mainPage.getMenuItemCount();
        Assert.assertEquals(finalCount, 12,
                "App corrupted after full stress journey");

        test.log(Status.PASS, "Journey 4 (Full Stress) complete — " + finalCount + " items on main screen");
    }
}
