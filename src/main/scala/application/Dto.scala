package application

object Dto {
    case class CardResponse(id: String, front: String, back: String, exampleOfUse: String)
    case class CreateCardRequest(front: Option[String], back: Option[String], exampleOfUse: Option[String])
}
