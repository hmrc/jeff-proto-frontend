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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.mappings.BandReviewReasonFormProvider
import play.api.i18n.I18nSupport
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.challenge.BandReviewReasonView
import controllers.challenge as challengeRoutes
import models.Mode
import navigation.Navigator
import pages.{BandReviewReasonPage, DoYouHaveATradingNamePage}
import repositories.SessionRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BandReviewReasonController @Inject()(
                                            val controllerComponents: MessagesControllerComponents,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            sessionRepository: SessionRepository,
                                            navigator: Navigator,
                                            formProvider: BandReviewReasonFormProvider,
                                            view: BandReviewReasonView
                                          )(implicit ec: ExecutionContext) extends FrontendBaseController
  with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] =
    identify { implicit request =>
      Ok(
        view(formProvider(), mode)
      )
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      formProvider()
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, mode))
            ),
          reason => 
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(BandReviewReasonPage, reason)
              )
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(BandReviewReasonPage, mode, updatedAnswers))
        )
    }
}