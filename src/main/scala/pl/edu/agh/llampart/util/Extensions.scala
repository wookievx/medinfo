package pl.edu.agh.llampart.util

import java.util.concurrent.ScheduledExecutorService
import ScheduleExtensions._
import scala.language.implicitConversions

trait Extensions {

  implicit def toRichScheduledExecutor(exec: ScheduledExecutorService): RichScheduledExecutor = new RichScheduledExecutor(exec)

}
