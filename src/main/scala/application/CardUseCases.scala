package application

import java.time.ZonedDateTime
import java.util.UUID

import application.Dto.UpdateCardRequest
import com.typesafe.scalalogging.Logger
import domain.Card
import infrastructure.{SystemMessage, SystemMessages}
import repository.CardDao

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class CardUseCases(logger: Logger, cardDao: CardDao) {


    def getAll: Either[SystemMessage, Seq[Dto.CardResponse]] = {
        val future = cardDao.getAll
        Await.ready(future, DurationInt(3).seconds).value.get match {
            case Success(t) => Right(t.map(CardResponseMapper(_)))
            case Failure(e) => {
                logger.error("CardDao.getAll", e)
                Left(SystemMessages.DatabaseError(e.getMessage))
            }
        }
    }

    def get(id: String): Either[SystemMessage, Dto.CardResponse] = {
        getWithStringId(id).right.map(CardResponseMapper(_))
    }

    private def getWithStringId(id: String): Either[SystemMessage, Card] = {
        for {
            uuid <- parseUuid(id).right
            card <- getWithUuid(uuid).right
        } yield card
    }

    private def getWithUuid(id: UUID): Either[SystemMessage, Card] = {
        val future = cardDao.getById(id).map {
            case None => Left(SystemMessages.InvalidId("Card", id))
            case Some(c) => Right(c)
        }


        Await.ready(future, DurationInt(3).seconds).value.get match {
            case Success(t) => t
            case Failure(e) => {
                logger.error("CardDao.findById", e)
                Left(SystemMessages.GeneralError(e.getMessage))
            }
        }
    }


    //    def getFuture(id: String): Future[Either[SystemMessage, Dto.CardResponse]] = {
    //        parseUuid(id) match {
    //            case Left(a) => Future(Left(a))
    //            case Right(uuid) => getWithUuidFuture(uuid)
    //        }
    //    }

    def win(id: String): Either[SystemMessage, Dto.CardResponse] = {
        parseUuid(id).right.flatMap(uuid => actionWithUpdate(uuid)(Card.win(ZonedDateTime.now)))
    }

    def lose(id: String): Either[SystemMessage, Dto.CardResponse] = {
        parseUuid(id).right.flatMap(uuid => actionWithUpdate(uuid)(Card.lose(ZonedDateTime.now)))
    }

    //        for {
        //            uuid <- parseUUID(id).right
        //            c1 <- winWithUuid(uuid).right
        //        } yield c1


        //        card match {
        //            case Some(c) => Right(CardResponseMapper(c))
        //            case None => Left(SystemMessages.InvalidId("Card", id))
        //        }
//    }


    //    private def winwin(card: Card): Future[Either[SystemMessage, Dto.CardResponse]] = {
    //        val futUpd = cardRepository.update(card.win(ZonedDateTime.now))
    //        futUpd.onFailure {
    //            case e => {
    //                logger.error("CardDao.update", e)
    //                Left(SystemMessages.GeneralError(e.getMessage))
    //            }
    //        }
    //        futUpd.map(card => Right(CardResponseMapper(card)))
    //    }
    //
    private def winWithUuid(id: UUID): Either[SystemMessage, Dto.CardResponse] = {
        val future = cardDao.getById(id)

        //        future.map {
        //            case None => Left(SystemMessages.InvalidId("Card", id))
        //            case Some(card: Card) => {
        //                logger.info("Found card, intenting to save.")
        //                val c = card.win(ZonedDateTime.now)
        //                val updateFuture = cardDao.update(c)
        //
        //                updateFuture.onFailure { case e =>
        //                    logger.error("CardDao.update", e)
        //                    Left(SystemMessages.DatabaseError(e.getMessage))
        //                }
        //
        //                Right(CardResponseMapper(c))
        //            }
        //        }

        Await.ready(future, DurationInt(3).seconds).value.get match {
            case Success(cardOption) => cardOption match {
                case None => Left(SystemMessages.InvalidId("Card", id))
                case Some(card: Card) => {
                    logger.info("Found card, intenting to save.")
                    val c = card.win(ZonedDateTime.now)
                    val updateFuture = cardDao.update(c)

                    updateFuture.onFailure { case e =>
                        logger.error("CardDao.update", e)
                        Left(SystemMessages.DatabaseError(e.getMessage))
                    }

                    Right(CardResponseMapper(c))
                }
            }
            case Failure(e) => {
                logger.error("CardDao.getAll", e)
                Left(SystemMessages.DatabaseError(e.getMessage))
            }

        }

    }

    private def actionWithUpdate(id: UUID)(action: (Card) => Card): Either[SystemMessage, Dto.CardResponse] = {
        val future = cardDao.getById(id)

        Await.ready(future, DurationInt(3).seconds).value.get match {
            case Success(cardOption) => cardOption match {
                case None => Left(SystemMessages.InvalidId("Card", id))
                case Some(card: Card) => {
                    logger.info("Found card, intenting to save.")
                    val c = action(card)
                    val updateFuture = cardDao.update(c)

                    updateFuture.onFailure { case e =>
                        logger.error("CardDao.update", e)
                        Left(SystemMessages.DatabaseError(e.getMessage))
                    }

                    Right(CardResponseMapper(c))
                }
            }
            case Failure(e) => {
                logger.error("CardDao.getAll", e)
                Left(SystemMessages.DatabaseError(e.getMessage))
            }

        }

    }

    //        val clux = future.map {
    //            case None => Future(Left(SystemMessages.InvalidId("Card", id)))
    //            case Some(card) => winwin(card)
    //        }
    //
    //                        clux.onFailure {
    //                            case e => {
    //                                logger.error("CardDao.findById", e)
    //                                Left(SystemMessages.GeneralError(e.getMessage))
    //                            }
    //                        }
    //
    //                        clux
    //    }


    //    private def winWithUuid(id: UUID): Future[Either[SystemMessage, Dto.CardResponse]] = {
    //
    //
    //        Await.ready(cardRepository.getById(id), DurationInt(3).seconds).value.get match {
    //            case Success(c1) => {
    //
    //            }
    //            case Failure(e) => {
    //                logger.error("CardDao.findById", e)
    //                //logger.error(s"${e.getClass.getCanonicalName}: ${e.getMessage}")
    //                Left(SystemMessages.GeneralError(e.getMessage))
    //            }
    //        }
    //
    //
    //        val cardOption = for {
    //            c1 <- cardRepository.getById(id)
    //            c2 <- cardRepository.update(c1.win(ZonedDateTime.now))
    //        } yield c2
    //
    //        cardOption match {
    //            case None => Left(SystemMessages.InvalidId("Card", id))
    //            case Some(cr) => Right(CardResponseMapper(cr))
    //        }
    //    }

    def createCard(request: Dto.CreateCardRequest): Either[SystemMessage, Dto.CardResponse] =
        CardRequestMapper(request) match {
            case Left(e) => Left(e)
            case Right(card) => {
                val future = cardDao.save(card)

                Await.ready(future, DurationInt(3).seconds).value.get match {
                    case Success(_) => getWithUuid(card.id).right.map(CardResponseMapper(_))
                    case Failure(e) => {
                        logger.error("CardDao.createCard", e)
                        Left(SystemMessages.GeneralError(e.getMessage))
                    }
                }
            }
        }


    def updateCard(request: UpdateCardRequest): Either[SystemMessage, Dto.CardResponse] =
        request.id match {
            case None => Left(SystemMessages.CannotBeEmpty("id"))
            case Some(id) => {
                getWithStringId(id) match {
                    case Left(e) => Left(e)
                    case Right(card) => {
                        CardRequestMapper(request) match {
                            case Left(e) => Left(e)
                            case Right(newCard) => {
                                val updatedCard = Card.updateFaces(card, newCard)
                                val future = cardDao.update(updatedCard)

                                Await.ready(future, DurationInt(3).seconds).value.get match {
                                    case Success(_) => Right(CardResponseMapper(updatedCard))
                                    case Failure(e) => {
                                        logger.error("CardDao.updateCard", e)
                                        Left(SystemMessages.GeneralError(e.getMessage))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


    //    def updateCard2(request: UpdateCardRequest): Either[SystemMessage, Dto.CardResponse] =
    //        request.id match {
    //            case None => Left(SystemMessages.CannotBeEmpty("id"))
    //            case Some(id) => {
    ////                val (x: Either[SystemMessage, (Future[Either[SystemMessage, Card]], Card)]) = for {
    //                val future = for {
    //                    card <- getWithStringId(id).right
    //                    newCard <- CardRequestMapper(request).right
    //                    future <- Card.updateFaces(card, newCard)
    //                } yield future
    ////                } yield (future, newCard)
    //
    //                future.right.map ((f:Future[Either[SystemMessage, Card]]) => {
    //                    Await.ready(f, DurationInt(3).seconds).value.get match {
    //                        case Success(_) => Right(CardResponseMapper(b))
    //                        case Failure(e) => {
    //                            logger.error("CardDao.updateCard", e)
    //                            Left(SystemMessages.GeneralError(e.getMessage))
    //                        }
    //                    }
    //                })
    //            }
    //        }


    //    def delete(id: String): Either[SystemMessage, Dto.CardResponse] = {
    //        val uuid = try {
    //            Right(UUID.fromString(id))
    //        }
    //        catch {
    //            case e: IllegalArgumentException => Left(SystemMessages.InvalidIdFormat(id))
    //        }
    //
    //        for {
    //            uuid <- uuid.right
    //            cardResponse <- deleteWithUuid(uuid).right
    //        } yield cardResponse
    //    }

    //    private def deleteWithUuid(id: UUID): Either[SystemMessage, Dto.CardResponse] = {
    //        cardRepository.delete(id) match {
    //            case None => Left(SystemMessages.InvalidId("Card", id))
    //            case Some(cr) => Right(CardResponseMapper(cr))
    //        }
    //    }

    def delete(id: String): Either[SystemMessage, Unit] = {
        for {
            uuid <- parseUuid(id).right
            result <- deleteWithUuid(uuid).right
        } yield result
    }

    def deleteWithUuid(id: UUID): Either[SystemMessage, Unit] = {
        val future = cardDao.delete(id)

        Await.ready(future, DurationInt(3).seconds).value.get match {
            case Success(affectedRowsCount) => {
                if (affectedRowsCount > 0) Right()
                else Left(SystemMessages.InvalidId("Card", id))
            }
            case Failure(e) => {
                logger.error("CardDao.delete", e)
                Left(SystemMessages.GeneralError(e.getMessage))
            }
        }
    }


    //    private def getWithUuidFuture(id: UUID): Future[Either[SystemMessage, Dto.CardResponse]] = {
    //        val future = cardDao.getById(id).map {
    //            case None => Left(SystemMessages.InvalidId("Card", id))
    //            case Some(c) => Right(CardResponseMapper(c))
    //        }
    //        future.onFailure { case e =>
    //            logger.error("CardDao.findById", e)
    //            //logger.error(s"${e.getClass.getCanonicalName}: ${e.getMessage}")
    //            Left(SystemMessages.GeneralError(e.getMessage))
    //        }
    //
    //        future
    //    }


    private def parseUuid(id: String): Either[SystemMessage, UUID] =
        try {
            Right(UUID.fromString(id))
        }
        catch {
            case e: IllegalArgumentException => Left(SystemMessages.InvalidIdFormat(id))
        }

}
