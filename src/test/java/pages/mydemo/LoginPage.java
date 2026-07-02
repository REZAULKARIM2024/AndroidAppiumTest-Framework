package pages.mydemo;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Login Page — SauceLabs My Demo App (Native Android v2.2.0)
 * Package: com.saucelabs.mydemoapp.android
 *
 * Test credentials:
 *   Username : bob@example.com
 *   Password : 10203040
 *
 * NOTE: The native app opens on the catalog; navigate to login via hamburger menu first.
 */
public class LoginPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    // Resource IDs (native Android)
    private static final String PKG = "com.saucelabs.mydemoapp.android";
    private final By usernameResId  = By.id(PKG + ":id/nameET");
    private final By passwordResId  = By.id(PKG + ":id/passwordET");
    private final By loginBtnResId  = By.id(PKG + ":id/loginBtn");

    // Accessibility IDs (may match if app uses contentDescription)
    private final By usernameField = AppiumBy.accessibilityId("Username input field");
    private final By passwordField = AppiumBy.accessibilityId("Password input field");
    private final By loginButton   = AppiumBy.accessibilityId("Login button");

    // XPath fallbacks (always reliable)
    private final By usernameFallback = By.xpath("//android.widget.EditText[1]");
    private final By passwordFallback = By.xpath("//android.widget.EditText[2]");
    private final By loginFallback    = By.xpath("//*[@text='Login' or @text='LOGIN' or @text='Sign In']");

    // Navigation to reach login screen from catalog
    // ID confirmed from UI dump: com.saucelabs.mydemoapp.android:id/menuIV, content-desc="View menu"
    private final By hamburgerMenu = By.id(PKG + ":id/menuIV");
    private final By loginNavItem  = By.xpath(
        "//*[@text='Log In' or @text='Login' or @text='Log in' or @text='LOG IN'"
        + " or @resource-id='" + PKG + ":id/loginTV']"
    );
    private final By logoutNavItem = By.xpath(
        "//*[@text='Log Out' or @text='Logout' or @text='Log out' or @text='LOG OUT'"
        + " or @resource-id='" + PKG + ":id/logoutTV']"
    );

    public LoginPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    /**
     * Click an element instantly (no elementToBeClickable wait).
     * Uses findElements() first; if not yet in DOM, falls back to presenceOfElementLocated.
     *
     * Background: Android elements can be present + enabled but still not in "clickable"
     * accessibility state, causing elementToBeClickable to time out for 15s even when the
     * element is fully interactable.  presenceOfElementLocated just checks the DOM, which
     * is all we need — direct .click() works once the element is in the tree.
     */
    private void clickInstant(By locator) {
        List<WebElement> els = driver.findElements(locator);
        if (!els.isEmpty()) {
            els.get(0).click();
        } else {
            wait.until(ExpectedConditions.presenceOfElementLocated(locator)).click();
        }
    }

    /**
     * Poll for the hamburger menu (menuIV) for up to {@code maxMs} milliseconds.
     * Returns true as soon as it appears in the DOM.
     *
     * WHY: after closing the nav drawer there is a brief animation window (~300-600ms)
     * where menuIV is transiently absent from the accessibility tree. An instant
     * findElements() check during that window returns empty and triggers a spurious
     * BACK press — pushing the app off the catalog and making all subsequent tests fail.
     * Polling for up to 2 seconds absorbs the animation without wasting meaningful time.
     */
    private boolean isHamburgerVisible(long maxMs) {
        long end = System.currentTimeMillis() + maxMs;
        do {
            try {
                if (!driver.findElements(hamburgerMenu).isEmpty()) return true;
            } catch (Exception ignored) {}
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        } while (System.currentTimeMillis() < end);
        return false;
    }

    /**
     * Navigate to login screen if not already there.
     * Native app opens on catalog → hamburger menu → "Log In" option.
     * If already logged in, clicks "Log Out" (which takes app to login screen directly).
     */
    public void navigateToLoginIfNeeded() {
        if (isLoginScreenVisible()) {
            System.out.println("[LoginPage] Already on login screen");
            return;
        }

        // If hamburger is not reachable (e.g., on checkout/cart/product-detail, or still
        // animating after drawer close), press Back until catalog (hamburger visible) or login.
        // Poll up to 2s before deciding hamburger is absent — absorbs brief animation windows.
        if (!isHamburgerVisible(2000)) {
            System.out.println("[LoginPage] Hamburger not visible — pressing back to reach catalog");
            for (int i = 0; i < 6; i++) {
                try {
                    driver.navigate().back();
                    Thread.sleep(1200);
                } catch (Exception e) { break; }
                if (isLoginScreenVisible()) {
                    System.out.println("[LoginPage] Reached login screen via back navigation");
                    return;
                }
                // Poll 1.5s after each back press before concluding hamburger is still absent
                if (isHamburgerVisible(1500)) {
                    System.out.println("[LoginPage] Hamburger visible after " + (i + 1) + " back press(es)");
                    break;
                }
            }
        }

        // If hamburger is still missing after back presses, the app may have been sent
        // to the Android home screen by a previous test pressing BACK from the catalog.
        // Re-activating the app brings us back to the catalog with menuIV visible.
        if (!isHamburgerVisible(1000) && !isLoginScreenVisible()) {
            System.out.println("[LoginPage] Hamburger still absent — re-activating app");
            try {
                driver.activateApp("com.saucelabs.mydemoapp.android");
                Thread.sleep(3000);
            } catch (Exception e) {
                System.out.println("[LoginPage] Re-activation failed: " + e.getMessage());
            }
        }

        if (isLoginScreenVisible()) return;

        try {
            // Open navigation drawer
            System.out.println("[LoginPage] Opening hamburger menu...");
            clickInstant(hamburgerMenu);
            Thread.sleep(1500); // wait for drawer animation

            // Poll up to 3s for drawer items to appear.
            // After ADB operations or heavy test sequences, the emulator can be sluggish
            // and the drawer may take longer than 1500ms to populate.
            List<WebElement> logoutItems = java.util.Collections.emptyList();
            long drawerPollEnd = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < drawerPollEnd) {
                logoutItems = driver.findElements(logoutNavItem);
                if (!logoutItems.isEmpty() || !driver.findElements(loginNavItem).isEmpty()) break;
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            }
            // If NEITHER Log Out NOR Log In appeared after 3s, first extend the wait.
            // Interrupt tests (SMS, call simulations) leave the emulator under heavy CPU load
            // and the drawer NavigationView can take 5-8s to inflate on a loaded emulator.
            if (logoutItems.isEmpty() && driver.findElements(loginNavItem).isEmpty()) {
                System.out.println("[LoginPage] Drawer items slow — extending wait 5s...");
                drawerPollEnd = System.currentTimeMillis() + 5000;
                while (System.currentTimeMillis() < drawerPollEnd) {
                    logoutItems = driver.findElements(logoutNavItem);
                    if (!logoutItems.isEmpty() || !driver.findElements(loginNavItem).isEmpty()) break;
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                }
            }
            // If STILL no items after 8s total, the hamburger click may not have registered.
            // DO NOT use navigate().back() — back from an open drawer exits the app on this device.
            // Strategy: toggle the hamburger once (closes drawer if open / opens if closed),
            // then check. If items appear, done. If drawer just closed (no items), reopen.
            if (logoutItems.isEmpty() && driver.findElements(loginNavItem).isEmpty()) {
                System.out.println("[LoginPage] Drawer items absent — toggling hamburger");
                try {
                    List<WebElement> hm = driver.findElements(hamburgerMenu);
                    if (!hm.isEmpty()) {
                        hm.get(0).click(); // toggle: closes open drawer OR opens closed drawer
                        Thread.sleep(1000);
                        // Check if items appeared (drawer was closed → just opened)
                        logoutItems = driver.findElements(logoutNavItem);
                        if (logoutItems.isEmpty() && driver.findElements(loginNavItem).isEmpty()) {
                            // Items still absent → drawer was open and we just closed it.
                            // Reopen the drawer now.
                            System.out.println("[LoginPage] Toggle closed drawer — reopening");
                            hm = driver.findElements(hamburgerMenu);
                            if (!hm.isEmpty()) { hm.get(0).click(); Thread.sleep(1500); }
                            else { clickInstant(hamburgerMenu); Thread.sleep(1500); }
                        }
                    } else {
                        // Hamburger not found at all — app may be on home screen; activate it
                        System.out.println("[LoginPage] Hamburger absent during retry — activating app");
                        driver.activateApp("com.saucelabs.mydemoapp.android");
                        Thread.sleep(2500);
                        clickInstant(hamburgerMenu);
                        Thread.sleep(1500);
                    }
                } catch (Exception ignored) {}
                drawerPollEnd = System.currentTimeMillis() + 5000;
                while (System.currentTimeMillis() < drawerPollEnd) {
                    logoutItems = driver.findElements(logoutNavItem);
                    if (!logoutItems.isEmpty() || !driver.findElements(loginNavItem).isEmpty()) break;
                    try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                }
            }
            if (!logoutItems.isEmpty()) {
                // User is logged in → click "Log Out" → confirmation dialog appears
                System.out.println("[LoginPage] Logged in → clicking Log Out in drawer");
                logoutItems.get(0).click();
                Thread.sleep(1500); // wait for confirmation dialog

                // Confirm logout — dialog has "LOGOUT" button (all caps).
                // Poll up to 4s rather than using clickInstant (which falls back to
                // presenceOfElementLocated and blocks 15s when the dialog is absent).
                By logoutConfirmBtn = By.xpath("//*[@text='LOGOUT']");
                List<WebElement> confirmBtns = java.util.Collections.emptyList();
                long confirmEnd = System.currentTimeMillis() + 4000;
                while (System.currentTimeMillis() < confirmEnd) {
                    confirmBtns = driver.findElements(logoutConfirmBtn);
                    if (!confirmBtns.isEmpty()) break;
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                }
                if (!confirmBtns.isEmpty()) {
                    confirmBtns.get(0).click();
                    System.out.println("[LoginPage] Clicked LOGOUT confirmation button");
                } else {
                    System.out.println("[LoginPage] No confirm dialog — may have auto-dismissed");
                }
                Thread.sleep(2000); // wait for navigation after logout

                // App v2.2.0 always returns to the Android home screen after logout.
                // Skip currentActivity() — it runs ADB "dumpsys window displays" which
                // can hang for 20s on a loaded emulator. Just always reactivate the app.
                System.out.println("[LoginPage] Reactivating app after logout");
                try {
                    driver.activateApp("com.saucelabs.mydemoapp.android");
                    Thread.sleep(3000);
                } catch (Exception actEx) {
                    System.out.println("[LoginPage] App activation failed: " + actEx.getMessage());
                }

                System.out.println("[LoginPage] After logout — login screen visible: " + isLoginScreenVisible());

                // After returning to app: may be on login screen directly, or on catalog.
                // If on catalog, open hamburger and navigate to Log In.
                if (!isLoginScreenVisible()) {
                    System.out.println("[LoginPage] On catalog after logout — navigating to Log In");
                    Thread.sleep(500);
                    clickInstant(hamburgerMenu);
                    Thread.sleep(1500);
                    clickInstant(loginNavItem);
                    Thread.sleep(1000);
                    System.out.println("[LoginPage] Login screen after re-nav: " + isLoginScreenVisible());
                }
                return;
            }

            // Not logged in → click "Log In" in drawer
            System.out.println("[LoginPage] Clicking Log In...");
            clickInstant(loginNavItem);

            // Poll for login screen — Thread.sleep(2500) is too short on emulator;
            // polling up to 10s handles slow screen transitions without wasting time.
            long loginWaitEnd = System.currentTimeMillis() + 10_000;
            while (System.currentTimeMillis() < loginWaitEnd) {
                if (isLoginScreenVisible()) break;
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
            }
            System.out.println("[LoginPage] Login screen visible: " + isLoginScreenVisible());
        } catch (Exception e) {
            System.out.println("[LoginPage] navigateToLoginIfNeeded error: " + e.getMessage());
        }
    }

    public boolean isLoginScreenVisible() {
        // Require BOTH username AND password fields — prevents false positives on
        // checkout/payment forms that also contain EditText fields.
        try {
            // Primary: resource IDs (both must be present)
            if (!driver.findElements(usernameResId).isEmpty()
                    && !driver.findElements(passwordResId).isEmpty()) return true;
            // Secondary: accessibility IDs (both must be present)
            if (!driver.findElements(usernameField).isEmpty()
                    && !driver.findElements(passwordField).isEmpty()) return true;
            // Tertiary: login button resource ID (unique to login screen)
            return !driver.findElements(loginBtnResId).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public void enterUsername(String username) {
        WebElement field = findBestField(usernameResId, usernameField, usernameFallback);
        field.clear();
        field.sendKeys(username);
    }

    public void enterPassword(String password) {
        WebElement field = findBestField(passwordResId, passwordField, passwordFallback);
        field.clear();
        field.sendKeys(password);
    }

    public void tapLoginButton() {
        try { driver.hideKeyboard(); } catch (Exception ignored) {}
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        findBestField(loginBtnResId, loginButton, loginFallback).click();
    }

    /** Navigate to login (if needed) and log in with credentials */
    public void login(String username, String password) {
        navigateToLoginIfNeeded();
        enterUsername(username);
        enterPassword(password);
        tapLoginButton();
    }

    /** Check if error message is displayed */
    public boolean isErrorDisplayed() {
        try {
            String src = driver.getPageSource().toLowerCase();
            return src.contains("provided credentials") || src.contains("generic err")
                || src.contains("wrong") || src.contains("invalid")
                || !driver.findElements(By.id(PKG + ":id/errorTV")).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /** Try resource-id first, then accessibilityId, then XPath fallback.
     *  Uses instant findElements() before falling back to a presence wait —
     *  avoids the 15s elementToBeClickable timeout when EditText fields are
     *  present in the DOM but not yet in a "clickable" accessibility state. */
    private WebElement findBestField(By resId, By accId, By xpath) {
        // 1. Instant check — no blocking wait
        java.util.List<org.openqa.selenium.WebElement> found;
        try {
            found = driver.findElements(resId);
            if (!found.isEmpty()) return found.get(0);
        } catch (Exception ignored) {}
        try {
            found = driver.findElements(accId);
            if (!found.isEmpty()) return found.get(0);
        } catch (Exception ignored) {}
        try {
            found = driver.findElements(xpath);
            if (!found.isEmpty()) return found.get(0);
        } catch (Exception ignored) {}
        // 2. Slow path — element not in DOM yet, wait for it to appear
        try {
            return wait.until(ExpectedConditions.presenceOfElementLocated(resId));
        } catch (Exception e1) {
            try {
                return wait.until(ExpectedConditions.presenceOfElementLocated(accId));
            } catch (Exception e2) {
                return wait.until(ExpectedConditions.presenceOfElementLocated(xpath));
            }
        }
    }
}
