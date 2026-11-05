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

package controllers.propertyLinking

import base.SpecBase
import connectors.BridgeIntegrationConnector
import models.propertyLinkingSubmission.{Capacity, CreatePropertyLink, Evidence, EvidenceType, ProvidedEvidence}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.propertyLinking.DeclarationView
import java.time.{LocalDate, LocalDateTime}

import scala.concurrent.Future

class DeclarationControllerSpec extends SpecBase with MockitoSugar {

  "Declaration Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.propertyLinking.routes.DeclarationController.show.url)
          .withSession("propertyAddress" -> "123 Test St", "propertyReference" -> "12345")

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeclarationView]

        status(result) mustEqual OK
        contentAsString(result) must include("123 Test St")
        contentAsString(result) must include("Owner")
        contentAsString(result) must include("rates-bill.pdf")
      }
    }

    "must redirect to the next page when valid data is submitted (declaration true)" in {

      val mockConnector = mock[BridgeIntegrationConnector]

      when(mockConnector.submitPropertyLink(any())(any()))
        .thenReturn(Future.successful(true))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[BridgeIntegrationConnector].toInstance(mockConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, controllers.propertyLinking.routes.DeclarationController.submit.url)
            .withFormUrlEncodedBody(("declaration", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.IndexController.onPageLoad().url
        verify(mockConnector).submitPropertyLink(any())(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, controllers.propertyLinking.routes.DeclarationController.submit.url)
            .withFormUrlEncodedBody(("declaration", "false"))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }
  }
}
