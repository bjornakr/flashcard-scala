package integration


import java.util.UUID

import application.{CardResponseMapper, Dto}
import domain._
import infrastructure.SystemMessages
import io.circe.generic.auto._
import io.circe.parser._
import org.http4s._
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.dsl._
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import webapi.Main
import repository.{CardDao, CardTable, Dataxase}
import scodec.bits.ByteVector
import slick.driver.H2Driver.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class TestApiSpec extends WordSpec with BeforeAndAfterAll {

    val server = Main.createServer
    val client = PooledHttp1Client()

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

    override def beforeAll {
        def createInMemoryDatabase = {
            val db = Dataxase.db
            val cards = TableQuery[CardTable]
            val setup = slick.dbio.DBIO.seq(cards.schema.create)
            val dbSetupFuture = db.run(setup)
            Await.result(dbSetupFuture, Duration.Inf)
        }

        def insertCards = {
            val createPromise = CardDao.saveAll(allCards)
            Await.result(createPromise, Duration.Inf)
        }

        createInMemoryDatabase
        insertCards
    }

    override def afterAll {
        server.shutdownNow()
        client.shutdownNow()
    }

    def extractBody(r: Response): String =
        EntityDecoder.decodeString(r).run

    val baseUri = Uri.fromString("http://localhost:8070/api/cards").valueOr(e => throw e)

    "test" in {
        val helloJames = client.expect[String]("http://localhost:8070/api/hello/James")
        val result = helloJames.run
        assert(result == "Hello, James")
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
                val expectedResponse = Dto.CardResponse(card1.id.toString, card1.front.text, card1.back.text, card1.back.exampleOfUse)

                assert(cardResponse == expectedResponse)
            }
        }
    }

    "POST card" when {

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

                val expectedRespone = Dto.CardResponse(cardResponse.id, "A", "B", Some("C"))
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
        }

        "invalid id" should {
            "give 404 Not Found" in {
                val uri = baseUri / "00000000-0000-0000-0000-000000009999"
                val request = Request(Method.DELETE, uri, HttpVersion.`HTTP/1.1`, Headers.empty, EmptyBody)
                def response = client.toHttpService.run(request).run
                assert(response.status == Status.NoContent)

            }
        }
    }

    def toBody(body: String): EntityBody = {
        val byteV: ByteVector = ByteVector.encodeUtf8(body).right.getOrElse(ByteVector(0))
        scalaz.stream.Process.emit(byteV)
    }
}
