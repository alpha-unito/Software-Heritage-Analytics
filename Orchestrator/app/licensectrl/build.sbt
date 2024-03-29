name := "licensectrl"

version := "0.1"

scalaVersion := "2.13.10"

//libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.1.1" % "provided"

//libraryDependencies += "com.github.mrpowers" %% "spark-daria" % "0.38.2"
//libraryDependencies += "com.github.mrpowers" %% "spark-fast-tests" % "0.21.3" % "test"
//libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

libraryDependencies ++= Seq("org.apache.spark" % "spark-core_2.13" % "3.3.1")
libraryDependencies ++= Seq("org.apache.spark" % "spark-streaming_2.13" % "3.3.1")
libraryDependencies ++= Seq("org.apache.spark" % "spark-sql_2.13" % "3.3.1")
//libraryDependencies ++= Seq("org.json4s" %% "json4s-jackson" % "4.1.0-M3")
//libraryDependencies ++= Seq("org.apache.spark" % "spark-sql_2.13" % "3.7.0-M11")
libraryDependencies ++= Seq("org.json4s" %% "json4s-native" % "3.6.11")

// test suite settings
fork in Test := true
javaOptions ++= Seq("-Xms512M", "-Xmx2048M", "-XX:MaxPermSize=2048M", "-XX:+CMSClassUnloadingEnabled")
// Show runtime of tests
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")

// JAR file settings

// don't include Scala in the JAR file
//assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

// Add the JAR file naming conventions described here: https://github.com/MrPowers/spark-style-guide#jar-files
// You can add the JAR file naming conventions by running the shell script
