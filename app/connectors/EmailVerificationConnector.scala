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

package connectors

import config.FrontendAppConfig
import models.ErrorResponse
import models.emailVerification.{EmailVerificationRequest, EmailVerificationResponse, GetVerificationStatusResponse, VerificationDetails}
import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.http.Status.{BAD_REQUEST, CREATED, NOT_FOUND, OK}
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.writeableOf_JsValue
import play.api.Logging

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class EmailVerificationConnector @Inject()(http: HttpClientV2, appConfig: FrontendAppConfig)(implicit ec: ExecutionContext) extends Logging {
  def startEmailVerification(model: EmailVerificationRequest)(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, EmailVerificationResponse]] = {
    val startEmailVerificationUrl = appConfig.startEmailVerificationJourneyUrl
    http.post(url"$startEmailVerificationUrl")
      .withBody(Json.toJson(model))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case CREATED | OK => response.json.validate[EmailVerificationResponse] match {
            case JsSuccess(value, _) => Right(value)
            case JsError(errors) =>
              Left(ErrorResponse(BAD_REQUEST, s"Json Validation Errors: $errors"))
          }
          case _ =>
            Left(ErrorResponse(response.status, response.body))
        }
      } recover {
      case _ =>
        Left(ErrorResponse(Status.INTERNAL_SERVER_ERROR, s"Call to email verification failed"))
    }
  }

  import uk.gov.hmrc.http.NotFoundException

  def getEmailVerificationStatus(
                                  verificationDetails: VerificationDetails
                                )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, GetVerificationStatusResponse]] = {

    http
      .get(url"${appConfig.getVerifiedEmailsUrl(verificationDetails.credId)}")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            response.json.validate[GetVerificationStatusResponse] match {
              case JsSuccess(value, _) =>
                Right(value)

              case JsError(errors) =>
                Left(
                  ErrorResponse(
                    BAD_REQUEST,
                    s"Json Validation Errors: $errors"
                  )
                )
            }

          case _ =>
            Left(
              ErrorResponse(
                response.status,
                response.body
              )
            )
        }
      }
      .recover {
        case _: NotFoundException =>
          Right(GetVerificationStatusResponse(Nil))

        case _ =>
          Left(
            ErrorResponse(
              Status.INTERNAL_SERVER_ERROR,
              "Call to email verification failed"
            )
          )
      }
  }
}