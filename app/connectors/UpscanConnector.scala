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

package connectors

import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import config.FrontendAppConfig
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import models.upscan.{PreparedUpload, UpscanFileReference, UpscanInitiateRequest, UpscanInitiateResponse}
import play.api.libs.ws.writeableOf_JsValue

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpscanConnector @Inject()(httpClientV2: HttpClientV2, appConfig: FrontendAppConfig)(implicit ec: ExecutionContext) {

  private val headers = Map(
    HeaderNames.CONTENT_TYPE -> "application/json"
  )

  def initiate(
                redirectOnSuccess: Option[String],
                redirectOnError: Option[String]
              )(implicit hc: HeaderCarrier): Future[UpscanInitiateResponse] = {
    val request = UpscanInitiateRequest(
      callbackUrl = appConfig.callbackEndpointTarget,
      successRedirect = redirectOnSuccess,
      errorRedirect = redirectOnError
    )

    val upscanInitiateUri = s"${appConfig.upscanHost}/upscan/v2/initiate"

    for
      response <- httpClientV2.post(url"$upscanInitiateUri")
        .withBody(Json.toJson(request))
        .setHeader(headers.toSeq: _*)
        .execute[PreparedUpload]
      fileReference = UpscanFileReference(response.reference.value)
      postTarget = response.uploadRequest.href
      formFields = response.uploadRequest.fields
    yield UpscanInitiateResponse(fileReference, postTarget, formFields)
  }
}
