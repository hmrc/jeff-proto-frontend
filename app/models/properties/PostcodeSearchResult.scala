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

import play.api.libs.json.{Format, Json, OFormat}

import play.api.libs.json.{Format, Json, OFormat}

case class PostcodeSearchResult(
                                 results: Results
                               )

object PostcodeSearchResult:
  implicit val format: OFormat[PostcodeSearchResult] = Json.format[PostcodeSearchResult]

case class Results(
                    current_page: Int,
                    page_size: Int,
                    total_results: Int,
                    total_pages: Int,
                    has_next: Boolean,
                    has_previous: Boolean,
                    self: String,
                    next: Option[String],
                    prev: Option[String],
                    first: String,
                    last: String,
                    records: Seq[Record]
                  )

object Results:
  implicit val format: OFormat[Results] = Json.format[Results]

case class Record(
                   list: ValuationList,
                   list_entry: ListEntry
                 )

object Record:
  implicit val format: OFormat[Record] = Json.format[Record]

case class ValuationList(
                          id: String,
                          classification: Classification,
                          collection_authority: CollectionAuthority
                        )

object ValuationList:
  implicit val format: OFormat[ValuationList] = Json.format[ValuationList]

case class Classification(
                           code: String,
                           label: String
                         )

object Classification:
  implicit val format: OFormat[Classification] = Json.format[Classification]

case class CollectionAuthority(
                                ons_code: String,
                                ons_code_label: String
                              )

object CollectionAuthority:
  implicit val format: OFormat[CollectionAuthority] = Json.format[CollectionAuthority]

case class ListEntry(
                      relevant_property: RelevantProperty,
                      addresses: Addresses,
                      valuation: Valuation
                    )

object ListEntry:
  implicit val format: OFormat[ListEntry] = Json.format[ListEntry]

case class RelevantProperty(
                             id: String
                           )

object RelevantProperty:
  implicit val format: OFormat[RelevantProperty] = Json.format[RelevantProperty]

case class Addresses(
                      property_full_address: String
                    )

object Addresses:
  implicit val format: OFormat[Addresses] = Json.format[Addresses]

case class Valuation(
                      value: String
                    )

object Valuation:
  implicit val format: OFormat[Valuation] = Json.format[Valuation]