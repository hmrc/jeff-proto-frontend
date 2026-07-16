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
import models.properties.{PostcodeSearchResult, StoredVMVProperties}
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
case class FindPropertyRepo @Inject()(
                                        mongo: MongoComponent,
                                        config: FrontendAppConfig
                                      )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[StoredVMVProperties](
    collectionName = "findAProperty",
    mongoComponent = mongo,
    domainFormat = StoredVMVProperties.format,
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
          .partialFilterExpression(Filters.gte("userId", ""))
      )
    )
  ) with Logging {

  override lazy val requiresTtlIndex: Boolean = false

  def upsert(userId: String, postcodeSearchResult: PostcodeSearchResult): Future[Boolean] = {
    val document = StoredVMVProperties(userId, postcodeSearchResult)
    val errorMsg = s"VMV properties have not been inserted"

    collection
      .replaceOne(
        filter = equal("userId", userId),
        replacement = document,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .transformWith {
        case Success(result) =>
          logger.info(s"VMV properties have been upserted for userId: $userId")
          result.wasAcknowledged()
          Future.successful(true)

        case Failure(exception) =>
          logger.error(errorMsg)
          Future.failed(new IllegalStateException(s"$errorMsg: ${exception.getMessage} ${exception.getCause}"))
      }
  }

  def findByUserId(userId: String): Future[Option[StoredVMVProperties]] =
    collection.find(equal("userId", userId)).headOption()
}

