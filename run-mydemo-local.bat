@echo off
cd /d C:\Users\rezau\eclipse-workspace\appium-tests
call mvn clean test -DsuiteXmlFile=testng-mydemo-local.xml > test-output\run-log.txt 2>&1
echo Done. Output in test-output\run-log.txt
pause
