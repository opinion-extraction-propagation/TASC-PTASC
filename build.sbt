import AssemblyKeys._  

assemblySettings

name := "MultiClassSVM"

version := "1.0"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
        "org.apache.spark" %% "spark-core" % "1.1.0" % "provided",
        "org.apache.spark" %% "spark-mllib" % "1.1.0" % "provided",
	"com.github.scopt" %% "scopt" % "3.2.0",
		"jaxen" % "jaxen" % "1.1.6",
		"dom4j" % "dom4j" % "1.6.1",
		"log4j" % "log4j" % "1.2.17",
		"com.googlecode.json-simple" % "json-simple" % "1.1.1"
)

resolvers += "Cloudera Repository" at "https://repository.cloudera.com/artifactory/cloudera-repos/"

 

