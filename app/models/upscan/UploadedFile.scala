/*
 * Copyright 2025 HM Revenue & Customs
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

package models.upscan

import play.api.libs.json.{Format, JsValue, Json}
import uk.gov.hmrc.govukfrontend.views.viewmodels.fileupload.FileUpload

import java.time.ZonedDateTime

case class UploadedFile(
                         ref: String,
                         upscanReference: String,
                         downloadUrl: String,
                         uploadTimestamp: ZonedDateTime,
                         fileSize: Int,
                         cargo: Option[JsValue]      = None, // data carried through, from and to host service
                         description: Option[String] = None,
                         previewUrl: Option[String]  = None
                       )

object UploadedFile {
  implicit val formats: Format[UploadedFile] = Json.format[UploadedFile]
}
