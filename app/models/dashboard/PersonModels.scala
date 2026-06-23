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

package models.dashboard

import models.dashboard.ProtoData.Metadata
import play.api.libs.json.{JsValue, Json, OFormat}

case class NameData(
                     title_common: Option[String],
                     title_uncommon: Option[String],
                     forenames: Option[String],
                     surname: Option[String],
                     post_nominals: Option[String],
                     corporate_name: Option[String],
                     crown_name: Option[String],
                     known_as: Option[String]
                   )

object NameData {
  implicit val format: OFormat[NameData] = Json.format[NameData]
}


case class Communications(
                           postal_address: Option[String],
                           telephone_number: Option[String],
                           email: Option[String]
                         )

object Communications {
  implicit val format: OFormat[Communications] = Json.format[Communications]
}

case class PersonItem(
                       id: Option[Long],
                       idx: String,
                       name: String,
                       label: String,
                       description: String,
                       origination: Option[String],
                       termination: Option[String],
                       category: CodeMeaning,
                       `type`: CodeMeaning,
                       `class`: CodeMeaning,
                       data: PersonItemData,
                       protodata: List[ProtoData],
                       metadata: Metadata,
                       compartments: Map[String, String],
                       items: List[JsValue]
                     )

object PersonItem {
  implicit val format: OFormat[PersonItem] = Json.format[PersonItem]
}

case class PersonItemData(
                           foreign_ids: List[ForeignId],
                           foreign_names: List[ForeignId],
                           foreign_labels: List[ForeignId],
                           names: NameData,
                           communications: Communications
                         )

object PersonItemData {
  implicit val format: OFormat[PersonItemData] = Json.format[PersonItemData]
}

case class Person(
                   id: Option[Long],
                   idx: String,
                   name: String,
                   label: String,
                   description: String,
                   origination: Option[String],
                   termination: Option[String],
                   category: CodeMeaning,
                   `type`: CodeMeaning,
                   `class`: CodeMeaning,
                   data: PersonItemData,
                   protodata: List[ProtoData],
                   metadata: Metadata,
                   compartments: Map[String, String],
                   items: List[PersonItem]
                 )

object Person {
  implicit val format: OFormat[Person] = Json.format[Person]
}

case class Persons(persons: List[Person])
object Persons {
  implicit val format:OFormat[Persons] = Json.format[Persons]
}

