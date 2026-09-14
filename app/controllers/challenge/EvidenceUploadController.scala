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

import config.FrontendAppConfig
import connectors.UpscanConnector
import controllers.actions.IdentifierAction
import controllers.challenge as challengeRoutes
import models.UploadForm
import models.upscan.{Reference, UploadId}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ProposalRepo
import services.UploadProgressTracker
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.challenge.EvidenceUploadView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EvidenceUploadController @Inject()(evidenceUploadView: EvidenceUploadView,
                                         upScanConnector: UpscanConnector,
                                         uploadProgressTracker: UploadProgressTracker,
                                         uploadForm: UploadForm,
                                         authenticate: IdentifierAction,
                                         proposalRepo: ProposalRepo,
                                         mcc: MessagesControllerComponents)(implicit appConfig: FrontendAppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport{

  val attributes: Map[String, String] = Map(
    "accept" -> ".pdf,.png,.jpg,.jpeg",
    "data-max-file-size" -> "25000000",
    "data-min-file-size" -> "10000",
  )

  def onPageLoad(errorCode: Option[String], evidenceType: Option[String]): Action[AnyContent] = {
    (authenticate).async { implicit request =>
      val errorToDisplay: Option[String] = renderError(errorCode)
      val uploadId = UploadId.generate()
      val successRedirectUrl = s"${appConfig.uploadRedirectTargetBase}${challengeRoutes.routes.UploadedEvidenceController.onPageLoad(uploadId).url}"
      val evidenceParameter = evidenceType.map(evidenceValue => s"?evidenceType=$evidenceValue").getOrElse("")
      val errorRedirectUrl = s"${appConfig.jeffFrontendUrl}/upload-business-rates-bill$evidenceParameter"

      for
        upscanInitiateResponse <- upScanConnector.initiate(Some(successRedirectUrl), Some(errorRedirectUrl))
        maybePropertyLinkingUserAnswers <- proposalRepo.findByCredId(request.userId)
        _ <- uploadProgressTracker.requestUpload(uploadId, Reference(upscanInitiateResponse.fileReference.reference))
      yield Ok(
        evidenceUploadView(
          uploadForm(),
          upscanInitiateResponse,
          attributes,
          errorToDisplay,
          evidenceType)
      )
    }
  }
  private def renderError(errorCode: Option[String])(implicit messages: Messages) : Option[String] = {
    errorCode match {
      case Some("InvalidArgument") => Some(messages("uploadBusinessRatesBill.error.noFileSelected"))
      case Some("EntityTooLarge") => Some(messages("uploadBusinessRatesBill.error.exceedsMaximumSize"))
      case Some("EntityTooSmall") => Some(messages("uploadBusinessRatesBill.error.belowMinimumSize"))
      case Some("InvalidFileType") => Some(messages("uploadBusinessRatesBill.error.invalidFileType"))
      case Some("QUARANTINE") => Some(messages("uploadBusinessRatesBill.error.virusDetected"))
      case Some("REJECTED") => Some(messages("uploadBusinessRatesBill.error.problemWithUpload"))
      case Some(reason) if reason.startsWith("UNKNOWN") => Some(messages("uploadBusinessRatesBill.error.problemWithUpload"))
      case Some(reason) => throw new RuntimeException(s"Error in errorToDisplay: unrecognisable error from upscan '$reason'")
      case None => None
    }
  }
}
