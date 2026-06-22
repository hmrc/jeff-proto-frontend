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

import play.api.libs.json.{Json, OFormat}

case class CodeMeaning(
                        code: Option[String],
                        meaning: Option[String]
                      )

object CodeMeaning:
  implicit val format: OFormat[CodeMeaning] = Json.format

case class ForeignId(
                      system: String,
                      location: String,
                      value: String
                    )

object ForeignId:
  implicit val format: OFormat[ForeignId] = Json.format[ForeignId]

case class ProtoData(
                      mime_type: String,
                      label: String,
                      is_pointer: Boolean,
                      pointer: String,
                      data: String
                    )

object ProtoData:
  implicit val format: OFormat[ProtoData] = Json.format[ProtoData]

  case class MetadataStage(
                            selecting: Map[String, String] = Map.empty,
                            filtering: Map[String, String] = Map.empty,
                            supplementing: Map[String, String] = Map.empty,
                            recontextualising: Map[String, String] = Map.empty,
                            readying: Map[String, String] = Map.empty,
                            assuring: Map[String, String] = Map.empty,
                            signing: Map[String, String] = Map.empty,
                            encrypting: Map[String, String] = Map.empty,
                            sending: Map[String, String] = Map.empty,
                            receiving: Map[String, String] = Map.empty,
                            decrypting: Map[String, String] = Map.empty,
                            verifying: Map[String, String] = Map.empty,
                            dropping: Map[String, String] = Map.empty,
                            restoring: Map[String, String] = Map.empty,
                            inserting: Map[String, String] = Map.empty
                          )

  object MetadataStage:
    implicit val format: OFormat[MetadataStage] = Json.format[MetadataStage]

  case class Metadata(
                       sending: SendingMetadata,
                       receiving: ReceivingMetadata
                     )

  object Metadata:
    implicit val format: OFormat[Metadata] = Json.format[Metadata]


  case class SendingMetadata(
                              extracting: MetadataStage,
                              transforming: MetadataStage,
                              loading: MetadataStage
                            )

  object SendingMetadata:
    implicit val format: OFormat[SendingMetadata] = Json.format[SendingMetadata]


  case class ReceivingMetadata(
                                unloading: MetadataStage,
                                transforming: MetadataStage,
                                storing: MetadataStage
                              )

  object ReceivingMetadata:
    implicit val format: OFormat[ReceivingMetadata] = Json.format[ReceivingMetadata]