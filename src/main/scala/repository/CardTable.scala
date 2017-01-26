package repository

import java.util.UUID

import domain._
import slick.driver.H2Driver.api._
import slick.lifted.ProvenShape

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


case class CardDto(id: String, front: String, back: String, exampleOfUse: Option[String],
                   wins: Int, losses: Int, winStreak: Int) {
    val toDomain = {
        val stats = new CardStatistics(None, Wins(wins), Losses(losses), WinStreak(winStreak)) {}
        new Card(UUID.fromString(id), new Front(front) {}, new Back(back, exampleOfUse) {}, stats) {}

    }
}

class CardTable(tag: Tag) extends Table[CardDto](tag, "cards") {
    def id = column[String]("id", O.PrimaryKey)

    def front = column[String]("front")

    def back = column[String]("back")

    def exampleOfUse = column[Option[String]]("example_of_use")

    //    def lastVisited = column[Option[ZonedDateTime]]("last_visited")
    def wins = column[Int]("wins")

    def losses = column[Int]("losses")

    def winStreak = column[Int]("win_streak")

    def * : ProvenShape[CardDto] = (id, front, back, exampleOfUse, wins, losses, winStreak) <>(CardDto.tupled, CardDto.unapply)
}


class CardDao(db: Database) {
    // extends TableQuery(new CardTable(_)) {
    type AffectedRowsCount = Int

//    val cardsTable = TableQuery(new CardTable(_))
    val cardsTable = TableQuery[CardTable]


    //    val db = Dataxase.db

    def getAll: Future[Seq[Card]] =
        db.run(cardsTable.result).map(_.map(_.toDomain))

    def getById(id: UUID): Future[Option[Card]] =
        db.run(cardsTable.filter(_.id === id.toString).result).map(_.headOption.map(_.toDomain))

    def save(card: Card): Future[AffectedRowsCount] =
        db.run(cardsTable += cardToDto(card))

    def saveAll(cards: Seq[Card]): Future[Option[AffectedRowsCount]] = {
        val dtos = cards.map(cardToDto(_))
        db.run(cardsTable ++= dtos)
    }

    def delete(uuid: UUID): Future[AffectedRowsCount] = {
        val q = cardsTable.filter(_.id === uuid.toString)
        db.run(q.delete)
    }

//    def update(card: Card): Future[Card] = {
//        val dto = cardToDto(card)
//        db.run(cards.update(dto))
//    }

    private def cardToDto(card: Card): CardDto =
        CardDto(card.id.toString, card.front.text, card.back.text, card.back.exampleOfUse,
            card.stats.wins.get, card.stats.losses.get, card.stats.winStreak.get)

}

//
//object CardDbRepository extends CardRepository {
//    val cards = TableQuery[CardTable]
//    val db = Dataxase.db
//
//    def getAll: Seq[Card] = db.run(cards.result).map(_.foreach(a => a.toDomain))
//
//    override def getById(cardId: UUID): Option[Card] = ???
//        //db.run(cards.result).map(a => a.find(b => b.id == cardId))
//
//    override def update(card: Card): Option[Card] = ???
//
//    override def delete(cardId: UUID): Option[Card] = ???
//
//    override def save(card: Card): Card = ???
//}