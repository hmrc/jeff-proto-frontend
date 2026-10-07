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
import forms.FindAPropertyBridgeForm
import models.Registration.frontend.RegisterRatepayerRequest
import models.dashboard.Persons
import models.properties.{ExploreResult, PostcodeSearchResult}

import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import java.net.URI
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.writeableOf_JsValue
import play.api.i18n.Lang.logger
import play.api.http.Status.*
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse

import scala.util.control.NonFatal


class BridgeIntegrationConnector @Inject()(
                                            http: HttpClientV2,
                                            appConfig: FrontendAppConfig
                                          )(implicit ec: ExecutionContext) {

  private def uri(path: String) = new URI(s"${appConfig.bridgeIntegration}/bridge-integration/$path")

  def registerRatePayer(ratepayerRegistration: RegisterRatepayerRequest)
                       (implicit hc: HeaderCarrier): Future[Boolean] = {

    http.post(uri(s"register-ratepayer/123456789567").toURL)
      .withBody(Json.toJson(ratepayerRegistration))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK => true
          case NOT_FOUND =>
            logger.warn("Ratepayer not found")
            false
          case BAD_REQUEST =>
            logger.warn("Invalid register ratepayer request")
            false
          case BAD_GATEWAY =>
            logger.error("Upstream service unavailable")
            false
          case INTERNAL_SERVER_ERROR =>
            logger.error(s"Server error: ${response.body}")
            false
          case other =>
            logger.error(s"Unexpected response status: $other")
            false
        }
      }
      .recover {
        case ex: Exception =>
          logger.error(s"Call to bridge-integration register-ratepayer failed: ${ex.getMessage}", ex)
          false
      }
  }

  def exploreRatePayer(credId: String = "123456789567")
                      (implicit hc: HeaderCarrier): Future[Option[Persons]] = {
    val url = uri(s"explore-ratepayer/$credId").toURL
    http.get(url)
      .execute[Option[Persons]]
      .recover {
        case ex =>
          logger.warn(s"Failed to retrieve explore ratepayer for credId=$credId: ${ex.getMessage}")
          None
      }
  }

  def postcodeSearch(
                      listType: String,
                      searchParams: FindAPropertyBridgeForm
                    )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, PostcodeSearchResult]] = {
    val postcode: String =
      searchParams.postcode.value.trim.toUpperCase

    val normalisedPostcode =
      postcode.replaceAll("\\s+", "").toUpperCase

    val url =
      uri(
        s"search?postcode=$normalisedPostcode&listType=$listType"
      ).toURL

    logger.info(
      Console.GREEN +
        s"[BridgeIntegrationConnector][postcodeSearch] Calling backend postcode search url=$url" + Console.RESET
    )

    http
      .get(url)
      .setHeader("Content-Type" -> "application/json")
      .setHeader("Authorization" -> appConfig.internalAuthToken)
      .execute[HttpResponse]
      .map { response =>
        logger.info(s"[BridgeIntegrationConnector][postcodeSearch] Response Status=${response.status}, body=${response.body}")
        response.status match {
          case OK =>
            response.json.validate[PostcodeSearchResult] match {
              case JsSuccess(result, _) =>
                Right(result)

              case JsError(errors) =>
                Left(ErrorResponse(BAD_REQUEST, s"Json Validation Error: $errors"))
            }

          case NOT_FOUND =>
            response.json.validate[PostcodeSearchResult] match {
              case JsSuccess(result, _) =>
                Right(result)

              case JsError(_) =>
                Left(ErrorResponse(NOT_FOUND, response.body))
            }

          case BAD_REQUEST =>
            Left(ErrorResponse(BAD_REQUEST, response.body))

          case status if status >= INTERNAL_SERVER_ERROR =>
            Left(ErrorResponse(status, response.body))

          case status =>
            Left(ErrorResponse(status, response.body))
        }
      }

      .recover {

        case e: UpstreamErrorResponse =>
          logger.error(Console.RED +
            s"[BridgeIntegrationConnector][postcodeSearch] Upstream status=${e.statusCode}, message=${e.message}" + Console.RESET,
            e
          )
          Left(ErrorResponse(e.statusCode, e.message))

        case NonFatal(ex) =>
          logger.error(Console.BLUE +
            s"[BridgeIntegrationConnector][postcodeSearch] Unexpected error calling postcode search: ${ex.getMessage}" + Console.RESET,
            ex
          )
          Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Call to Bridge postcode search failed"))
      }
  }

  def explore(
               propertyReference: String,
               listType: String
             )(
               implicit hc: HeaderCarrier
             ): Future[Either[ErrorResponse, ExploreResult]] = {

    val urlEndpoint =
      url"${appConfig.bridgeIntegration}/bridge-integration/explore/$propertyReference/$listType"

    http.get(urlEndpoint)
      .execute[HttpResponse]
      .map { response =>

        response.status match {

          case OK =>
            response.json.validate[ExploreResult] match {

              case JsSuccess(valid, _) =>
                Right(valid)

              case JsError(errors) =>
                Left(
                  ErrorResponse(
                    BAD_REQUEST,
                    s"Json Validation Error: $errors"
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
        case ex =>
          logger.error(
            s"Call to bridge-integration /explore failed: ${ex.getMessage}",
            ex
          )

          Left(
            ErrorResponse(
              INTERNAL_SERVER_ERROR,
              "Call to bridge-integration explore failed"
            )
          )
      }
  }


}
