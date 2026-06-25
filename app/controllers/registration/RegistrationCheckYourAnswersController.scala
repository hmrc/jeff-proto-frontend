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
import controllers.Execution.trampoline
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import forms.mappings.{ContactNumber, Email, MobilePhone, TradingName}
import models.NormalMode
import models.Registration.frontend.RegisterRatepayerRequest
import navigation.Navigator
import pages.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UniqueIdGenerator
import viewmodels.RegistrationCheckAnswers.createRegistrationSummaryRows
import views.html.registration.RegistrationCheckYourAnswersView

import scala.concurrent.Future

class RegistrationCheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            bridgeIntegrationConnector: BridgeIntegrationConnector,
                                            navigator: Navigator,
                                            val controllerComponents: MessagesControllerComponents,
                                            view: RegistrationCheckYourAnswersView
                                          ) extends FrontendBaseController with I18nSupport with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      Ok(view(createRegistrationSummaryRows(request.userAnswers)))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      submitData(
        request.userId,
        Some(
          createRatePayer(
            tradingName = request.userAnswers.get(TradingNamePage),
            email = request.userAnswers.get(EmailPage),
            contactNumber = request.userAnswers.get(ContactNumberPage),
            secondaryContactNumber = request.userAnswers.get(MobilePhonePage)
          )
        )
      )
      Redirect(
        navigator.nextPage(
          CompleteContactDetailsPage,
          NormalMode,
          request.userAnswers
        )
      )
  }

  private def createRatePayer(
                               tradingName: Option[TradingName], 
                               email: Option[Email],
                               contactNumber: Option[ContactNumber],
                               secondaryContactNumber: Option[MobilePhone]
                             ): RegisterRatepayerRequest = {
    RegisterRatepayerRequest(
      ratepayerCredId = None,
      userType = None,
      agentStatus = None,
      name = None,
      tradingName = tradingName.map(_.value),
      email = email.map(_.email),
      nino = None,
      contactNumber = contactNumber.map(_.value),
      secondaryNumber = secondaryContactNumber.map(_.mobilePhone),
      address = None,
      trnReferenceNumber = None,
      isRegistered = Some(true),
      recoveryId = None
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
