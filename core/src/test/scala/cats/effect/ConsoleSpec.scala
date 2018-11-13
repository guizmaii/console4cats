/*
 * Copyright 2018 Typelevel
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

package cats.effect

import cats.data.{ Chain, EitherT }
import cats.effect.concurrent.Ref
import cats.instances.boolean._
import cats.instances.double._
import cats.instances.int._
import cats.instances.string._
import cats.syntax.all._
import cats.FlatMap
import org.scalatest.FunSuite

class ConsoleSpec extends FunSuite {

  def program[F[_]: FlatMap](implicit C: Console[F]): F[String] =
    for {
      _ <- C.putStrLn("a")
      _ <- C.putStrLn(true)
      _ <- C.putStr(123)
      _ <- C.putStr("b")
      n <- C.readLn
      _ <- C.putError(n)
      _ <- C.putError(1.5)
    } yield n

  test("Console") {
    val test =
      for {
        out1 <- Ref.of[IO, Chain[String]](Chain.empty[String])
        out2 <- Ref.of[IO, Chain[String]](Chain.empty[String])
        out3 <- Ref.of[IO, Chain[String]](Chain.empty[String])
        rs <- {
          implicit val console: Console[IO] = new TestConsole(out1, out2, out3)
          program[IO]
        }
        rs1 <- out1.get
        rs2 <- out2.get
        rs3 <- out3.get
      } yield {
        assert(rs == "test")
        assert(rs1.mkString_("", ",", "") == "a,true")
        assert(rs2.mkString_("", ",", "") == "123,b")
        assert(rs3.mkString_("", ",", "") == "test,1.5")
      }

    test.unsafeRunSync()
  }

  test("mapK") {
    type E[A] = EitherT[IO, String, A]

    val test =
      for {
        out1 <- Ref[IO].of(Chain.empty[String])
        out2 <- Ref[IO].of(Chain.empty[String])
        out3 <- Ref[IO].of(Chain.empty[String])
        rs <- {
          implicit val console: Console[E] =
            new TestConsole[IO](out1, out2, out3)
              .mapK(EitherT.liftK[IO, String])
          program[E].value
        }
        rs1 <- out1.get
        rs2 <- out2.get
        rs3 <- out3.get
      } yield {
        assert(rs.right.get == "test")
        assert(rs1.mkString_("", ",", "") == "a,true")
        assert(rs2.mkString_("", ",", "") == "123,b")
        assert(rs3.mkString_("", ",", "") == "test,1.5")
      }

    test.value.unsafeRunSync()
  }

}
