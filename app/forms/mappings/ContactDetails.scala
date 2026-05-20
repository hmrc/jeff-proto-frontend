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
import models.Registration.frontend.Address

import javax.inject.Inject
import play.api.data.Form
import play.api.data.Forms.{mapping, optional, text}
import play.api.libs.json.{Json, OFormat}
import utils.PhoneNumberValidation.{maximumLength, phoneRegex}

final case class ContactDetails(
                                 // address: Address,
                                 email: String,
                                 confirmedEmail: String,
                                 phone: String,
                                 mobilePhone: String,
                                 tradingName: Option[String],
                                 //  selectedAddress: Option[String] = None
                               )

object ContactDetails extends Mappings {

  implicit val format: OFormat[ContactDetails] = Json.format[ContactDetails]

  def unapply(contactDetails: ContactDetails): Option[(String, String, String, String, Option[String])] = Some((contactDetails.email, contactDetails.confirmedEmail, contactDetails.phone, contactDetails.mobilePhone, contactDetails.tradingName))

  def form: Form[ContactDetails] =
    Form(
      mapping(
        "email" -> isValidEmail,
        "confirmedEmail" -> TextMatching("email", "Email does not match"),
        "phone" -> validatePhoneNumber,
        "mobilePhone" -> validatePhoneNumber,
        "tradingName" -> optional(textWithLimit(maxLength = 45)),
        //  "selectedAddress" -> Option[String] = None
      )(ContactDetails.apply)(ContactDetails.unapply)
    )
}