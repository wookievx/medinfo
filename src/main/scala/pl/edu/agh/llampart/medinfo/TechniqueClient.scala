package pl.edu.agh.llampart.medinfo

import java.util.{Calendar, Date}
import java.util.logging.Logger

import io.grpc.ManagedChannelBuilder
import pl.edu.agh.llampart.medinfo.medinfo.{DateTime, Examination, ExaminationReport, LedgerGrpc}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Random, Success}

class TechniqueClient
object TechniqueClient extends App {
  private val logger = Logger.getLogger(classOf[TechniqueClient].getSimpleName)
  private val channel = ManagedChannelBuilder.forAddress("localhost", MainServer.port).usePlaintext(true).build()
  private val stub = LedgerGrpc.stub(channel)

  case class Query(doctorName: String = "", when: Option[Calendar] = None, patientId: String = "")

  private val parser = new scopt.OptionParser[Query]("technique") {

    opt[String]('d', "doctor_id").action((doctorName, q) => q.copy(doctorName = doctorName)).text("Doctor name").required()

    opt[Calendar]('t', "date").action((when, q) => q.copy(when = Some(when))).text("Date of the examination").required()

    opt[String]('p', "patient_id").action((patientId, q) => q.copy(patientId = patientId)).text("Id of the patient examined").required()

  }

  private val testDoctors = "DoctorA"
  private val testPatients = Seq("PatientA", "PatientB")
  private val types = (1 to 10).map(Integer.toString)

  parser.showUsage()
  println(
    """After defining basic information you can provide more examination to raport by typing:
      |:e <name> <result> <unit>
      |If your intention is to generate some test data (doctor: "DoctorA", patients: "PatientA", "PatientB", types are numbers from 1 to 10, unit is "u")
      |just type :generate
    """.stripMargin)
  println("To exit type :quit, or :q")
  Iterator.continually {
    val line = scala.io.StdIn.readLine("Type your task\n").split("""\s+""")
    if (line.isEmpty) {
      true
    } else {
      line.head match {
        case ":q" | ":quit" =>
          channel.shutdown()
          false
        case ":generate" =>
          val Seq(pt, _) = Random.shuffle(testPatients)
          val examinations = for (t <- types) yield Examination(t, "u").withFlt(Random.nextFloat())
          val report = ExaminationReport(testDoctors, DateTime(new Date().getTime), pt, examinations)
          val future = stub.saveExaminationReport(report).andThen {
            case Success(_) =>
              logger.info("Successfully saved report")
            case Failure(exception) =>
              logger.severe(s"Failed to save report on the server, reason: ${exception.getMessage}")
          }
          Await.ready(future, Duration.Inf)
          true
        case _ =>
          parser.parse(line, Query()) match {
            case Some(Query(name, Some(c), patientId)) =>
              val examinations = Iterator.continually {
                val line = scala.io.StdIn.readLine().split("""\s+""").toSeq
                line match {
                  case Seq(":e", etype, result, unit) =>
                    Some(Examination(etype, unit, Examination.Result.Str(result)))
                  case _ =>
                    None
                }
              }.takeWhile(_.isDefined).flatten.toSeq
              val report = ExaminationReport(name, DateTime(c.getTimeInMillis), patientId, examinations)
              val future = stub.saveExaminationReport(report).andThen {
                case Success(_) =>
                  logger.info("Successfully saved report")
                case Failure(exception) =>
                  logger.severe(s"Failed to save report on the server, reason: ${exception.getMessage}")
              }
              Await.ready(future, Duration.Inf)
          }
          true
      }
    }
  }.takeWhile(identity).foreach(_ => ())


}
