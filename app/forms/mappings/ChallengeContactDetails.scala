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
import play.api.data.Forms.list
import play.api.data.Forms.mapping
import play.api.data.Forms.text

import javax.inject.Inject

final case class ChallengeContactDetails(
                                          firstName: String,
                                          lastName: String,
                                          emailAddress: String,
                                          telephoneNumber: String,
                                          contactPreferences: List[String]
                                        )

object ChallengeContactDetails {

  val validContactPreferences =
    Set(
      "email",
      "phone",
      "textMessage"
    )


  def unapply(contactDetailsFormProvider: ChallengeContactDetails): Some[(String, String, String, String, List[String])] = Some(contactDetailsFormProvider.firstName, contactDetailsFormProvider.lastName, contactDetailsFormProvider.telephoneNumber, contactDetailsFormProvider.emailAddress, contactDetailsFormProvider.contactPreferences) 
  
  def form(): Form[ChallengeContactDetails] =
    Form(
      mapping(
        "firstName" -> text
          .verifying(
            "contactDetails.firstName.error.required",
            value => value.trim.nonEmpty
          )
          .verifying(
            "contactDetails.firstName.error.length",
            value => value.length <= 100
          ),

        "lastName" -> text
          .verifying(
            "contactDetails.lastName.error.required",
            value => value.trim.nonEmpty
          )
          .verifying(
            "contactDetails.lastName.error.length",
            value => value.length <= 100
          ),

        "emailAddress" -> text
          .verifying(
            "contactDetails.emailAddress.error.required",
            value => value.trim.nonEmpty
          ),

        "telephoneNumber" -> text
          .verifying(
            "contactDetails.telephoneNumber.error.length",
            value => value.length <= 50
          ),

        "contactPreferences" -> list(text)
          .verifying(
            "contactDetails.contactPreferences.error.required",
            values => values.nonEmpty
          )
          .verifying(
            "contactDetails.contactPreferences.error.invalid",
            values => values.forall(validContactPreferences.contains)
          )
      )(ChallengeContactDetails.apply)(ChallengeContactDetails.unapply)
    )
}