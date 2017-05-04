package pl.edu.agh.llampart.medinfo

import java.util.concurrent.Executors
import java.util.logging.Logger

import io.grpc.{Server, ServerBuilder}
import io.grpc.stub.StreamObserver
import pl.edu.agh.llampart.medinfo.medinfo._
import pl.edu.agh.llampart.medinfo.medinfo.LedgerGrpc.Ledger

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import pl.edu.agh.llampart.util._

import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class MainServer private(innerServer: Server) {


  def stop(): Stopped.type = {
    MainServer.logger.info("Server shutdown begins")
    innerServer.shutdown()
    Stopped
  }

  object Stopped {
    def awaitTermination(): Unit = {
      innerServer.awaitTermination()
      MainServer.logger.info("Server shutdown complete!")
    }
  }

}

object MainServer {
  private val logger = Logger.getLogger(classOf[MainServer].getSimpleName)


  private class ServiceImpl(storage: Storage) extends Ledger {
    private val schedExec = Executors.newSingleThreadScheduledExecutor()

    override def saveExaminationReport(request: ExaminationReport): Future[Empty] = Future {
      storage.saveReport(request)
      Empty()
    }

    override def requestExamination(request: Request, responseObserver: StreamObserver[Response]): Unit = {
      val storageIt = storage.query(request)
      logger.info(s"Started handling of the request: $request")
      val resultFuture = (storageIt foldLeft Future.successful[Unit]()) { (f, response) =>
        for {
          _ <- f
          _ <- schedExec.after(100 millis) {
            responseObserver.onNext(response)
          }
        } yield ()
      }
      resultFuture.onComplete {
        case Success(_) =>
          logger.info("Successfully handled request!")
          responseObserver.onCompleted()
        case Failure(error) =>
          logger.warning("")
          responseObserver.onError(error)
      }
    }
  }

  val port = 50051


  def main(args: Array[String]): Unit = {
    val storage = new Storage
    val server = ServerBuilder.forPort(port).addService(LedgerGrpc.bindService(new ServiceImpl(storage), global)).build().start()
    val mainServer = new MainServer(server)
    logger.info(s"Server start successful! Listening on port ${server.getPort}.")
    scala.io.StdIn.readLine()
    try {
      mainServer.stop().awaitTermination()
    } catch {
      case NonFatal(e) =>
        logger.severe(e.getMessage)
    }
  }


}
