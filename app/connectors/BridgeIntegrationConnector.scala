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
import forms.mappings.FindPropertyForm
import models.Registration.frontend.RegisterRatepayerRequest
import models.dashboard.Persons
import play.api.libs.json.*
import models.properties.PostcodeSearchResult
import play.api.http.Status.*
import play.api.i18n.Lang.logger
import play.api.libs.json.{JsError, JsObject, JsValue, Json}
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.StringContextOps
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK}

import java.net.URI
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.ws.writeableOf_JsValue
import play.api.i18n.Lang.logger
import play.api.http.Status.*

import scala.util.control.NonFatal
import uk.gov.hmrc.play.bootstrap.http.ErrorResponse


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


  def findPropertyPostcodeSearch(
                                  searchParams: FindPropertyForm
                                )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, PostcodeSearchResult]] = {
    val urlEndpoint =
      if (appConfig.useStubForVmv) {
        uri(s"postcode/${searchParams.postcode.value.toUpperCase.replaceAll("\\s", "")}").toURL
      } else {
        if (searchParams.propertyName.nonEmpty) {
          val cleanedName = searchParams.propertyName.map(_.replaceAll("['()]", "")).getOrElse("")
          url"${appConfig.vmvAddressLookup}/vmv/rating-listing/api/properties?postcode=${searchParams.postcode.value}&propertyNameNumber=$cleanedName&size=15&searchDirection=FORWARD"
        } else {
          url"${appConfig.vmvAddressLookup}/vmv/rating-listing/api/properties?postcode=${searchParams.postcode.value}&size=15&searchDirection=FORWARD"
        }
      }

    http.get(urlEndpoint)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK | NOT_FOUND =>
            response.json.validate[PostcodeSearchResult] match {
              case JsSuccess(valid, _) => Right(valid)
              case JsError(errors) =>
                println(Console.RED + s"I still screwed up and need to fix: ${response.json}" + Console.RESET)
                Left(ErrorResponse(BAD_REQUEST, s"Json Validation Error: $errors"))
            }
          case _ =>
            Left(ErrorResponse(response.status, response.body))
        }
      }
      .recover {
        case _ =>
          Left(ErrorResponse(Status.INTERNAL_SERVER_ERROR, "Call to VMV find a property failed"))
      }
  }

  def postcodeSearch(
                      searchParams: FindPropertyForm
                    )(implicit hc: HeaderCarrier): Future[Either[ErrorResponse, PostcodeSearchResult]] = {

    val postcode: String =
      searchParams.postcode.value.trim.toUpperCase

    val normalisedPostcode = postcode.replaceAll("\\s+", "").toUpperCase

    val url = uri(s"postcode/$normalisedPostcode").toURL

    logger.info(
      Console.GREEN +
        s"[BridgeIntegrationConnector][postcodeSearch] Calling backend postcode search url=$url" + Console.RESET
    )

    http
      .get(url)
      .setHeader("Content-Type" -> "application/json")
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
            // If backend returns 404 for no results, preserve the shape expected by the UI flow.
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
}
