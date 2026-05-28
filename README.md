# rogueweb
A very rough initial AI port of the 1980-1983, 1985, 1999 Unix game Rogue from C to Java
Original Authors: Michael Toy, Ken Arnold, and Glenn Wichman (1980-1983, 1985, 1999)
Based on the excellent archive by https://github.com/Davidslv/ , at: https://github.com/Davidslv/rogue
I own no copyright myself, all belong to the original authors above.

This should:
- Run locally on MS Windows 10/11 with Java installed
- Run on AWS Elastic Beanstalk with the required permissions

How it came about:
- I was always a long time fan of the Hack, Rogue and Nethack games, and was looking to for an online version
- Also it appealed to me, to have it ported to either Java, Python, or another more modern accessible programming language
- Not being a programmer (anymore) , the recent appearance of LLM's gave me a chance to play around with the code and make a rough prototype
- This version was produced by Claude, on the basis of the rogue archive above, with changes by me where I wanted to make it more OOP-based
- I see this purely as a prototype which I will abandon in time, as I find the time and hopefully skills to build a more original version myself.

How to run locally:
- Install Java (17+)
- Unzip the package
- Commandline, Powershell or cmd.exe:
- - Build with: mvn package -DskipTests -q
- - Start a webbrowser pointed at http://localhost:8080 : start "" /b cmd /c "timeout /t 3 >nul && start http://localhost:8080"
- - Start app with: java -jar target\rogue-aws.jar
 
