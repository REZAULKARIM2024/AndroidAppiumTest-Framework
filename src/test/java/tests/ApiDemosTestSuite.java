package tests;

import base.BaseTest;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import pages.AccessibilityPage;
import pages.AnimationPage;
import pages.MainScreenPage;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import utils.ExtentReportManager;

public class ApiDemosTestSuite extends BaseTest {

    ExtentTest test;

    @BeforeMethod
    public void setupReport(java.lang.reflect.Method method) {
        test = ExtentReportManager.createTest(method.getName());
    }

    @Test(priority = 1)
    public void testMainScreenHasMenuItems() {
        MainScreenPage mainPage = new MainScreenPage(driver);
        int count = mainPage.getMenuItemCount();

        test.log(Status.INFO, "Total menu items found: " + count);
        Assert.assertTrue(count > 0, "Main screen should have menu items");
        test.log(Status.PASS, "Main screen has menu items");
    }

    @Test(priority = 2)
    public void testAccessibilityMenuIsVisible() {
        MainScreenPage mainPage = new MainScreenPage(driver);
        boolean visible = mainPage.isMenuItemVisible("Accessibility");

        Assert.assertTrue(visible, "Accessibility menu item should be visible");
        test.log(Status.PASS, "Accessibility menu is visible");
    }

    @Test(priority = 3, dependsOnMethods = "testAccessibilityMenuIsVisible")
    public void testNavigateToAccessibilityScreen() {
        MainScreenPage mainPage = new MainScreenPage(driver);
        mainPage.clickMenuItemByText("Accessibility");

        AccessibilityPage accessibilityPage = new AccessibilityPage(driver);
        Assert.assertTrue(accessibilityPage.isAccessibilityScreenDisplayed(),
                "Should navigate to Accessibility screen");

        test.log(Status.INFO, "Sub-menu items: " + accessibilityPage.getSubMenuItemCount());
        test.log(Status.PASS, "Navigated to Accessibility screen successfully");

        accessibilityPage.goBack();
    }

    @Test(priority = 4)
    public void testNavigateToAnimationScreen() {
        MainScreenPage mainPage = new MainScreenPage(driver);
        mainPage.clickMenuItemByText("Animation");

        AnimationPage animationPage = new AnimationPage(driver);
        Assert.assertTrue(animationPage.isAnimationScreenDisplayed(),
                "Should navigate to Animation screen");

        test.log(Status.INFO, "Sub-menu items: " + animationPage.getSubMenuItemCount());
        test.log(Status.PASS, "Navigated to Animation screen successfully");

        animationPage.goBack();
    }
}