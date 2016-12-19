package repository

import java.util.UUID

import domain.Card

import scala.concurrent.Future

trait CardRepository {
    def getAll: Future[Seq[Card]]
    def getById(id: UUID): Future[Option[Card]]
    def create(card: Card): Future[Int]
    def update(card: Card): Option[Card]
    def delete(cardId: UUID): Option[Card]
}
