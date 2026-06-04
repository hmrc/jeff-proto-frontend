/*
 * Copyright 2023 HM Revenue & Customs
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

package models.requests

import play.api.mvc.{Request, WrappedRequest}

import models.accounts.{GroupAccount, DetailedIndividualAccount}
sealed trait AuthenticatedRequest[A] extends Request[A] {
  val organisationAccount: GroupAccount

  val individualAccount: DetailedIndividualAccount

  val sessionId: String

  def organisationId: Long = organisationAccount.id

  def personId: Long = individualAccount.individualId

  def sid: String = sessionId
}

case class CcaAuthenticatedRequest[A](
      organisationAccount: GroupAccount,
      individualAccount: DetailedIndividualAccount,
      agentCode: Option[Long],
      request: Request[A],
      sessionId: String
) extends WrappedRequest[A](request) with AuthenticatedRequest[A]
