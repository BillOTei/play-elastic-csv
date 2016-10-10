package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

/**
  * The airport model
  *
  * @param id               the unique id is the only mandatory field as long
  * @param ident            seems to be an identifier
  * @param aType            the airport type
  * @param name             the airport name
  * @param lat              the latitude of the airport
  * @param long             the longitude of the airport
  * @param elevationFt      the altitude
  * @param continent        the continent where it is located
  * @param country          the country code
  * @param region           the region code where it is located
  * @param municipality     the municipality
  * @param scheduledService whether a service is scheduled or not
  * @param gpsCode          the gps code
  * @param iataCode         no clue
  * @param localCode        no clue whatsoever
  * @param url              the website url of the airport
  * @param wikiUrl          the wiki url if there is any
  * @param keywords         some keywords
  */
case class Airport(
                    id: Long,
                    ident: Option[String],
                    aType: Option[String],
                    name: Option[String],
                    lat: Option[Double],
                    long: Option[Double],
                    elevationFt: Option[Float],
                    continent: Option[String],
                    country: Option[String],
                    region: Option[String],
                    municipality: Option[String],
                    scheduledService: Option[Boolean],
                    gpsCode: Option[String],
                    iataCode: Option[String],
                    localCode: Option[String],
                    url: Option[String],
                    wikiUrl: Option[String],
                    keywords: Option[List[String]],
                    countryObj: Country = null,
                    runways: Option[Vector[Runway]] = None
                  )

object Airport extends CsvParser {

  type T = Airport

  lazy val airports = getAll("airports.csv")
  implicit val jsonWrites: Writes[Airport] = new Writes[Airport] {
    override def writes(o: Airport): JsValue = Json.obj(
      "id" -> o.id,
      "ident" -> o.ident,
      "type" -> o.aType,
      "name" -> o.name,
      "latitude_deg" -> o.lat,
      "longitude_deg" -> o.long,
      "elevation_ft" -> o.elevationFt,
      "continent" -> o.continent,
      "iso_country" -> o.country,
      "iso_region" -> o.region,
      "municipality" -> o.municipality,
      "scheduled_service" -> o.scheduledService,
      "gps_code" -> o.gpsCode,
      "iata_code" -> o.iataCode,
      "local_code" -> o.localCode,
      "home_link" -> o.url,
      "wikipedia_link" -> o.wikiUrl,
      "keywords" -> Json.toJson(o.keywords.getOrElse(Nil)),
      "country_obj" -> o.countryObj,
      "runways" -> Json.toJson(o.runways.getOrElse(Vector.empty))
    )
  }
  implicit val jsonReads: Reads[Airport] = (
    (JsPath \ "id").read[Long] and
      (JsPath \ "ident").readNullable[String] and
      (JsPath \ "type").readNullable[String] and
      (JsPath \ "name").readNullable[String] and
      (JsPath \ "latitude_deg").readNullable[Double] and
      (JsPath \ "longitude_deg").readNullable[Double] and
      (JsPath \ "elevation_ft").readNullable[Float] and
      (JsPath \ "continent").readNullable[String] and
      (JsPath \ "iso_country").readNullable[String] and
      (JsPath \ "iso_region").readNullable[String] and
      (JsPath \ "municipality").readNullable[String] and
      (JsPath \ "scheduled_service").readNullable[Boolean] and
      (JsPath \ "gps_code").readNullable[String] and
      (JsPath \ "iata_code").readNullable[String] and
      (JsPath \ "local_code").readNullable[String] and
      (JsPath \ "home_link").readNullable[String] and
      (JsPath \ "wikipedia_link").readNullable[String] and
      (JsPath \ "keywords").readNullable[List[String]] and
      (JsPath \ "country_obj").read[Country] and
      (JsPath \ "runways").readNullable[Vector[Runway]]
    ) (Airport.apply _)

  override def fromMap(data: Map[String, String]): Airport = Airport(
    id = data.get("id").map(_.toLong).get, // Should fail and bubble up if no id
    ident = sanitizeString(data.get("ident")),
    aType = sanitizeString(data.get("type")),
    name = sanitizeString(data.get("name")),
    long = sanitizeString(data.get("longitude_deg")).map(_.toDouble),
    lat = sanitizeString(data.get("latitude_deg")).map(_.toDouble),
    elevationFt = sanitizeString(data.get("elevation_ft")).map(_.toFloat),
    continent = sanitizeString(data.get("continent")),
    country = sanitizeString(data.get("iso_country")),
    region = sanitizeString(data.get("iso_region")),
    municipality = sanitizeString(data.get("municipality")),
    scheduledService = sanitizeString(data.get("scheduled_service")).map(_ == "yes"),
    gpsCode = sanitizeString(data.get("gps_code")),
    iataCode = sanitizeString(data.get("iata_code")),
    localCode = sanitizeString(data.get("local_code")),
    url = sanitizeString(data.get("home_link")),
    wikiUrl = sanitizeString(data.get("wikipedia_link")),
    keywords = sanitizeString(data.get("keywords")).map(_.split(',').toList.map(_.trim))
  )

  /**
    * Gets all the airports by input country name or code
    *
    * @param c fuzzy name or code
    * @return
    */
  def byCountry(c: Country): Future[Vector[Airport]] = airports.flatMap {
    apts => Future.sequence(apts.filter(_.country.getOrElse("").toLowerCase == c.code.toLowerCase).map {
      a => Runway.byAirport(a).map {
        rs => a.copy(runways = Some(rs), countryObj = c)
      }
    })
  }

  def allFull: Future[Vector[Airport]] = airports.flatMap {
    apts => Future.sequence(apts.map {
      a => for {
        rs <- Runway.byAirport(a)
        c <- Country.byCode(a.country.get)
      } yield a.copy(countryObj = c.get, runways = Some(rs))
    })
  }

  /**
    * @todo optimize
    *       Gets the list of airport runways by country
    * @return
    */
  def runwaysGroupedByCountry: Future[Map[Option[String], Vector[Runway]]] = airports.flatMap {
    apts => Future.sequence(apts.map {
      a => Runway.byAirport(a).map {
        rs => a.copy(runways = Some(rs))
      }
    }).map(aps => aps.groupBy(_.country).map(grp => (grp._1, grp._2.flatMap(_.runways.getOrElse(Vector.empty)))))
  }

  /**
    * Gets the list of countries with airports count
    *
    * @return
    */
  def countByCountry: Future[Vector[Country]] = airports.flatMap {
    apts => Future.sequence(apts.groupBy(_.country).map {
      gp => Country.byCode(gp._1.get).map(_.get.copy(airportsCount = Some(gp._2.length)))
    })
  }.map(_.toVector.sortBy(_.airportsCount))

}
