package application

object Dto {
    case class CardResponse(id: String, front: String, back: String, exampleOfUse: Option[String])
    trait CardRequest {
        def front: Option[String]
        def back: Option[String]
        def exampleOfUse: Option[String]
    }
    case class CreateCardRequest(front: Option[String], back: Option[String], exampleOfUse: Option[String]) extends CardRequest
    case class UpdateCardRequest(id: Option[String], front: Option[String], back: Option[String], exampleOfUse: Option[String]) extends CardRequest
}
