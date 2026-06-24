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
import play.api.data.Forms.{mapping, optional}
import play.api.libs.json.{Json, OFormat}

final case class MobilePhone(
                                 mobilePhone: String,
                               )

object MobilePhone extends Mappings {

  implicit val format: OFormat[MobilePhone] = Json.format[MobilePhone]

  def unapply(contactDetails: MobilePhone): Option[(String)] = Some(contactDetails.mobilePhone)

  def form: Form[MobilePhone] =
    Form(
      mapping(
        "mobileNumber" -> validatePhoneNumber,
      )(MobilePhone.apply)(MobilePhone.unapply)
    )
}