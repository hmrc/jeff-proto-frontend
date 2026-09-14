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

package repositories

import com.google.inject.Singleton
import com.mongodb.client.model.Indexes.descending
import org.bson.BsonDocument
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.*
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Updates.combine
import org.mongodb.scala.{SingleObservable, SingleObservableFuture}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import config.FrontendAppConfig
import models.ProposalUserAnswers
import models.sdes.*
import models.upscan.{UploadId, UploadedFile}

import java.time.{Instant, LocalDate}
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.impl.Promise
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
case class ProposalRepo @Inject()(mongo: MongoComponent,
                                  config: FrontendAppConfig
                                        )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[ProposalUserAnswers](
    collectionName = "propertyLinking",
    mongoComponent = mongo,
    domainFormat = ProposalUserAnswers.format,
    indexes = Seq(
      IndexModel(
        descending("createdAt"),
        IndexOptions()
          .unique(false)
          .name("createdAt")
          .expireAfter(config.timeToLive.toLong, TimeUnit.HOURS)
      ),
      IndexModel(
        ascending("credId.value"),
        IndexOptions()
          .background(false)
          .name("credId.value")
          .unique(true)
          .partialFilterExpression(Filters.gte("credId.value", ""))
      )
    )
  ) with Logging {

  override lazy val requiresTtlIndex: Boolean = false

  private def filterByCredId(credId: String): Bson = equal("credId.value", credId)

  def upsertProperty(propertyLinking: ProposalUserAnswers): Future[Boolean] = {
    val errorMsg = s"Property has not been inserted"

    collection.replaceOne(
      filter = equal("credId.value", propertyLinking.credId),
      replacement = propertyLinking,
      options = ReplaceOptions().upsert(true)
    ).toFuture().transformWith {
      case Success(result) =>
        logger.info(s"PropertyLinking has been upsert for credId: ${propertyLinking.credId}")
        Future.successful(result.wasAcknowledged())
      case Failure(exception) =>
        logger.error(errorMsg)
        Future.failed(new IllegalStateException(s"$errorMsg: ${exception.getMessage} ${exception.getCause}"))
    }
  }

  private def findAndUpdateByCredId(credId: String, updates: Bson*): Future[Option[ProposalUserAnswers]] = {
    collection.findOneAndUpdate(filterByCredId(credId),
        combine(updates :+ Updates.set("createdAt", Instant.now()): _*),
        FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER))
      .toFutureOption()
  }

  def insertCurrentRatepayer(credId: String, currentRatepayer: Boolean, maybeRatepayerDate: Option[String]): Future[Option[ProposalUserAnswers]] = {
    val update = Seq(Updates.set("currentRatepayer.isBeforeApril", currentRatepayer))
    val updates = maybeRatepayerDate match {
      case Some(date) => update :+ Updates.set("currentRatepayer.becomeRatepayerDate", date)
      case None => update
    }
    findAndUpdateByCredId(credId, updates: _*)
  }

  def insertConnectionToProperty(credId: String, connectionToProperty: String): Future[Option[ProposalUserAnswers]] = {
    findAndUpdateByCredId(credId, Updates.set("connectionToProperty", connectionToProperty))
  }

  def insertBusinessRatesBill(credId: String, businessRatesBill: String): Future[Option[ProposalUserAnswers]] = {
    findAndUpdateByCredId(credId,
      Seq(
        Updates.set("businessRatesBill", businessRatesBill),
        Updates.unset("uploadEvidence")
      ): _*
    )
  }

  def insertEvidenceDocument(credId: String, evidenceDocument: String, evidenceDocumentUrl: String, evidenceDocumentUploadId: String): Future[Option[ProposalUserAnswers]] = {
    findAndUpdateByCredId(credId, Updates.combine(Updates.set("evidenceDocument", evidenceDocument), Updates.set("evidenceDocumentUrl", evidenceDocumentUrl), Updates.set("evidenceDocumentUploadId", evidenceDocumentUploadId)))
  }

  def insertRequestSentReference(credId: String, ref: String): Future[Option[ProposalUserAnswers]] = {
    findAndUpdateByCredId(credId, Updates.set("requestSentReference", ref))
  }

  def insertUploadEvidence(credId: String, uploadEvidence: String): Future[Option[ProposalUserAnswers]] = {
    findAndUpdateByCredId(credId, Updates.set("uploadEvidence", uploadEvidence))
  }

  def insertUploadId(credId: String, uploadId: UploadId): Future[Option[ProposalUserAnswers]] = {
    findAndUpdateByCredId(credId, Updates.set("evidenceDocumentUploadId", uploadId.value))
  }

  def insertReferenceNumber(credId: String, ref: String): Future[Option[ProposalUserAnswers]] = {
    findAndUpdateByCredId(credId, Updates.set("referenceNumber", ref))
  }


  def insertUploadedFile(credId: String, uploadedFile: File): Future[Option[ProposalUserAnswers]] = {
    val bson = BsonDocument.parse(Json.stringify(Json.toJson(uploadedFile)))
    findAndUpdateByCredId(
      credId,
      Updates.set("objectStoreFile", bson)
    )
  }


  def deleteEvidenceDocument(credId: String): Future[Boolean] = {
    collection.updateOne(filterByCredId(credId),
        combine(
          Updates.unset("evidenceDocument"),
          Updates.unset("evidenceDocumentUrl"),
          Updates.unset("evidenceDocumentUploadId")
        )
      ).toFuture()
      .map(_.wasAcknowledged())
  }

  def findByCredId(credId: String): Future[Option[ProposalUserAnswers]] = {
    collection.find(
      equal("credId.value", credId)
    ).headOption()
  }

}
