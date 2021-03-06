package application

import domain.{Back, Card, Front}
import infrastructure.{SystemMessage, SystemMessages}

object CardRequestMapper {
    def apply(request: Dto.CardRequest): Either[SystemMessage, Card] =
        for {
            front <- Front(request.front.getOrElse("")).right
            back <- Back(request.back.getOrElse(""), request.exampleOfUse).right
        } yield Card(front, back)
}
