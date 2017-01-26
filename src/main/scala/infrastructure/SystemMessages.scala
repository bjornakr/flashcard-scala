package infrastructure

import java.util.UUID

trait SystemMessage {
    def message: String
}

object SystemMessages {
    case class GeneralError(errorMessage: String) extends SystemMessage {
        def message = errorMessage
    }

    case class InvalidIdFormat(id: String) extends SystemMessage {
        def message = "Invalid UUID format: " + id
    }

    case class InvalidId(entity: String, id: UUID) extends SystemMessage {
        def message = "Could not find " + entity + " with ID: " + id
    }

    case class CannotBeEmpty(field: String) extends SystemMessage {
        def message = "Parameter \"" + field + "\" cannot be empty."
    }

    case class DatabaseError(errorMessage: String) extends SystemMessage {
        def message = errorMessage
    }

}
