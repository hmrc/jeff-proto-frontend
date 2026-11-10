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

import connectors.BridgeIntegrationConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import controllers.enquiry as enquiryRoutes
import models.dashboard.CodeMeaning
import models.enquiry.premises.{Information, Premise, Statement}
import models.enquiry.product.ProductContext
import models.enquiry.product.person.{Communications, Person}
import models.enquiry.product.relationship.{Manifestations, Relationships}
import models.enquiry.{EnquiryProtoData, Proposition, SubmissionRequest}
import pages.*
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.*
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.challenge.CheckYourAnswersView

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
          property <-               request.userAnswers.get(PropertyDetailsPage)
          propertyId <-             property.id.flatMap(_.value)
          scenario <-               request.userAnswers.get(ScenarioPage)
          responsibleFromDate <-    request.userAnswers.get(ResponsibleFromDatePage)
          contactDetails <-         request.userAnswers.get(ChallengeContactDetailsPage)
          bandReviewReason <-       request.userAnswers.get(BandReviewReasonPage)
          expectedCouncilTaxBand <- request.userAnswers.get(ExpectedCouncilTaxBandPage)
        } yield (
          propertyId,
          property,
          scenario,
          responsibleFromDate,
          contactDetails,
          bandReviewReason,
          expectedCouncilTaxBand
        )

      submissionData match {

        case Some(
          (
            propertyId,
            property,
            scenario,
            responsibleFromDate,
            contactDetails,
            bandReviewReason,
            expectedCouncilTaxBand
          )
        ) =>

          val submissionRequest: SubmissionRequest =
            SubmissionRequest(
              submissionId = "01", //UFS GENERATED
              submission_date = "", //AS SOON AS SUBMITTED
              files = List(
                EnquiryProtoData(
                  node = Some("node"),
                  mime_type = "mimeType",
                  label = "label",
                  is_pointer = true,
                  pointer = "pointer",
                  data = Some("data")
                )
              ),
              product =
                ProductContext(
                  Person(
                    communications = Communications(
                      forenames = Some(contactDetails.firstName),
                      surname = Some(contactDetails.lastName),
                      email = Some(contactDetails.emailAddress),
                      telephone_number = Some(contactDetails.telephoneNumber),
                      CouncilTaxPayerFirstName = Some("CouncilTaxPayerFirstName"),
                      CouncilTaxPayerLastName = Some("CouncilTaxPayerLastName"),
                      CouncilTaxPayerEmail = Some("CouncilTaxPayerEmail"),
                      CouncilTaxPayerNumber = Some("CouncilTaxPayerNumber"),
                      CouncilTaxPayerContactPreference = Some(contactDetails.contactPreferences.toString()),
                      CouncilTaxPayerContactAddress = Some("CouncilTaxPayerContactAddress"),
                    ),
                    capacity =  CodeMeaning( 
                      code = Some(""),
                      meaning = Some("")
                    )//Are you responsible for the Council Tax on this property?
                  ),
                  relationship = Relationships(
                    manifestation = Manifestations(
                      artifact_code = "artifactCode",
                      protodata_ptr = Some("protodataPtr")
                    ),
                    `type` = CodeMeaning(
                      code = Some(""),
                      meaning = Some("")
                    ),
                    `class` = CodeMeaning(
                      code = Some(""),
                      meaning = Some("")
                    )
                  )
                ),
              premises = Seq(
                Premise(
                  //Reasons for reviewing your Council Tax band
                  //How long have you been responsible for paying Council Tax on the property?
                  //Tell us why you think the Council Tax band is wrong?
                  unique_statutory_ref = scenario match {
                    case scenario if scenario == "lessThanSixMonths" => "JH1"
                    case scenario => "JH2"
                  },
                  text = scenario,
                  statements = List(
                    Statement(
                      unique_statutory_ref = "S01",
                      text = bandReviewReason
                    )
                  ),
                  information =
                    List(
                      Information(
                        unique_statutory_ref = "",
                        text = ""
                      )
                    )
                )
              ),
              proposition = List(
                //When did you become responsible for paying Council Tax for your home?
                //What do you think the Council Tax band should be?
                Proposition(
                  unique_statutory_ref = "PO1",
                  alterations = List(expectedCouncilTaxBand),
                ),
                Proposition(
                  unique_statutory_ref = "PO2",
                  alterations = List(responsibleFromDate.toString),
                )
              )
            )

          connector.submitEnquiry(submissionRequest).map {

            case true =>
              Redirect(
                routes.EnquirySubmittedController.onPageLoad()
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