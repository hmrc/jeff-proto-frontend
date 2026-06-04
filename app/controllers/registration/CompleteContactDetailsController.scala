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
import connectors.BridgeIntegrationConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.mappings.ContactDetails.form
import models.NormalMode
import models.Registration.frontend.{RegisterRatepayerRequest, TradingName}
import navigation.Navigator
import pages.CompleteContactDetailsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UniqueIdGenerator
import views.html.{CheckYourAnswersView, CompleteContactDetailsView}
import play.api.Logging

import scala.concurrent.{ExecutionContext, Future}

class CompleteContactDetailsController  @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   identify: IdentifierAction,
                                                   getData: DataRetrievalAction,
                                                   requireData: DataRequiredAction,
                                                   sessionRepository: SessionRepository,
                                                   navigator: Navigator,
                                                   bridgeIntegrationConnector: BridgeIntegrationConnector,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   view: CompleteContactDetailsView
                                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport with Logging {
  
  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(CompleteContactDetailsPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors))),
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(CompleteContactDetailsPage, value))
            _              <- sessionRepository.set(updatedAnswers)
            maybeAnswers <- sessionRepository.get(request.userId)
            _ <- maybeAnswers match {
              case None =>
                Future.successful(())
              case Some(existingAnswers) =>
                val contactOpt = existingAnswers.get(CompleteContactDetailsPage)
                val ratepayerRequest = RegisterRatepayerRequest(
                  ratepayerCredId    = None,
                  userType           = None,
                  agentStatus        = None,
                  name               = None,
                  tradingName        = None,
                  email              = None,
                  nino               = None,
                  contactNumber      = contactOpt.map(_.phone),
                  secondaryNumber    = None,
                  address            = None,
                  trnReferenceNumber = None,
                  isRegistered        = Some(true),
                  recoveryId         = None
                )

                submitData(request.userId, Some(ratepayerRequest))
                  .recover {
                    case _ => ()  // swallow failure, optionally log
                  }
            }
          } yield Redirect(navigator.nextPage(CompleteContactDetailsPage,NormalMode, updatedAnswers))
      )
  }

  private def submitData(
                          userId: String,
                          ratepayerDataOpt: Option[RegisterRatepayerRequest]
                        )(implicit request: Request[AnyContent]): Future[Result] = {
    ratepayerDataOpt match {
      case Some(ratepayerData) =>
        val updatedRatepayerData =
          ratepayerData.copy(
            ratepayerCredId = Some(userId),
            recoveryId = Some(UniqueIdGenerator.generateId)
          )

        bridgeIntegrationConnector.registerRatePayer(updatedRatepayerData).flatMap { notifySuccess =>
          if (notifySuccess) {
            logger.info(s" ${Console.MAGENTA} Registered user: $userId ${Console.RESET}")
            Future.successful(
              Redirect(routes.CreateConfirmationController.onPageLoad())
            )
          } else {
            logger.error(s"Failed to send to the bridge for credId: $userId")
            Future.failed(new Exception(s"Failed to send to the bridge for credId: $userId"))
          }
        }

      case None =>
        logger.error(s"No ratepayer data found in request: $userId")
        Future.failed(new Exception("No ratepayer data found in request"))
    }
  }
  
}

