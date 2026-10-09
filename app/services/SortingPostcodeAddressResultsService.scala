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

package services

import javax.inject.{Inject, Singleton}
import models.properties.Record

@Singleton
class SortingPostcodeAddressResultsService @Inject()() {

  def sort(records: Seq[Record], sortBy: String): Seq[Record] = {

    def normalise(value: Option[String]): String =
      value.getOrElse("").toLowerCase.trim

    def safeAddress(record: Record): String =
      normalise(
        record.list_entry.property
          .flatMap(_.address)
          .flatMap(_.full)
      )

    def safePropertyReference(record: Record): Long =
      record.list_entry.relevant_property
        .flatMap(_.vos_property_id)
        .flatMap(_.trim.toLongOption)
        .getOrElse(0L)

    def safeListReference(record: Record): String =
      normalise(record.list.id.value)

    def safeClassificationCode(record: Record): String =
      normalise(record.list.classification.code)

    def safeClassificationLabel(record: Record): String =
      normalise(record.list.classification.meaning)

    def safeLocalAuthorityCode(record: Record): String =
      normalise(record.list.collection_authority.code)

    def safeLocalAuthorityName(record: Record): String =
      normalise(record.list.collection_authority.label)

    def safeValuation(record: Record): String =
      normalise(record.list_entry.valuation.value)

    sortBy match {
      case "AddressASC" =>
        records.sortBy(safeAddress)

      case "AddressDESC" =>
        records.sortBy(safeAddress)(Ordering[String].reverse)

      case "PropertyReferenceASC" =>
        records.sortBy(safePropertyReference)

      case "PropertyReferenceDESC" =>
        records.sortBy(safePropertyReference)(Ordering[Long].reverse)

      case "ListReferenceASC" =>
        records.sortBy(safeListReference)

      case "ListReferenceDESC" =>
        records.sortBy(safeListReference)(Ordering[String].reverse)

      case "ClassificationCodeASC" =>
        records.sortBy(safeClassificationCode)

      case "ClassificationCodeDESC" =>
        records.sortBy(safeClassificationCode)(Ordering[String].reverse)

      case "ClassificationLabelASC" =>
        records.sortBy(safeClassificationLabel)

      case "ClassificationLabelDESC" =>
        records.sortBy(safeClassificationLabel)(Ordering[String].reverse)

      case "LocalAuthorityCodeASC" =>
        records.sortBy(safeLocalAuthorityCode)

      case "LocalAuthorityCodeDESC" =>
        records.sortBy(safeLocalAuthorityCode)(Ordering[String].reverse)

      case "LocalAuthorityNameASC" =>
        records.sortBy(safeLocalAuthorityName)

      case "LocalAuthorityNameDESC" =>
        records.sortBy(safeLocalAuthorityName)(Ordering[String].reverse)

      case "ValuationASC" =>
        records.sortBy(safeValuation)

      case "ValuationDESC" =>
        records.sortBy(safeValuation)(Ordering[String].reverse)

      case _ =>
        records.sortBy(safeAddress)
    }
  }
}
