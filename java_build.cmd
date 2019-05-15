@echo off

setlocal

rem Uncomment  this "set" line and change it to the correct path if you are using a JDK other than the default one installed on your system.
rem set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_162

ant %*

endlocal
