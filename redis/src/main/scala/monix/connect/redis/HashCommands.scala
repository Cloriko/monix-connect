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

import io.lettuce.core.api.reactive.RedisHashReactiveCommands
import monix.eval.Task
import monix.execution.internal.InternalApi
import monix.reactive.Observable

import scala.jdk.CollectionConverters._

private[redis] class HashCommands[K, V](reactiveCmd: RedisHashReactiveCommands[K, V]) {

  /**
    * Delete one or more hash fields.
    * @return Number of fields that were removed from the hash, not including specified but non existing
    *         fields.
    */
  def hDel(key: K, fields: List[K]): Task[Long] =
    Task.fromReactivePublisher(reactiveCmd.hdel(key, fields: _*)).map(_.map(_.longValue).getOrElse(0L))

  def hDel(key: K, field: K): Task[Boolean] =
    Task.fromReactivePublisher(reactiveCmd.hdel(key, field)).map(_.map(_.longValue).getOrElse(0L) > 0L)

  /**
    * Determine if a hash field exists.
    * @return True if the hash contains the field.
    *         False if the hash does not contain the field, or key does not exist.                                                                                                                  .
    */
  def hExists(key: K, field: K): Task[Boolean] =
    Task.fromReactivePublisher(reactiveCmd.hexists(key, field)).map(_.exists(_.booleanValue))

  /**
    * Get the value of a hash field.
    * @return The value associated with field, or null when field is not present in the hash or key does not exist.
    *         A failed task with [[NoSuchElementException]] will be returned when the underlying api
    *         returns an empty publisher. i.e: when the key did not exist.
    */
  def hGet(key: K, field: K): Task[Option[V]] =
    Task.fromReactivePublisher(reactiveCmd.hget(key, field))

  /**
    * Increment the integer value of a hash field by the given number.
    * @return The value at field after the increment operation.
    */
  def hIncrBy(key: K, field: K, amount: Long): Task[Option[Long]] =
    Task.fromReactivePublisher(reactiveCmd.hincrby(key, field, amount)).map(_.map(_.longValue))

  /**
    * Increment the float value of a hash field by the given amount.
    * @return The value of field after the increment.
    */
  def hIncrBy(key: K, field: K, amount: Double): Task[Option[Double]] =
    Task.fromReactivePublisher(reactiveCmd.hincrbyfloat(key, field, amount)).map(_.map(_.doubleValue))

  /**
    * Get all the fields and values in a hash.
    *
    * @param key the key
    * @return Map of the fields and their values stored in the hash, or an empty list when key does not exist.
    */
  def hGetAll(key: K): Task[Map[K, V]] =
    Task.fromReactivePublisher(reactiveCmd.hgetall(key)).map(_.map(_.asScala.toMap).getOrElse(Map.empty))

  /**
    * Get all the fields in a hash.
    * @return The fields in the hash.
    */
  def hKeys(key: K): Observable[K] =
    Observable.fromReactivePublisher(reactiveCmd.hkeys(key))

  /**
    * Get the number of fields in a hash.
    * @return Number of fields in the hash, or 0 when key does not exist.
    */
  def hLen(key: K): Task[Long] =
    Task.fromReactivePublisher(reactiveCmd.hlen(key)).map(_.map(_.longValue).getOrElse(0L))

  /**
    * Get the values of all the given hash fields.
    * @return Values associated with the given fields.
    */
  def hMGet(key: K, fields: K*): Observable[(K, Option[V])] =
    Observable.fromReactivePublisher(reactiveCmd.hmget(key, fields: _*)).map(kvToTuple)

  /** Set multiple hash fields to multiple values. */
  def hMSet(key: K, map: Map[K, V]): Task[Unit] =
    Task.fromReactivePublisher(reactiveCmd.hmset(key, map.asJava)).void

  /// todo - create observable that iterates over
  //  * Incrementally iterate hash fields and associated values.
  //  * @return Map scan cursor.
  //  */
  //def hScan(key: K): Task[MapScanCursor[K, V]] =
  //  Task.fromReactivePublisher(reactiveCmd.hscan(key))

  //def hScan(key: K, scanCursor: ScanCursor): Task[MapScanCursor[K, V]] =
  //  Task.fromReactivePublisher(reactiveCmd.hscan(key, scanCursor))

  /**
    * Set the string value of a hash field.
    * @return True if field is a new field in the hash and value was set.
    *         False if field already exists in the hash and the value was updated.
    */
  def hSet(key: K, field: K, value: V): Task[Boolean] =
    Task.fromReactivePublisher(reactiveCmd.hset(key, field, value)).map(_.exists(_.booleanValue))

  /**
    * Set the value of a hash field, only if the field does not exist.
    *
    * @return True if field is a new field in the hash and value was set.
    *         False if field already exists in the hash and the value was updated.
    */
  def hSetNx(key: K, field: K, value: V): Task[Boolean] =
    Task.fromReactivePublisher(reactiveCmd.hsetnx(key, field, value)).map(_.exists(_.booleanValue))

  /**
    * Get the string length of the field value in a hash.
    * @return Length of the field value, or 0 when field is not present in the hash
    *         or key does not exist at all.
    */
  def hStrLen(key: K, field: K): Task[Long] =
    Task.fromReactivePublisher(reactiveCmd.hstrlen(key, field)).map(_.map(_.longValue).getOrElse(0L))

  /**
    * Get all the values in a hash.
    * @return Values in the hash, or an empty list when key does not exist.
    */
  def hVals(key: K): Observable[V] = Observable.fromReactivePublisher(reactiveCmd.hvals(key))

}

@InternalApi
private[redis] object HashCommands {
  def apply[K, V](reactiveCmd: RedisHashReactiveCommands[K, V]): HashCommands[K, V] =
    new HashCommands[K, V](reactiveCmd)

}