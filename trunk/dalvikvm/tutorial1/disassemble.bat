@echo off

if "%1" == "" goto ERROR

adb push "%1" /data
adb shell "dexdump -d /data/%~nx1 > /data/%~n1.txt"
adb pull /data/%~n1.txt "%~dpn1.txt"
goto END

:ERROR
echo Please, pass a dex file.

:END

