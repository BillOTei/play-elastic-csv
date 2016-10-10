package controllers

import javax.inject.Inject

import com.evojam.play.elastic4s.PlayElasticFactory
import com.evojam.play.elastic4s.configuration.ClusterSetup
import com.sksamuel.elastic4s.ElasticDsl
import models.{Airport, ElasticDao}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ElasticController @Inject()(cs: ClusterSetup, elasticFactory: PlayElasticFactory, configuration: Configuration, elasticDao: ElasticDao) extends
  Controller with ElasticDsl {

  private lazy val client = elasticFactory(cs)
  private val logger = Logger(getClass)

  def getStats = Action.async {
    try {
      client execute {
        get cluster stats
      } map (response => Ok(response.toString))
    } catch {
      case e: Exception =>
        logger.error("Error connecting to Elasticsearch", e)
        Future(InternalServerError("Error connecting to Elasticsearch. Is application.conf filled in properly?\n"))
    }
  }

  def createIndex = Action.async {
    client execute {
      create index "lunatech" replicas 0
    } map { _ => Ok("Index lunatech created") }
  }

  def loadData = Action.async {
    for {
      airports <- Airport.allFull
      result <- elasticDao.bulkIndex(airports)
    } yield result match {
      case resp if !resp.hasFailures => Ok("data imported")
      case resp => InternalServerError(resp.failures.map(f => f.failureMessage) mkString ";")
    }
  }

  def getAirportById(id: Long) = Action.async {
    elasticDao.byId(id).map {
      optApt => if (optApt.isEmpty) NotFound else Ok(Json.toJson(optApt))
    }
  }

  def airportsByCountry(country: String) = Action.async {
    elasticDao.byCountry(country).map {
      optApt => if (optApt.isEmpty) NotFound else Ok(Json.toJson(optApt.length))
    }
  }

}
