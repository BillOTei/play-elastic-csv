package models

import javax.inject.Inject

import com.evojam.play.elastic4s.configuration.ClusterSetup
import com.evojam.play.elastic4s.{PlayElasticFactory, PlayElasticJsonSupport}
import com.sksamuel.elastic4s.{BulkResult, ElasticDsl, IndexAndType}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// Todo make that generic trait
class ElasticDao @Inject()(cs: ClusterSetup, elasticFactory: PlayElasticFactory)
  extends ElasticDsl with PlayElasticJsonSupport {

  lazy val client = elasticFactory(cs)

  /**
    * Gets an airport by id
    * @param mId the identifier
    * @return
    */
  def byId(mId: Long): Future[Array[Airport]] = searchBy("id", mId)

  def byCountry(cStr: String): Future[Array[Airport]] = searchBy("iso_country", cStr.toUpperCase)

  private def searchBy(fieldName: String, value: Any): Future[Array[Airport]] = client execute {
    search in IndexAndType("lunatech", "airport") query matchQuery(fieldName, value)
  } map (_.as[Airport])

  /**
    * Writes data to lunatech/airport
    * @param models the data vector
    * @return
    */
  def bulkIndex(models: Vector[Airport]): Future[BulkResult] = client execute {
    bulk {
      models map (m => index into IndexAndType("lunatech", "airport") source m)
    }
  }

}
