package models

import com.github.tototoshi.csv._
import java.io.File

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait CsvParser {

  type T

  val filePath: String = "resources/"

  /**
    * Gets all the data in the file as obj
    * @param fileName the name of the file
    * @return
    */
  def getAll(fileName: String): Future[Vector[T]] = Future(
      reader(
      filePath + fileName
    ).allWithHeaders().map(fromMap).toVector
  )

  def getAllSync(fileName: String): Vector[T] = reader(
      filePath + fileName
    ).allWithHeaders().map(fromMap).toVector

  /**
    * Gets an obj from mapped csv data,
    * an id is required at least
    * @param data the data to map from csv row
    * @return
    */
  def fromMap(data: Map[String, String]): T

  /**
    * Reads a csv file, let the errors bubble up is anything is not correct about the file
    * @param filePath the path to the file
    * @return
    */
  def reader(filePath: String): CSVReader = CSVReader.open(new File(filePath))

  /**
    * Filters out the empty string value
    * @param optValue the data valu to filter
    * @return
    */
  def sanitizeString(optValue: Option[String]): Option[String] = optValue.filter(_.trim.nonEmpty)

}
