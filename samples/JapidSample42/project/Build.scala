import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "JapidSample42"
    val appVersion      = "0.5"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "japid42" % "japid42_2.9.1" % "0.5",
      "com.google.code.javaparser" % "javaparser" % "1.0.8"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = JAVA).settings(
      // Add your own project settings here  
      resolvers += "Local Play Repository" at "/Users/bran/projects/playscala/repository/local"
    )

}
