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

package services

import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import controllers.internal.{CallbackBody, FailedCallbackBody, ReadyCallbackBody}
import models.upscan.UploadStatus
import services.UploadProgressTracker

import javax.inject.Inject
import scala.concurrent.Future

class UpscanCallbackDispatcher @Inject() (sessionStorage: UploadProgressTracker):

  private val allowedMimeTypes: Set[String] = Set(
    "application/pdf", // .pdf
    "image/png",
    "image/jpg",
    "image/jpeg",
  )
  
  
  def handleCallback(callback: CallbackBody)
                    (using HeaderCarrier): Future[Unit] =
    
    val uploadStatus =
      callback match
        case s: ReadyCallbackBody  =>
          if (allowedMimeTypes.contains(s.uploadDetails.fileMimeType)) {
            UploadStatus.UploadedSuccessfully(
              name = s.uploadDetails.fileName,
              mimeType = s.uploadDetails.fileMimeType,
              downloadUrl = s.downloadUrl,
              size = Some(s.uploadDetails.size),
              checksum    = s.uploadDetails.checksum
            )
          } else {
            throw new BadRequestException(s"Incorrect file type uploaded, preferred file type was: ${s.uploadDetails.fileMimeType}")
          }
        case _: FailedCallbackBody =>
          UploadStatus.Failed

    sessionStorage.registerUploadResult(callback.reference, uploadStatus)
