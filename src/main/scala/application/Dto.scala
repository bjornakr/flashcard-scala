package application

object Dto {
    case class CardResponse(id: String, front: String, back: String, exampleOfUse: String)
    case class CreateCardRequest(front: String, back: String, exampleOfUse: String)
}
