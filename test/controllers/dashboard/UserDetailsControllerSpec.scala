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
import controllers.actions.{DataRequiredActionImpl, FakeDataRetrievalAction, FakeIdentifierAction}
import models.Registration.frontend.Address
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import views.html.dashboard.UserDetailsView

class UserDetailsControllerSpec extends SpecBase {

  "UserDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      val controller = application.injector.instanceOf[UserDetailsController]

      val request = FakeRequest(GET, "/user-details")

      val result = controller.show()(request)

      status(result) mustEqual OK

      contentAsString(result) must include("Jake Reid")
      contentAsString(result) must include("12345")
      contentAsString(result) must include("Demo Company Ltd")
      contentAsString(result) must include("07123456789")
      contentAsString(result) must include("test@gmail.com")
      contentAsString(result) must include("456")
    }
  }
}
