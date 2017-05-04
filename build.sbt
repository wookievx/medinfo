name := "medinfo"

version := "1.0"

scalaVersion := "2.12.2"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value
)

PB.pythonExe := "C:\\Python27\\python.exe"

libraryDependencies ++= Seq(
  "io.grpc" % "grpc-netty" % com.trueaccord.scalapb.compiler.Version.grpcJavaVersion,
  "com.github.scopt" %% "scopt" % "3.5.0",
  "com.trueaccord.scalapb" %% "scalapb-runtime-grpc" % com.trueaccord.scalapb.compiler.Version.scalapbVersion
)