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

package monix.connect.benchmarks.redis

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalatest.flatspec.AnyFlatSpec

class RedisTest extends AnyFlatSpec with RedisBenchFixture {

  "it" should "connect to redis" in {

    val keys = (0 to maxKey).toList.map(_.toString)
    val keysCycle = scala.Stream.continually(keys).flatten.iterator

    Task.parSequence {
      (1 to maxKey).map { key =>
        val value = key.toString
        val field = key.toString
        monixRedis.use(_.hash.hSet(key.toString, field, value))
      }
    }.runSyncUnsafe()
  }

}
