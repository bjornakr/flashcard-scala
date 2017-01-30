package application

import java.util.UUID

import infrastructure.{SystemMessage, SystemMessages}

object UuidParser {
    def apply(id: String): Either[SystemMessage, UUID] =
        try {
            Right(UUID.fromString(id))
        }
        catch {
            case e: IllegalArgumentException => Left(SystemMessages.InvalidIdFormat(id))
        }
}
