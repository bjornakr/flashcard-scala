package repository

import slick.driver.H2Driver.api._
import scala.concurrent.ExecutionContext.Implicits.global

object Dataxase {
    val db = Database.forConfig("h2mem1")
}
