name := "First Project"

version := "1.0"

scalaVersion := "2.13.10"

libraryDependencies ++= Seq("org.apache.spark" % "spark-core_2.13" % "3.3.1")
libraryDependencies ++= Seq("org.apache.spark" % "spark-streaming_2.13" % "3.3.1")
libraryDependencies ++= Seq("org.apache.spark" % "spark-sql_2.13" % "3.3.1")

