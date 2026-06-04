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

package models.analytics

import models.requests.AuthenticatedRequest

case class GoogleAnalyticsUserData(personId: String, loggedIn: String, ccaAgent: String)

object GoogleAnalyticsUserData {
  def apply(personId: Long, ccaAgent: Boolean): GoogleAnalyticsUserData =
    GoogleAnalyticsUserData(personId = personId.toString, loggedIn = "Yes", ccaAgent = if (ccaAgent) "Yes" else "No")

  def apply(req: AuthenticatedRequest[_]): GoogleAnalyticsUserData =
    GoogleAnalyticsUserData(req.personId, req.organisationAccount.isAgent)
}
