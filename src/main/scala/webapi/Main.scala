package webapi

import application.CardUseCases
import org.http4s.server.Server
import org.http4s.server.blaze._

class Main(cardApi: TestApi) {
    def createServer: Server = {
        val builder = BlazeBuilder.bindHttp(8070, "localhost").mountService(cardApi.helloService, "/api")
        builder.run
    }
}
