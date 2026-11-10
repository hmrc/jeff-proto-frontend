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

package models.enquiry.product.person

import play.api.libs.json.{Json, OFormat}

case class Communications(
                           forenames:                Option[String],
                           surname:                  Option[String],
                           email:                    Option[String],
                           telephone_number:         Option[String],
                           CouncilTaxPayerFirstName: Option[String],
                           CouncilTaxPayerLastName:  Option[String],
                           CouncilTaxPayerEmail:     Option[String],
                           CouncilTaxPayerNumber:    Option[String],
                           CouncilTaxPayerContactPreference: Option[String],
                           CouncilTaxPayerContactAddress: Option[String],
                         )

object Communications{
  implicit val format: OFormat[Communications] =
    Json.format[Communications]
}
