package application

import domain.{Back, Card, Front}
import infrastructure.SystemMessage

// TODO: I suspect this is not used? Delete.
object UpdateCardRequestMapper {
    def apply(request: Dto.UpdateCardRequest): Either[SystemMessage, Card] =
        for {
//            id <- UuidParser(request.id)
            front <- Front(request.front.getOrElse("")).right
            back <- Back(request.back.getOrElse(""), request.exampleOfUse).right
        } yield Card(front, back)
}
