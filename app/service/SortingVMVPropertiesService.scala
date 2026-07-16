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

import models.properties.Record

import javax.inject.{Inject, Singleton}

@Singleton
class SortingPostcodeAddressResultsService @Inject()() {

  def sort(records: List[Record], sortBy: String): List[Record] = {

    def safeAddress(record: Record): String =
      record.list_entry.addresses.property_full_address.toLowerCase

    def safePropertyReference(record: Record): String =
      record.list_entry.relevant_property.id.toLowerCase

    def safeListReference(record: Record): String =
      record.list.id.toLowerCase

    def safeClassificationCode(record: Record): String =
      record.list.classification.code.toLowerCase

    def safeClassificationLabel(record: Record): String =
      record.list.classification.label.toLowerCase

    def safeLocalAuthorityCode(record: Record): String =
      record.list.collection_authority.ons_code.toLowerCase

    def safeLocalAuthorityName(record: Record): String =
      record.list.collection_authority.ons_code_label.toLowerCase

    def safeValuation(record: Record): String =
      record.list_entry.valuation.value.toLowerCase

    sortBy match {

      case "AddressASC" =>
        records.sortBy(safeAddress)

      case "AddressDESC" =>
        records.sortBy(safeAddress)(Ordering[String].reverse)

      case "PropertyReferenceASC" =>
        records.sortBy(safePropertyReference)

      case "PropertyReferenceDESC" =>
        records.sortBy(safePropertyReference)(Ordering[String].reverse)

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
        // Default sort: Address A → Z
        records.sortBy(safeAddress)
    }
  }
}