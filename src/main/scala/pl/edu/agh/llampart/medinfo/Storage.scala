package pl.edu.agh.llampart.medinfo

import java.util.logging.Logger

import pl.edu.agh.llampart.medinfo.medinfo._

import scala.collection.concurrent.TrieMap


class Storage {

  import Storage._

  private val storageImpl = TrieMap.empty[StorageEntry, Unit]

  private def getUsersExaminations(query: Request)(staged: Iterator[StorageEntry]): Iterator[StorageEntry] = query match {
    case Request(_, _, Some(patientId), _, _) =>
      staged.filter(_.patientId == patientId)
    case _ => staged
  }

  private def getDoctorExaminations(query: Request)(staged: Iterator[StorageEntry]): Iterator[StorageEntry] = query match {
    case Request(_, Some(examinerId), _, _, _) =>
      staged.filter(_.doctorId == examinerId)
    case _ =>
      staged
  }

  private def getExaminationByType(query: Request)(staged: Iterator[StorageEntry]): Iterator[StorageEntry] = query match {
    case Request(names, _, _, _, _) if names.nonEmpty =>
      val namesSet = names.toSet
      staged.filter(e => namesSet.contains(e.examination.name))
    case _ =>
      staged
  }


  private def getExaminationsByDates(query: Request)(staged: Iterator[StorageEntry])(implicit ord: Ordering[DateTime]): Iterator[StorageEntry] = query match {
    case Request(_, _, _, None, Some(dt)) =>
      staged.filter(e => ord.lteq(e.date, dt))
    case Request(_, _, _, Some(dt), None) =>
      staged.filter(e => ord.lteq(dt, e.date))
    case Request(_, _, _, Some(from), Some(to)) =>
      staged.filter(e => ord.lteq(from, e.date) && ord.lteq(e.date, to))
    case _ =>
      staged
  }

  def saveReport(report: ExaminationReport): Unit = {
    val storageEntries = for (e <- report.examination) yield StorageEntry(report.date, report.examinerId, report.patientId, e) -> ()
    storageImpl ++= storageEntries
  }

  private type ChainFunction = Iterator[StorageEntry] => Request => Iterator[StorageEntry]

  def query(request: Request): Iterator[Response] = {
    val func = getUsersExaminations(request) _ andThen getDoctorExaminations(request) andThen getExaminationByType(request) andThen getExaminationsByDates(request)
    func(storageImpl.keysIterator).map(_.toResponse(request))
  }

}

object Storage {

  private val logger = Logger.getLogger(classOf[Storage].getSimpleName)

  private case class StorageEntry(date: DateTime, doctorId: String, patientId: String, examination: Examination) {
    def toResponse(request: Request): Response = request match {
      case Request(_, _, Some(_), _, _) =>
        Response(date).withSimple(examination)
      case _ =>
        Response(date).withPerson(PersonExamination(examination, patientId))
    }
  }

}
