package pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;

public class AnimationPage {

    private AndroidDriver driver;

    private final By screenItems = By.id("android:id/text1");

    public AnimationPage(AndroidDriver driver) {
        this.driver = driver;
    }

    public boolean isAnimationScreenDisplayed() {
        return driver.getPageSource().contains("Animation");
    }

    public int getSubMenuItemCount() {
        return driver.findElements(screenItems).size();
    }

    public void goBack() {
        driver.navigate().back();
    }
}