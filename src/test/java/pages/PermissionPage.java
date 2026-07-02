package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;

public class PermissionPage {

    private AndroidDriver driver;

    public PermissionPage(AndroidDriver driver) {
        this.driver = driver;
    }

    public boolean isPermissionDialogVisible() {
        return !driver.findElements(AppiumBy.id("com.android.permissioncontroller:id/permission_message")).isEmpty()
                || driver.getPageSource().toLowerCase().contains("allow")
                && driver.getPageSource().toLowerCase().contains("permission");
    }

    public void grantPermissionIfPrompted() {
        try {
            driver.findElement(AppiumBy.id("com.android.permissioncontroller:id/permission_allow_button")).click();
        } catch (Exception ignored) {
            // permission dialog না থাকলে silently skip
        }
    }

    public void denyPermissionIfPrompted() {
        try {
            driver.findElement(AppiumBy.id("com.android.permissioncontroller:id/permission_deny_button")).click();
        } catch (Exception ignored) {
        }
    }
}