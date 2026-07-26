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

import com.google.inject.Inject
import config.FrontendAppConfig
import connectors.EmailVerificationConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.mappings.Email.form
import models.emailVerification.{EmailVerificationDetails, EmailVerificationRequest, VerificationDetails}
import models.{Mode, NormalMode, UserAnswers}
import navigation.Navigator
import pages.EmailPage
import play.api.Logging
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.EmailVerificationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.registration.EmailView

import scala.concurrent.{ExecutionContext, Future}

class EmailController @Inject()(
                                 override val messagesApi: MessagesApi,
                                 identify: IdentifierAction,
                                 getData: DataRetrievalAction,
                                 requireData: DataRequiredAction,
                                 sessionRepository: SessionRepository,
                                 navigator: Navigator,
                                 emailVerificationService: EmailVerificationService,
                                 emailVerificationConnector: EmailVerificationConnector,
                                 config: FrontendAppConfig,
                                 val controllerComponents: MessagesControllerComponents,
                                 view: EmailView,
                               )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(EmailPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }
      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),
        value => {

          Future.fromTry(request.userAnswers.set(EmailPage, value)).flatMap { updatedAnswers =>
            emailVerificationService
              .retrieveAddressStatus(
                VerificationDetails("0000000026936462"),
                value.email,
                updatedAnswers
              )
              .flatMap {
                case Left(error) =>
                  logger.warn(
                    "[EmailController][onSubmit] Error retrieving email verification status: " +
                      s"${error.code} and message: ${error.message}"
                  )
                  Future.successful(
                    Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                  )

                case Right(verificationDetails) =>
                  logger.warn(Console.RED + s"Reached Right branch: $verificationDetails" + Console.RESET)
                  emailVerificationService.redirectIfLocked(
                    handleRedirect(
                      updatedAnswers,
                      verificationDetails,
                      "0000000026936462"
                    ),
                    verificationDetails.isLocked
                  )
              }
          }
        }
      )
  }


  private def handleRedirect(
                              updatedAnswers: UserAnswers,
                              details: EmailVerificationDetails,
                              credId: String
                            )(implicit hc: HeaderCarrier, messages: Messages) = {

    sessionRepository.set(updatedAnswers)
      .flatMap { _ =>
        if (details.isVerified) {
          Future.successful(
            Redirect(navigator.nextPage(EmailPage, NormalMode, updatedAnswers))
          )
        } else {
          startEmailVerification(details.emailAddress, credId)
        }
      }
      .recover {
        case e =>
          logger.warn(
            s"[EnterEmailController][handleRedirect] Error setting user answers: ${e.getMessage}"
          )
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }


  private def startEmailVerification(email: String, credId: String)
                                    (implicit hc: HeaderCarrier, messages: Messages) = {

    val evRequest = emailVerificationService.createRequest(credId, email)
    handoffToEmailVerification(evRequest)
  }

  private def handoffToEmailVerification(evRequest: EmailVerificationRequest)
                                        (implicit hc: HeaderCarrier) = {

    emailVerificationConnector.startEmailVerification(evRequest).map {
      case Left(error) =>
        logger.warn("[EnterEmailController][handoffToEmailVerification] Error starting email verification with status: " +
          s"${error.code} and message: ${error.message}")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Right(redirectUri) =>
        val redirectTo = s"${config.emailVerificationRedirectBaseUrl}${redirectUri.redirectUri}"
        Redirect(redirectTo)
    }
  }


}