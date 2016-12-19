package presentation

import application.{CardUseCases, Dto}
import cats.data.Xor
import infrastructure.{SystemMessage, SystemMessages}
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.dsl.{Root, _}
import org.http4s._
import io.circe.generic.auto._
import io.circe.parser._
import scala.concurrent.ExecutionContext.Implicits.global

import scalaz.concurrent.Task

object TestApi {
    val cardService = new application.CardUseCases

    def decideStatus(message: SystemMessage): Task[Response] = {
        message match {
            case SystemMessages.InvalidIdFormat(_) => BadRequest(message.message)
            case _ => NotFound(message.message)
        }
    }

    val helloService = HttpService {
        case GET -> Root / "hello" / name =>
            Ok(s"Hello, $name")

        case GET -> Root / "cards" / "" => {
            Ok()
            //            val result = cardService.getAll(id)
            //            result match {
            //                case Left(e) => decideStatus(e)
            //                case Right(r) => {
            //                    Ok()
            //                }
            //            }
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

//        case GET -> Root / "cards" / id => {
//            val result = cardService.getFuture(id)
//            result.map {
//                case Left(e) => decideStatus(e)
//                case Right(r) => {
//                    val jsonResult = r.asJson.noSpaces
//                    Ok(jsonResult)
//                }
//            }
//            Ok(result)
//        }



        case request@POST -> Root / "cards" => {
            val body = EntityDecoder.decodeString(request).run
            val card = decode[Dto.CreateCardRequest](body)

            card match {
                case Xor.Left(_) => BadRequest()
                case Xor.Right(c) => {
                    cardService.createCard(c) match {
                        case Left(e) => decideStatus(e)
                        case Right(_) => Created()
                    }
                }

            }


            //            val xy = xx.body.runLog.run.head.decodeUtf8.right.getOrElse("FAIL")


            //            BadRequest(body)
        }
    }
}
