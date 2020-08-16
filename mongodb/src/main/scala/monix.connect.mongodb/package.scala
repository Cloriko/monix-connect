package monix.connect

import com.mongodb.client.model.{
  CountOptions,
  DeleteOptions,
  FindOneAndDeleteOptions,
  FindOneAndReplaceOptions,
  FindOneAndUpdateOptions,
  InsertManyOptions,
  InsertOneOptions,
  ReplaceOptions,
  UpdateOptions
}
import monix.eval.{Coeval, Task}
import monix.execution.internal.InternalApi
import org.reactivestreams.Publisher

import scala.concurrent.duration.FiniteDuration

package object mongodb {

  @InternalApi private[mongodb] val DefaultDeleteOptions = new DeleteOptions()
  @InternalApi private[mongodb] val DefaultCountOptions = new CountOptions()
  @InternalApi private[mongodb] val DefaultFindOneAndDeleteOptions = new FindOneAndDeleteOptions()
  @InternalApi private[mongodb] val DefaultFindOneAndReplaceOptions = new FindOneAndReplaceOptions()
  @InternalApi private[mongodb] val DefaultFindOneAndUpdateOptions = new FindOneAndUpdateOptions()
  @InternalApi private[mongodb] val DefaultInsertOneOptions = new InsertOneOptions()
  @InternalApi private[mongodb] val DefaultInsertManyOptions = new InsertManyOptions()
  @InternalApi private[mongodb] val DefaultUpdateOptions = new UpdateOptions()
  @InternalApi private[mongodb] val DefaultReplaceOptions = new ReplaceOptions()

  /**
    * An internal method used by those operations that wants to implement a retry interface based on
    * a given limit and timeout.
    *
    * @param publisher a reactivestreams publisher that represents the generic mongodb operation.
    * @param retries the number of times the operation will be retried in case of unexpected failure,
    *                being zero retries by default.
    * @param timeout expected timeout that the operation is expected to be executed or else return a failure.
    * @tparam T the type of the publisher
    * @return a [[Task]] with an optional [[T]] or a failed one if the failures exceeded the retries.
    */
  @InternalApi private[mongodb] def retryOnFailure[T](
    publisher: Coeval[Publisher[T]],
    retries: Int,
    timeout: Option[FiniteDuration],
    delayAfterFailure: Option[FiniteDuration]): Task[Option[T]] = {

    require(retries >= 0, "Retries per operation must be higher or equal than 0.")
    val t = Task.fromReactivePublisher(publisher.value())
    timeout
      .map(t.timeout(_))
      .getOrElse(t)
      .redeemWith(
        ex =>
          if (retries > 0) {
            val retry = retryOnFailure(publisher, retries - 1, timeout, delayAfterFailure)
            delayAfterFailure match {
              case Some(delay) => retry.delayExecution(delay)
              case None => retry
            }
          } else Task.raiseError(ex),
        _ match {
          case None =>
            if (retries > 0) retryOnFailure(publisher, retries - 1, timeout, delayAfterFailure)
            else Task(None)
          case some: Some[T] => Task(some)
        }
      )
  }

}
