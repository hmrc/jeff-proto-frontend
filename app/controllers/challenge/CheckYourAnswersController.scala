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

import connectors.BridgeIntegrationConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.enquiry.{EnquiryProtoData, MakeAnEnquiryRequest}
import pages.{ChallengeContactDetailsPage, PropertyDetailsPage, ResponsibleFromDatePage, ScenarioPage}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.challenge.CheckYourAnswersView
import controllers.challenge as challengeRoutes

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject()(
                                            val controllerComponents: MessagesControllerComponents,
                                            identify: IdentifierAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            connector: BridgeIntegrationConnector,
                                            view: CheckYourAnswersView
                                          )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>

      val propertyDetails =
        request.userAnswers.get(PropertyDetailsPage)

      val scenario =
        request.userAnswers.get(ScenarioPage)

      val responsibleFromDate =
        request.userAnswers.get(ResponsibleFromDatePage)

      val contactDetails =
        request.userAnswers.get(ChallengeContactDetailsPage)

      Ok(
        view(
          propertyDetails = propertyDetails,
          scenario = scenario,
          responsibleFromDate = responsibleFromDate,
          contactDetails = contactDetails
        )
      )
    }

  def onSubmit(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      val submissionData =
        for {
          property <- request.userAnswers.get(PropertyDetailsPage)
          propertyId <- property.id.flatMap(_.value)
          scenario <- request.userAnswers.get(ScenarioPage)
          responsibleFromDate <- request.userAnswers.get(ResponsibleFromDatePage)
          contactDetails <- request.userAnswers.get(ChallengeContactDetailsPage)
        } yield (
          propertyId,
          property,
          scenario,
          responsibleFromDate,
          contactDetails
        )

      submissionData match {

        case Some(
          (
            propertyId,
            property,
            scenario,
            responsibleFromDate,
            contactDetails
          )
        ) =>

          val makeAnEnquiryRequest =
            MakeAnEnquiryRequest(
              propertyID = propertyId,
              firstName = contactDetails.firstName,
              lastName = contactDetails.lastName,
              email = contactDetails.emailAddress,
              contactNumber = contactDetails.telephoneNumber,
              files = List(
                  EnquiryProtoData(
                    node = "node",
                    mime_type = "mime_type",
                    label = "label",
                    is_pointer = true,
                    pointer = "pointer",
                    data = "data"
                  )
                )
            )

          connector.submitEnquiry(makeAnEnquiryRequest).map {

            case true =>
              Redirect(
                challengeRoutes.routes.EnquirySubmittedController.onPageLoad()
              )

            case false =>
              logger.error(
                s"Bridge Integration failed to submit enquiry for propertyId: $propertyId"
              )

              Redirect(
                controllers.routes.JourneyRecoveryController.onPageLoad()
              )
          }

        case None =>
          logger.error(
            "Unable to submit enquiry because required answers or the property ID are missing"
          )

          Future.successful(
            Redirect(
              controllers.routes.JourneyRecoveryController.onPageLoad()
            )
          )
      }
    }
}