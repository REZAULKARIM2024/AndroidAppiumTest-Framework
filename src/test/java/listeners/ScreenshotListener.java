package listeners;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.OutputType;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.lang.reflect.Field;

public class ScreenshotListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        try {
            Object testInstance = result.getInstance();
            Field driverField = testInstance.getClass().getSuperclass().getDeclaredField("driver");
            driverField.setAccessible(true);
            AndroidDriver driver = (AndroidDriver) driverField.get(testInstance);

            if (driver != null) {
                File srcFile = driver.getScreenshotAs(OutputType.FILE);
                String folderPath = System.getProperty("user.dir") + "/test-output/screenshots/";
                new File(folderPath).mkdirs();
                String fileName = result.getMethod().getMethodName() + ".png";
                Files.copy(srcFile.toPath(), Paths.get(folderPath + fileName),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Screenshot saved: " + folderPath + fileName);
            }
        } catch (Exception e) {
            System.out.println("Could not capture screenshot: " + e.getMessage());
        }
    }
}