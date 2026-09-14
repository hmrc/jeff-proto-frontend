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

package models

import play.api.data.{FieldMapping, Form, FormError}
import play.api.data.Forms.of
import play.api.data.format.Formatter

import javax.inject.Inject

class UploadForm @Inject() {

  def apply(): Form[String] = {
    Form(
      "file" -> text("uploadFile.error.noFileSelected")
    )
  }

  def text(errorKey: String = "error.required", args: Seq[Any] = Seq.empty): FieldMapping[String] = {

    of(stringFormatter(errorKey, args))
  }

  def stringFormatter(errorKey: String, args: Seq[Any] = Seq.empty): Formatter[String] =
    new Formatter[String] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
        data.get(key) match {
          case None => Left(Seq(FormError(key, errorKey, args)))
          case Some(s) =>
            val sanitisedInput = s.replace("\u0000", "").trim
            if (sanitisedInput.isEmpty) {
              Left(Seq(FormError(key, errorKey, args)))
            } else {
              Right(sanitisedInput)
            }
        }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value.trim)
    }
}

