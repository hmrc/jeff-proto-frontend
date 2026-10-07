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

package controllers

import connectors.BridgeIntegrationConnector
import controllers.actions.IdentifierAction
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.ExplorePropertyRepo
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.PropertyDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsController @Inject()(
                                           val controllerComponents: MessagesControllerComponents,
                                           identify: IdentifierAction,
                                           view: PropertyDetailsView,
                                           connector: BridgeIntegrationConnector,
                                           repo: ExplorePropertyRepo
                                         )(implicit ec: ExecutionContext) extends FrontendBaseController
  with I18nSupport with Logging {

  def onPageLoad(): Action[AnyContent] =
    identify.async { implicit request =>
      val hc = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      val userId = hc.sessionId.map(_.value).getOrElse("id")

      val propertyReference = request.session.get("propertyReference").getOrElse("")

      if (propertyReference.isEmpty) {
        logger.warn("No propertyReference found in session")
        Future.successful(Redirect(routes.SearchController.onPageLoad()))
      } else {
        connector.explore(propertyReference, "CVW")(hc).flatMap {

          case Right(result) =>

            logger.info(s"Explore result received: $result")

            repo.upsert(userId, result).map { _ =>
              Ok(view(result))
            }

          case Left(error) =>

            logger.error(
              s"Explore call failed for reference $propertyReference. Status=${error.statusCode}, message=${error.message}"
            )

            Future.successful(
              Status(error.statusCode)(error.message)
            )
        }
      }
    }
}