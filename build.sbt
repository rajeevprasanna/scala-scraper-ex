name := "scala-scraper-ex"
version := "0.1"

scalaVersion := "2.12.4"

libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "2.1.0"
libraryDependencies += "com.github.etaty" %% "rediscala" % "1.8.0"
libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.3.11"
libraryDependencies += "commons-io" % "commons-io" % "2.6"
libraryDependencies += "io.spray" %%  "spray-json" % "1.3.3"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.9"
libraryDependencies += "org.typelevel" %% "cats" % "0.9.0"
libraryDependencies += "io.github.bonigarcia" % "webdrivermanager" % "2.1.0"
libraryDependencies += "org.seleniumhq.selenium" % "selenium-chrome-driver" % "3.11.0"


assemblyJarName in assembly := "scraper_16032018.jar"
mainClass in assembly := Some("Main")
