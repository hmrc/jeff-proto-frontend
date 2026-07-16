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

import java.time.Instant

case class StoredVMVProperties(
                                userId: String,
                                properties: PostcodeSearchResult,
                                createdAt: Instant = Instant.now()
                              )

object StoredVMVProperties {
  implicit val format: Format[StoredVMVProperties] = Json.format[StoredVMVProperties]
}


