/*
 * Copyright (c) 2017-2018 The Typelevel Cats-effect Project Developers
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

package cats.effect.internals

import cats.effect.IO

import scala.concurrent.ExecutionContext

private[effect] object IOShift {
  /** Implementation for `IO.shift`. */
  def apply(ec: ExecutionContext): IO[Unit] =
    IO.Async(new IOForkedStart[Unit] {
      def apply(conn: IOConnection, cb: Callback.T[Unit]): Unit =
        ec.execute(new Tick(cb))
    })

  def shiftOn[A](shiftContext: ExecutionContext, targetEc: ExecutionContext, io: IO[A]): IO[A] =
     IOBracket[Unit, A](IOShift(shiftContext))(_ => io)((_, _) => IOShift(targetEc))

  private[internals] final class Tick(cb: Either[Throwable, Unit] => Unit)
    extends Runnable {
    def run() = cb(Callback.rightUnit)
  }
}
