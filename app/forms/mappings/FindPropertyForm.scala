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

import forms.mappings.Constraints
import models.properties.Postcode
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.libs.json.{Json, OFormat}

case class FindPropertyForm(postcode: Postcode, propertyName: Option[String]) {
  override def toString: String = Seq(propertyName, postcode.value).mkString(",")
}

object FindPropertyForm extends Constraints {
  implicit val format: OFormat[FindPropertyForm] = Json.format[FindPropertyForm]
  private val postcode = "postcode-value"
  private val propertyName = "property-name-value"

  def unapply(findAProperty: FindPropertyForm): Option[(Postcode, Option[String])] = Some((findAProperty.postcode, findAProperty.propertyName))

  def form: Form[FindPropertyForm] =
    Form(
      mapping(
        postcode -> text()
          .transform[String](_.strip(), identity)
          .verifying(
            firstError(
              isNotEmpty(postcode, "findAProperty.postcode.empty.error"),
              regexp(postcodeRegexPattern.pattern(), "findAProperty.postcode.invalid.error")
            )
          )
          .transform[Postcode](Postcode.apply, _.value),
        propertyName -> optional(text
          .verifying(maxLength(100, "findAProperty.property.invalid.error")))
      )(FindPropertyForm.apply)(FindPropertyForm.unapply)
    )
}
