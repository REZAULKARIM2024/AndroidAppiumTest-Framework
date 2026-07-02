package pages.mydemo;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Cart Page — SauceLabs My Demo App (Native Android v2.2.0)
 * Package: com.saucelabs.mydemoapp.android
 *
 * The cart screen shows added products and a "Proceed To Checkout" button.
 */
public class CartPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    private static final String PKG = "com.saucelabs.mydemoapp.android";

    // Cart items use the same titleTV/priceTV IDs as catalog
    private final By cartItemTitle      = By.id(PKG + ":id/titleTV");
    private final By cartItemPrice      = By.id(PKG + ":id/priceTV");

    // Checkout button — common IDs/text for this app
    private final By proceedToCheckout  = By.id(PKG + ":id/proceedToCheckoutBt");
    private final By checkoutFallback   = By.xpath(
        "//*[@text='Proceed To Checkout' or @text='PROCEED TO CHECKOUT'"
        + " or @text='Checkout' or @text='CHECKOUT'"
        + " or contains(@content-desc,'Proceed To Checkout')]"
    );

    // Cart page indicator — "Cart" header text.
    // NOTE: cartTV is the cart COUNT BADGE on the catalog toolbar, NOT a cart page header —
    // including it here caused isCartPageVisible() to return true on the catalog screen.
    private final By cartHeader = By.xpath(
        "//*[@text='Cart' or @text='MY CART' or @text='Your Cart']"
    );

    public CartPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    /** True when the cart screen is visible (instant check — no blocking wait) */
    public boolean isCartPageVisible() {
        // Use instant findElements() — wait.until() here blocks 15s per call, causing
        // cascade failures in smoke tests and perf poll loops (30s wasted per check).
        try {
            if (!driver.findElements(proceedToCheckout).isEmpty()) return true;
            if (!driver.findElements(checkoutFallback).isEmpty()) return true;
            return !driver.findElements(cartHeader).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /** Number of distinct products in the cart */
    public int getCartItemCount() {
        try {
            return driver.findElements(cartItemTitle).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Name of the first item in the cart */
    public String getFirstItemName() {
        try {
            List<WebElement> items = driver.findElements(cartItemTitle);
            return items.isEmpty() ? "Unknown" : items.get(0).getText();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /** Tap "Proceed To Checkout" */
    public void proceedToCheckout() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(proceedToCheckout)).click();
        } catch (Exception e) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(checkoutFallback)).click();
            } catch (Exception ex) {
                System.out.println("[CartPage] proceedToCheckout not found: " + ex.getMessage());
            }
        }
    }
}
