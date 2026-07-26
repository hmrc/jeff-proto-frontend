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

package controllers.registration

import config.FrontendAppConfig
import connectors.EmailVerificationConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.NormalMode
import models.emailVerification.VerificationDetails
import models.requests.DataRequest
import navigation.Navigator
import pages.EmailPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.EmailVerificationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.registration.SubmitEmailView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmitEmailController @Inject()(override val messagesApi: MessagesApi,
                                      identify: IdentifierAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      sessionRepository: SessionRepository,
                                      navigator: Navigator,
                                      emailVerificationService: EmailVerificationService,
                                      val controllerComponents: MessagesControllerComponents,
                                      view: SubmitEmailView)(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging{

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val email = request.userAnswers.get(EmailPage)
      email match {
        case Some(value) =>
          retrieveStatus(emailVerificationService, request, value.email).flatMap {
            case Left(_) =>
              Future.successful(
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
              )
            case Right(verificationDetails) =>
              emailVerificationService.redirectIfLocked(
                Future.successful(Ok(view(verificationDetails.emailAddress))),
                verificationDetails.isLocked
              )
          }

        case None =>
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )
      }
  }

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(EmailPage) match {
        case Some(email) =>
          retrieveStatus(emailVerificationService, request, email.email).flatMap {
            case Left(error) =>
              logger.warn(
                s"[EmailVerificationController][onSubmit] Error retrieving verification status: ${error.message}"
              )
              Future.successful(
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
              )
            case Right(emailVerificationDetails) =>
              sessionRepository.set(request.userAnswers).map { _ =>
                Redirect(
                  routes.ContactNumberController.onPageLoad(NormalMode)
                )
              }
          }
        case None =>
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )
      }
  }

  private def retrieveStatus(emailVerificationService: EmailVerificationService,
                             request: DataRequest[AnyContent],
                             email: String)
                            (implicit hc: HeaderCarrier) = {
    emailVerificationService.retrieveAddressStatus(
      VerificationDetails("0000000026936462"),
      email,
      request.userAnswers
    )
  }

}
