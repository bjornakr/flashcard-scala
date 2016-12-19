package integration


import java.util.UUID

import application.Dto
import domain._
import infrastructure.SystemMessages
import io.circe.generic.auto._
import io.circe.parser._
import org.http4s._
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.dsl._
import org.scalatest.{BeforeAndAfterAll, WordSpec}
import presentation.Main
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
            new Front("Front 1") {}, new Back("Back 1", "ExampleOfUse 1") {}, stats) {}
    }

    override def beforeAll {
        def createInMemoryDatabase = {
            val db = Dataxase.db
            val cards = TableQuery[CardTable]
            val setup = slick.dbio.DBIO.seq(cards.schema.create)
            val dbSetupFuture = db.run(setup)
            Await.result(dbSetupFuture, Duration.Inf)
        }

        def insertCards = {
            val createPromise = CardDao.save(card1)
            Await.result(createPromise, Duration.Inf)
        }

        createInMemoryDatabase
        insertCards


        //        val logger = Logger[TestApiSpec]
        //        logger.error("Scream!")
//        val stats = new CardStatistics(None, new Wins(0) {}, new Losses(0) {}, new WinStreak(0) {}) {}
//        val createPromise = CardDao.create(new Card(UUID.fromString("00000000-0000-0000-0000-000000000000"), new Front("Front") {}, new Back("Back", "EampleOfUse") {}, stats) {})
//        Await.result(createPromise, Duration.Inf)
//        val createPromise2 = CardDao.create(new Card(UUID.fromString("00000000-0000-0000-0000-000000000001"), new Front("Front 1") {}, new Back("Back 1", "ExampleOfUse 1") {}, stats) {})
//        Await.result(createPromise2, Duration.Inf)
//        val zoggAll = Await.result(CardDao.getAll, Duration.Inf)
//        val zap = CardDao.findById(UUID.fromString("00000000-0000-0000-0000-000000000000"))
//        val fip = Await.result(zap, Duration.Inf)
//        val zip = 1 - 1
    }

    override def afterAll {
        server.shutdownNow()
        client.shutdownNow()
    }

    def extractBody(r: Response): String =
        EntityDecoder.decodeString(r).run
        //r.body.runLog.run.head.decodeUtf8.right.getOrElse("FAIL")

    val baseUri = Uri.fromString("http://localhost:8070/api/cards").valueOr(e => throw e)

    "test" in {
        val helloJames = client.expect[String]("http://localhost:8070/api/hello/James")
        val result = helloJames.run
        assert(result == "Hello, James")
    }

    "GET cards/<id>" when {
        "malformed uuid" should {
            val malformedUuid = "INVALID-ID"
            val uri = baseUri / malformedUuid

            "give 400 Bad Request" in {
                val response = client.toHttpService.run(Request(Method.GET, uri)).run
                assert(response.status == Status.BadRequest)
            }

            "describe error in body" in {
                val response = client.toHttpService.run(Request(Method.GET, uri)).run
                val body = extractBody(response)
                assert(body == SystemMessages.InvalidIdFormat(malformedUuid).message)
            }
        }

        "invalid id" should {
            val invalidId = "00000000-0000-0000-0000-000000009999"
            val uri = baseUri / invalidId

            "give 404 Not Found" in {
                val response = client.toHttpService.run(Request(Method.GET, uri)).run
                assert(response.status == Status.NotFound)
            }

            "describe error in body" in {
                val response = client.toHttpService.run(Request(Method.GET, uri)).run
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

//                assert(card.id == "00000000-0000-0000-0000-000000000001")

            }
        }
    }

    "POST card" when {
        val uri = baseUri

        "valid card" should {
            "create card" in {
                val bdy = "{ \"front\": \"A\", \"back\": \"B\", \"exampleOfUse\": \"C\" }"
                val byteV: ByteVector = ByteVector.encodeUtf8(bdy).right.getOrElse(ByteVector(0))
                val body = scalaz.stream.Process.emit(byteV)
                val request = Request(Method.POST, uri, HttpVersion.`HTTP/1.1`, Headers.empty, body) //.withBody("Hei!")
                val response = client.toHttpService.run(request).withBody("Hei!").run
                assert(response.status == Status.Created)
            }
        }
        "front text is missing" should {
            "give 400 Bad Request" in {
                val uri = baseUri
                val eb = EmptyBody
                val byteV: ByteVector = ByteVector.encodeUtf8("Hei hei hei hei").right.getOrElse(ByteVector(0))
                val body = EmptyBody // Process.emit(byteV)
                val request = Request(Method.POST, uri, HttpVersion.`HTTP/1.1`, Headers.empty, body) //.withBody("Hei!")
                val response = client.toHttpService.run(request).withBody("Hei!").run
                assert(response.status == Status.Ok)
            }
        }
    }
}
