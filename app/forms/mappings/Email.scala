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

import forms.mappings.EmailAddressValidation.isValidEmail
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.libs.json.{Json, OFormat}

final case class Email(
                        email: String,
                        confirmedEmail: String,
                       )

object Email extends Mappings {

  implicit val format: OFormat[Email] = Json.format[Email]

  def unapply(email: Email): Option[(String, String)] = Some(email.email, email.confirmedEmail)

  def form: Form[Email] =
    Form(
      mapping(
        "email" -> isValidEmail,
        "confirmedEmail" -> TextMatching("email", "Email does not match"),
      )(Email.apply)(Email.unapply)
    )
}