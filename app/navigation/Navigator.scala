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

package navigation

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import controllers.routes
import controllers.registration.routes as registrationRoutes
import controllers.dashboard.routes as dashboardRoutes
import pages.*
import models.*

@Singleton
class Navigator @Inject()() {

  private val normalRoutes: Page => UserAnswers => Call = {
    case AddressPage => _ => registrationRoutes.EmailController.onPageLoad(NormalMode)
    case EmailPage => _ => registrationRoutes.ContactNumberController.onPageLoad(NormalMode)
    case ContactNumberPage => _ => registrationRoutes.DoYouHaveASecondaryContactNumberController.onPageLoad(NormalMode)
    case DoYouHaveASecondaryContactNumberPage => userAnswers => if(userAnswers.get(DoYouHaveASecondaryContactNumberPage).get.value) registrationRoutes.MobileNumberController.onPageLoad(NormalMode) else registrationRoutes.DoYouHaveATradingNameController.onPageLoad(NormalMode)
    case MobilePhonePage => _ => registrationRoutes.DoYouHaveATradingNameController.onPageLoad(NormalMode)
    case DoYouHaveATradingNamePage => userAnswers => if(userAnswers.get(DoYouHaveATradingNamePage).get.value) registrationRoutes.TradingNameController.onPageLoad(NormalMode) else registrationRoutes.RegistrationCheckYourAnswersController.onPageLoad()
    case TradingNamePage => _ => registrationRoutes.RegistrationCheckYourAnswersController.onPageLoad()
    case CompleteContactDetailsPage => _ => registrationRoutes.CreateConfirmationController.onPageLoad()
    case UpdateTelephoneNumberPage => _ => dashboardRoutes.HomeController.onPageLoad()
    case UpdateEmailPage => _ => dashboardRoutes.HomeController.onPageLoad()
    case _ => _ => routes.IndexController.onPageLoad()
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case _ => userAnswers =>
      userAnswers match {
        case userAnswers if userAnswers.get(DoYouHaveATradingNamePage).get.value =>
          registrationRoutes.TradingNameController.onPageLoad(CheckMode)
        case userAnswers if userAnswers.get(DoYouHaveASecondaryContactNumberPage).get.value =>
          registrationRoutes.MobileNumberController.onPageLoad (CheckMode)
        case _ => registrationRoutes.RegistrationCheckYourAnswersController.onPageLoad()
      }

  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode =>
      checkRouteMap(page)(userAnswers)
  }
}
