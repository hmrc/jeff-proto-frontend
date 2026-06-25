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

package forms.mappings

import play.api.data.Form
import play.api.data.Forms.{longNumber, mapping, optional, text}
import play.api.libs.json.{Json, OFormat}

case class Address(
                    addressUnitId: Option[Long],
                    line1: String,
                    line2: Option[String],
                    line3: Option[String],
                    line4: Option[String],
                    postcode: String
                  ) {
  override def toString: String =
    Seq(
      Some(line1),
      line2,
      line3,
      line4,
      Some(postcode)
    ).flatten.filter(_.nonEmpty).mkString(", ")
}

object Address {
  implicit val format: OFormat[Address] = Json.format[Address]

  val empty: Address =
    Address(
      addressUnitId = None,
      line1 = "",
      line2 = None,
      line3 = None,
      line4 = None,
      postcode = ""
    )

  private val optionalTrimmedText =
    optional(text).transform[Option[String]](
      _.map(_.trim).filter(_.nonEmpty),
      identity
    )

  def unapply(address: Address): Option[(Option[Long], String, Option[String], Option[String], Option[String], String)] =
    Some(address.addressUnitId, address.line1, address.line2, address.line3, address.line4, address.postcode)


  def form: Form[Address] =
    Form(
      mapping(
        "addressUnitId" -> optional(longNumber),
        "addressLine1" ->
          text
            .transform(_.trim, identity)
            .verifying("address.line1.error.required", _.nonEmpty)
            .verifying("address.line1.error.length", _.length <= 250),
        "addressLine2" ->
          optionalTrimmedText
            .verifying("address.line2.error.length", _.forall(_.length <= 250)),
        "addressLine3" ->
          optionalTrimmedText
            .verifying("address.line3.error.length", _.forall(_.length <= 250)),
        "addressLine4" ->
          optionalTrimmedText
            .verifying("address.line4.error.length", _.forall(_.length <= 250)),
        "postcode" ->
          text
            .transform(_.trim, identity)
            .verifying("address.postcode.error.required", _.nonEmpty)
            .verifying("address.postcode.error.length", _.length <= 20)
      )(Address.apply)(Address.unapply)
    )
}
