@echo off
cd /d C:\Users\rezau\eclipse-workspace\appium-tests
echo ════════════════════════════════════════════════════
echo  MyDemoApp — Full Test Suite (All Categories)
echo ════════════════════════════════════════════════════
echo  Pre-requisites:
echo  1. Emulator running (AVD Manager)
echo  2. Appium server running (npx appium)
echo  3. APK present: C:\Users\rezau\MyDemoApp.apk
echo ════════════════════════════════════════════════════
echo.
call mvn clean test -DsuiteXmlFile=testng-mydemo-all.xml > test-output\run-all-log.txt 2>&1
echo Done. Output: test-output\run-all-log.txt
pause
