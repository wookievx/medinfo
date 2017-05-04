package pl.edu.agh.llampart.medinfo

import java.text.SimpleDateFormat

import java.util.logging.Logger
import java.util.{Calendar, Date}

import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import pl.edu.agh.llampart.medinfo.medinfo._

import scala.language.postfixOps

class DoctorClient

object DoctorClient extends App {
  private val logger = Logger.getLogger(classOf[DoctorClient].getSimpleName)
  private val channel = ManagedChannelBuilder.forAddress("localhost", MainServer.port).usePlaintext(true).build()
  private val stub = LedgerGrpc.stub(channel)

  case class Query(id: String = "", patients: Option[String] = None, types: Seq[String] = Seq.empty,
                   from: Option[Calendar] = None, to: Option[Calendar] = None, exit: Boolean = false)

  private val parser = new scopt.OptionParser[Query]("find") {
    head("Medical information system 0.1")

    opt[String]('i', "id").action((id, q) => q.copy(id = id)).text("Your identification")

    opt[String]('p', "patient_ids").valueName("<patient_1>, <patient_2>").action((patient, q) => q.copy(patients = Some(patient))).text("Patients of interest")

    opt[Seq[String]]('t', "examination_types").valueName("<type_1>, <type_2>").action((types, q) => q.copy(types = types)).text("Types of examinations to be listed")

    opt[Calendar]('f', "from").action((from, q) => q.copy(from = Some(from))).text("Date from which examinations should be displayed")

    opt[Calendar]('u', "until").action((to, q) => q.copy(to = Some(to))).text("Date until which examination should be displayed")

    opt[Unit]('q', "quit").action((_, q) => q.copy(exit = true)).text("Exit the loop (still needs id)")

  }

  parser.showUsage()
  Iterator.continually {
    parser.parse(scala.io.StdIn.readLine("Type your query\n").split("""\s+"""), Query()) match {
      case Some(q) if q.exit =>
        channel.shutdown()
        false
      case Some(Query(id, patients, types, from, to, false)) =>
        val fromDt = from.map(c => DateTime(c.getTimeInMillis))
        val toDt = to.map(c => DateTime(c.getTimeInMillis))
        stub.requestExamination(Request(types, Some(id), patients, fromDt, toDt), new StreamObserver[Response] {
          override def onError(t: Throwable): Unit = {
            logger.severe(s"Error failed with error: ${t.getMessage}")
          }

          override def onCompleted(): Unit = {
            logger.info(s"Query proceeded!")
          }

          override def onNext(value: Response): Unit = {
            val df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
            value match {
              case Response(DateTime(i, _), Response.Examination.Person(p)) =>
                println(s"${df.format(new Date(i))}: [${p.patientId}, ${p.examination}]")
              case Response(DateTime(i, _), Response.Examination.Simple(e)) =>
                println(s"${df.format(new Date(i))}: $e")
              case _ =>
                logger.warning("Invalid message format!")
            }
          }
        })
        true
      case None =>
        true
    }
  }.takeWhile(identity).foreach(_ => ())



}
