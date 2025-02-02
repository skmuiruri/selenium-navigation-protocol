import Dependencies._

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "com.skm"
ThisBuild / organizationName := "skmuiruri"

lazy val root = (project in file("."))
  .settings(
    name := "selenium-navigation-protocol",
    libraryDependencies ++= Seq(
      "org.seleniumhq.selenium" % "selenium-java" % "4.15.0"
    )
  )

addCommandAlias("fmt", "all scalafmtSbt scalafmtAll")
