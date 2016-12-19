package presentation

import org.http4s.server.Server
import org.http4s.server.blaze._

object Main {
    def createServer: Server = {
        val builder = BlazeBuilder.bindHttp(8070, "localhost").mountService(TestApi.helloService, "/api")
        builder.run
    }
}
