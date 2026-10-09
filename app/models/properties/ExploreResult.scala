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


import play.api.libs.functional.syntax.*
import play.api.libs.json.*


case class ExploreResult(
                          results: ExploreResults
                        )

object ExploreResult:
  implicit val format: OFormat[ExploreResult] =
    Json.format[ExploreResult]

case class ExploreResults(
                           records: Seq[ExploreRecord]
                         )

object ExploreResults:
  implicit val format: OFormat[ExploreResults] =
    Json.format[ExploreResults]

case class ExploreRecord(
                          data: ExploreData
                        )

object ExploreRecord:
  implicit val format: OFormat[ExploreRecord] =
    Json.format[ExploreRecord]

case class ExploreData(
                        list: ValuationList,
                        list_entry: ListEntry
                      )

object ExploreData:
  implicit val format: OFormat[ExploreData] =
    Json.format[ExploreData]

case class Id(
               value: Option[String]
             )

object Id:
  implicit val format: OFormat[Id] =
    Json.format[Id]

case class Classification(
                           code: Option[String],
                           meaning: Option[String]
                         )

object Classification:
  implicit val format: OFormat[Classification] =
    Json.format[Classification]

case class Country(
                    code: Option[String],
                    label: Option[String]
                  )

object Country:
  implicit val format: OFormat[Country] =
    Json.format[Country]

case class CollectionAuthority(
                                code: Option[String],
                                label: Option[String]
                              )

object CollectionAuthority:
  implicit val format: OFormat[CollectionAuthority] =
    Json.format[CollectionAuthority]

case class InforcementPeriod(
                              commencement_date: Option[String],
                              expiration_date: Option[String]
                            )

object InforcementPeriod:
  implicit val format: OFormat[InforcementPeriod] =
    Json.format[InforcementPeriod]

case class ListAdministration(
                               compilation_date: Option[String],
                               valuation_date: Option[String],
                               total_of_all_valuations: Option[String]
                             )

object ListAdministration:
  implicit val format: OFormat[ListAdministration] =
    Json.format[ListAdministration]

case class ValuationList(
                          id: Id,
                          classification: Classification,
                          country: Option[Country],
                          collection_authority: CollectionAuthority,
                          inforcement_period: Option[InforcementPeriod],
                          administration: Option[ListAdministration]
                        )

object ValuationList:

  implicit val format: OFormat[ValuationList] = (
    (__ \ "id").format[Id] and
      (__ \ "class").format[Classification] and
      (__ \ "country").formatNullable[Country] and
      (__ \ "collection_authority").format[CollectionAuthority] and
      (__ \ "inforcement_period").formatNullable[InforcementPeriod] and
      (__ \ "administration").formatNullable[ListAdministration]
    )(ValuationList.apply, Tuple.fromProductTyped)

case class DesignatedPerson(
                             name: Option[String],
                             address: Option[String],
                             company_number: Option[String]
                           )

object DesignatedPerson:
  implicit val format: OFormat[DesignatedPerson] =
    Json.format[DesignatedPerson]

case class RelevantProperty(
                             vos_property_id: Option[String]
                           )

object RelevantProperty:
  implicit val format: OFormat[RelevantProperty] =
    Json.format[RelevantProperty]

case class Use(
                description: Option[String],
                composite_ind: Option[String],
                part_exempt_ind: Option[String]
              )

object Use:
  implicit val format: OFormat[Use] =
    Json.format[Use]

case class Method(
                   code: Option[String],
                   meaning: Option[String]
                 )

object Method:
  implicit val format: OFormat[Method] =
    Json.format[Method]

case class Valuation(
                      value: Option[String],
                      method: Option[Method],
                      previous: Option[String]
                    )

object Valuation:
  implicit val format: OFormat[Valuation] =
    Json.format[Valuation]

case class Period(
                   effective_from_date: Option[String],
                   effective_to_date: Option[String]
                 )

object Period:
  implicit val format: OFormat[Period] =
    Json.format[Period]

case class Administration(
                           alteration_date: Option[String],
                           alteration_seq_no: Option[String],
                           entry_seq_no: Option[String],
                           judicially_ordered_by: Option[String],
                           transitionally_certified: Option[String],
                           collection_authority_ref: Option[String]
                         )

object Administration:
  implicit val format: OFormat[Administration] =
    Json.format[Administration]

case class Workflow(
                     creating_job_id: Option[String]
                   )

object Workflow:
  implicit val format: OFormat[Workflow] =
    Json.format[Workflow]

case class Address(
                    full: Option[String],
                    first_line: Option[String],
                    postcode: Option[String],
                    known_as: Option[String]
                  )

object Address:
  implicit val format: OFormat[Address] =
    Json.format[Address]

case class PropertyWorkflow(
                             improvement_ind: Option[String]
                           )

object PropertyWorkflow:
  implicit val format: OFormat[PropertyWorkflow] =
    Json.format[PropertyWorkflow]

case class Property(
                     id: Option[Id],
                     collection_authority_ref: Option[String],
                     address: Option[Address],
                     workflow: Option[PropertyWorkflow]
                   )

object Property:
  implicit val format: OFormat[Property] =
    Json.format[Property]

case class ListEntry(
                      id: Option[Id],
                      designated_person: Option[DesignatedPerson],
                      relevant_property: Option[RelevantProperty],
                      use: Option[Use],
                      valuation: Valuation,
                      period: Option[Period],
                      administration: Option[Administration],
                      workflow: Option[Workflow],
                      property: Option[Property]
                    )

object ListEntry:
  implicit val format: OFormat[ListEntry] =
    Json.format[ListEntry]
