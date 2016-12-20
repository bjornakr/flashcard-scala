package application

import domain.{Back, Card, Front}
import infrastructure.{SystemMessage, SystemMessages}

object CardRequestMapper {
    private def checkEmpty(request: Dto.CreateCardRequest): Either[SystemMessage, Dto.CreateCardRequest] = {
        def cannotBeEmpty(field: String) = Left(SystemMessages.CannotBeEmpty(field))

        request match {
            case Dto.CreateCardRequest(None, _, _) => cannotBeEmpty("front")
            case Dto.CreateCardRequest(_, None, _) => cannotBeEmpty("back")
            case Dto.CreateCardRequest(_, _, None) => cannotBeEmpty("exampleOfUse")
            case _ => Right(request)
        }
    }

    def apply(request: Dto.CreateCardRequest): Either[SystemMessage, Card] =
        for {
            req <- checkEmpty(request).right
            front <- Front(req.front.getOrElse("")).right
            back <- Back(req.back.getOrElse(""), req.exampleOfUse.getOrElse("")).right
        } yield Card(front, back)
}
