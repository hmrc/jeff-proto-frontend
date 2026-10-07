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

package models.properties


import play.api.libs.json.{Format, Json}

import java.time.LocalDate

case class PropertyValuation(
                              assessmentRef: Long,
                              assessmentStatus: String,
                              rateableValue: Option[BigDecimal],
                              scatCode: Option[String],
                              effectiveDate: LocalDate,
                              currentFromDate: LocalDate,
                              currentToDate: Option[LocalDate],
                              descriptionText: String,
                              listYear: String,
                              primaryDescription: String,
                              allowedActions: List[String],
                              listType: String,
                            )

object PropertyValuation {

  implicit val format:Format[PropertyValuation] = Json.format[PropertyValuation]

}