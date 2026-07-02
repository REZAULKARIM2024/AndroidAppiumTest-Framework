package pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class TextScreenPage {

    private AndroidDriver driver;
    private final By editTextField = By.className("android.widget.EditText");

    public TextScreenPage(AndroidDriver driver) {
        this.driver = driver;
    }

    public void enterText(String text) {
        WebElement field = driver.findElement(editTextField);
        field.clear();
        field.sendKeys(text);
    }

    public String getEnteredText() {
        return driver.findElement(editTextField).getAttribute("text");
    }

    public boolean isEditTextPresent() {
        return !driver.findElements(editTextField).isEmpty();
    }
}
