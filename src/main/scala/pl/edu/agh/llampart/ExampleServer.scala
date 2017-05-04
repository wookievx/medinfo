package pl.edu.agh.llampart

import java.util.logging.Logger

import io.grpc.{Server, ServerBuilder}
import pl.edu.agh.llampart.hello.{GreeterGrpc, HelloReply, HelloRequest}
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._
class ExampleServer
object ExampleServer {
  private val logger = Logger.getLogger(classOf[ExampleServer].getName)
  val port = 50051

  private class GreeterImplementation extends GreeterGrpc.Greeter {

    override def sayHello(request: HelloRequest): Future[HelloReply] = Future {
      HelloReply(s"Witaj ${request.name}")
    }
  }


  trait Stopable[T] {
    def stop(instance: T)
  }

  trait Terminating[T] {
    def awaitTermination(instance: T)
  }
  object Terminating {
    def apply[T: Terminating]: Terminating[T] = implicitly
  }

  def buildServer(): Server = {
    val server = ServerBuilder.forPort(port).addService(GreeterGrpc.bindService(new GreeterImplementation, global)).build().start()
    logger.info(s"Successfully created server with services: ${server.getServices.asScala.mkString("[", ", ", "]")}")
    server
  }

  implicit object ServerStobable extends Stopable[Server] with Terminating[Server] {
    override def stop(instance: Server): Unit = {
      logger.info(s"Server $instance shutdown")
      instance.shutdown()
    }

    override def awaitTermination(instance: Server): Unit = {
      instance.awaitTermination()
    }
  }






}
