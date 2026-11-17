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
import play.api.data.Forms.{list, mapping, optional, text}
import play.api.libs.json.{Json, OFormat}

final case class ChallengeContactDetails(
                                          firstName: String,
                                          lastName: String,
                                          emailAddress: String,
                                          telephoneNumber: String,
                                          addressLine1: String,
                                          addressLine2: Option[String],
                                          townOrCity: String,
                                          county: Option[String],
                                          postcode: String,
                                          contactPreferences: List[String]
                                        )

object ChallengeContactDetails {

  implicit val format: OFormat[ChallengeContactDetails] =
    Json.format[ChallengeContactDetails]

  private val validContactPreferences: Set[String] =
    Set(
      "email",
      "phone",
      "post"
    )

  def form(): Form[ChallengeContactDetails] =
    Form(
      mapping(
        "firstName" -> text
          .verifying(
            "contactDetails.firstName.error.required",
            _.trim.nonEmpty
          )
          .verifying(
            "contactDetails.firstName.error.length",
            _.length <= 100
          ),

        "lastName" -> text
          .verifying(
            "contactDetails.lastName.error.required",
            _.trim.nonEmpty
          )
          .verifying(
            "contactDetails.lastName.error.length",
            _.length <= 100
          ),

        "emailAddress" -> text
          .verifying(
            "contactDetails.emailAddress.error.required",
            _.trim.nonEmpty
          ),

        "telephoneNumber" -> text
          .verifying(
            "contactDetails.telephoneNumber.error.length",
            _.length <= 50
          ),

        "addressLine1" -> text
          .verifying(
            "contactDetails.addressLine1.error.required",
            _.trim.nonEmpty
          )
          .verifying(
            "contactDetails.addressLine1.error.length",
            _.length <= 100
          ),

        "addressLine2" -> optional(
          text.verifying(
            "contactDetails.addressLine2.error.length",
            _.length <= 100
          )
        ),

        "townOrCity" -> text
          .verifying(
            "contactDetails.townOrCity.error.required",
            _.trim.nonEmpty
          )
          .verifying(
            "contactDetails.townOrCity.error.length",
            _.length <= 100
          ),

        "county" -> optional(
          text.verifying(
            "contactDetails.county.error.length",
            _.length <= 100
          )
        ),

        "postcode" -> text
          .verifying(
            "contactDetails.postcode.error.required",
            _.trim.nonEmpty
          )
          .verifying(
            "contactDetails.postcode.error.length",
            _.length <= 10
          ),

        "contactPreferences" -> list(text)
          .verifying(
            "contactDetails.contactPreferences.error.required",
            _.nonEmpty
          )
          .verifying(
            "contactDetails.contactPreferences.error.invalid",
            _.forall(validContactPreferences.contains)
          )
      )(
        (
          firstName,
          lastName,
          emailAddress,
          telephoneNumber,
          addressLine1,
          addressLine2,
          townOrCity,
          county,
          postcode,
          contactPreferences
        ) =>
          ChallengeContactDetails(
            firstName = firstName,
            lastName = lastName,
            emailAddress = emailAddress,
            telephoneNumber = telephoneNumber,
            addressLine1 = addressLine1,
            addressLine2 = addressLine2,
            townOrCity = townOrCity,
            county = county,
            postcode = postcode,
            contactPreferences = contactPreferences
          )
      )(
        contactDetails =>
          Some(
            (
              contactDetails.firstName,
              contactDetails.lastName,
              contactDetails.emailAddress,
              contactDetails.telephoneNumber,
              contactDetails.addressLine1,
              contactDetails.addressLine2,
              contactDetails.townOrCity,
              contactDetails.county,
              contactDetails.postcode,
              contactDetails.contactPreferences
            )
          )
      )
    )
}