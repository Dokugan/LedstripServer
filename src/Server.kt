import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress

fun main(args: Array<String>) {
    Server()
}

class Server(ip: String = "0.0.0.0"){

    var controllers = ArrayList<LedControllerStatus>()

    val httpServer = HttpServer.create(InetSocketAddress(ip, 80), 0)

    init {
        val testColors: ArrayList<Color> = arrayListOf(Color(255.toByte(), 0.toByte(), 0.toByte()), Color(127, 0, -1), Color(120.toByte(), 44.toByte(), 70.toByte()), Color(127, 0, -1), Color(120.toByte(), 44.toByte(), 70.toByte()), Color(127, 0, -1))
        val testColors2: ArrayList<Color> = arrayListOf(Color(52.toByte(), 67.toByte(), 213.toByte()), Color(127, 0, -1), Color(120.toByte(), 44.toByte(), 70.toByte()), Color(127, 0, -1))


        controllers.add(LedControllerStatus(0, "Test Strip" , testColors.size.toByte(),Pattern(testColors)))
        controllers.add(LedControllerStatus(1, "Test Strip2" , testColors2.size.toByte(),Pattern(testColors2)))
        httpServer.createContext("/", RequestHandler(this))
        httpServer.start()
    }

    fun addController(item: LedControllerStatus) = controllers.add(item)
}
