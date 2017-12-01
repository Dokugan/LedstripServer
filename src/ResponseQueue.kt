import com.sun.net.httpserver.HttpExchange
import java.net.HttpURLConnection
import java.util.*

class ResponseQueue{

    val requests = Collections.synchronizedCollection(ArrayList<Pair<Byte, HttpExchange>>())

    fun addRequest(controllerId: Byte, httpExchange: HttpExchange){
        requests.add(Pair(controllerId, httpExchange))
    }

    /*
       Sends response to controllers waiting for change
     */
    fun sendResponse(controller: LedControllerStatus){
        var responseStr: String
        val i = requests.iterator()
        while (i.hasNext()){
            val req = i.next()
            if (req.first == controller.id){
                responseStr = "patternlength="+controller.pattern.colors.size.toString()+"\n"
                for (c in controller.pattern.colors){
                    responseStr += "{r=${c.r.toPositiveInt()};g=${c.g.toPositiveInt()};b=${c.b.toPositiveInt()}}\n"
                }
                req.second.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseStr.length.toLong())
                val response: ByteArray
                response = responseStr.toByteArray()
                val out = req.second.responseBody
                out.write(response)
                out.close()
                req.second.close()
                i.remove()
            }
        }
    }
}