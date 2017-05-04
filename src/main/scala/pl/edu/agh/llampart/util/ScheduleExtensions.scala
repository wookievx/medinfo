package pl.edu.agh.llampart.util

import java.util.concurrent.{ScheduledExecutorService, TimeUnit}

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

object ScheduleExtensions {

  class RichScheduledExecutor(private val exec: ScheduledExecutorService) extends AnyVal {

    def after[T](time: FiniteDuration)(code: => T): Future[T] = {
      val p = Promise[T]()
      exec.schedule(() => p.complete(Try(code)), time.toNanos, TimeUnit.NANOSECONDS)
      p.future
    }

  }


}
