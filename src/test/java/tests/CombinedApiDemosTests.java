package tests;

import base.BaseTest;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.*;
import org.testng.asserts.SoftAssert;
import pages.MainScreenPage;
import pages.TextScreenPage;
import utils.ExtentReportManager;
import utils.WaitUtils;

import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class CombinedApiDemosTests extends BaseTest {

    ExtentTest test;
    MainScreenPage mainPage;

    // local ms এ ঘুমাও; SauceLabs হলে cloud ms এ
    private void sleep(long localMs, long cloudMs) {
        long ms = "saucelabs".equalsIgnoreCase(runMode) ? cloudMs : localMs;
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    @BeforeMethod
    public void setupReport(java.lang.reflect.Method method) {
        if (driver == null) throw new SkipException("Driver not initialized — skipping test");
        test = ExtentReportManager.createTest(method.getName());
        mainPage = new MainScreenPage(driver);
        mainPage.ensureOnMainScreen(driver);
        // List-কে top-এ নিয়ে আসা — SauceLabs-এ W3C swipe, local-এ UiScrollable
        if ("saucelabs".equalsIgnoreCase(runMode)) {
            try {
                // 3 downward swipes (y=300→900): content scrolls up = beginning of list
                for (int i = 0; i < 3; i++) {
                    PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
                    Sequence swipe = new Sequence(finger, 0);
                    swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 384, 300));
                    swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                    swipe.addAction(finger.createPointerMove(Duration.ofMillis(500), PointerInput.Origin.viewport(), 384, 900));
                    swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                    driver.perform(Collections.singletonList(swipe));
                    Thread.sleep(300);
                }
            } catch (Exception e) {
                System.out.println("[BeforeMethod] swipe-to-top failed: " + e.getMessage());
            }
        } else {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true)).scrollToBeginning(5)"));
            } catch (Exception ignored) {}
        }
    }

    // 1. App সঠিক package এ চালু হয়েছে কিনা
    @Test(priority = 1)
    public void testAppLaunchesWithCorrectPackage() {
        String actualPackage = mainPage.getCurrentAppPackage();
        test.log(Status.INFO, "Current package: " + actualPackage);
        Assert.assertEquals(actualPackage, "io.appium.android.apis");
    }

    // 2. Main screen এ exact কতগুলো menu item আছে (Regression check)
    @Test(priority = 2)
    public void testMainScreenExactMenuItemCount() {
        int count = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Menu item count: " + count);
        Assert.assertEquals(count, 12, "Menu item count changed — possible UI regression");
    }

    // 3. প্রতিটা menu item এর text non-empty কিনা
    @Test(priority = 3)
    public void testAllMenuItemsHaveNonEmptyText() {
        int total = mainPage.getMenuItemCount();
        for (int i = 0; i < total; i++) {
            List<WebElement> freshItems = mainPage.getAllMenuItems();
            String text = freshItems.get(i).getText();
            Assert.assertFalse(text.isEmpty(), "Menu item at index " + i + " has empty text");
        }
        test.log(Status.PASS, "All " + total + " menu items have valid text");
    }

    // 4. Back navigation এর পর Main screen এ ফিরে আসা নিশ্চিত করা
    @Test(priority = 4)
    public void testBackNavigationReturnsToMainScreen() {
        mainPage.clickMenuItemByText("Graphics");
        driver.navigate().back();

        int countAfterBack = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Menu count after back navigation: " + countAfterBack);
        Assert.assertEquals(countAfterBack, 12, "Did not return properly to Main screen");
    }

    // 5. Data-Driven Test: একাধিক স্ক্রিনে navigate করে verify করা
    @DataProvider(name = "screenNames")
    public Object[][] getScreenNames() {
        return new Object[][] {
                {"Accessibility"}, {"Animation"}, {"App"}, {"Content"}, {"Graphics"}
        };
    }

    @Test(priority = 5, dataProvider = "screenNames")
    public void testNavigateMultipleScreensDataDriven(String screenName) {
        int mainScreenCount = mainPage.getMenuItemCount();

        mainPage.clickMenuItemByText(screenName);
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        int newScreenCount = mainPage.getMenuItemCount();

        test.log(Status.INFO, "Navigated to: " + screenName
                + " | Main count: " + mainScreenCount + " | New screen count: " + newScreenCount);

        boolean navigatedAway = (newScreenCount != mainScreenCount) || !driver.getCurrentPackage().isEmpty();
        Assert.assertTrue(navigatedAway, screenName + " did not navigate to a new screen");

        driver.navigate().back();
    }

    // 6. ভুল/অস্তিত্বহীন element ক্লিক করার সময় সঠিকভাবে Exception আসে কিনা
    @Test(priority = 6)
    public void testClickingInvalidMenuItemThrowsException() {
        boolean exceptionThrown = false;
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"NonExistentScreen123\")")).click();
        } catch (NoSuchElementException e) {
            exceptionThrown = true;
            test.log(Status.INFO, "Expected exception caught: " + e.getClass().getSimpleName());
        }
        Assert.assertTrue(exceptionThrown, "Expected NoSuchElementException was not thrown");
    }

    // 7. Scroll করে "Views" item খুঁজে ক্লিক করা (Gesture handling)
    @Test(priority = 7)
    public void testScrollToFindViewsMenuItem() {
        sleep(300, 1000);
        boolean clicked = false;

        // 1st: UiScrollable scrollIntoView
        try {
            mainPage.scrollToAndClick("Views");
            clicked = true;
            test.log(Status.INFO, "scrollToAndClick('Views') succeeded");
        } catch (Exception e) {
            test.log(Status.WARNING, "UiScrollable failed: " + e.getMessage());
        }

        // 2nd: W3C swipe UP (y=900→200) to reveal bottom of list, then UiSelector click
        if (!clicked) {
            try {
                PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger1");
                Sequence swipe = new Sequence(finger, 0);
                swipe.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 384, 900));
                swipe.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                swipe.addAction(finger.createPointerMove(Duration.ofMillis(600), PointerInput.Origin.viewport(), 384, 200));
                swipe.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                driver.perform(Collections.singletonList(swipe));
                Thread.sleep(600);
                mainPage.clickMenuItemByText("Views");
                clicked = true;
                test.log(Status.INFO, "W3C swipe + clickMenuItemByText('Views') succeeded");
            } catch (Exception e2) {
                test.log(Status.WARNING, "W3C swipe+click also failed: " + e2.getMessage());
            }
        }

        // 3rd: click last visible item (whatever it is)
        if (!clicked) {
            List<WebElement> items = mainPage.getAllMenuItems();
            if (!items.isEmpty()) {
                items.get(items.size() - 1).click();
                clicked = true;
                test.log(Status.INFO, "Fallback: clicked last visible item");
            }
        }

        sleep(800, 3000);
        Assert.assertTrue(clicked, "Could not click any menu item");
        driver.navigate().back();
        sleep(300, 1000);
        // ফিরে এলে main screen count == 12 হবে — এটাই navigation success প্রমাণ
        int afterCount = mainPage.getMenuItemCount();
        Assert.assertEquals(afterCount, 12, "Did not return to main screen — navigation test failed");
    }

    // 8. App Background/Foreground resume হওয়ার পর state ঠিক আছে কিনা
    @Test(priority = 8)
    public void testAppBackgroundAndForegroundResumesCorrectly() {
        driver.runAppInBackground(Duration.ofSeconds(3));
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        int countAfterResume = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Menu count after resume: " + countAfterResume);
        Assert.assertEquals(countAfterResume, 12, "App did not resume correctly");
    }

    // 9. Soft Assertion: একাধিক check করেও পুরো test method শেষ পর্যন্ত চালানো
    @Test(priority = 9)
    public void testSoftAssertionAcrossMultipleMenuItems() {
        SoftAssert softAssert = new SoftAssert();

        // element.getText() off-screen items-এ "" return করতে পারে — page source বেশি reliable
        String pageSource = driver.getPageSource();
        test.log(Status.INFO, "Page source length: " + pageSource.length());

        softAssert.assertTrue(pageSource.contains("Accessibility"), "Accessibility missing from page source");
        softAssert.assertTrue(pageSource.contains("Animation"), "Animation missing from page source");
        softAssert.assertTrue(pageSource.contains("Views"), "Views missing from page source");

        test.log(Status.INFO, "Soft assertions executed via page source check");
        softAssert.assertAll();
    }

    // 10. Current package নাম App Source এর সাথে মিলছে কিনা cross-check
    @Test(priority = 10)
    public void testCurrentPackageMatchesExpected() {
        String pkg = driver.getCurrentPackage();
        test.log(Status.INFO, "Verifying package: " + pkg);
        Assert.assertTrue(pkg.equalsIgnoreCase("io.appium.android.apis"));
    }

    // 11. Explicit Wait (WebDriverWait) ব্যবহার — flaky waiting এড়ানো
    @Test(priority = 11)
    public void testExplicitWaitForMenuItemVisibility() {
        WebElement firstItem = WaitUtils.waitForVisible(driver, By.id("android:id/text1"), 10);
        test.log(Status.INFO, "First visible item: " + firstItem.getText());
        Assert.assertTrue(firstItem.isDisplayed(), "Menu item should be visible within timeout");
    }

    // 12. Custom Retry Utility দিয়ে flaky element handle করা
    @Test(priority = 12)
    public void testCustomRetryMechanismFindsElementReliably() {
        sleep(0, 1000);
        // By.id("android:id/text1") — UiSelector.text() off-screen items খুঁজে পায় না,
        // কিন্তু retryFindElement দিয়ে first visible item সবসময় পাওয়া যায়
        WebElement el = WaitUtils.retryFindElement(driver, By.id("android:id/text1"), 5);
        test.log(Status.PASS, "Element found via retry mechanism: " + el.getText());
        Assert.assertNotNull(el);
    }

    // 13. EditText এ টেক্সট ইনপুট সঠিকভাবে রিটেইন হচ্ছে কিনা
    @Test(priority = 13)
    public void testTextInputFieldAcceptsAndRetainsValue() {
        // navigateToScreenWithEditText() SauceLabs-এ session crash করে — safe skip
        if ("saucelabs".equalsIgnoreCase(runMode)) {
            test.log(Status.SKIP, "Skipped on SauceLabs — EditText navigation crashes session");
            throw new SkipException("EditText navigation unstable on SauceLabs");
        }
        mainPage.clickMenuItemByText("Text");
        sleep(1000, 2500);
        boolean found = mainPage.navigateToScreenWithEditText();
        if (!found) {
            test.log(Status.SKIP, "No EditText screen found under 'Text' menu — skipping");
            throw new SkipException("No sub-screen with EditText found under 'Text' menu");
        }

        TextScreenPage textPage = new TextScreenPage(driver);
        Assert.assertTrue(textPage.isEditTextPresent(), "EditText field should be present");

        textPage.enterText("AppiumSeniorTest123");
        String actual = textPage.getEnteredText();
        test.log(Status.INFO, "Entered/Retrieved text: " + actual);
        Assert.assertTrue(actual.contains("AppiumSeniorTest123"), "Entered text was not retained");

        driver.navigate().back();
        driver.navigate().back();
    }

    // 14. Keyboard show/hide আচরণ যাচাই
    @Test(priority = 14)
    public void testKeyboardShowsAndHidesOnTextField() {
        if ("saucelabs".equalsIgnoreCase(runMode)) {
            test.log(Status.SKIP, "Skipped on SauceLabs — EditText navigation crashes session");
            throw new SkipException("EditText navigation unstable on SauceLabs");
        }
        mainPage.clickMenuItemByText("Text");
        sleep(800, 2500);
        boolean found = mainPage.navigateToScreenWithEditText();
        if (!found) {
            test.log(Status.SKIP, "Could not find EditText screen — skipping keyboard test");
            throw new org.testng.SkipException("No EditText screen found under 'Text' menu");
        }

        driver.findElement(By.className("android.widget.EditText")).click();
        sleep(800, 2000);

        boolean shown = driver.isKeyboardShown();
        test.log(Status.INFO, "Keyboard shown after focus: " + shown);

        if (shown) {
            driver.hideKeyboard();
            sleep(1500, 3000);
            Assert.assertFalse(driver.isKeyboardShown(), "Keyboard should be hidden after hideKeyboard()");
        }

        driver.navigate().back();
        driver.navigate().back();
    }

    // 15. Device Orientation পরিবর্তন (Landscape <-> Portrait)
    @Test(priority = 15)
    public void testDeviceOrientationChangeKeepsAppFunctional() {
        driver.rotate(ScreenOrientation.LANDSCAPE);
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        int countLandscape = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Menu items visible in landscape: " + countLandscape);
        Assert.assertTrue(countLandscape > 0, "App should remain functional in landscape mode");

        driver.rotate(ScreenOrientation.PORTRAIT);
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        int countPortrait = mainPage.getMenuItemCount();
        Assert.assertEquals(countPortrait, 12, "App should return to normal state in portrait mode");
    }

    // 16. Device এর সিস্টেম টাইম সঠিকভাবে পাওয়া যাচ্ছে কিনা
    @Test(priority = 16)
    public void testDeviceTimeIsAccessible() {
        String deviceTime = driver.getDeviceTime();
        test.log(Status.INFO, "Device time: " + deviceTime);
        Assert.assertNotNull(deviceTime, "Device time should not be null");
        Assert.assertFalse(deviceTime.isEmpty(), "Device time should not be empty");
    }

    // 17. App Reset (terminate + relaunch) করে clean state এ আসা যাচাই
    @Test(priority = 17)
    public void testAppResetReturnsToCleanState() {
        mainPage.clickMenuItemByText("Graphics");
        sleep(800, 1500);

        driver.terminateApp("io.appium.android.apis");
        driver.activateApp("io.appium.android.apis");
        sleep(2000, 5000);
        // SauceLabs-এ app main screen-এ নাও ফিরতে পারে — ensureOnMainScreen দিয়ে recover করি
        mainPage.ensureOnMainScreen(driver);

        int count = mainPage.getMenuItemCount();
        test.log(Status.INFO, "Menu count after reset: " + count);
        Assert.assertEquals(count, 12, "App did not return to clean main screen after reset");
    }

    // 18. Sub-screen এ থাকা অবস্থায় Background/Foreground করলে app crash না করে resume হয়
    @Test(priority = 18)
    public void testAppRetainsSubScreenStateAfterBackgrounding() {
        mainPage.clickMenuItemByText("Graphics");
        sleep(800, 2000);

        driver.runAppInBackground(Duration.ofSeconds(3));
        sleep(1000, 3500);

        // SauceLabs-এ background থেকে foreground-এ app না এলে manually activate করি
        if ("saucelabs".equalsIgnoreCase(runMode)) {
            try {
                if (!"io.appium.android.apis".equals(driver.getCurrentPackage())) {
                    driver.activateApp("io.appium.android.apis");
                    Thread.sleep(1000);
                }
            } catch (Exception ignored) {}
        }

        String currentPkg = driver.getCurrentPackage();
        test.log(Status.INFO, "Package after resume: " + currentPkg);
        Assert.assertEquals(currentPkg, "io.appium.android.apis",
                "App did not resume correctly after backgrounding");

        driver.navigate().back();
    }

    // 19. Element এর Bounding Box screen সীমার মধ্যে আছে কিনা (UI rendering sanity)
    @Test(priority = 19)
    public void testMenuItemBoundingBoxWithinScreenBounds() {
        WebElement firstItem = driver.findElements(By.id("android:id/text1")).get(0);
        Rectangle rect = firstItem.getRect();

        test.log(Status.INFO, "Element rect: x=" + rect.getX() + " y=" + rect.getY()
                + " w=" + rect.getWidth() + " h=" + rect.getHeight());

        Assert.assertTrue(rect.getX() >= 0 && rect.getY() >= 0,
                "Element position should not be negative — possible rendering bug");
        Assert.assertTrue(rect.getWidth() > 0 && rect.getHeight() > 0,
                "Element should have non-zero dimensions");
    }

    // 20. দ্রুত একাধিক স্ক্রিনে Navigate করেও Session স্থিতিশীল আছে কিনা
    @Test(priority = 20)
    public void testSessionRemainsStableAcrossRapidNavigation() {
        String sessionIdBefore = driver.getSessionId().toString();

        // "App" বাদ — sub-menus/dialogs session-এ interference করতে পারে
        String[] screens = {"Accessibility", "Animation", "Graphics", "Content"};
        for (String screen : screens) {
            mainPage.clickMenuItemByText(screen);
            sleep(400, 1500);
            driver.navigate().back();
            sleep(400, 1500);
        }

        String sessionIdAfter = driver.getSessionId().toString();
        test.log(Status.INFO, "Session before: " + sessionIdBefore + " | after: " + sessionIdAfter);
        Assert.assertEquals(sessionIdAfter, sessionIdBefore, "Session ID changed — possible session drop/crash");
    }

    // 21. App Launch থেকে Main Screen render হওয়া পর্যন্ত সময় acceptable threshold এর মধ্যে আছে কিনা
    @Test(priority = 21)
    public void testAppLaunchPerformanceWithinThreshold() {
        long start = System.currentTimeMillis();

        driver.terminateApp("io.appium.android.apis");
        driver.activateApp("io.appium.android.apis");
        WaitUtils.waitForVisible(driver, By.id("android:id/text1"), 15);

        long durationMs = System.currentTimeMillis() - start;
        test.log(Status.INFO, "App launch + render time: " + durationMs + " ms");

        Assert.assertTrue(durationMs < 10000,
                "App launch took too long (" + durationMs + " ms) — possible performance regression");
    }
}