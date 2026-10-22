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
import forms.mappings.ResponsibleFromDateFormProvider
import models.{Mode, NormalMode}
import pages.ResponsibleFromDatePage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.challenge.ResponsibleFromDateView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ResponsibleFromDateController @Inject()(
                                               val controllerComponents: MessagesControllerComponents,
                                               identify: IdentifierAction,
                                               getData: DataRetrievalAction,
                                               requireData: DataRequiredAction,
                                               sessionRepository: SessionRepository,
                                               formProvider: ResponsibleFromDateFormProvider,
                                               view: ResponsibleFromDateView
                                             )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>

      val preparedForm =
        request.userAnswers
          .get(ResponsibleFromDatePage)
          .fold(formProvider())(formProvider().fill)

      Ok(view(preparedForm, mode))
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

        responsibleFromDate =>
          for {
            updatedAnswers <- Future.fromTry(
              request.userAnswers.set(
                ResponsibleFromDatePage,
                responsibleFromDate
              )
            )
            _ <- sessionRepository.set(updatedAnswers)
          } yield Redirect(
            routes.BandReviewReasonController.onPageLoad(NormalMode)
          )
      )
    }
}