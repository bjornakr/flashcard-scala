package webapi

import application.{CardUseCases, Dto}
import cats.data.Xor
import infrastructure.{SystemMessage, SystemMessages}
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import org.http4s._
import org.http4s.dsl.{Root, _}

import scalaz.concurrent.Task
import Tsf.TimestampFormat

class TestApi(cardService: CardUseCases) {

//    implicit val TimestampFormat : Encoder[ZonedDateTime] with Decoder[ZonedDateTime] = new Encoder[ZonedDateTime] with Decoder[ZonedDateTime] {
//        val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
//        override def apply(a: ZonedDateTime): Json = Encoder.encodeString.apply(a.toString) //  df.format(a.toEpochSecond))
//
//        override def apply(c: HCursor): Result[ZonedDateTime] = Decoder.decodeLong.map(s => ZonedDateTime.now).apply(c)
//    }


    def decideStatus(message: SystemMessage): Task[Response] = {
        message match {
            case SystemMessages.InvalidId(_, _) => NotFound(message.message)
            case SystemMessages.InvalidIdFormat(_) => BadRequest(message.message)
            case SystemMessages.CannotBeEmpty(_) => BadRequest(message.message)
            case SystemMessages.DatabaseError(_) => InternalServerError()
            case _ => NotFound(message.message)
        }
    }


    val helloService = HttpService {
        case GET -> Root / "hello" / name =>
            Ok(s"Hello, $name")

        case GET -> Root / "cards" => {
            val result = cardService.getAll
            result match {
                case Left(e) => decideStatus(e)
                case Right(r) => {
                    val jsonResult = r.asJson.noSpaces
                    Ok(jsonResult)
                }
            }
        }

        case GET -> Root / "cards" / id => {
            val result = cardService.get(id)
            result match {
                case Left(e) => decideStatus(e)
                case Right(r) => {
                    val jsonResult = r.asJson.noSpaces
                    Ok(jsonResult)
                }
            }
        }

        case request@POST -> Root / "cards" => {
            val body = EntityDecoder.decodeString(request).run
            val card = decode[Dto.CreateCardRequest](body)

            val result = card match {
                case Xor.Left(_) => BadRequest("Could not parse Card from body.")
                case Xor.Right(c) => {
                    cardService.createCard(c) match {
                        case Left(e) => decideStatus(e)
                        case Right(r) => Created(r.asJson.noSpaces)
                    }
                }

            }
            result
        }

        case POST -> Root / "cards" / id / "win" => {
            val card = cardService.win(id)

            card match {
                case Left(e) => decideStatus(e)
                case Right(r) => {
                    val jsonResult = r.asJson.noSpaces
                    Ok(jsonResult)
                }
            }
        }


        case request@PUT -> Root / "cards" => {
            val body = EntityDecoder.decodeString(request).run
            val card = decode[Dto.UpdateCardRequest](body)

            card match {
                case Xor.Left(_) => BadRequest("Could not parse Card from body.")
                case Xor.Right(c) => {
                    cardService.updateCard(c) match {
                        case Left(e) => decideStatus(e)
                        case Right(r) => Ok(r.asJson.noSpaces)
                    }
                }
            }
        }

        case DELETE -> Root / "cards" / id => {
            val result = cardService.delete(id)
            result match {
                case Left(e) => decideStatus(e)
                case Right(_) => NoContent()
            }
        }
    }
}
