package models

import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class Runway(
                 id: Long,
                 airportId: Long,
                 airportIdent: String,
                 lengthFt: Option[Float],
                 widthFt: Option[Float],
                 surface: Option[String],
                 lighted: Boolean,
                 closed: Boolean,
                 leIdent: Option[String],
                 leLat: Option[Double],
                 leLong: Option[Double],
                 leElevationFt: Option[Float],
                 leHeadDegT: Option[Double],
                 leDisplacedThresholdFt: Option[Double],
                 heIdent: Option[String] = None,
                 heLat: Option[Double] = None,
                 heLong: Option[Double] = None,
                 heElevationFt: Option[Float] = None,
                 heHeadDegT: Option[Double] = None,
                 heDisplacedThresholdFt: Option[Double] = None
                 )

object Runway extends CsvParser {

  type T = Runway

  lazy val runways = getAll("runways.csv")

  // Todo add he_* data parsing if needed
  override def fromMap(data: Map[String, String]): Runway = Runway(
    id = data.get("id").map(_.toLong).get, // Should fail and bubble up if no id, airportId and ident
    airportId = data.get("airport_ref").map(_.toLong).get,
    airportIdent = sanitizeString(data.get("airport_ident")).get,
    lengthFt = sanitizeString(data.get("length_ft")).map(_.toFloat),
    widthFt = sanitizeString(data.get("width_ft")).map(_.toFloat),
    surface = sanitizeString(data.get("surface")),
    lighted = sanitizeString(data.get("lighted")).map(_ == "1").get,
    closed = sanitizeString(data.get("closed")).map(_ == "1").get,
    leIdent = sanitizeString(data.get("le_ident")),
    leLat = sanitizeString(data.get("le_latitude_deg")).map(_.toDouble),
    leLong = sanitizeString(data.get("le_longitude_deg")).map(_.toDouble),
    leElevationFt = sanitizeString(data.get("le_elevation_ft")).map(_.toFloat),
    leHeadDegT = sanitizeString(data.get("le_heading_degT")).map(_.toDouble),
    leDisplacedThresholdFt = sanitizeString(data.get("le_displaced_threshold_ft")).map(_.toDouble)
  )

  def byAirport(airport: Airport): Future[Vector[Runway]] = runways.map(_.filter {
    _.airportId == airport.id
  })

  implicit val jsonsWrites: Writes[Runway] = new Writes[Runway] {
    override def writes(o: Runway): JsValue = Json.obj(
      "id" -> o.id,
      "airport_ref" -> o.airportId,
      "airport_ident" -> o.airportIdent,
      "length_ft" -> o.lengthFt,
      "width_ft" -> o.widthFt,
      "surface" -> o.surface,
      "lighted" -> o.lighted,
      "closed" -> o.closed,
      "le_ident" -> o.leIdent,
      "le_latitude_deg" -> o.leLat,
      "le_longitude_deg" -> o.leLong,
      "le_elevation_ft" -> o.leElevationFt,
      "le_heading_degT" -> o.leHeadDegT,
      "le_displaced_threshold_ft" -> o.leDisplacedThresholdFt
    )
  }

  implicit val jsonReads: Reads[Runway] = (
    (JsPath \ "id").read[Long] and
    (JsPath \"airport_ref").read[Long] and
    (JsPath \ "airport_ident").read[String] and
    (JsPath \ "length_ft").readNullable[Float] and
    (JsPath \ "width_ft").readNullable[Float] and
    (JsPath \ "surface").readNullable[String] and
    (JsPath \ "lighted").read[Boolean] and
    (JsPath \ "closed").read[Boolean] and
    (JsPath \ "le_ident").readNullable[String] and
    (JsPath \ "le_latitude_deg").readNullable[Double] and
    (JsPath \ "le_longitude_deg").readNullable[Double] and
    (JsPath \ "le_elevation_ft").readNullable[Float] and
    (JsPath \ "le_heading_degT").readNullable[Double] and
    (JsPath \ "le_displaced_threshold_ft").readNullable[Double] and
    (JsPath \ "he_ident").readNullable[String] and
    (JsPath \ "he_latitude_deg").readNullable[Double] and
    (JsPath \ "he_longitude_deg").readNullable[Double] and
    (JsPath \ "he_elevation_ft").readNullable[Float] and
    (JsPath \ "he_heading_degT").readNullable[Double] and
    (JsPath \ "he_displaced_threshold_ft").readNullable[Double]
    )(Runway.apply _)

  implicit val writesMap: Writes[Map[Option[String], Vector[Runway]]] = new Writes[Map[Option[String], Vector[Runway]]] {
    override def writes(m: Map[Option[String], Vector[Runway]]) = {
      Json.toJson(m.map {
        case (countryCode, rws) =>
          countryCode.getOrElse("unknown") -> rws.map(_.surface.getOrElse("unk").toLowerCase.trim.replace("'", "")).distinct
      })
    }
  }

}
