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

import play.api.libs.json.{Json, OFormat}

case class PostcodeSearchResult(
                                 results: Results
                               )

object PostcodeSearchResult:
  implicit val format: OFormat[PostcodeSearchResult] =
    Json.format[PostcodeSearchResult]

case class Results(
                    current_page: Option[Int],
                    page_size: Option[Int],
                    total_results: Option[Int],
                    total_pages: Option[Int],
                    has_next: Option[Boolean],
                    has_previous: Option[Boolean],
                    self: Option[String],
                    next: Option[String],
                    prev: Option[String],
                    first: Option[String],
                    last: Option[String],
                    records: Seq[Record]
                  )

object Results:
  implicit val format: OFormat[Results] =
    Json.format[Results]

case class Record(
                   list: ValuationList,
                   list_entry: ListEntry
                 )

object Record:
  implicit val format: OFormat[Record] =   Json.format[Record]
  
