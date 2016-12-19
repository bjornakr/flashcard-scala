//package integration
//
//import java.util.UUID
//
//import application.Dto.CardResponse
//import application.{CardUseCases, Dto}
//import domain.Card
//import infrastructure.SystemMessages
//import org.scalatest.{Matchers, WordSpec}
//import repository.CardRepository
//
//class CardUseCasesSpec extends WordSpec with Matchers {
//    object TestRepository extends CardRepository {
//        var cards = Map.empty[UUID, Card]
//
//        override def getById(cardId: UUID): Option[Card] =
//            cards.get(cardId)
//
//        override def update(card: Card): Option[Card] = {
//            cards.get(card.id).map(_ => {
//                cards += card.id -> card
//                card
//                })
//        }
//
//        override def delete(cardId: UUID): Option[Card] = {
//            val card = getById(cardId)
//            cards -= cardId
//            card
//        }
//
////        override def save(card: Card): Card = {
////            cards += card.id -> card
////            card
////        }
//    }
//
//    val cardUseCases = new CardUseCases(TestRepository)
//
//
//    "get" when {
//        "uuid format is invalid" should {
//            "give error" in {
//                val invalidId = "invalid id"
//                val result = cardUseCases.get(invalidId)
//                assert(result == Left(SystemMessages.InvalidIdFormat(invalidId)))
//            }
//        }
//
//        "card with specified id does not exist"  should{
//            "give error" in {
//                val invalidId = UUID.randomUUID
//                val result = cardUseCases.get(invalidId.toString)
//                assert(result == Left(SystemMessages.InvalidId("Card", invalidId)))
//            }
//        }
//
//        "valid id" should {
//            "return Card" in {
//                val result = for {
//                    card <- cardUseCases.createCard(Dto.CreateCardRequest("front", "back", "example")).right
//                    retrieved <- cardUseCases.get(card.id).right
//                } yield retrieved
//
//                result should matchPattern { case Right(Dto.CardResponse(_, "front", "back", "example")) => }
//            }
//        }
//    }
//
//    "createCard" when {
//        "front is null" should {
//            "give error" in {
//                val request = Dto.CreateCardRequest(null, "back", "example")
//                val result = cardUseCases.createCard(request)
//                assert(result == Left(SystemMessages.CannotBeNull("front")))
//            }
//        }
//
//        "front is empty" should {
//            "give error" in {
//                val request = Dto.CreateCardRequest(" ", "back", "example")
//                val result = cardUseCases.createCard(request)
//                assert(result == Left(SystemMessages.CannotBeEmpty("front")))
//            }
//        }
//
//        "back is null" should {
//            "give error" in {
//                val request = Dto.CreateCardRequest("front", null, "example")
//                val result = cardUseCases.createCard(request)
//                assert(result == Left(SystemMessages.CannotBeNull("back")))
//            }
//        }
//
//        "back is empty" should {
//            "give error" in {
//                val request = Dto.CreateCardRequest("front", "  ", "example")
//                val result = cardUseCases.createCard(request)
//                assert(result == Left(SystemMessages.CannotBeEmpty("back")))
//            }
//        }
//
//        "exampleOfUse is null" should {
//            "give error" in {
//                val request = Dto.CreateCardRequest("front", "back", null)
//                val result = cardUseCases.createCard(request)
//                assert(result == Left(SystemMessages.CannotBeNull("exampleOfUse")))
//            }
//        }
//
//        "valid parameters" should {
//            "return valid card" in {
//                val request = Dto.CreateCardRequest("front", "back", "example")
//                val result = cardUseCases.createCard(request)
//                result should matchPattern { case Right(CardResponse(_, "front", "back", "example")) => }
//            }
//        }
//    }
//
//    "delete" when {
//        "uuid format is invalid" should {
//            "give error" in {
//                val invalidId = "invalid id"
//                val result = cardUseCases.delete(invalidId)
//                assert(result == Left(SystemMessages.InvalidIdFormat(invalidId)))
//            }
//        }
//
//        "entity with specified id does not exist" should {
//            "give error" in {
//                val invalidId = "067e6162-3b6f-4ae2-a171-2470b63dff00"
//                val result = cardUseCases.delete(invalidId)
//                assert(result == Left(SystemMessages.InvalidId("Card", UUID.fromString(invalidId))))
//            }
//        }
//
//        "valid id" should {
//            "delete card" in {
//                val result = for {
//                    card <- cardUseCases.createCard(Dto.CreateCardRequest("front", "back", "")).right
//                    deleteResponse <- cardUseCases.delete(card.id).right
//                    getResponse <- cardUseCases.get(deleteResponse.id).right
//                } yield getResponse
//
//                result should matchPattern { case Left(SystemMessages.InvalidId("Card", _)) => }
//            }
//
//            "return card" in {
//                val result = for {
//                    card <- cardUseCases.createCard(Dto.CreateCardRequest("front", "back", "example")).right
//                    deleteResponse <- cardUseCases.delete(card.id).right
//                } yield deleteResponse
//
//                result should matchPattern { case Right(Dto.CardResponse(_, "front", "back", "example")) => }
//            }
//        }
//    }
//}
