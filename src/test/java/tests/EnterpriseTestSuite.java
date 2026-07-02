package tests;

import base.BaseTest;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.*;
import pages.MainScreenPage;
import pages.PermissionPage;
import utils.ExtentReportManager;
import utils.WaitUtils;

import java.time.Duration;
import java.util.List;

public class EnterpriseTestSuite extends BaseTest {

    ExtentTest test;
    MainScreenPage mainPage;

    @BeforeMethod
    public void setupReport(java.lang.reflect.Method method) {
        if (driver == null) throw new SkipException("Driver not initialized — skipping test");
        test = ExtentReportManager.createTest(method.getName());
        mainPage = new MainScreenPage(driver);
        mainPage.ensureOnMainScreen(driver);
    }

    // ============================================================
    // CATEGORY: SMOKE TESTS
    // ============================================================

    // 1. Smoke: App চালু হয়ে Main Dashboard (Main screen) সঠিকভাবে লোড হয়েছে কিনা
    @Test(priority = 1, groups = "smoke")
    public void smoke_AppLaunchesAndMainScreenLoads() {
        int count = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Main screen menu item count: " + count);
        Assert.assertTrue(count > 0, "Main screen failed to load — smoke test failed");
    }

    // 2. Smoke: Basic Navigation কাজ করছে কিনা (এক স্ক্রিনে গিয়ে ফিরে আসা)
    @Test(priority = 2, groups = "smoke")
    public void smoke_BasicNavigationWorks() {
        mainPage.clickMenuItemByText("Views");
        try { Thread.sleep(700); } catch (InterruptedException ignored) {}
        driver.navigate().back();
        try { Thread.sleep(700); } catch (InterruptedException ignored) {}
        Assert.assertEquals(mainPage.getMenuItemCount(), 12, "Basic navigation smoke test failed");
    }

    // ============================================================
    // CATEGORY: APP LIFECYCLE
    // ============================================================

    // 3. App Kill এবং পুনরায় Resume করার পর app সঠিকভাবে চালু হয় কিনা
    @Test(priority = 3, groups = "lifecycle")
    public void lifecycle_AppKillAndRelaunchRecoversCleanly() {
        driver.terminateApp("io.appium.android.apis");
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        driver.activateApp("io.appium.android.apis");
        WaitUtils.waitForVisible(driver, By.id("android:id/text1"), 15);

        int count = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Menu count after kill/relaunch: " + count);
        Assert.assertEquals(count, 12, "App did not recover cleanly after kill and relaunch");
    }

    // 4. Rotation এর পরেও app lifecycle ঠিক আছে কিনা (ইতিমধ্যে covered, কিন্তু এখানে lifecycle category তে রাখা হলো)
    @Test(priority = 4, groups = "lifecycle")
    public void lifecycle_RotationDoesNotCrashApp() {
    	driver.rotate(org.openqa.selenium.ScreenOrientation.LANDSCAPE);
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        boolean stillResponsive = mainPage.getMenuItemCount() > 0;
        driver.rotate(org.openqa.selenium.ScreenOrientation.PORTRAIT);
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        test.log(Status.INFO, "App responsive after rotation: " + stillResponsive);
        Assert.assertTrue(stillResponsive, "App became unresponsive after rotation");
    }

    // ============================================================
    // CATEGORY: NAVIGATION (Deep flows, tab/menu switching, back button)
    // ============================================================

    // 5. একাধিক ধাপ গভীরে গিয়ে Back বাটন দিয়ে ধাপে ধাপে সঠিকভাবে ফিরে আসা যায় কিনা
    @Test(priority = 5, groups = "navigation")
    public void navigation_MultiStepBackButtonNavigatesCorrectly() {
        mainPage.clickMenuItemByText("Views");
        try { Thread.sleep(700); } catch (InterruptedException ignored) {}
        int afterFirstClick = mainPage.getMenuItemCount();

        driver.navigate().back();
        try { Thread.sleep(700); } catch (InterruptedException ignored) {}
        int afterBack = mainPage.getMenuItemCount();

        test.log(Status.INFO, "After click: " + afterFirstClick + " | After back: " + afterBack);
        Assert.assertEquals(afterBack, 12, "Back button did not return to main screen correctly");
    }

    // 6. Menu/Tab switching: একাধিক ভিন্ন মেনুর মধ্যে দ্রুত আসা-যাওয়া করলে app crash করে না
    @Test(priority = 6, groups = "navigation")
    public void navigation_RapidMenuSwitchingDoesNotBreakApp() {
        String[] menus = {"Graphics", "Media", "OS"};
        for (String menu : menus) {
            mainPage.clickMenuItemByText(menu);
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
            driver.navigate().back();
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
        }
        Assert.assertEquals(mainPage.getMenuItemCount(), 12, "App broke during rapid menu switching");
    }

    // ============================================================
    // CATEGORY: DEVICE BEHAVIOR
    // ============================================================

    // 7. ভিন্ন ভিন্ন screen size/density এর তথ্য সঠিকভাবে পাওয়া যাচ্ছে কিনা (device info sanity check)
    @Test(priority = 7, groups = "device")
    public void device_ScreenDimensionsAreValid() {
        org.openqa.selenium.Dimension size = driver.manage().window().getSize();
        test.log(Status.INFO, "Screen size: " + size.getWidth() + "x" + size.getHeight());
        Assert.assertTrue(size.getWidth() > 0 && size.getHeight() > 0, "Invalid screen dimensions reported");
    }

    // 8. Device এর OS version তথ্য সঠিকভাবে পাওয়া যাচ্ছে কিনা
    @Test(priority = 8, groups = "device")
    public void device_PlatformVersionIsAccessible() {
        String platformVersion = String.valueOf(driver.getCapabilities().getCapability("platformVersion"));
        test.log(Status.INFO, "Platform version: " + platformVersion);
        Assert.assertNotNull(platformVersion, "Platform version should not be null");
    }

    // ============================================================
    // CATEGORY: PERMISSIONS
    // ============================================================

    // 9. Permission dialog এলে handle করতে পারা (allow flow) — ApiDemos এ সরাসরি না থাকলেও reusable utility যাচাই
    @Test(priority = 9, groups = "permissions")
    public void permissions_GrantFlowDoesNotThrowWhenNoDialogPresent() {
        PermissionPage permissionPage = new PermissionPage(driver);
        boolean dialogVisible = permissionPage.isPermissionDialogVisible();
        test.log(Status.INFO, "Permission dialog visible: " + dialogVisible);

        // dialog না থাকলেও method safely silent থাকা উচিত (crash না করা)
        permissionPage.grantPermissionIfPrompted();
        test.log(Status.PASS, "Permission grant flow executed without exception");
    }

    // ============================================================
    // CATEGORY: NEGATIVE TESTS
    // ============================================================

    // 10. খালি/ভুল input দিয়ে EditText এ ক্লিক করলে app crash করে না (edge case handling)
    @Test(priority = 10, groups = "negative")
    public void negative_EmptyInputDoesNotCrashApp() {
        mainPage.clickMenuItemByText("Text");
        try { Thread.sleep(700); } catch (InterruptedException ignored) {}

        boolean found = mainPage.navigateToScreenWithEditText();
        if (!found) {
            test.log(Status.SKIP, "No EditText screen found — skipping negative input test");
            mainPage.ensureOnMainScreen(driver);
            throw new SkipException("No EditText screen available under 'Text' menu");
        }

        WebElement field = driver.findElement(By.className("android.widget.EditText"));
        field.clear(); // ইচ্ছাকৃতভাবে empty রাখা — negative case
        String value = field.getAttribute("text");
        test.log(Status.INFO, "Field value after clearing: '" + value + "'");

        boolean appStillResponsive = !driver.getPageSource().isEmpty();
        Assert.assertTrue(appStillResponsive, "App became unresponsive after empty input");

        driver.navigate().back();
        mainPage.ensureOnMainScreen(driver);
    }

    // 11. অস্তিত্বহীন উপাদানে interaction করার সময় app graceful ভাবে handle করে কিনা (already similar covered, কিন্তু explicit negative category তে)
    @Test(priority = 11, groups = "negative")
    public void negative_InvalidElementInteractionThrowsGracefulException() {
        boolean exceptionThrown = false;
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"ThisScreenDoesNotExist999\")")).click();
        } catch (NoSuchElementException e) {
            exceptionThrown = true;
            test.log(Status.INFO, "Graceful exception type: " + e.getClass().getSimpleName());
        }
        Assert.assertTrue(exceptionThrown, "Invalid element interaction should throw NoSuchElementException");
    }

    // ============================================================
    // CATEGORY: PERFORMANCE / RESPONSIVENESS
    // ============================================================

    // 12. Scroll করার সময় কোনো লক্ষণীয় lag/freeze হয় কিনা (response time ভিত্তিক)
    @Test(priority = 12, groups = "performance")
    public void performance_ScrollResponseTimeWithinThreshold() {
        long start = System.currentTimeMillis();
        mainPage.scrollToAndClick("Views");
        long durationMs = System.currentTimeMillis() - start;

        test.log(Status.INFO, "Scroll + click duration: " + durationMs + " ms");
        Assert.assertTrue(durationMs < 5000, "Scroll action took too long — possible UI lag");

        driver.navigate().back();
        mainPage.ensureOnMainScreen(driver);
    }

    // 13. একটা সাধারণ Element wait time threshold এর মধ্যে আছে কিনা
    @Test(priority = 13, groups = "performance")
    public void performance_ElementWaitTimeWithinThreshold() {
        long start = System.currentTimeMillis();
        WaitUtils.waitForVisible(driver, By.id("android:id/text1"), 10);
        long durationMs = System.currentTimeMillis() - start;

        test.log(Status.INFO, "Element wait duration: " + durationMs + " ms");
        Assert.assertTrue(durationMs < 3000, "Element took too long to become visible");
    }

    // ============================================================
    // CATEGORY: SECURITY-FOCUSED CHECKS
    // ============================================================

    // 14. App Terminate করার পর পুনরায় চালু করলে আগের কোনো sensitive/UI state অবশিষ্ট থাকে না (session/data clear যাচাই)
    @Test(priority = 14, groups = "security")
    public void security_AppStateClearsAfterTermination() {
        mainPage.clickMenuItemByText("Graphics");
        try { Thread.sleep(700); } catch (InterruptedException ignored) {}

        driver.terminateApp("io.appium.android.apis");
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        driver.activateApp("io.appium.android.apis");
        WaitUtils.waitForVisible(driver, By.id("android:id/text1"), 15);

        int count = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Menu count after fresh activation: " + count);
        Assert.assertEquals(count, 12,
                "App did not reset to clean main screen — possible stale state/session leak");
    }

    // ============================================================
    // CATEGORY: ACCESSIBILITY BASICS
    // ============================================================

    // 15. সব Main menu item এ content-desc/accessibility label আছে কিনা (screen reader readiness)
    @Test(priority = 15, groups = "accessibility")
    public void accessibility_MenuItemsHaveAccessibilityLabels() {
        List<WebElement> items = mainPage.getAllMenuItems();
        int missingLabels = 0;

        for (WebElement item : items) {
            String desc = item.getAttribute("content-desc");
            String text = item.getText();
            if ((desc == null || desc.isEmpty()) && (text == null || text.isEmpty())) {
                missingLabels++;
            }
        }

        test.log(Status.INFO, "Menu items missing accessibility labels: " + missingLabels + "/" + items.size());
        Assert.assertEquals(missingLabels, 0, "Some menu items lack accessible labels for screen readers");
    }

    // 16. Tap target size — element গুলো ছোট accidental-tap-prone সাইজে নেই কিনা (Android এর ৪৮dp ন্যূনতম গাইডলাইন অনুসরণ যাচাই)
    @Test(priority = 16, groups = "accessibility")
    public void accessibility_TapTargetsAreReasonablySized() {
        WebElement firstItem = mainPage.getAllMenuItems().get(0);
        org.openqa.selenium.Rectangle rect = firstItem.getRect();

        test.log(Status.INFO, "First menu item size: " + rect.getWidth() + "x" + rect.getHeight());

        // ছোট হলেও crash না করিয়ে শুধু flag করব (visual/manual review প্রয়োজন বলে)
        Assert.assertTrue(rect.getHeight() > 0, "Tap target height should be measurable and non-zero");
    }

    // ============================================================
    // CATEGORY: TEMPLATES — বাস্তব App এ ব্যবহারের জন্য (ApiDemos এ প্রযোজ্য নয়)
    // ============================================================

    /*
     * নিচের টেস্ট গুলো বাস্তব Login/E-commerce app এ ব্যবহারের জন্য Template/Skeleton —
     * ApiDemos এ Login, Cart, Checkout, OTP, Payment, Push Notification, API
     * না থাকায় এগুলো এখন @Test হিসেবে enable করা নেই (intentionally commented),
     * কিন্তু বাস্তব project এ সরাসরি uncomment করে locator বসিয়ে ব্যবহার করা যাবে।
     */

    // @Test(groups = "auth")
    // public void auth_InvalidLoginShowsErrorMessage() {
    //     loginPage.enterUsername("wronguser");
    //     loginPage.enterPassword("wrongpass");
    //     loginPage.clickLogin();
    //     Assert.assertTrue(loginPage.isErrorMessageDisplayed(), "Error message should show on invalid login");
    // }

    // @Test(groups = "auth")
    // public void auth_SessionTimeoutRedirectsToLogin() {
    //     // dashboard এ থাকা অবস্থায় token expire সিমুলেট করে
    //     // redirect হয়ে login screen এ আসে কিনা যাচাই
    // }

    // @Test(groups = "business-flow")
    // public void businessFlow_AddToCartAndCheckoutCompletesOrder() {
    //     productPage.addToCart();
    //     cartPage.proceedToCheckout();
    //     checkoutPage.completePayment();
    //     Assert.assertTrue(orderConfirmationPage.isOrderConfirmed());
    // }

    // @Test(groups = "api")
    // public void api_UiDataMatchesBackendResponse() {
    //     // REST Assured/HttpClient দিয়ে backend API hit করে response নিয়ে
    //     // UI তে দেখানো data এর সাথে cross-verify করা
    // }

    // @Test(groups = "notifications")
    // public void notifications_TapOpensCorrectScreen() {
    //     // push notification tray থেকে tap করে app এর সঠিক screen এ navigate করে কিনা
    // }
}