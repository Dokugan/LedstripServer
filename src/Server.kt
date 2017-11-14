import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    Server()
}

class Server(ip: String = "127.0.0.1"){

    var controllers = ArrayList<LedControllerStatus>()

    val httpServer = HttpServer.create(InetSocketAddress(ip, 80), 0)

    init {
        val testColors: ArrayList<Color> = arrayListOf(Color(233.toByte(), -128, 127), Color(127, 0, -1))
        controllers.add(LedControllerStatus(0, Pattern(testColors)))
        httpServer.createContext("/", RequestHandler(this))
        httpServer.start()
    }

    fun addController(item: LedControllerStatus) = controllers.add(item)
}
