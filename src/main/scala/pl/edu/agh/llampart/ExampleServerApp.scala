package pl.edu.agh.llampart
import ExampleServer._
import io.grpc.Server

object ExampleServerApp extends App {
  val server = ExampleServer.buildServer()
  Terminating[Server].awaitTermination(server)
}
