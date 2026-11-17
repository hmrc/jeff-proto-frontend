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
import models.Journey1.*

case class ComparableValuationContext(
                                       id: Long,
                                       idx: String,
                                       name: String,
                                       label: String,
                                       origination: String,
                                       category: Classification,
                                       comparableType: Classification,
                                       comparableClass: Classification,
                                       framework: CodeLabel,
                                       documentationWebpage: String
                                     )

object ComparableValuationContext {
  implicit val format: OFormat[ComparableValuationContext] =
    Json.format[ComparableValuationContext]
}