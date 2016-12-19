package application

import domain.{Back, Card, Front}
import infrastructure.{SystemMessage, SystemMessages}

object CardRequestMapper {
    private def checkNull(request: Dto.CreateCardRequest): Either[SystemMessage, Dto.CreateCardRequest] = {
        def cannotBeNull(field: String) = Left(SystemMessages.CannotBeNull(field))

        request match {
            case Dto.CreateCardRequest(null, _, _) => cannotBeNull("front")
            case Dto.CreateCardRequest(_, null, _) => cannotBeNull("back")
            case Dto.CreateCardRequest(_, _, null) => cannotBeNull("exampleOfUse")
            case _ => Right(request)
        }
    }

    def apply(request: Dto.CreateCardRequest): Either[SystemMessage, Card] =
        for {
            req <- checkNull(request).right
            front <- Front(req.front).right
            back <- Back(req.back, req.exampleOfUse).right
        } yield Card(front, back)
}
