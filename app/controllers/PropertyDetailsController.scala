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
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.{NormalMode, UserAnswers}
import navigation.Navigator
import pages.{PropertyDetailsPage, VosPropertyIdPage}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.PropertyDetailsView
import repositories.{ExplorePropertyRepo, SessionRepository}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyDetailsController @Inject()(
                                           val controllerComponents: MessagesControllerComponents,
                                           identify: IdentifierAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           view: PropertyDetailsView,
                                           connector: BridgeIntegrationConnector,
                                           navigator: Navigator,
                                           explorePropertyRepo: ExplorePropertyRepo,
                                           sessionRepository: SessionRepository
                                         )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] =
    identify.async { implicit request =>
      val hc =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val userId =
        hc.sessionId
          .map(_.value)
          .getOrElse(request.userId)

      val propertyReference =
        request.session.get("propertyReference").getOrElse("")

      if (propertyReference.isEmpty) {
        logger.warn("No propertyReference found in session")

        Future.successful(
          Redirect(routes.SearchController.onPageLoad())
        )
      } else {
        connector.explore(propertyReference, "CVW")(hc).flatMap {

          case Right(result) =>
            logger.info(
              s"Explore result received for property reference $propertyReference"
            )

            explorePropertyRepo
              .upsert(userId, result)
              .map { _ =>
                Ok(view(result))
              }

          case Left(error) =>
            logger.error(
              s"Explore call failed for reference $propertyReference. " +
                s"Status=${error.statusCode}, message=${error.message}"
            )

            Future.successful(
              Status(error.statusCode)(error.message)
            )
        }
      }
    }

  def onSubmit(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      val hc =
        HeaderCarrierConverter.fromRequestAndSession(
          request,
          request.session
        )

      val userId =
        hc.sessionId
          .map(_.value)
          .getOrElse(request.userId)

      explorePropertyRepo.findByUserId(userId).flatMap {

        case Some(storedProperty) =>

          val maybeProperty =
            storedProperty.exploreResult.results.records
              .headOption
              .flatMap(_.data.list_entry.property)

          maybeProperty match {

            case Some(property) if property.id.isDefined =>

              request.userAnswers
                .set(PropertyDetailsPage, property)
                .fold(
                  error => {
                    logger.error(
                      s"Unable to add property to UserAnswers for userId: $userId",
                      error
                    )

                    Future.failed(error)
                  },
                  updatedAnswers =>
                    sessionRepository
                      .set(updatedAnswers)
                      .map { stored =>
                        if (stored) {
                          Redirect(
                            navigator.nextPage(
                              PropertyDetailsPage,
                              NormalMode,
                              updatedAnswers
                            )
                          )
                        } else {
                          logger.error(
                            s"Mongo did not acknowledge UserAnswers update for userId: $userId"
                          )

                          InternalServerError(
                            "Unable to save the property"
                          )
                        }
                      }
                )

            case Some(_) =>

              logger.error(
                s"Property found but no vos_property_id was present for userId: $userId"
              )

              Future.successful(
                InternalServerError(
                  "No VOS property identifier was found"
                )
              )

            case None =>

              logger.error(
                s"No property found in the Explore result for userId: $userId"
              )

              Future.successful(
                InternalServerError(
                  "No property was found"
                )
              )
          }

        case None =>

          logger.error(
            s"No stored Explore result found for userId: $userId"
          )

          Future.successful(
            Redirect(routes.SearchController.onPageLoad())
          )
      }
    }
}