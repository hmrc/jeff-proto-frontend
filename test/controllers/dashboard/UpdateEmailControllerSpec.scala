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

package controllers.dashboard

import base.SpecBase
import connectors.BridgeIntegrationConnector
import models.Registration.frontend.RegisterRatepayerRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.dashboard.UpdateEmailView

import scala.concurrent.Future

class UpdateEmailControllerSpec extends SpecBase with MockitoSugar {

  "UpdateEmail Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.UpdateEmailController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[UpdateEmailView]

        status(result) mustEqual OK
      }
    }

    "must redirect to the next page and call bridge integration when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      val mockBridgeConnector = mock[BridgeIntegrationConnector]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      when(mockBridgeConnector.registerRatePayer(any())(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[BridgeIntegrationConnector].toInstance(mockBridgeConnector)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.UpdateEmailController.onSubmit().url)
            .withFormUrlEncodedBody(("email", "new@email.com"), ("confirmedEmail", "new@email.com"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.dashboard.routes.HomeController.onPageLoad().url
        
        verify(mockBridgeConnector, times(1)).registerRatePayer( any[RegisterRatepayerRequest])(any())
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, routes.UpdateEmailController.onSubmit().url)
            .withFormUrlEncodedBody(("value", ""))

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }
  }
}
