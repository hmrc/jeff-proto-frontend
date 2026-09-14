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

import org.bson.types.ObjectId
import play.api.http.Status.CREATED
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.mvc.Results.BadRequest
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import config.FrontendAppConfig
import models.*
import models.sdes.*
import models.upscan.*
import repositories.{FileUploadRepo, ProposalRepo}
import utils.UniqueIdGenerator
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient
import uk.gov.hmrc.objectstore.client.{Path, RetentionPeriod, Sha256Checksum}

import java.net.URL
import java.time.ZonedDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UploadProgressTracker @Inject()(
                                       repository: FileUploadRepo,
                                       appConfig: FrontendAppConfig,
                                       osClient  : PlayObjectStoreClient,
                                       proposalRepo: ProposalRepo,
                                       httpClient: HttpClientV2,
                                     )(using
                                       ExecutionContext
                                     ):

  def requestUpload(uploadId: UploadId, fileReference: Reference): Future[Unit] =
    repository.insert(UploadDetails(ObjectId.get(), uploadId, fileReference, UploadStatus.InProgress))

  def registerUploadResult(fileReference: Reference, uploadStatus: UploadStatus)
                          (using hc: HeaderCarrier): Future[Unit] =
    for
      _ <- repository.updateStatus(fileReference, uploadStatus)
    yield
      ()

  def getUploadResult(id: UploadId): Future[Option[UploadStatus]] =
    repository
      .findByUploadId(id)
      .map(_.map(_.status))


  private def createClientAuthToken(): Future[Unit] = {
    httpClient
      .post(url"${appConfig.internalAuthService}/test-only/token")(HeaderCarrier())
      .withBody(
        Json.obj(
          "token" -> appConfig.internalAuthToken,
          "principal" -> appConfig.appName,
          "permissions" -> Seq(
            Json.obj(
              "resourceType" -> "object-store",
              "resourceLocation" -> "jeff-proto-frontend",
              "actions" -> List("READ", "WRITE", "DELETE")
            ),
            Json.obj(
              "resourceType" -> "business-rates-bill",
              "resourceLocation" -> "*",
              "actions" -> List("*")
            )
          )
        )
      )
      .execute
      .flatMap { response =>
        if (response.status == CREATED) {
          Future.successful("")
        } else {
          Future.failed(new RuntimeException("Unable to initialise internal-auth token"))
        }
      }
  }


  def transferToObjectStore(
                             credId: String,
                             downloadUrl: URL,
                             mimeType: String,
                             checksum: String,
                             evidenceDocument: String,
                             fileReference: Reference,
                             uploadStatus: UploadStatus,
                             appConfig: FrontendAppConfig
                           )(using hc: HeaderCarrier): Future[Unit] = {
    val fileLocation = Path.File(s"${fileReference.value}/${evidenceDocument}")
    val contentSha256 = Sha256Checksum.fromHex(checksum)
    createClientAuthToken()
    osClient
      .uploadFromUrl(
        from = url"${downloadUrl}",
        to = fileLocation,
        retentionPeriod = RetentionPeriod.OneDay,
        contentType = Some(mimeType),
        contentSha256 = Some(contentSha256)
      )(using hc.withExtraHeaders("Authorization" -> appConfig.internalAuthToken))
      .transformWith {
        case scala.util.Failure(exception) =>
          exception.printStackTrace()
          Future.successful(BadRequest(s"Failure to store object because of $exception"))
        case scala.util.Success(objectWithMD5) =>
          osClient
            .presignedDownloadUrl(path = fileLocation)
            .transformWith {
              case scala.util.Failure(exception) =>
                exception.printStackTrace()
                Future.successful(
                  BadRequest(s"Failure to get pre-signed URL to $fileLocation because of $exception")
                )
              case scala.util.Success(presignedDownloadUrl) =>
                val ref = UniqueIdGenerator.generateId
                val uploadedFile =
                  UploadedFile(
                    ref = ref,
                    upscanReference = fileLocation.asUri,
                    downloadUrl = presignedDownloadUrl.downloadUrl.toExternalForm(),
                    uploadTimestamp = ZonedDateTime.now(),
                    fileSize = presignedDownloadUrl.contentLength.toInt
                  )
                Future.successful(
                  for {
                    referenceNumberInsert <- proposalRepo.insertReferenceNumber(credId, ref)
                    success <- referenceNumberInsert match {
                      case Some(result) => Future.successful(result)
                      case None => Future.failed(new Exception(s"Could not save reference for credId: ${credId}"))
                    }
                    uploadFile <-
                      proposalRepo.insertUploadedFile(
                        credId,
                        File(
                          recipientOrSender = Some("jeff-proposal"),
                          name = ref,
                          location = Some(uploadedFile.downloadUrl),
                          Checksum(appConfig.sdesChecksumAlgorithm, checksum),
                          size = presignedDownloadUrl.contentLength.toInt,
                          properties = List.empty
                        )
                      )
                    success <- referenceNumberInsert match {
                      case Some(result) =>
                        Future.successful(result)
                      case None => Future.failed(new Exception(s"Could not save reference for credId: ${credId}"))
                    }
                  } yield success
                )
            }
      }
  }