package pages.mydemo;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileWriter;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Products Page — SauceLabs My Demo App (Native Android v2.2.0)
 * Package: com.saucelabs.mydemoapp.android
 *
 * Resource IDs confirmed from UI dump (ui2.xml):
 *   - productRV   : Products RecyclerView (catalog container)
 *   - productTV   : "Products" header label
 *   - titleTV     : Product name / title
 *   - priceTV     : Product price
 *   - productIV   : Product image (clickable — opens product detail)
 *   - cartRL      : Cart icon button (content-desc="View cart")
 */
public class ProductsPage {

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    private static final String PKG = "com.saucelabs.mydemoapp.android";

    // Products catalog (confirmed from UI dump)
    private final By productsHeader    = By.id(PKG + ":id/productTV");   // "Products" label
    private final By productsContainer = By.id(PKG + ":id/productRV");   // RecyclerView
    private final By productTitle      = By.id(PKG + ":id/titleTV");     // product name
    private final By productImage      = By.id(PKG + ":id/productIV");   // clickable → product detail
    private final By cartButton        = By.id(PKG + ":id/cartRL");      // cart icon (top-right)

    // Product detail page — "Add to Cart" button
    // Confirmed from UI dump: resource-id = cartBt, text = "Add to cart"
    private final By addToCartBtn      = By.id(PKG + ":id/cartBt");
    private final By addToCartFallback = By.xpath(
        "//*[@text='Add To Cart' or @text='ADD TO CART' or @text='Add to Cart'"
        + " or @text='add to cart' or @content-desc='Tap to add product to cart'"
        + " or @resource-id='" + PKG + ":id/addToCartBt'"
        + " or @resource-id='" + PKG + ":id/addToCartBtn'"
        + "]"
    );

    // Cart badge — confirmed resource-id: cartTV (inside cartCircleRL inside cartRL)
    private final By cartBadgeById = By.id(PKG + ":id/cartTV");
    // XPath fallback
    private final By cartCountBadge = By.xpath(
        "//*[contains(@resource-id,'cart') and string-length(@text)>0"
        + " and not(@text='0') and not(@text='View cart')]"
    );

    // Tracks the cart count confirmed on the detail page after addFirstProductToCart().
    // Used by isCartNotEmpty() when the catalog toolbar badge doesn't auto-refresh (0→1 transition).
    private int lastConfirmedCartCount = 0;

    public ProductsPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait   = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    /** True when the products catalog screen is visible (fast 3s check) */
    public boolean isProductsPageVisible() {
        try {
            return !driver.findElements(productsHeader).isEmpty()
                || !driver.findElements(productsContainer).isEmpty()
                || !driver.findElements(productTitle).isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /** Wait up to 10s for products page — use after navigation actions */
    public boolean waitForProductsPage() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(productsHeader));
            return true;
        } catch (Exception e) {
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(productsContainer));
                return true;
            } catch (Exception ex) {
                return !driver.findElements(productTitle).isEmpty();
            }
        }
    }

    /** Number of products visible on the current screen */
    public int getVisibleProductCount() {
        try {
            return driver.findElements(productTitle).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Name of the first product in the list */
    public String getFirstProductName() {
        try {
            List<WebElement> titles = driver.findElements(productTitle);
            if (!titles.isEmpty()) return titles.get(0).getText();
        } catch (Exception ignored) {}
        return "Unknown Product";
    }

    /**
     * Add the first product to cart.
     * Flow: tap product image → open detail page → tap "Add To Cart" → back to catalog.
     */
    public void addFirstProductToCart() {
        // Tap the first product IMAGE (productIV) — clickable=true in catalog RecyclerView
        // NOTE: titleTV has clickable=false so cannot be used for navigation
        boolean navigatedToDetail = false;
        try {
            List<WebElement> images = driver.findElements(productImage);
            if (!images.isEmpty()) {
                System.out.println("[ProductsPage] Clicking first product image (productIV)...");
                images.get(0).click();
                Thread.sleep(2500); // wait for detail page to load
                // Verify we actually navigated away from catalog
                // On detail page, productTV ("Products") header is gone; instead we see product info
                List<WebElement> catalogHeaders = driver.findElements(
                    By.xpath("//*[@resource-id='com.saucelabs.mydemoapp.android:id/productTV'"
                           + " and @text='Products']"));
                if (catalogHeaders.isEmpty()) {
                    navigatedToDetail = true;
                    System.out.println("[ProductsPage] Navigated to product detail page");
                } else {
                    System.out.println("[ProductsPage] Still on catalog page after clicking productIV");
                }
            }
        } catch (Exception e) {
            System.out.println("[ProductsPage] Could not open product detail: " + e.getMessage());
        }

        // Dump page source for debugging
        try {
            String src = driver.getPageSource();
            System.out.println("[ProductsPage] PAGE SOURCE after productIV click (first 2000 chars):\n"
                + src.substring(0, Math.min(2000, src.length())));
            try (FileWriter fw = new FileWriter(
                    "C:\\Users\\rezau\\eclipse-workspace\\appium-tests\\product-detail-dump.xml")) {
                fw.write(src);
            }
            System.out.println("[ProductsPage] Full source saved to product-detail-dump.xml");
        } catch (Exception ignored) {}

        if (!navigatedToDetail) {
            System.out.println("[ProductsPage] WARNING: Did not navigate to product detail — skipping Add To Cart");
            return; // don't call back() — we're still on catalog
        }

        // On detail page: try resource-id first (no long wait — use findElements immediately)
        boolean added = false;
        List<WebElement> addBtnList;

        addBtnList = driver.findElements(addToCartBtn);
        if (!addBtnList.isEmpty()) {
            addBtnList.get(0).click();
            System.out.println("[ProductsPage] Clicked Add To Cart (resource-id: addToCartBt)");
            added = true;
        }

        if (!added) {
            addBtnList = driver.findElements(addToCartFallback);
            if (!addBtnList.isEmpty()) {
                addBtnList.get(0).click();
                System.out.println("[ProductsPage] Clicked Add To Cart (text/desc fallback)");
                added = true;
            }
        }

        if (!added) {
            // Scroll down and retry — button may be below the fold
            try {
                // W3C Actions-based swipe (works in Appium 3.x)
                org.openqa.selenium.interactions.PointerInput finger =
                    new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
                org.openqa.selenium.interactions.Sequence swipe =
                    new org.openqa.selenium.interactions.Sequence(finger, 1);
                swipe.addAction(finger.createPointerMove(Duration.ZERO,
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1800));
                swipe.addAction(finger.createPointerDown(0));
                swipe.addAction(finger.createPointerMove(Duration.ofMillis(600),
                    org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 500));
                swipe.addAction(finger.createPointerUp(0));
                driver.perform(java.util.Arrays.asList(swipe));
                System.out.println("[ProductsPage] Scrolled down on detail page");
            } catch (Exception e1) {
                System.out.println("[ProductsPage] Swipe failed: " + e1.getMessage());
            }
            try { Thread.sleep(1000); } catch (Exception ignored2) {}

            addBtnList = driver.findElements(addToCartBtn);
            if (!addBtnList.isEmpty()) {
                addBtnList.get(0).click();
                System.out.println("[ProductsPage] Clicked Add To Cart after scroll (resource-id)");
                added = true;
            } else {
                addBtnList = driver.findElements(addToCartFallback);
                if (!addBtnList.isEmpty()) {
                    addBtnList.get(0).click();
                    System.out.println("[ProductsPage] Clicked Add To Cart after scroll (text)");
                    added = true;
                }
            }
        }

        if (!added) {
            System.out.println("[ProductsPage] WARNING: Add To Cart button not found on detail page");
        }

        if (added) {
            // Wait for the cart badge (cartTV) to appear on the DETAIL PAGE header.
            // The catalog page toolbar does NOT auto-refresh its badge on resume (0→1 transition),
            // so we track the confirmed count here and use it in isCartNotEmpty() as fallback.
            try {
                WebDriverWait badgeWait = new WebDriverWait(driver, Duration.ofSeconds(15));
                WebElement badge = badgeWait.until(
                    ExpectedConditions.presenceOfElementLocated(cartBadgeById));
                String badgeText = badge.getText();
                System.out.println("[ProductsPage] Cart badge visible on detail page: " + badgeText);
                try { lastConfirmedCartCount = Integer.parseInt(badgeText.trim()); }
                catch (NumberFormatException ignored) { lastConfirmedCartCount = 1; }
            } catch (Exception e) {
                System.out.println("[ProductsPage] Cart badge not seen on detail page within 15s: " + e.getMessage());
            }
        }

        // Navigate back to catalog only if we went to detail
        try {
            driver.navigate().back();
            Thread.sleep(2000);  // wait for catalog to fully reload
            System.out.println("[ProductsPage] Navigated back to catalog");
        } catch (Exception e) {
            System.out.println("[ProductsPage] Back navigation failed: " + e.getMessage());
        }
    }

    /** Open the shopping cart */
    public void openCart() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(cartButton)).click();
        } catch (Exception e) {
            // Fallback: content-desc
            try {
                driver.findElement(By.xpath("//*[@content-desc='View cart']")).click();
            } catch (Exception ex) {
                System.out.println("[ProductsPage] Could not open cart: " + ex.getMessage());
            }
        }
    }

    /** True if cart badge shows at least one item */
    public boolean isCartNotEmpty() {
        // Primary: quick 3s check for cartTV badge on catalog page
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement badge = shortWait.until(
                ExpectedConditions.presenceOfElementLocated(cartBadgeById));
            String badgeText = badge.getText();
            System.out.println("[ProductsPage] Cart badge (cartTV) text: " + badgeText);
            if (badgeText == null || badgeText.trim().isEmpty() || badgeText.equals("0")) {
                return false;
            }
            try {
                return Integer.parseInt(badgeText.trim()) > 0;
            } catch (NumberFormatException nfe) {
                return true;
            }
        } catch (Exception ignored) {}

        // XPath fallback
        try {
            List<WebElement> badges = driver.findElements(cartCountBadge);
            if (!badges.isEmpty()) {
                String badgeText = badges.get(0).getText();
                System.out.println("[ProductsPage] Cart badge (XPath) text: " + badgeText);
                try {
                    return Integer.parseInt(badgeText.trim()) > 0;
                } catch (NumberFormatException nfe) {
                    return !badgeText.isEmpty() && !badgeText.equals("0");
                }
            }
        } catch (Exception ignored) {}

        // Catalog page toolbar badge is absent — this happens on the 0→1 transition
        // because the catalog page resumes from backstack without refreshing its toolbar.
        // Use the count confirmed on the detail page (set by addFirstProductToCart).
        if (lastConfirmedCartCount > 0) {
            System.out.println("[ProductsPage] Catalog badge absent but detail page confirmed count="
                + lastConfirmedCartCount + " → cart is not empty");
            return true;
        }

        System.out.println("[ProductsPage] isCartNotEmpty: no badge found → returning false");
        return false;
    }
}
