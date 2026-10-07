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

package repositories

import com.google.inject.Singleton
import com.mongodb.client.model.Indexes.{ascending, descending}
import config.FrontendAppConfig
import models.properties.{ExploreResult, StoredExploreProperty}
import org.mongodb.scala.model.*
import org.mongodb.scala.model.Filters.equal
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class ExplorePropertyRepo @Inject()(
                                     mongo: MongoComponent,
                                     config: FrontendAppConfig
                                   )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[StoredExploreProperty](
    collectionName =  "exploreProperty",
    mongoComponent = mongo,
    domainFormat = StoredExploreProperty.format,
    indexes = Seq(
      IndexModel(
        descending("createdAt"),
        IndexOptions()
          .unique(false)
          .name("createdAt")
          .expireAfter(config.timeToLive.toLong, TimeUnit.HOURS)
      ),
      IndexModel(
        ascending("userId"),
        IndexOptions()
          .background(false)
          .name("userId")
          .unique(true)
          .partialFilterExpression(
            Filters.gte("userId", "")
          )
      )
    )
  ) with Logging {

  override lazy val requiresTtlIndex: Boolean = false

  def upsert(
              userId: String,
              exploreResult: ExploreResult
            ): Future[Boolean] = {

    val document =
      StoredExploreProperty(
        userId = userId,
        exploreResult = exploreResult
      )

    val errorMsg =
      "Explore property details have not been inserted"

    collection
      .replaceOne(
        filter = equal("userId", userId),
        replacement = document,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .transformWith {

        case Success(result) =>

          logger.info(
            s"Explore property details have been upserted for userId: $userId"
          )

          Future.successful(
            result.wasAcknowledged()
          )

        case Failure(exception) =>

          logger.error(
            s"$errorMsg: ${exception.getMessage}",
            exception
          )

          Future.failed(
            new IllegalStateException(
              s"$errorMsg: ${exception.getMessage}"
            )
          )
      }
  }

  def findByUserId(
                    userId: String
                  ): Future[Option[StoredExploreProperty]] =
    collection
      .find(equal("userId", userId))
      .headOption()
}
