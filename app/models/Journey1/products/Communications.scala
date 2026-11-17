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

package models.Journey1.products

import play.api.libs.json.{Json, OFormat}

case class Communications(
                           representativeEmail: String,
                           customerMobile: String,
                           CustomerEmail: String,
                           contactAddress: Option[String], //This will not be optional in release 2
                           telephone_number: Option[String], //This will not be optional in release 2
                           contactPreferences: Option[String],//This will not be optional in release 2
                           postal_address: Option[String],//This will not be optional in release 2
                         )

object Communications{
  implicit val format: OFormat[Communications] =
    Json.format[Communications]
}
