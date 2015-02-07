package models.util

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.{Promise, ExecutionContext, Future}
import scala.util.Try

object Futures {
  def mapTry[T, S](future: Future[T])(f: Try[T] => S)
                  (implicit context: ExecutionContext): Future[S] = {
    val promise = Promise[S]()
    future.onComplete[Unit](tryT => promise.complete(Try { f(tryT) }))
    promise.future
  }

  def all[T](futures: Future[T]*)(implicit executor: ExecutionContext): Future[Unit] = {
    val promise = Promise[Unit]()
    for (future <- futures)
      future.onFailure[Unit] { case t => promise.tryFailure(t) }
    val counter = new AtomicInteger(futures.size)
    for (future <- futures)
      future.onSuccess { case _ =>
        if (counter.decrementAndGet() == 0)
          promise.success(())
      }
    promise.future
  }
}
