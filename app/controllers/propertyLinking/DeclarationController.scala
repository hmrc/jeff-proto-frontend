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

package controllers.propertyLinking

import connectors.BridgeIntegrationConnector
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.propertyLinkingSubmission.{Capacity, CapacityDeclaration, CreatePropertyLink, Evidence, EvidenceType, FileInfo, PropertyLinkRequest, ProvidedEvidence}
import play.api.Logging
import play.api.data.Form
import play.api.data.Forms.{boolean, mapping}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import java.time.{Instant, LocalDate, LocalDateTime}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import views.html.propertyLinking.DeclarationView

class DeclarationController @Inject()(
    val controllerComponents: MessagesControllerComponents,
    identify: IdentifierAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    view: DeclarationView,
    connector: BridgeIntegrationConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean] = Form(
    mapping(
      "declaration" -> boolean
        .verifying("declaration.required", value => value)
    )(identity)(Some(_))
  )

  def show: Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val address = request.session.get("propertyAddress").getOrElse("123 Test Street, BS123C")
    
    val model = CreatePropertyLink(
      uarn = 123456L,
      capacity = Capacity.OWNER,
      startDate = LocalDate.of(2020, 1, 1),
      endDate = Some(LocalDate.of(2023, 10, 1)),
      method = ProvidedEvidence.EXAMPLE,
      PLsubmissionId = "test123",
      createDatetime = LocalDateTime.now(),
      uploadedFiles = Seq(Evidence("rates-bill.pdf", EvidenceType.EXAMPLE)),
      submissionSource = "JEFF"
    )

    Ok(view(form, model, address, false))
  }

  def submit: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    form.bindFromRequest().fold(
      formWithErrors => {
        val address = request.session.get("propertyAddress").getOrElse("123 Test Street, BS123C")
        
        val model = CreatePropertyLink(
          uarn = 123456L,
          capacity = Capacity.OWNER,
          startDate = LocalDate.of(2020, 1, 1),
          endDate = Some(LocalDate.of(2023, 10, 1)),
          method = ProvidedEvidence.EXAMPLE,
          PLsubmissionId = "test123",
          createDatetime = LocalDateTime.now(),
          uploadedFiles = Seq(Evidence("rates-bill.pdf", EvidenceType.EXAMPLE)),
          submissionSource = "JEFF"
        )
        
        Future.successful(BadRequest(view(formWithErrors, model, address, false)))
      },
      _ => {
        val propertyLinkRequest = PropertyLinkRequest(
          uarn = 123456L,
          organisationId = 1L,
          individualId = 1L,
          capacityDeclaration = CapacityDeclaration(Capacity.OWNER.toString, LocalDate.now()),
          linkedDate = Instant.now(),
          linkBasis = "TEST_BILL",
          fileInfo = Seq(FileInfo("evidence123.pdf", "TEST_BILL")),
          submissionId = "test123"
        )

        connector.submitPropertyLink(propertyLinkRequest).map {
          case true => 
            logger.info(s"Property link submitted successfully for ${propertyLinkRequest.submissionId}")
            Redirect(controllers.routes.IndexController.onPageLoad())
          case false =>
            logger.error(s"Failed to submit property link for ${propertyLinkRequest.submissionId}")
            InternalServerError("Failed to submit property link")
        }
      }
    )
  }
}
