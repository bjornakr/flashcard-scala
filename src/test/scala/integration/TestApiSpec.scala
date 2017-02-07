package integration


import java.time.ZonedDateTime
import java.util.UUID

import application.{CardResponseMapper, CardUseCases, Dto}
import com.typesafe.scalalogging.Logger
import domain._
import infrastructure.{DateFormat, SystemMessages}
import io.circe.Decoder._
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.auto._
import io.circe.parser._
import org.http4s._
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.dsl._
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, WordSpec}
import org.slf4j.{Logger => UnderlyingLogger}
import repository.{CardDao, CardTable}
import scodec.bits.ByteVector
import slick.driver.H2Driver.api._
import webapi.{Main, TestApi}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class TestApiSpec extends WordSpec with BeforeAndAfter with BeforeAndAfterAll with MockFactory {
//    implicit val TimestampFormat: Encoder[ZonedDateTime] with Decoder[ZonedDateTime] = new Encoder[ZonedDateTime] with Decoder[ZonedDateTime] {
//        override def apply(a: ZonedDateTime): Json = Encoder.encodeString.apply(a.toString) //  df.format(a.toEpochSecond))
//        override def apply(c: HCursor): Result[ZonedDateTime] = Decoder.decodeString.map(s => ZonedDateTime.now).apply(c)
//    }


    val underlyingLoggerMock = stub[UnderlyingLogger]
    // Using a mock logger to prevent real logging.
    // val db = Database.forConfig("h2mem1")
    val db = Database.forURL("jdbc:h2:mem:test1;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")
    //    val main = new Main(new TestApi(new CardUseCases(Logger(underlyingLoggerMock), new CardDao(db))))
    val main = new Main(new TestApi(new CardUseCases(Logger(underlyingLoggerMock), new CardDao(db))))
    val server = main.createServer
    var client = PooledHttp1Client()
    val cardDao = new CardDao(db)

    val card1 = {
        val stats = new CardStatistics(None, new Wins(0) {}, new Losses(0) {}, new WinStreak(0) {}) {}
        new Card(UUID.fromString("00000000-0000-0000-0000-000000000001"),
            new Front("Front 1") {}, new Back("Back 1", Some("ExampleOfUse 1")) {}, stats) {}
    }

    val card2 = {
        val stats = new CardStatistics(None, new Wins(0) {}, new Losses(0) {}, new WinStreak(0) {}) {}
        new Card(UUID.fromString("00000000-0000-0000-0000-000000000002"),
            new Front("Front 2") {}, new Back("Back 2", Some("ExampleOfUse 2")) {}, stats) {}
    }

    val allCards = List(card1, card2)


    def clearDatabase() = {
        val cards = TableQuery[CardTable]
        val dropCardsAction = slick.dbio.DBIO.seq(cards.schema.drop)
        val dropCardsFuture = db.run(dropCardsAction)
        Await.ready(dropCardsFuture, Duration.Inf) //.value.get
    }

    def initDatabase() = {
        val cards = TableQuery[CardTable]
        val setup = slick.dbio.DBIO.seq(cards.schema.create)
        val dbSetupFuture = db.run(setup)
        Await.ready(dbSetupFuture, Duration.Inf).value.get
    }

    def insertCards() = {
        val createPromise = cardDao.saveAll(allCards)
        Await.result(createPromise, Duration.Inf)
    }


    before {
        clearDatabase()
        initDatabase()
        insertCards()
        //        server = main.createServer
        client = PooledHttp1Client()

    }

    after {
        //        server.shutdownNow()
        client.shutdownNow() // I need to shut down the client after each call, otherwise it hangs after a certain no of calls.
    }

    override def afterAll {
        server.shutdownNow()
        //        client.shutdownNow()
        Database.forURL("jdbc:h2:mem:test1").close()
    }

    def extractBody(r: Response): String =
        EntityDecoder.decodeString(r).run

    val baseUri = Uri.fromString("http://localhost:8070/api/cards").valueOr(e => throw e)

    "test" in {
        val helloJames = client.expect[String]("http://localhost:8070/api/hello/James")
        val result = helloJames.run
        assert(result == "Hello, James")
    }


    "Any request" when {
        "database has errors" should {
            "give 500 Internal Server Error" in {
                clearDatabase()

                val request = Request(Method.GET, baseUri)
                def response = client.toHttpService.run(request).run

                assert(response.status == Status.InternalServerError)
            }

            "log errors" in {
                val underlyingLoggerMock2 = mock[UnderlyingLogger]
                val main2 = new Main(new TestApi(new CardUseCases(Logger(underlyingLoggerMock2), new CardDao(db))))
                (underlyingLoggerMock2.isErrorEnabled: () => Boolean).expects().returning(true)
                (underlyingLoggerMock2.error(_: String, _: Throwable)).expects(*, *).once()

                val server2 = main2.createServer(9000)
                val client2 = PooledHttp1Client()



                clearDatabase()
                val request = Request(Method.GET, Uri.fromString("http://localhost:9000/api/cards").valueOr(e => throw e))
                client2.toHttpService.run(request).run


                server2.shutdownNow()

            }
        }
    }


    "GET cards" should {
        val request = Request(Method.GET, baseUri)
        def response = client.toHttpService.run(request).run

        "give status 200 OK" in {
            assert(response.status == Status.Ok)
        }

        "give all cards" in {
            val body = extractBody(response)
            val cards = decode[Seq[Dto.CardResponse]](body).valueOr(e => throw e)
            assert(cards.length == 2)
            assert(cards.contains(CardResponseMapper(card1)))
            assert(cards.contains(CardResponseMapper(card2)))
        }
    }


    "GET cards/<id>" when {
        "malformed uuid" should {
            val malformedUuid = "INVALID-ID"
            val uri = baseUri / malformedUuid
            def response = client.toHttpService.run(Request(Method.GET, uri)).run

            "give 400 Bad Request" in {
                assert(response.status == Status.BadRequest)
            }

            "describe error in body" in {
                val body = extractBody(response)
                assert(body == SystemMessages.InvalidIdFormat(malformedUuid).message)
            }
        }

        "invalid id" should {
            val invalidId = "00000000-0000-0000-0000-000000009999"
            val uri = baseUri / invalidId
            def response = client.toHttpService.run(Request(Method.GET, uri)).run

            "give 404 Not Found" in {
                assert(response.status == Status.NotFound)
            }

            "describe error in body" in {
                val body = extractBody(response)
                assert(body == SystemMessages.InvalidId("Card", UUID.fromString(invalidId)).message)
            }
        }

        "valid id" should {
            "return card" in {
                val uri = baseUri / card1.id.toString
                val response = client.toHttpService.run(Request(Method.GET, uri)).run
                assert(response.status == Status.Ok)

                val body = extractBody(response)
                val cardResponse = decode[Dto.CardResponse](body).valueOr(e => throw e)
                val expectedResponse = Dto.CardResponse(card1.id.toString, card1.front.text, card1.back.text, card1.back.exampleOfUse,
                    Dto.CardStatsResponse(None, 0, 0, 0, 0))

                assert(cardResponse == expectedResponse)
            }
        }
    }

    "POST card" when {

        "badly formulated card" should {
            "give 400 Bad Request w/ error message" in {
                val body = toBody("""{ "thisis": notacard }""")
                val request = Request(Method.POST, baseUri, HttpVersion.`HTTP/1.1`, Headers.empty, body)
                val response = client.toHttpService.run(request).run
                val responseBody = extractBody(response)

                assert(response.status == Status.BadRequest)
                assert(responseBody == "Could not parse Card from body.")
            }
        }

        "valid card" should {
            val body = toBody("{ \"front\": \"A\", \"back\": \"B\", \"exampleOfUse\": \"C\" }")
            val request = Request(Method.POST, baseUri, HttpVersion.`HTTP/1.1`, Headers.empty, body)
            def response = client.toHttpService.run(request).run

            "give 201 created" in {
                assert(response.status == Status.Created)
            }

            "return card in body" in {
                val responseBody = extractBody(response)
                val cardResponse = decode[Dto.CardResponse](responseBody).valueOr(e => throw e)

                val expectedRespone = Dto.CardResponse(cardResponse.id, "A", "B", Some("C"),
                    Dto.CardStatsResponse(None, 0, 0, 0, 0))
                assert(cardResponse == expectedRespone)
            }
        }

        "front text is missing" should {
            val body = toBody("{ \"back\": \"B\", \"exampleOfUse\": \"C\" }")
            val request = Request(Method.POST, baseUri, HttpVersion.`HTTP/1.1`, Headers.empty, body)
            def response = client.toHttpService.run(request).run

            "give 400 Bad Request" in {
                assert(response.status == Status.BadRequest)
            }

            "describe error in body" in {
                val responseBody = extractBody(response)
                assert(responseBody == SystemMessages.CannotBeEmpty("front").message)

            }
        }

        "back text is missing" should {
            val body = toBody("{ \"front\": \"A\" }")
            val request = Request(Method.POST, baseUri, HttpVersion.`HTTP/1.1`, Headers.empty, body)

            def response = client.toHttpService.run(request).run

            "give 400 Bad Request" in {
                assert(response.status == Status.BadRequest)
            }

            "describe error in body" in {
                val responseBody = extractBody(response)
                assert(responseBody == SystemMessages.CannotBeEmpty("back").message)
            }
        }
    }

    "DELETE card" when {
        "valid id" should {
            "give 204 No Content" in {
                val uri = baseUri / "00000000-0000-0000-0000-000000000001"
                val request = Request(Method.DELETE, uri, HttpVersion.`HTTP/1.1`, Headers.empty, EmptyBody)
                def response = client.toHttpService.run(request).run
                assert(response.status == Status.NoContent)
            }

            "delete card" in {
                val uri = baseUri / "00000000-0000-0000-0000-000000000001"
                val deleteRequest = Request(Method.DELETE, uri, HttpVersion.`HTTP/1.1`, Headers.empty, EmptyBody)
                client.toHttpService.run(deleteRequest).run

                val getRequest = Request(Method.GET, uri, HttpVersion.`HTTP/1.1`, Headers.empty, EmptyBody)
                def response = client.toHttpService.run(getRequest).run
                assert(response.status == Status.NotFound)
            }
        }

        "invalid id" should {
            "give 404 Not Found" in {
                val uri = baseUri / "99999999-9999-9999-9999-999999999999"
                val request = Request(Method.DELETE, uri, HttpVersion.`HTTP/1.1`, Headers.empty, EmptyBody)
                def response = client.toHttpService.run(request).run
                assert(response.status == Status.NotFound)

            }
        }
    }


    "PUT card" when {

        "valid card" should {
            "give 200 Ok /w updated card in body" in {
                val body = toBody(s"""{ "id": "${card1.id.toString}", "front": "Front 1 mod", "back": "Back 1 mod" }""")
                val request = Request(Method.PUT, baseUri, HttpVersion.`HTTP/1.1`, Headers.empty, body)
                lazy val response = client.toHttpService.run(request).run
                val responseBody = extractBody(response)
                val cardResponse = decode[Dto.CardResponse](responseBody).valueOr(e => throw e)
                val expectedRespone = Dto.CardResponse(card1.id.toString, "Front 1 mod", "Back 1 mod", None,
                    Dto.CardStatsResponse(None, 0, 0, 0, 0))

                assert(response.status == Status.Ok)
                assert(cardResponse == expectedRespone)
            }
        }

        "card cannot be parsed from body" should {
            "give 400 Bad Request" in {
                assert(true)
                val body = toBody("""{ "thisis": notacard }""")
                val request = Request(Method.PUT, baseUri, HttpVersion.`HTTP/1.1`, Headers.empty, body)
                val response = client.toHttpService.run(request).run
                assert(response.status == Status.BadRequest)

            }

            "describe error in body" in {
                val body = toBody("""{ "thisis": notacard }""")
                val request = Request(Method.PUT, baseUri, HttpVersion.`HTTP/1.1`, Headers.empty, body)
                val response = client.toHttpService.run(request).run
                assert(response.status == Status.BadRequest)
                val responseBody = extractBody(response)
                assert(responseBody == "Could not parse Card from body.")
            }
        }

        "front text is missing" should {
            "give 400 Bad Request" in {
                val body = toBody(s"""{ "id": "${card1.id.toString}", "back": "Back 1 mod" }""")
                val request = Request(Method.PUT, baseUri, HttpVersion.`HTTP/1.1`, Headers.empty, body)
                val response = client.toHttpService.run(request).run
                assert(response.status == Status.BadRequest)

            }
            "describe error in body" in {
                val body = toBody(s"""{ "id": "${card1.id.toString}", "back": "Back 1 mod" }""")
                val request = Request(Method.PUT, baseUri, HttpVersion.`HTTP/1.1`, Headers.empty, body)
                val response = client.toHttpService.run(request).run
                val responseBody = extractBody(response)
                assert(responseBody == SystemMessages.CannotBeEmpty("front").message)
            }
        }

        "back text is missing" should {
            "give 400 Bad Request w/ error message" in {
                val body = toBody(s"""{ "id": "${card1.id.toString}", "front": "Front 1 mod" }""")
                val request = Request(Method.PUT, baseUri, HttpVersion.`HTTP/1.1`, Headers.empty, body)
                val response = client.toHttpService.run(request).run
                val responseBody = extractBody(response)
                assert(response.status == Status.BadRequest)
                assert(responseBody == SystemMessages.CannotBeEmpty("back").message)
            }
        }
    }

    "WIN" when {
        "valid id" should {
            "give 200 Ok w/ updated stats in body" in {
                val uri = baseUri / card1.id.toString / "win"
                val request = Request(Method.POST, uri, HttpVersion.`HTTP/1.1`, Headers.empty, EmptyBody)
                val response = client.toHttpService.run(request).run
                val responseBody = extractBody(response)
                val cardResponse = decode[Dto.CardResponse](responseBody).valueOr(e => throw e)
                val lastVisited = cardResponse.stats.lastVisited.get
                val expected = Dto.CardResponse(card1.id.toString, card1.front.text, card1.back.text, card1.back.exampleOfUse,
                    Dto.CardStatsResponse(Some(lastVisited), 1, 0, 1, 1))

                assert(response.status == Status.Ok)
                assert(cardResponse == expected)
                assert(DateFormat.standard.parse(lastVisited).getTime - ZonedDateTime.now.toEpochSecond < 60)
            }
        }

        "invalid id" should {
            "give 404 Not found" in {
                val uri = baseUri / "99999999-9999-9999-9999-999999999999" / "win"
                val request = Request(Method.POST, uri, HttpVersion.`HTTP/1.1`, Headers.empty, EmptyBody)
                val response = client.toHttpService.run(request).run
                assert(response.status == Status.NotFound)
            }
        }
    }


    def toBody(body: String): EntityBody = {
        val byteV: ByteVector = ByteVector.encodeUtf8(body).right.getOrElse(ByteVector(0))
        scalaz.stream.Process.emit(byteV)
    }
}
