package pl.edu.agh.llampart

import java.util.concurrent.Executors
import java.util.logging.Logger

import io.grpc.ManagedChannelBuilder
import pl.edu.agh.llampart.hello.{GreeterGrpc, HelloReply, HelloRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import util._

import scala.concurrent.Await
import scala.language.postfixOps
import scala.util.{Failure, Success}

class ExampleClient

object ExampleClient extends App {
  private val logger = Logger.getLogger(classOf[ExampleClient].getSimpleName)
  private val executor = Executors.newSingleThreadScheduledExecutor()

  private val channel = ManagedChannelBuilder.forAddress("localhost", ExampleServer.port).usePlaintext(true).build()
  private val stub = GreeterGrpc.stub(channel)
  private val finishedCommunication = for {
    HelloReply(str) <- {
      logger.info("Asking server")
      stub.sayHello(HelloRequest("Example"))
    }
  } yield s"Server responded with msg: $str"
  finishedCommunication.onComplete {
    case Success(str) => println(str)
    case Failure(error) => logger.severe(s"Communication failed due to: ${error.getMessage}")
  }
  println(Await.result(finishedCommunication.flatMap(_ => executor.after(10 seconds)(channel.shutdownNow())), Duration.Inf))
}
