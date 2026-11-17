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
import controllers.actions.{
  DataRequiredAction,
  DataRetrievalAction,
  IdentifierAction
}
import models.dashboard.CodeMeaning
import models.enquiry.premises.{Premise, Statement}
import models.enquiry.product.ProductContext
import models.enquiry.product.person.{Communications, Person}
import models.enquiry.product.relationship.{
  Manifestations,
  Relationships
}
import models.enquiry.{
  EnquiryProtoData,
  Proposition,
  SubmissionRequest
}
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
                                          )(
                                            implicit ec: ExecutionContext
                                          )
  extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    (identify andThen getData andThen requireData) {
      implicit request =>

        val propertyDetails =
          request.userAnswers.get(PropertyDetailsPage)

        val reviewReason =
          request.userAnswers.get(ScenarioReviewOptionsPage)

        val responsiblePeriod =
          request.userAnswers.get(ScenarioResponsiblePage)

        val noticeAlteration =
          request.userAnswers.get(NoticeAlterationPage)

        val responsibleFromDate =
          request.userAnswers.get(ResponsibleFromDatePage)

        val bandReviewReason =
          request.userAnswers.get(BandReviewReasonPage)

        val expectedCouncilTaxBand =
          request.userAnswers.get(ExpectedCouncilTaxBandPage)

        val contactDetails =
          request.userAnswers.get(ChallengeContactDetailsPage)

        Ok(
          view(
            propertyDetails = propertyDetails,
            reviewReason = reviewReason,
            responsiblePeriod = responsiblePeriod,
            noticeAlteration = noticeAlteration,
            responsibleFromDate = responsibleFromDate,
            bandReviewReason = bandReviewReason,
            expectedCouncilTaxBand = expectedCouncilTaxBand,
            contactDetails = contactDetails
          )
        )
    }

  def onSubmit(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>

        val property =
          request.userAnswers.get(PropertyDetailsPage)

        val propertyId =
          property
            .flatMap(_.id)
            .flatMap(_.value)

        val reviewReason =
          request.userAnswers.get(ScenarioReviewOptionsPage)

        val responsiblePeriod =
          request.userAnswers.get(ScenarioResponsiblePage)

        /*
         * NoticeAlterationPage is optional because users who select
         * "Less than 6 months" do not visit the Notice of Alteration page.
         */
        val noticeAlteration =
          request.userAnswers.get(NoticeAlterationPage)

        val responsibleFromDate =
          request.userAnswers.get(ResponsibleFromDatePage)

        val contactDetails =
          request.userAnswers.get(ChallengeContactDetailsPage)

        val bandReviewReason =
          request.userAnswers.get(BandReviewReasonPage)

        val expectedCouncilTaxBand =
          request.userAnswers.get(ExpectedCouncilTaxBandPage)

        logger.info(
          s"""
             |Check Your Answers submission data:
             |property=${property.isDefined}
             |propertyId=${propertyId.isDefined}
             |reviewReason=${reviewReason.isDefined}
             |responsiblePeriod=${responsiblePeriod.isDefined}
             |noticeAlteration=${noticeAlteration.isDefined}
             |responsibleFromDate=${responsibleFromDate.isDefined}
             |contactDetails=${contactDetails.isDefined}
             |bandReviewReason=${bandReviewReason.isDefined}
             |expectedCouncilTaxBand=${expectedCouncilTaxBand.isDefined}
             |""".stripMargin
        )

        val submissionData =
          for {
            resolvedPropertyId <-
              propertyId

            resolvedProperty <-
              property

            resolvedReviewReason <-
              reviewReason

            resolvedResponsiblePeriod <-
              responsiblePeriod

            resolvedResponsibleFromDate <-
              responsibleFromDate

            resolvedContactDetails <-
              contactDetails

            resolvedBandReviewReason <-
              bandReviewReason

            resolvedExpectedCouncilTaxBand <-
              expectedCouncilTaxBand

          } yield (
            resolvedPropertyId,
            resolvedProperty,
            resolvedReviewReason,
            resolvedResponsiblePeriod,
            noticeAlteration,
            resolvedResponsibleFromDate,
            resolvedContactDetails,
            resolvedBandReviewReason,
            resolvedExpectedCouncilTaxBand
          )

        submissionData match {

          case Some(
            (
              resolvedPropertyId,
              _,
              resolvedReviewReason,
              resolvedResponsiblePeriod,
              resolvedNoticeAlteration,
              resolvedResponsibleFromDate,
              resolvedContactDetails,
              resolvedBandReviewReason,
              resolvedExpectedCouncilTaxBand
            )
          ) =>

            val contactAddress =
              List(
                Some(resolvedContactDetails.addressLine1),
                resolvedContactDetails.addressLine2,
                Some(resolvedContactDetails.townOrCity),
                resolvedContactDetails.county,
                Some(resolvedContactDetails.postcode)
              )
                .flatten
                .map(_.trim)
                .filter(_.nonEmpty)
                .mkString(", ")

            val contactPreferences =
              resolvedContactDetails
                .contactPreferences
                .mkString(",")

            val journeyReference =
              resolvedResponsiblePeriod match {
                case "lessThanSixMonths" =>
                  "JH1"

                case "moreThanSixMonths" =>
                  "JH2"

                case _ =>
                  "JH2"
              }

            val statements =
              List(
                Statement(
                  unique_statutory_ref = "S01",
                  text = resolvedReviewReason
                ),
                Statement(
                  unique_statutory_ref = "S02",
                  text = resolvedResponsiblePeriod
                ),
                Statement(
                  unique_statutory_ref = "S03",
                  text = resolvedBandReviewReason
                )
              ) ++ resolvedNoticeAlteration.map { answer =>
                Statement(
                  unique_statutory_ref = "S04",
                  text = answer
                )
              }

            val submissionRequest =
              SubmissionRequest(
                submissionId = Some("01"),
                submission_date = Some(""),
                files = Some(
                  List(
                    EnquiryProtoData(
                      node = Some("node"),
                      mime_type = "mimeType",
                      label = "label",
                      is_pointer = true,
                      pointer = "pointer",
                      data = Some("data")
                    )
                  )
                ),
                product = Some(
                  ProductContext(
                    person = Person(
                      communications = Communications(
                        forenames =
                          Some(
                            resolvedContactDetails.firstName
                          ),
                        surname =
                          Some(
                            resolvedContactDetails.lastName
                          ),
                        email =
                          Some(
                            resolvedContactDetails.emailAddress
                          ),
                        telephone_number =
                          Some(
                            resolvedContactDetails.telephoneNumber
                          ),
                        CouncilTaxPayerFirstName =
                          Some(
                            resolvedContactDetails.firstName
                          ),
                        CouncilTaxPayerLastName =
                          Some(
                            resolvedContactDetails.lastName
                          ),
                        CouncilTaxPayerEmail =
                          Some(
                            resolvedContactDetails.emailAddress
                          ),
                        CouncilTaxPayerNumber =
                          Some(
                            resolvedContactDetails.telephoneNumber
                          ),
                        CouncilTaxPayerContactPreference =
                          Some(contactPreferences),
                        CouncilTaxPayerContactAddress =
                          Some(contactAddress)
                      ),
                      capacity = CodeMeaning(
                        code = Some(""),
                        meaning = Some("")
                      )
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
                  )
                ),
                premises = Some(
                  Seq(
                    Premise(
                      unique_statutory_ref = journeyReference,
                      text = resolvedReviewReason,
                      statements = statements,
                      information = List.empty
                    )
                  )
                ),
                proposition = Some(
                  List(
                    Proposition(
                      unique_statutory_ref = "PO1",
                      alterations =
                        List(
                          resolvedExpectedCouncilTaxBand
                        )
                    ),
                    Proposition(
                      unique_statutory_ref = "PO2",
                      alterations =
                        List(
                          resolvedResponsibleFromDate.toString
                        )
                    )
                  )
                )
              )

            connector
              .submitEnquiry(submissionRequest)
              .map {
                case true =>
                  Redirect(
                    routes.EnquirySubmittedController
                      .onPageLoad()
                  )

                case false =>
                  logger.error(
                    s"Bridge Integration failed to submit enquiry " +
                      s"for propertyId: $resolvedPropertyId"
                  )

                  Redirect(
                    controllers.routes.JourneyRecoveryController
                      .onPageLoad()
                  )
              }
              .recover {
                case exception =>
                  logger.error(
                    s"Bridge Integration threw an exception while " +
                      s"submitting enquiry for propertyId: " +
                      s"$resolvedPropertyId",
                    exception
                  )

                  Redirect(
                    controllers.routes.JourneyRecoveryController
                      .onPageLoad()
                  )
              }

          case None =>
            val missingValues =
              Seq(
                "property" ->
                  property.isEmpty,
                "propertyId" ->
                  propertyId.isEmpty,
                "reviewReason" ->
                  reviewReason.isEmpty,
                "responsiblePeriod" ->
                  responsiblePeriod.isEmpty,
                "responsibleFromDate" ->
                  responsibleFromDate.isEmpty,
                "contactDetails" ->
                  contactDetails.isEmpty,
                "bandReviewReason" ->
                  bandReviewReason.isEmpty,
                "expectedCouncilTaxBand" ->
                  expectedCouncilTaxBand.isEmpty
              )
                .collect {
                  case (name, true) =>
                    name
                }
                .mkString(", ")

            logger.error(
              s"Unable to submit enquiry because the following " +
                s"required values are missing: $missingValues"
            )

            Future.successful(
              Redirect(
                controllers.routes.JourneyRecoveryController
                  .onPageLoad()
              )
            )
        }
    }
}