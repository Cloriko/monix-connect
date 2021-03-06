package monix.connect.redis

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.scalacheck.Gen
import org.scalatest.concurrent.Eventually
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.concurrent.duration._

class HashCommandsSuite
  extends AsyncFlatSpec with RedisIntegrationFixture with Matchers with BeforeAndAfterEach with BeforeAndAfterAll
    with Eventually {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(4.seconds, 100.milliseconds)

  override def beforeEach(): Unit = {
    super.beforeEach()
    utfConnection.use(cmd => cmd.server.flushAll).runSyncUnsafe()
  }

  "hDel" should "delete single hash field" in {
    //given
    val k1: K = genRedisKey.sample.get
    val f1: K = genRedisKey.sample.get
    val v: K = genRedisKey.sample.get

    utfConnection.use { cmd =>
      //when
      for {
        _ <- cmd.hash.hSet(k1, f1, v)
        existsBefore <- cmd.hash.hExists(k1, f1)
        delete <- cmd.hash.hDel(k1, f1)
        existsAfter <- cmd.hash.hExists(k1, f1)
        deleteNoKey <- cmd.hash.hDel(k1, f1)
      } yield {
        //then
        existsBefore shouldBe true
        delete shouldBe true
        existsAfter shouldBe false
        deleteNoKey shouldBe false
      }
    }.runToFuture
  }

  "hDel" should "delete multiple hash fields" in {
    //given
    val k1: K = genRedisKey.sample.get
    val f1: K = genRedisKey.sample.get
    val f2: K = genRedisKey.sample.get
    val f3: K = genRedisKey.sample.get
    val v: K = genRedisKey.sample.get

    utfConnection.use { cmd =>
      //when
      for {
        _ <- cmd.hash.hMSet(k1, Map(f1 -> v, f2 -> v, f3 -> v))
        initialLen <- cmd.hash.hLen(k1)
        delete <- cmd.hash.hDel(k1, List(f1, f2))
        finalLen <- cmd.hash.hLen(k1)
        deleteNoKey <- cmd.hash.hDel(k1, List(f1, f2))
      } yield {
        //then
        initialLen shouldBe 3L
        delete shouldBe 2L
        finalLen shouldBe 1L
        deleteNoKey shouldBe 0L
      }
    }.runToFuture
  }

  "hExists" should "determine whether a hash field exists or not" in {
    //given
    val k: K = genRedisKey.sample.get
    val f: K = genRedisKey.sample.get
    val v: V = genRedisKey.sample.get

    utfConnection.use { cmd =>
      //when
      for {
        _ <- cmd.hash.hSet(k, f, v)
        exists1 <- cmd.hash.hExists(k, f)
        exists2 <- cmd.hash.hExists(k, "non-existing-field")
      } yield {
        //then
        exists1 shouldBe true
        exists2 shouldBe false
      }
    }.runToFuture
  }

  "hGet" should "return an empty value when accessing to a non existing hash" in {
    //given
    val key: K = genRedisKey.sample.get
    val field: K = genRedisKey.sample.get

    //when
    val r: Option[String] = utfConnection.use(_.hash.hGet(key, field)).runSyncUnsafe()

    //then
    r shouldBe None
  }

  "hIncr" should "return None when the hash does not exists " in {
    //given
    val key: K = genRedisKey.sample.get
    val field: K = genRedisKey.sample.get

    //when
    val r: Option[String] = utfConnection.use(_.hash.hGet(key, field)).runSyncUnsafe()

    //then
    r shouldBe None
  }

  "hIncrBy" should "increment the integer value of a hash field by the given number" in {
    //given
    val k: K = genRedisKey.sample.get
    val f1: K = genRedisKey.sample.get
    val f2: K = genRedisKey.sample.get
    val v: V = "1"

    utfConnection.use { cmd =>
      //when
      for {
        _ <- cmd.hash.hSet(k, f1, v)
        inc0 <- cmd.hash.hIncrBy(k, f1, 0)
        inc1 <- cmd.hash.hIncrBy(k, f1, 1)
        inc2 <- cmd.hash.hIncrBy(k, f1, 2)
        inc3 <- cmd.hash.hIncrBy(k, f1, 3)
        _ <- cmd.hash.hSet(k, f2, "someString")
        incNonNumber <- cmd.hash.hIncrBy(k, f2, 1).onErrorHandleWith(ex => if (ex.getMessage.contains("not an integer")) Task.now(-10) else Task.now(-11))
        incNotExistingField <- cmd.hash.hIncrBy(k, "none", 1)
        incNotExistingKey <- cmd.hash.hIncrBy("none", "none", 4)
      } yield {
        //then
        inc0 shouldBe v.toLong
        inc1 shouldBe 2L
        inc2 shouldBe 4L
        inc3 shouldBe 7L
        incNonNumber shouldBe -10
        incNotExistingField shouldBe 1
        incNotExistingKey shouldBe 4
      }
    }.runToFuture
  }

  it should "increment the float value of a hash field by the given amount " in {
    //given
    val k: K = genRedisKey.sample.get
    val f1: K = genRedisKey.sample.get
    val f2: K = genRedisKey.sample.get
    val v: V = "1"

    utfConnection.use { cmd =>
      //when
      for {
        _ <- cmd.hash.hSet(k, f1, v)
        inc0 <- cmd.hash.hIncrBy(k, f1, 0.0)
        inc1 <- cmd.hash.hIncrBy(k, f1, 1.1)
        inc2 <- cmd.hash.hIncrBy(k, f1, 2.1)
        inc3 <- cmd.hash.hIncrBy(k, f1, 3.1)
        _ <- cmd.hash.hSet(k, f2, "someString")
        incNonNumber <- cmd.hash.hIncrBy(k, f2, 1.1).onErrorHandleWith(ex => if (ex.getMessage.contains(" not a float")) Task.now(-10.10) else Task.now(-11.11))
        incNotExistingField <- cmd.hash.hIncrBy(k, "none", 1.1)
        incNotExistingKey <- cmd.hash.hIncrBy("none", "none", 0.1)
      } yield {
        //then
        inc0 shouldBe v.toDouble
        inc1 shouldBe 2.1
        inc2 shouldBe 4.2
        inc3 shouldBe 7.3
        incNonNumber shouldBe -10.10
        incNotExistingField shouldBe 1.1
        incNotExistingKey shouldBe 0.1
      }
    }.runToFuture
  }

  "hGetAll" should "get all the fields and values in a hash" in {
    //given
    val k: K = genRedisKey.sample.get
    val mSet = Gen.mapOfN(10, genKv).sample.get

    utfConnection.use { cmd =>
      //when
      for {
        _ <- cmd.hash.hMSet(k, mSet)
        getAll <- cmd.hash.hGetAll(k).toListL
        emptyHash <- cmd.hash.hGetAll("none").toListL
      } yield {
        //then
        getAll should contain theSameElementsAs mSet.toList.map{ case (k, v) => (k, v) }
        emptyHash shouldBe List.empty
      }
    }.runToFuture
  }

  "hKeys" should "get all the fields in a hash" in {
    //given
    val k: K = genRedisKey.sample.get
    val mSet = Gen.mapOfN(10, genKv).sample.get

    utfConnection.use { cmd =>
      //when
      for {
        _ <- cmd.hash.hMSet(k, mSet)
        keys <- cmd.hash.hKeys(k).toListL
        emptyKeys <- cmd.hash.hKeys("none").toListL

      } yield {
        //then
        keys should contain theSameElementsAs mSet.toList.map(_._1)
        emptyKeys shouldBe List.empty
      }
    }.runToFuture
  }

  "hLen" should "set the number of hash fields within a key" in {
    //given
    val k1: K = genRedisKey.sample.get
    val f1: K = genRedisKey.sample.get
    val f2: K = genRedisKey.sample.get
    val f3: K = genRedisKey.sample.get
    val v: K = genRedisKey.sample.get

    utfConnection.use { cmd =>
      //when
      for {
        _ <- cmd.hash.hMSet(k1, Map(f1 -> v, f2 -> v, f3 -> v))
        initialLen <- cmd.hash.hLen(k1)
        _ <- cmd.hash.hDel(k1, List(f1, f2, f3))
        finalLen <- cmd.hash.hLen(k1)
        _ <- cmd.hash.hDel(k1, List(f1, f2))
      } yield {
        //then
        initialLen shouldBe 3L
        finalLen shouldBe 0L
      }
    }.runToFuture
  }

  "hMSet" should "get the specified hash keys and values" in {
    //given
    val k1: K = genRedisKey.sample.get
    val f1: K = genRedisKey.sample.get
    val f2: K = genRedisKey.sample.get
    val f3: K = genRedisKey.sample.get
    val v: K = genRedisKey.sample.get

    utfConnection.use { cmd =>
      //when
      for {
        _ <- cmd.hash.hMSet(k1, Map(f1 -> v, f2 -> v))
        kV <- cmd.hash.hMGet(k1, f1, f2, f3).toListL
      } yield {
        //then
        kV should contain theSameElementsAs List((f1, Some(v)), (f2, Some(v)), (f3, None))
      }
    }.runToFuture
  }

  "hSet" should "set the string value of a hash field." in {
    //given
    val key: K = genRedisKey.sample.get
    val field: K = genRedisKey.sample.get
    val value1: String = genRedisValue.sample.get
    val value2: String = genRedisValue.sample.get

    //when
    utfConnection.use { cmd =>
      for {
        setFirst <- cmd.hash.hSet(key, field, value1)
        setSecond <- cmd.hash.hSet(key, field, value2)
        finalValue <- cmd.hash.hGet(key, field)
      } yield {
        setFirst shouldBe true
        setSecond shouldBe false
        finalValue shouldBe Some(value2)
      }
    }.runSyncUnsafe()
  }

  "hSetNx" should "set the value of a hash field, only if the field does not exist" in {
    //given
    val key: K = genRedisKey.sample.get
    val field: K = genRedisKey.sample.get
    val value: V= genRedisValue.sample.get

    //when
    utfConnection.use(cmd =>
      for {
        firstSet <- cmd.hash.hSetNx(key, field, value)
        secondSet <- cmd.hash.hSetNx(key, field, value)
        get <- cmd.hash.hGet(key, field)
        getNone <- cmd.hash.hGet(key, "none")
      } yield {
        //then
        firstSet shouldBe true
        secondSet shouldBe false
        get shouldBe Some(value)
        getNone shouldBe None
      }
    ).runToFuture
  }

  "hStrLen" should "get the string length of the field value" in {
    //given
    val k1: K = genRedisKey.sample.get
    val f1: K = genRedisKey.sample.get
    val f2: K = genRedisKey.sample.get
    val v: K = genRedisKey.sample.get

    utfConnection.use {
      cmd =>
        //when
        for {
          _ <- cmd.hash.hSet(k1, f1, v)
          f1StrLen <- cmd.hash.hStrLen(k1, f1)
          f2StrLen <- cmd.hash.hStrLen(k1, f2)
          k2StrLen <- cmd.hash.hStrLen("non-existing-key", f1)
        } yield {
          //then
          f1StrLen shouldBe v.length
          f2StrLen shouldBe 0L
          k2StrLen shouldBe 0L
        }
    }.runToFuture
  }

  "hVals" should "get all hash values of a key " in {
    //given
    val k1: K = genRedisKey.sample.get
    val k2: K = genRedisKey.sample.get
    //and
    val f1: K = genRedisKey.sample.get
    val f2: K = genRedisKey.sample.get
    val f3: K = genRedisKey.sample.get
    //and
    val v1: K = genRedisKey.sample.get
    val v2: K = genRedisKey.sample.get
    val v3: K = genRedisKey.sample.get

    utfConnection.use {
      cmd =>
        //when
        for {
          _ <- cmd.hash.hMSet(k1, Map(f1 -> v1, f2 -> v2, f3 -> v3))
          k1Vals <- cmd.hash.hVals(k1).toListL
          k2Vals <- cmd.hash.hVals(k2).toListL
        } yield {
          //then
          k1Vals should contain theSameElementsAs List(v1, v2, v3)
          k2Vals shouldBe List.empty
        }
    }.runToFuture
  }

}
