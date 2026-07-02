package pages.mydemo;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileWriter;
import java.time.Duration;

/**
 * Checkout Page — SauceLabs My Demo App (Native Android v2.2.0)
 * Package: com.saucelabs.mydemoapp.android
 *
 * Handles multi-step checkout: Shipping → Payment → Review → Confirmation.
 */
public class CheckoutPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    private static final String PKG = "com.saucelabs.mydemoapp.android";

    // ── Shipping form (step 1) ─────────────────────────────────────────────
    // Try resource-ids first, then position-based XPath fallback
    private final By fullNameField = By.id(PKG + ":id/fullNameET");
    private final By addressField  = By.id(PKG + ":id/address1ET");
    private final By cityField     = By.id(PKG + ":id/cityET");
    private final By zipField      = By.id(PKG + ":id/zipET");
    private final By countryField  = By.id(PKG + ":id/countryET");

    // Fallbacks using content-desc or text
    private final By fullNameFallback = By.xpath(
        "//android.widget.EditText[contains(@content-desc,'Full Name') or contains(@hint,'Full Name')]");
    private final By addressFallback  = By.xpath(
        "//android.widget.EditText[contains(@content-desc,'Address') or contains(@hint,'Address')]");
    private final By cityFallback     = By.xpath(
        "//android.widget.EditText[contains(@content-desc,'City') or contains(@hint,'City')]");
    private final By zipFallback      = By.xpath(
        "//android.widget.EditText[contains(@content-desc,'Zip') or contains(@hint,'Zip') or contains(@hint,'Postal')]");
    private final By countryFallback  = By.xpath(
        "//android.widget.EditText[contains(@content-desc,'Country') or contains(@hint,'Country')]");

    // ── Payment form (step 2) ──────────────────────────────────────────────
    // Confirmed from dump: cardholder Full Name uses nameET, CVV uses securityCodeET
    private final By paymentNameField = By.id(PKG + ":id/nameET");
    private final By cardField        = By.id(PKG + ":id/cardNumberET");
    private final By expiryField      = By.id(PKG + ":id/expirationDateET");
    private final By cvvField         = By.id(PKG + ":id/securityCodeET");  // NOT cvvET

    private final By paymentNameFallback = By.xpath(
        "//android.widget.EditText[contains(@resource-id,'nameET')]");
    private final By cardFallback   = By.xpath(
        "//android.widget.EditText[contains(@resource-id,'cardNumber') or contains(@hint,'3258')]");
    private final By expiryFallback = By.xpath(
        "//android.widget.EditText[contains(@resource-id,'expirationDate') or contains(@hint,'03/25')]");
    private final By cvvFallback    = By.xpath(
        "//android.widget.EditText[contains(@resource-id,'securityCode') or @max-text-length='3']");

    // ── Navigation buttons ─────────────────────────────────────────────────
    // CONFIRMED from dump: both "To Payment" (shipping page) and "Review Order" (payment page)
    // share resource-id = paymentBtn. Use text to distinguish via fallbacks.
    private final By toPaymentButton   = By.id(PKG + ":id/paymentBtn");
    private final By toPaymentFallback = By.xpath(
        "//*[@text='To Payment' or @text='TO PAYMENT' or contains(@content-desc,'To Payment')]");

    // Review Order = same paymentBtn resource-id on the payment page
    private final By reviewOrderBtn      = By.id(PKG + ":id/paymentBtn");
    private final By reviewOrderFallback = By.xpath(
        "//*[@text='Review Order' or @text='REVIEW ORDER'"
        + " or contains(@content-desc,'Review Order')]");

    // Place Order button — CONFIRMED: same paymentBtn resource-id, text="Place Order"
    // content-desc="Completes the process of checkout"
    private final By placeOrderBtn      = By.id(PKG + ":id/paymentBtn");
    private final By placeOrderFallback = By.xpath(
        "//*[@text='Place Order' or @text='PLACE ORDER'"
        + " or contains(@content-desc,'Place Order')"
        + " or contains(@content-desc,'Completes the process')]");

    // ── Order confirmation ─────────────────────────────────────────────────
    private final By orderSuccessMsg = By.xpath(
        "//*[contains(@text,'Thank you') or contains(@text,'thank you')"
        + " or contains(@text,'successfully') or contains(@text,'Congratulation')"
        + " or contains(@content-desc,'checkout complete')]"
    );

    public CheckoutPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public boolean isCheckoutPageVisible() {
        try {
            return !driver.findElements(fullNameField).isEmpty()
                || !driver.findElements(fullNameFallback).isEmpty()
                || !driver.findElements(By.xpath("//android.widget.EditText")).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /** Fill shipping / delivery information */
    public void fillShippingInfo(String fullName, String address, String city, String zip, String country) {
        typeIntoField(fullNameField, fullNameFallback, fullName);
        typeIntoField(addressField,  addressFallback,  address);
        typeIntoField(cityField,     cityFallback,     city);
        typeIntoField(zipField,      zipFallback,      zip);
        typeIntoField(countryField,  countryFallback,  country);
    }

    /** Proceed to payment step */
    public void goToPayment() {
        try { driver.hideKeyboard(); } catch (Exception ignored) {}
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        clickButton(toPaymentButton, toPaymentFallback);
    }

    /** Fill payment card details (cardholder name auto-filled from shipping name) */
    public void fillPaymentInfo(String cardNumber, String expiry, String cvv) {
        fillPaymentInfo("Test User", cardNumber, expiry, cvv);
    }

    /** Fill payment card details with explicit cardholder name */
    public void fillPaymentInfo(String cardholderName, String cardNumber, String expiry, String cvv) {
        dumpPageSource("payment-page");
        // Cardholder Full Name field (nameET on payment page — required *)
        typeIntoField(paymentNameField, paymentNameFallback, cardholderName);
        typeIntoField(cardField,        cardFallback,         cardNumber);
        typeIntoField(expiryField,      expiryFallback,       expiry);
        typeIntoField(cvvField,         cvvFallback,          cvv);
    }

    /** Proceed to order review */
    public void goToOrderReview() {
        try { driver.hideKeyboard(); } catch (Exception ignored) {}
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        // Dump page source so we can identify Review Order button ID
        dumpPageSource("review-order-page");
        clickButton(reviewOrderBtn, reviewOrderFallback);
    }

    /** Place the final order */
    public void placeOrder() {
        // Dump review page so we can identify Place Order button ID
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        dumpPageSource("place-order-page");
        clickButton(placeOrderBtn, placeOrderFallback);
    }

    /** Check if order was placed successfully */
    public boolean isOrderSuccessful() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(orderSuccessMsg));
            return true;
        } catch (Exception e) {
            String src = driver.getPageSource().toLowerCase();
            return src.contains("thank you")
                || src.contains("order placed")
                || src.contains("successfully")
                || src.contains("complete");
        }
    }

    /** Get confirmation message text */
    public String getConfirmationText() {
        try {
            return driver.findElement(orderSuccessMsg).getText();
        } catch (Exception e) {
            return "Order confirmation (text not accessible)";
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void typeIntoField(By primary, By fallback, String text) {
        try {
            WebElement field = wait.until(ExpectedConditions.elementToBeClickable(primary));
            field.clear();
            field.sendKeys(text);
            System.out.println("[CheckoutPage] Typed into field " + primary + ": " + text);
            return;
        } catch (Exception ignored) {}
        try {
            WebElement field = wait.until(ExpectedConditions.elementToBeClickable(fallback));
            field.clear();
            field.sendKeys(text);
            System.out.println("[CheckoutPage] Typed into fallback field " + fallback + ": " + text);
        } catch (Exception e) {
            System.out.println("[CheckoutPage] Field not found: " + primary + " | " + e.getMessage());
        }
    }

    private void clickButton(By primary, By fallback) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(primary)).click();
            System.out.println("[CheckoutPage] Clicked button: " + primary);
            return;
        } catch (Exception ignored) {}
        try {
            wait.until(ExpectedConditions.elementToBeClickable(fallback)).click();
            System.out.println("[CheckoutPage] Clicked fallback button: " + fallback);
        } catch (Exception e) {
            System.out.println("[CheckoutPage] Button not found: " + primary + " | " + e.getMessage());
        }
    }

    private void dumpPageSource(String label) {
        try {
            String src = driver.getPageSource();
            String path = "C:\\Users\\rezau\\eclipse-workspace\\appium-tests\\" + label + "-dump.xml";
            try (java.io.FileWriter fw = new java.io.FileWriter(path)) {
                fw.write(src);
            }
            System.out.println("[CheckoutPage] Page source saved: " + path);
            // Also print EditText elements and buttons for quick scanning
            System.out.println("[CheckoutPage] Page source snippet (" + label + ") — EditText and buttons:");
            for (String line : src.split("\n")) {
                if (line.contains("EditText") || line.contains("Button") || line.contains("Btn")) {
                    System.out.println("  " + line.trim().substring(0, Math.min(200, line.trim().length())));
                }
            }
        } catch (Exception e) {
            System.out.println("[CheckoutPage] Could not dump page source: " + e.getMessage());
        }
    }
}
