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

package controllers.enquiry

import controllers.actions.{
  DataRequiredAction,
  DataRetrievalAction,
  IdentifierAction
}
import forms.mappings.ScenarioReviewOptions
import pages.ScenarioReviewOptionsPage
import play.api.i18n.I18nSupport
import play.api.mvc.{
  Action,
  AnyContent,
  MessagesControllerComponents
}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.challenge.ScenarioReviewOptionsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ScenarioReviewOptionsController @Inject()(
                                                 val controllerComponents: MessagesControllerComponents,
                                                 identify: IdentifierAction,
                                                 getData: DataRetrievalAction,
                                                 requireData: DataRequiredAction,
                                                 sessionRepository: SessionRepository,
                                                 view: ScenarioReviewOptionsView
                                               )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData andThen requireData) {
      implicit request =>

        val preparedForm =
          request.userAnswers
            .get(ScenarioReviewOptionsPage)
            .fold(ScenarioReviewOptions.form()) {
              ScenarioReviewOptions.form().fill
            }

        Ok(view(preparedForm))
    }

  def onSubmit(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>

        ScenarioReviewOptions
          .form()
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(view(formWithErrors))
              ),

            scenarioReviewOption =>
              for {
                updatedAnswers <- Future.fromTry(
                  request.userAnswers.set(
                    ScenarioReviewOptionsPage,
                    scenarioReviewOption
                  )
                )

                _ <- sessionRepository.set(updatedAnswers)

              } yield scenarioReviewOption match {

                case ScenarioReviewOptions.BandIsWrong =>
                  Redirect(
                    routes.ScenarioResponsibleController.onPageLoad()
                  )

                case ScenarioReviewOptions.ChangesHaveAffectedProperty =>
                  Redirect(
                    routes.CheckYourAnswersController.onPageLoad()
                  )
              }
          )
    }
}