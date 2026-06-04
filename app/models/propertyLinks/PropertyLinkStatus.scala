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

package models.propertyLinks

import play.api.libs.json.Format
import utils.JsonUtils


object PropertyLinkStatus extends Enumeration {
  type PropertyLinkStatus = Value

  val APPROVED: PropertyLinkStatus.Value = Value("APPROVED")
  val PENDING: PropertyLinkStatus.Value = Value("PENDING")
  val DECLINED: PropertyLinkStatus.Value = Value("DECLINED")
  val REVOKED: PropertyLinkStatus.Value = Value("REVOKED")

  implicit val format: Format[PropertyLinkStatus] = JsonUtils.enumFormat(PropertyLinkStatus)
}
