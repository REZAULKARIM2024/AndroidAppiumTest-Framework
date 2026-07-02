package pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

import java.util.List;

public class AccessibilityPage {

    private AndroidDriver driver;

    private final By screenItems = By.id("android:id/text1");

    public AccessibilityPage(AndroidDriver driver) {
        this.driver = driver;
    }

    public boolean isAccessibilityScreenDisplayed() {
        return driver.getPageSource().contains("Accessibility");
    }

    public int getSubMenuItemCount() {
        return driver.findElements(screenItems).size();
    }

    public void goBack() {
        driver.navigate().back();
    }
}