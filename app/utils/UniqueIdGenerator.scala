/*
 * Copyright 2026 HM Revenue & Customs
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

package utils

import java.security.SecureRandom

object UniqueIdGenerator {

  private[utils] val allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
  private val generator = new SecureRandom()
  private val referenceLength = 12
  private val groupSize = 4

  def generateId: String = {
    val raw = (1 to referenceLength)
      .map(_ => allowedChars(generator.nextInt(allowedChars.length)))
      .mkString

    format(raw)
  }

  def validateId(id: String): Either[Error, String] = {
    val sanitised = id.replaceAll("\\s", "").replaceAll("-", "").toUpperCase

    if (sanitised.length != referenceLength || !sanitised.forall(allowedChars.contains(_)))
      Left(new Error("Invalid reference"))
    else
      Right(format(sanitised))
  }

  def format(raw: String): String =
    raw.grouped(groupSize).mkString("-")

  def parse(formatted: String): String =
    formatted.replaceAll("-", "").toUpperCase
}
