mvn package -DskipTests -q
start "" /b cmd /c "timeout /t 3 >nul && start http://localhost:8080"
java -jar target\rogue-aws.jar

