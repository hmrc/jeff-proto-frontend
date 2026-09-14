/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.http.NotFoundException
import models.upscan.{Reference, UploadId, UploadStatus}
import repositories.{ProposalRepo, SessionRepository}
import services.UploadProgressTracker
import views.html.challenge.UploadedEvidenceUploadView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import config.FrontendAppConfig
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.*
import controllers.challenge as challengeRoutes
import models.JeffSummaryListRow.*

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext


@Singleton
class UploadedEvidenceController @Inject()(uploadProgressTracker: UploadProgressTracker,
                                           uploadedEvidenceView: UploadedEvidenceUploadView,
                                           identify: IdentifierAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           proposalRepo: ProposalRepo,
                                           mcc: MessagesControllerComponents)(implicit appConfig: FrontendAppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  def onPageLoad(uploadId: UploadId): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      for {
        userAnswers <- proposalRepo.findByCredId(request.userId)
        evidenceType = userAnswers.flatMap(_.uploadEvidence)
        uploadResult <- uploadProgressTracker.getUploadResult(uploadId)
      }
      yield {
        uploadResult match
          case Some(UploadStatus.UploadedSuccessfully(evidenceDocument, mimeType, downloadUrl, size, checksum)) =>
            val downloadUrlString: String = downloadUrl.toString
            proposalRepo.insertEvidenceDocument(request.userId, evidenceDocument, downloadUrlString, uploadId.value)
            uploadProgressTracker.transferToObjectStore(request.userId, downloadUrl, mimeType, checksum, evidenceDocument, fileReference = Reference(uploadId.value), uploadResult.get, appConfig)
            Ok(uploadedEvidenceView(
              buildSuccessSummaryList(evidenceDocument, downloadUrlString),
              uploadId,
              UploadStatus.UploadedSuccessfully(evidenceDocument, mimeType, downloadUrl, size, checksum),
              evidenceType))
          case Some(UploadStatus.InProgress) =>
            Ok(uploadedEvidenceView(
              buildInProgressOrFailedSummaryList("Uploading"),
              uploadId,
              UploadStatus.InProgress,
              evidenceType))
          case Some(UploadStatus.Failed) =>
            Ok(uploadedEvidenceView(
              buildInProgressOrFailedSummaryList("Failed"),
              uploadId,
              UploadStatus.Failed,
              evidenceType))
          case None => BadRequest(s"Upload with id ${uploadId.value} not found")
      }
    }

  def buildSuccessSummaryList(evidenceDocument: String, downloadUrl: String)(implicit messages: Messages): SummaryList = {
    SummaryList(
      Seq(
        JeffSummaryListRow(
          titleMessageKey = evidenceDocument,
          captionKey = None,
          value = Seq(messages("uploadedBusinessRatesBill.uploaded")),
          changeLink = Some(Link(Call("GET", challengeRoutes.routes.RemoveUploadedEvidenceController.onPageLoad.url), "remove-link", "Remove")),
          titleLink = Some(Link(Call("GET", downloadUrl), "file-download-link", "")),
          valueClasses = Some("govuk-tag govuk-tag--green")
        )
      ).map(summarise),
      classes = "govuk-summary-list--long-key"
    )
  }

  def buildInProgressOrFailedSummaryList(uploadStatusString: String)(implicit messages: Messages): SummaryList = {
    SummaryList(
      Seq(
        JeffSummaryListRow(
          uploadStatusString,
          None,
          Seq(""),
          None,
          None,
          None
        )
      ).map(summarise)
    )
  }
}
