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

package models.Journey1

import play.api.libs.json.{Json, OFormat}
import models.Journey1.premises.*
import models.Journey1.products.ProductContext
import models.Journey1.proposition.*

//This model covers everything that can be submitted from the enquiries journeys.

case class SubmissionRequest(
                              process: ProcessContext,
                              product: ProductContext,
                              premises: Seq[Premise], //Complete
                              proposition: Seq[Proposition] //Complete
                            )

object SubmissionRequest {
  implicit val format: OFormat[SubmissionRequest] =
    Json.format[SubmissionRequest]
}