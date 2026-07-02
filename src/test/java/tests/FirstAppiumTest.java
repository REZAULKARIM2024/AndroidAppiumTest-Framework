package tests;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.net.URL;
import java.time.Duration;
import java.util.List;

public class FirstAppiumTest {

    public static void main(String[] args) throws Exception {

        UiAutomator2Options options = new UiAutomator2Options();
        options.setDeviceName("emulator-5554");
        options.setPlatformName("Android");
        options.setAutomationName("UiAutomator2");
        options.setApp("C:\\Users\\rezau\\ApiDemos-debug.apk");
        options.setNewCommandTimeout(Duration.ofSeconds(120));

        AndroidDriver driver = new AndroidDriver(
                new URL("http://127.0.0.1:4723"), options);

        try {
            Thread.sleep(3000);

            // প্রথমে কতগুলো item আছে সেটা বের করুন
            List<WebElement> items = driver.findElements(By.id("android:id/text1"));
            int totalItems = items.size();
            System.out.println("Total items found: " + totalItems);

            // প্রতিবার fresh ভাবে list খুঁজে text বের করুন (stale এড়াতে)
            for (int i = 0; i < totalItems; i++) {
                List<WebElement> freshItems = driver.findElements(By.id("android:id/text1"));
                if (i < freshItems.size()) {
                    System.out.println(freshItems.get(i).getText());
                }
            }

            // প্রথম আইটেমে ক্লিক করুন (Accessibility)
            List<WebElement> clickItems = driver.findElements(By.id("android:id/text1"));
            if (!clickItems.isEmpty()) {
                String name = clickItems.get(0).getText();
                clickItems.get(0).click();
                System.out.println("Clicked on: " + name);
            }

            Thread.sleep(3000);

        } finally {
            driver.quit();
        }
    }
}