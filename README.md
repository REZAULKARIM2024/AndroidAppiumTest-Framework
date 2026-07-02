# 📱 Android Appium Test Framework

A comprehensive **Android UI automation framework** built with **Appium 3**, **UiAutomator2**, **TestNG**, and **Java 17**.

Features two fully automated test suites:
- **MyDemo App Suite** — 75 tests across 12 categories (⭐ primary suite)
- **ApiDemos Suite** — core UI regression tests

> **MyDemo final result: 72 pass ✅ · 0 fail · 3 skip (by design)**

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Test runner | TestNG 7.10.2 |
| Automation | Appium 3.5.2 + UiAutomator2 |
| Driver client | Appium Java Client 9.5.0 |
| Selenium core | Selenium 4.32.0 |
| REST testing | REST Assured 5.4.0 |
| Reporting | ExtentReports 5.1.2 |
| Build tool | Maven 3 (Surefire 3.2.5) |

---

## Prerequisites

1. **Java 17** — `JAVA_HOME` must be set
2. **Maven 3** on PATH
3. **Appium 3** and UiAutomator2 driver:
   ```bash
   npm install -g appium
   appium driver install uiautomator2
   ```
4. **Android SDK** with `adb` on PATH
5. **Android Emulator** running at `emulator-5554` (API 31, Android 12)
6. **MyDemo APK** placed at `C:\Users\rezau\MyDemoApp.apk`
   - Download: https://github.com/saucelabs/my-demo-app-android/releases

---

## Project Structure

```
appium-tests/
├── src/test/java/
│   ├── base/
│   │   ├── MyDemoBaseTest.java         # MyDemo: Appium session setup/teardown
│   │   └── BaseTest.java               # ApiDemos: Appium session setup/teardown
│   │
│   ├── pages/
│   │   ├── mydemo/                     # MyDemo Page Object Model
│   │   │   ├── LoginPage.java
│   │   │   ├── ProductsPage.java
│   │   │   ├── CartPage.java
│   │   │   └── CheckoutPage.java
│   │   ├── MainScreenPage.java         # ApiDemos pages
│   │   └── TextScreenPage.java
│   │
│   ├── tests/
│   │   ├── MyDemoSmokeTests.java       # [SMOKE]     6 tests
│   │   ├── MyDemoE2ETests.java         # [E2E]       9 tests
│   │   ├── MyDemoAppLifecycleTests.java# [LIFECYCLE] 6 tests
│   │   ├── MyDemoNavigationTests.java  # [NAV]       7 tests
│   │   ├── MyDemoDeviceBehaviorTests.java# [DEVICE]  5 tests
│   │   ├── MyDemoNegativeTests.java    # [NEGATIVE]  8 tests
│   │   ├── MyDemoPerformanceTests.java # [PERF]      5 tests
│   │   ├── MyDemoSecurityTests.java    # [SECURITY]  5 tests
│   │   ├── MyDemoAccessibilityTests.java# [A11Y]     6 tests
│   │   ├── MyDemoDataDrivenTests.java  # [DATA]      9 tests
│   │   ├── MyDemoInstallTests.java     # [INSTALL]   4 tests
│   │   ├── MyDemoInterruptTests.java   # [INTERRUPT] 5 tests
│   │   └── CombinedApiDemosTests.java  # ApiDemos core tests
│   │
│   ├── listeners/
│   │   └── ScreenshotListener.java     # Auto-screenshot on failure
│   └── utils/
│       └── ExtentReportManager.java    # HTML report singleton
│
├── testng-mydemo-all.xml               # MyDemo full suite (75 tests)
├── testng.xml                          # ApiDemos suite
├── pom.xml
└── README.md
```

---

## Running the MyDemo Suite

### 1. Start the Appium server

```bash
npx appium
```

### 2. Verify the emulator is online

```bash
adb devices
# Expected: emulator-5554   device
```

### 3. Run all 75 tests

```bash
mvn clean test -DsuiteXmlFile=testng-mydemo-all.xml
```

### 4. View the HTML report

```
test-output/ExtentReport.html
```

---

## MyDemo Test Categories

| # | Class | Tag | Tests | What it covers |
|---|---|---|---|---|
| 1 | `MyDemoSmokeTests` | `[SMOKE]` | 6 | App launches, login screen loads, catalog visible |
| 2 | `MyDemoE2ETests` | `[E2E]` | 9 | Full login → add to cart → checkout flows |
| 3 | `MyDemoAppLifecycleTests` | `[LIFECYCLE]` | 6 | Background/foreground, kill & relaunch |
| 4 | `MyDemoNavigationTests` | `[NAV]` | 7 | Hamburger menu, product detail, cart, checkout steps |
| 5 | `MyDemoDeviceBehaviorTests` | `[DEVICE]` | 5 | Back button, home button, screen orientation (2 skip) |
| 6 | `MyDemoNegativeTests` | `[NEGATIVE]` | 8 | Invalid credentials, empty fields, boundary inputs |
| 7 | `MyDemoPerformanceTests` | `[PERF]` | 5 | Page load SLA checks (login ≤5s, cart open ≤3s) |
| 8 | `MyDemoSecurityTests` | `[SECURITY]` | 5 | SQL injection, XSS in input fields, session handling |
| 9 | `MyDemoAccessibilityTests` | `[A11Y]` | 6 | Content descriptions, focusability, label presence |
| 10 | `MyDemoDataDrivenTests` | `[DATA]` | 9 | Multiple credential sets, addresses, card numbers |
| 11 | `MyDemoInstallTests` | `[INSTALL]` | 4 | Fresh install, upgrade, uninstall verification |
| 12 | `MyDemoInterruptTests` | `[INTERRUPT]` | 5 | Incoming call, SMS, WiFi toggle during active session |

**Total: 75 tests — 72 pass · 0 fail · 3 skip**

---

## Design Decisions

### One Appium session per test class
`@BeforeClass` / `@AfterClass` creates and destroys one driver session per class — significantly faster than spinning up a session per test method.

### Fast app reset via ADB
Instead of `fullReset` (slow APK reinstall), `MyDemoBaseTest` runs:
```bash
adb -s emulator-5554 shell pm clear com.saucelabs.mydemoapp.android
```
Same clean-state effect, much faster session startup.

### Session health check in `@BeforeMethod`
Every test class guards against a dead Appium session so cascade failures become clean skips:
```java
@BeforeMethod
public void checkSession() {
    try {
        driver.getSessionId();
        driver.findElements(By.id(PKG + ":id/menuIV")); // real Appium round-trip
    } catch (Exception e) {
        throw new SkipException("Session dead — skipping: " + e.getMessage());
    }
}
```

### Navigation drawer — never press BACK
On this app + emulator combination, pressing `navigate().back()` from an open navigation drawer exits the app to the Android home screen instead of closing the drawer. All drawer code closes by tapping the hamburger icon a second time.

### Logout → `activateApp()` rescue
After logout, the app navigates to the Android home screen. `LoginPage` automatically calls `driver.activateApp()` before any further navigation to bring the app back.

### Avoid `driver.currentActivity()`
This method internally calls `adb dumpsys window displays`, which hangs 20+ seconds on a loaded emulator. Session health is checked using `driver.findElements()` instead — a real Appium round-trip with no ADB dependency.

---

## Test Reports & Screenshots

| Artifact | Location |
|---|---|
| HTML Report | `test-output/ExtentReport.html` |
| Failure Screenshots | `test-output/screenshots/<testMethodName>.png` |

Screenshots are captured automatically by `ScreenshotListener` on every test failure.

---

## Test Credentials (MyDemo App)

| Field | Value |
|---|---|
| Username | `bob@example.com` |
| Password | `10203040` |

---

## Known Limitations

| Limitation | Detail |
|---|---|
| **3 rotation tests skip** | `lifecycle_ScreenRotationDoesNotCrash`, `device_CatalogLandscapeRotation`, `device_CheckoutFormLandscapeRotation` throw `SkipException` when `driver.rotate()` is not supported. This is by design. |
| **Emulator CPU load after interrupt tests** | SMS / GSM / WiFi ADB simulations leave elevated CPU for ~30–60 seconds. Tests in subsequent classes may need slightly longer waits. |
| **APK path is hardcoded** | The local APK path is `C:\Users\rezau\MyDemoApp.apk`. Update `MyDemoBaseTest.setupLocal()` if your path differs. |

---

## SauceLabs Cloud (Optional)

Suite files for SauceLabs are **excluded from Git** via `.gitignore` because they contain credentials:

```
testng-saucelabs.xml
testng-saucelabs-crossdevice.xml
testng-saucelabs-mydemo.xml
```

To run on SauceLabs, create your own copy based on `testng-mydemo-all.xml` and add your credentials as parameters. **Never commit credential files.**

Credentials can also be passed as environment variables:

```bash
export SAUCE_USERNAME=your-username
export SAUCE_ACCESS_KEY=your-key
```

---

## Run History (MyDemo Suite)

| Run | Pass | Fail | Skip | Notes |
|---|---|---|---|---|
| Run 5 | 61 | 10 | 4 | Initial baseline |
| Run 6 | 69 | 3 | 3 | Navigation drawer BACK bug fixed |
| Run 7 | 71 | 1 | 3 | Logout → home screen rescue added |
| Run 8 | 70 | 2 | 3 | Emulator flakiness regression |
| **Run 9** | **72** | **0** | **3** | **✅ Target achieved** |

---

## Running the ApiDemos Suite

```bash
mvn clean test -DsuiteXmlFile=testng.xml
```

Targets the [ApiDemos](https://github.com/appium/android-apidemos) reference app. APK should be at `C:\Users\rezau\ApiDemos-debug.apk`.

---

## License

This project is for personal learning and portfolio purposes.  
The SauceLabs My Demo App is owned by [Sauce Labs, Inc](https://saucelabs.com/).
