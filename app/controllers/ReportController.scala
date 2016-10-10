package controllers

import models.{Airport, Country, Runway}
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class ReportController extends Controller {

  def reportCountries = Action.async {
    Airport.countByCountry map {
      countriesList => Ok(
        Json.toJson(
          countriesList.take(10) ++ countriesList.takeRight(10)
        )(Country.countryVectorWrites)
      )
    }
  }

  def reportCountriesRunways = Action.async {
    Airport.runwaysGroupedByCountry.map {
      m => Ok(Json.toJson(m)(Runway.writesMap))
    }
  }

  def reportRunwaysIdent = Action.async {
    Runway.runways.map {
      rws => {
        val ttt = rws.groupBy(_.leIdent.getOrElse("unk").trim.toLowerCase).map(grp => (grp._1, grp._2.length)).toVector
        Ok(Json.toJson(ttt.sortBy(_._2) takeRight 10 toMap))
      }
    }
  }

  def allAirports = Action.async {
    Airport.allFull.map {
      airports => Ok(Json.toJson(airports))
    }
  }

}
