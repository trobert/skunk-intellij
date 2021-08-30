name         := "skunk-intellij"
organization := "org.trobert"
version      := "0.1"

scalaVersion := "2.13.6"

Global / intellijAttachSources := true
ThisBuild / intellijBuild      := "2021.2"

enablePlugins(SbtIdeaPlugin)
intellijPlugins += "org.intellij.scala".toPlugin

githubOwner       := "trobert"
githubRepository  := "skunk-intellij"
githubTokenSource := TokenSource.Environment("GITHUB_TOKEN") || TokenSource.GitConfig("github.token")
