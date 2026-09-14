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
import uk.gov.hmrc.http.{NotFoundException, StringContextOps}
import config.FrontendAppConfig
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.JeffSummaryListRow.summarise
import models.upscan.UploadId
import models.{JeffSummaryListRow, Link, ProposalUserAnswers}
import repositories.{FileUploadRepo, ProposalRepo}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import controllers.challenge as challengeRoutes
import views.html.challenge.RemoveUploadedEvidenceView

import java.net.{URI, URL}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RemoveUploadedEvidenceController @Inject()(removeView: RemoveUploadedEvidenceView,
                                                 identify: IdentifierAction,
                                                 getData: DataRetrievalAction,
                                                 requireData: DataRequiredAction,
                                                 proposalRepo: ProposalRepo,
                                                 fileUploadRepo: FileUploadRepo,
                                                 mcc: MessagesControllerComponents)(implicit appConfig: FrontendAppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  def onPageLoad: Action[AnyContent] = {
    (identify andThen getData andThen requireData).async { implicit request =>

      proposalRepo.findByCredId(request.userId).map {
        case Some(propertyLinkingUserAnswers) if isEvidenceExist(propertyLinkingUserAnswers) =>
          val summaryList: SummaryList = buildSummaryList(propertyLinkingUserAnswers.evidenceDocument.get, URI(propertyLinkingUserAnswers.evidenceDocumentUrl.get).toURL)
          Ok(removeView(summaryList, propertyLinkingUserAnswers.evidenceDocumentUploadId.map(UploadId(_)).get))
        case Some(_) => throw new NotFoundException("Fields not found in RemoveBusinessRatesBillController.show()")
        case None => throw new NotFoundException("Property not found in RemoveBusinessRatesBillController.show()")
      }
    }
  }

  def remove: Action[AnyContent] = {
    (identify andThen getData andThen requireData).async { implicit request =>

      proposalRepo.findByCredId(request.userId).flatMap {
        case Some(propertyLinkingUserAnswers) if isEvidenceExist(propertyLinkingUserAnswers) =>
          for {
            deletedEvidence <- proposalRepo.deleteEvidenceDocument(propertyLinkingUserAnswers.credId)
            deletedUpload <- fileUploadRepo.deleteByUploadId(UploadId(propertyLinkingUserAnswers.evidenceDocumentUploadId.getOrElse(throw new NotFoundException("evidenceDocumentUploadId not found in propertyLinking repo"))))
          } yield {
            if (deletedEvidence && deletedUpload) {
              Redirect(challengeRoutes.routes.EvidenceUploadController.onPageLoad(None, propertyLinkingUserAnswers.uploadEvidence))
            } else {
              throw new RuntimeException("Failed to delete evidence document in RemoveBusinessRatesBillController.remove()")
            }
          }
        case Some(_) => throw new NotFoundException("EvidenceDocumentUploadId not found in RemoveBusinessRatesBillController.remove()")
        case None => throw new NotFoundException("Property not found in RemoveBusinessRatesBillController.remove()")
      }
    }
  }

  def buildSummaryList(fileName: String, downloadUrl: URL)(implicit messages: Messages): SummaryList = {
    SummaryList(
      rows = Seq(
        JeffSummaryListRow(
          titleMessageKey = fileName,
          captionKey = None,
          value = Seq(messages("uploadedBusinessRatesBill.uploaded")),
          changeLink = None,
          titleLink = Some(Link(Call("GET", downloadUrl.toString), "file-download-link", "")),
          valueClasses = Some("govuk-tag govuk-tag--green")
        )
      ).map(summarise),
      classes = "govuk-summary-list--long-key"
    )
  }
  
  def isEvidenceExist(proposalUserAnswers: ProposalUserAnswers): Boolean =
      proposalUserAnswers.evidenceDocument.isDefined &&
      proposalUserAnswers.evidenceDocumentUrl.isDefined &&
      proposalUserAnswers.evidenceDocumentUploadId.isDefined
}
