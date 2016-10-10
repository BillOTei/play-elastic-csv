package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Country(
                    id: Long,
                    name: String,
                    code: String,
                    continent: Option[String] = None,
                    wikiUrl: Option[String] = None,
                    keywords: Option[List[String]] = None,
                    airportsCount: Option[Int] = None,
                    runwayTypes: Option[List[String]] = None
                  )

object Country extends CsvParser {

  type T = Country

  lazy val countries = getAll("countries.csv")
  implicit val jsonWrites: Writes[Country] = new Writes[Country] {
    override def writes(o: Country): JsValue = Json.obj(
      "id" -> o.id,
      "code" -> o.code,
      "name" -> o.name,
      "continent" -> o.continent,
      "wikipedia_link" -> o.wikiUrl,
      "keywords" -> Json.toJson(o.keywords.getOrElse(Nil)),
      "airports_count" -> o.airportsCount
    )
  }
  implicit val jsonReads: Reads[Country] = (
    (JsPath \ "id").read[Long] and
      (JsPath \ "code").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "continent").readNullable[String] and
      (JsPath \ "wikipedia_link").readNullable[String] and
      (JsPath \ "keywords").readNullable[List[String]] and
      (JsPath \ "airports_count").readNullable[Int] and
      (JsPath \ "runway_types").readNullable[List[String]]
    ) (Country.apply _)
  implicit val countryVectorWrites: Writes[Vector[Country]] = (JsPath \ "countries").lazyWrite(Writes.seq[Country](jsonWrites))

  override def fromMap(data: Map[String, String]): Country = Country(
    id = data.get("id").map(_.toLong).get, // Should fail and bubble up if no id
    name = sanitizeString(data.get("name")).get,
    code = sanitizeString(data.get("code")).get,
    continent = sanitizeString(data.get("continent")),
    wikiUrl = sanitizeString(data.get("wikipedia_link")),
    keywords = sanitizeString(data.get("keywords")).map(_.split(',').toList.map(_.trim))
  )

  /**
    * Gets the country with fuzzy search either code
    * if 2 chars long or name if more
    *
    * @param queryStr the string to search
    * @return
    */
  def byNameOrCode(queryStr: String): Future[Vector[Country]] = {
    val searchStr = queryStr.toLowerCase.trim
    if (searchStr.length < 2) Future.failed(new Exception("pls be more specific: search length > 1 char"))
    else if (searchStr.length > 2) countries map (_.filter(c => nameFuzzyCompare(c, searchStr)))
    else countries map (_.filter(c => c.code.toLowerCase == searchStr))
  }

  private def nameFuzzyCompare(c: Country, needle: String): Boolean =
    c.name.toLowerCase == needle || c.name.toLowerCase.contains(needle)

  def byCode(code: String): Future[Option[Country]] = countries.map(_.find(_.code == code))

}
