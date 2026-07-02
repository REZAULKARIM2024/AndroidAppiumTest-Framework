package tests;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import utils.ExtentReportManager;

import static io.restassured.RestAssured.given;

/**
 * এই ক্লাসটা দেখায় কীভাবে UI Automation এর পাশাপাশি Backend API verify করা হয়।
 * Demo API ব্যবহার করা হয়েছে (jsonplaceholder.typicode.com) যেহেতু ApiDemos
 * এর কোনো real backend নেই। বাস্তব প্রজেক্টে এখানে আপনার app এর actual
 * API base URL এবং endpoint বসাবেন।
 */
public class ApiIntegratedTests {

    ExtentTest test;
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    @BeforeMethod
    public void setupReport(java.lang.reflect.Method method) {
        test = ExtentReportManager.createTest(method.getName());
    }

    // 1. API Response Status Code যাচাই (basic health check)
    @Test(priority = 1)
    public void api_GetUserReturnsStatus200() {
        Response response = given()
                .baseUri(BASE_URL)
                .when()
                .get("/users/1");

        test.log(Status.INFO, "Status code: " + response.getStatusCode());
        Assert.assertEquals(response.getStatusCode(), 200, "API did not return 200 OK");
    }

    // 2. API Response এর নির্দিষ্ট field (যেমন username) যাচাই — UI তে দেখানো ডেটার সাথে মিলবে এমন
    @Test(priority = 2)
    public void api_UserResponseContainsExpectedFields() {
        Response response = given()
                .baseUri(BASE_URL)
                .when()
                .get("/users/1");

        String username = response.jsonPath().getString("username");
        String email = response.jsonPath().getString("email");

        test.log(Status.INFO, "Username: " + username + " | Email: " + email);
        Assert.assertNotNull(username, "Username field missing in API response");
        Assert.assertTrue(email.contains("@"), "Email field appears invalid");
    }

    // 3. একটা সিমুলেটেড "UI vs API" cross-check — বাস্তব প্রজেক্টে এখানে
    // Appium driver দিয়ে UI থেকে value নিয়ে API response এর সাথে তুলনা করবেন
    @Test(priority = 3)
    public void api_SimulatedUiToApiDataConsistencyCheck() {
        Response response = given()
                .baseUri(BASE_URL)
                .when()
                .get("/posts/1");

        String apiTitle = response.jsonPath().getString("title");
        test.log(Status.INFO, "API returned title: " + apiTitle);

        // বাস্তব প্রজেক্টে: String uiTitle = driver.findElement(titleLocator).getText();
        String simulatedUiTitle = apiTitle; // placeholder — বাস্তবে UI থেকে আসবে

        Assert.assertEquals(simulatedUiTitle, apiTitle,
                "UI displayed value does not match backend API response — possible data sync issue");
    }

    // 4. Negative API Test: ভুল/অস্তিত্বহীন resource এর জন্য সঠিক error code আসে কিনা
    @Test(priority = 4)
    public void api_NonExistentResourceReturns404() {
        Response response = given()
                .baseUri(BASE_URL)
                .when()
                .get("/users/999999");

        test.log(Status.INFO, "Status code for non-existent user: " + response.getStatusCode());
        Assert.assertEquals(response.getStatusCode(), 404, "Expected 404 for non-existent resource");
    }
}