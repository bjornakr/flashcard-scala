package webapi

import org.http4s.server.Server
import org.http4s.server.blaze._

class Main(cardApi: CardApi) {
    def createServer: Server = {
        val builder = BlazeBuilder.bindHttp(8070, "localhost").mountService(cardApi.helloService, "/api")
        builder.run
    }

    def createServer(port: Int): Server = {
        val builder = BlazeBuilder.bindHttp(port, "localhost").mountService(cardApi.helloService, "/api")
        builder.run
    }
}
