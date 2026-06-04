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

package models.Registration.frontend

import play.api.libs.json.{Format, Json, OFormat}

final case class RegisterRatepayerRequest(
                                           ratepayerCredId: Option[String] = None,
                                           userType: Option[RatepayerType] = None,
                                           agentStatus: Option[AgentStatus] = None,
                                           name: Option[String] = None,
                                           tradingName: Option[TradingName] = None,
                                           email: Option[String] = None,
                                           nino: Option[Nino] = None,
                                           contactNumber: Option[String] = None,
                                           secondaryNumber: Option[String] = None,
                                           address: Option[String] = None,
                                           trnReferenceNumber: Option[TRNReferenceNumber] = None,
                                           isRegistered: Option[Boolean] = Some(false),
                                           recoveryId: Option[String] = None
                                         )

object RegisterRatepayerRequest:
  implicit val format: OFormat[RegisterRatepayerRequest] = Json.format
