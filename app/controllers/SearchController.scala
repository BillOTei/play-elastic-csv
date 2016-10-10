package controllers

import models.{Airport, Country}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

class SearchController extends Controller {

  def airportsByCountry(country: String) = Action.async {
    Country.byNameOrCode(country).flatMap {
      cs => Future.sequence(cs.map(Airport.byCountry))
    }.map {
      airports => if (airports.isEmpty) NotFound else Ok(Json.toJson(airports))
    }.recover {
      case e: Exception => BadRequest(e.getMessage)
      case _ => InternalServerError
    }
  }

}
