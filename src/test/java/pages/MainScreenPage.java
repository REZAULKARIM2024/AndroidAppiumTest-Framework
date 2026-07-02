package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.util.List;

public class MainScreenPage {

    private AndroidDriver driver;
    private final By menuItems = By.id("android:id/text1");

    public MainScreenPage(AndroidDriver driver) {
        this.driver = driver;
    }

    public int getMenuItemCount() {
        return driver.findElements(menuItems).size();
    }

    public List<WebElement> getAllMenuItems() {
        return driver.findElements(menuItems);
    }

    public void clickMenuItemByText(String text) {
        driver.findElement(AppiumBy.androidUIAutomator(
                "new UiSelector().text(\"" + text + "\")")).click();
    }

    public void clickMenuItemContainingText(String partialText) {
        driver.findElement(AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\"" + partialText + "\")")).click();
    }

    public boolean isMenuItemVisible(String text) {
        return driver.getPageSource().contains(text);
    }

    public boolean navigateToScreenWithEditText() {
        List<WebElement> items = getAllMenuItems();
        int total = items.size();

        for (int i = 0; i < total; i++) {
            List<WebElement> freshItems = getAllMenuItems();
            if (i >= freshItems.size()) break;

            freshItems.get(i).click();
            // Cloud-এ screen transition বেশি সময় নেয় — 2500ms
            try { Thread.sleep(2500); } catch (InterruptedException ignored) {}

            // EditText, AutoCompleteTextView, MultiAutoCompleteTextView সব match করে
            boolean hasEditText = !driver.findElements(
                    By.xpath("//*[contains(@class, 'EditText')]")).isEmpty();
            if (hasEditText) {
                return true;
            }

            driver.navigate().back();
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
        return false;
    }

    public void scrollToAndClick(String text) {
        WebElement el = driver.findElement(AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true).instance(0))"
                        + ".setMaxSearchSwipes(10)"
                        + ".scrollIntoView(new UiSelector().textContains(\"" + text + "\"))"));
        el.click();
    }

    public String getCurrentAppPackage() {
        return driver.getCurrentPackage();
    }

    public void ensureOnMainScreen(AndroidDriver driver) {
        int attempts = 0;
        while (getMenuItemCount() != 12 && attempts < 5) {
            driver.navigate().back();
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            attempts++;
        }
        if (getMenuItemCount() != 12) {
            driver.terminateApp("io.appium.android.apis");
            driver.activateApp("io.appium.android.apis");
            // App পুরোপুরি load হওয়া পর্যন্ত explicit wait (max 20s)
            try {
                new WebDriverWait(driver, Duration.ofSeconds(20))
                        .until(d -> d.findElements(By.id("android:id/text1")).size() >= 10);
            } catch (Exception ignored) {}
        }
    }
}
