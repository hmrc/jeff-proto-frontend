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

package controllers.challenge

import controllers.actions.IdentifierAction
import controllers.challenge as challengeRoutes
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.{FrontendBaseController, FrontendController}
import views.html.challenge.FormalReviewView


import javax.inject.Inject

class FormalReviewController @Inject()(
                                        identify: IdentifierAction,
                                        view: FormalReviewView,
                                        mcc: MessagesControllerComponents
                                      )
extends FrontendController(mcc) with I18nSupport{

  def onPageLoad(): Action[AnyContent] =
    identify { implicit request =>
      Ok(view())
    }

  def onContinue(): Action[AnyContent] =
    identify { implicit request =>
      // Replace this with the next page in the formal review journey.
      Redirect(challengeRoutes.routes.FormalReviewController.onPageLoad())
    }
}