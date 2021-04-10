/*
 * Copyright (c) 2020-2021 by The Monix Connect Project Developers.
 * See the project homepage at: https://connect.monix.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package monix.connect.redis

import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.KeyValue
import monix.eval.Task
import monix.reactive.Observable

@deprecated("use the pure `monix.connect.redis.client.RedisConnection`", "0.6.0")
private[redis] trait RedisList {

  /**
    * Remove and get the first element in a list, or block until one is available.
    * @return A null multi-bulk when no element could be popped and the timeout expired.
    *         A two-element multi-bulk with the first element being the name of the key
    *         where an element was popped and the second element being the value of the popped element.
    */
  def blpop[K, V](timeout: Long, keys: K*)(implicit connection: StatefulRedisConnection[K, V]): Task[KeyValue[K, V]] =
    Task.from(connection.reactive().blpop(timeout, keys: _*))

  /**
    * Remove and get the last element in a list, or block until one is available.
    * @return A null multi-bulk when no element could be popped and the timeout expired.
    *          A two-element multi-bulk with the first element being the name of the key
    *          where an element was popped and the second element being the value of the popped element.
    */
  def brpop[K, V](timeout: Long, keys: K*)(implicit connection: StatefulRedisConnection[K, V]): Task[KeyValue[K, V]] =
    Task.from(connection.reactive().brpop(timeout, keys: _*))

  /**
    * Pop a value from a list, push it to another list and return it; or block until one is available.
    * @return The element being popped from source and pushed to destination.
    */
  def brpoplpush[K, V](timeout: Long, source: K, destination: K)(
    implicit
    connection: StatefulRedisConnection[K, V]): Task[V] =
    Task.from(connection.reactive().brpoplpush(timeout, source, destination))

  /**
    * Get an element from a list by its index.
    * @return The requested element, or null when index is out of range.
    */
  def lindex[K, V](key: K, index: Long)(implicit connection: StatefulRedisConnection[K, V]): Task[V] =
    Task.from(connection.reactive().lindex(key, index))

  /**
    * Insert an element before or after another element in a list.
    * @return The length of the list after the insert operation, or -1 when the value pivot was not found.
    */
  def linsert[K, V](key: K, before: Boolean, pivot: V, value: V)(
    implicit
    connection: StatefulRedisConnection[K, V]): Task[Long] =
    Task.from(connection.reactive().linsert(key, before, pivot, value)).map(_.longValue)

  /**
    * Get the length of a list.
    * @return Long integer-reply the length of the list at { @code key}.
    */
  def llen[K, V](key: K)(implicit connection: StatefulRedisConnection[K, V]): Task[Long] =
    Task.from(connection.reactive().llen(key)).map(_.longValue)

  /**
    * Remove and get the first element in a list.
    * @return The value of the first element, or null when key does not exist.
    */
  def lpop[K, V](key: K)(implicit connection: StatefulRedisConnection[K, V]): Task[V] =
    Task.from(connection.reactive().lpop(key))

  /**
    * Prepend one or multiple values to a list.
    * @return The length of the list after the push operations.
    */
  def lpush[K, V](key: K, values: V*)(implicit connection: StatefulRedisConnection[K, V]): Task[Long] =
    Task.from(connection.reactive().lpush(key, values: _*)).map(_.longValue)

  /**
    * Prepend values to a list, only if the list exists.
    * @return The length of the list after the push operation.
    */
  def lpushx[K, V](key: K, values: V*)(implicit connection: StatefulRedisConnection[K, V]): Task[Long] =
    Task.from(connection.reactive().lpushx(key, values: _*)).map(_.longValue)

  /**
    * Get a range of elements from a list.
    * @return List of elements in the specified range.
    */
  def lrange[K, V](key: K, start: Long, stop: Long)(implicit connection: StatefulRedisConnection[K, V]): Observable[V] =
    Observable.fromReactivePublisher(connection.reactive().lrange(key, start, stop))

  /**
    * Remove elements from a list.
    * @return The number of removed elements.
    */
  def lrem[K, V](key: K, count: Long, value: V)(implicit connection: StatefulRedisConnection[K, V]): Task[Long] =
    Task.from(connection.reactive().lrem(key, count, value)).map(_.longValue)

  /**
    * Set the value of an element in a list by its index.
    * @return The same inserted value
    */
  def lset[K, V](key: K, index: Long, value: V)(implicit connection: StatefulRedisConnection[K, V]): Task[String] =
    Task.from(connection.reactive().lset(key, index, value))

  /**
    * Trim a list to the specified range.
    * @return Simple string reply
    */
  def ltrim[K, V](key: K, start: Long, stop: Long)(implicit connection: StatefulRedisConnection[K, V]): Task[String] =
    Task.from(connection.reactive().ltrim(key, start, stop))

  /**
    * Remove and get the last element in a list.
    * @return The value of the last element, or null when key does not exist.
    */
  def rpop[K, V](key: K)(implicit connection: StatefulRedisConnection[K, V]): Task[V] =
    Task.from(connection.reactive().rpop(key))

  /**
    * Remove the last element in a list, append it to another list and return it.
    * @return The element being popped and pushed.
    */
  def rpoplpush[K, V](source: K, destination: K)(implicit connection: StatefulRedisConnection[K, V]): Task[V] =
    Task.from(connection.reactive().rpoplpush(source, destination))

  /**
    * Append one or multiple values to a list.
    * @return The length of the list after the push operation.
    */
  def rpush[K, V](key: K, values: V*)(implicit connection: StatefulRedisConnection[K, V]): Task[Long] =
    Task.from(connection.reactive().rpush(key, values: _*)).map(_.longValue)

  /**
    * Append values to a list, only if the list exists.
    * @return The length of the list after the push operation.
    */
  def rpushx[K, V](key: K, values: V*)(implicit connection: StatefulRedisConnection[K, V]): Task[Long] =
    Task.from(connection.reactive().rpushx(key, values: _*)).map(_.longValue)
}

@deprecated("use the pure `monix.connect.redis.client.RedisConnection`", "0.6.0")
object RedisList extends RedisList
