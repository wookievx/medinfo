package pl.edu.agh.llampart.medinfo

import java.util.Date

import pl.edu.agh.llampart.medinfo.Extensions.{ConvertibleDateTime, DateTimeOrd}
import pl.edu.agh.llampart.medinfo.medinfo.DateTime

import scala.language.implicitConversions

trait Extensions {

  implicit def extendedDt(dateTime: DateTime): ConvertibleDateTime = new ConvertibleDateTime(dateTime)
  implicit def dateTimeOrd: Ordering[DateTime] = DateTimeOrd

}

object Extensions {

  class ConvertibleDateTime(private val dt: DateTime) extends AnyVal {
    def toDate: Date = new Date(dt.instant)
  }


  object DateTimeOrd extends Ordering[DateTime] {
    override def compare(x: DateTime, y: DateTime): Int = {
      if (x.instant < y.instant) {
        -1
      } else if (x.instant > y.instant) {
        1
      } else 0
    }
  }

}
