import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.lang.RuntimeException
import java.net.HttpURLConnection
import java.net.URLDecoder

class RequestHandler(val server: Server) : HttpHandler {

    val HTTP_POST = "POST"
    val HTTP_GET = "GET"

    override fun handle(exchange: HttpExchange) {

        val response: ByteArray
        var responseStr = ""
        val out = exchange.responseBody
        when(exchange.requestMethod){
            HTTP_GET -> {
                when(exchange.requestURI.path){
                    "/controller" -> {

                        //Try to parse querry
                        try {
                            val params = parseQueryString(exchange.requestURI.query)

                            //Find controller with id from querry, if it does not exists make new controller with given id
                            val controller = server.controllers.findWithId(params.getValue("id").toInt().toByte())
                            if(controller != null){

                                //If controller exists, return length of the pattern followed by colors in the pattern
                                responseStr = "patternlength="+controller.pattern.colors.size.toString()+"\n"
                                for (c in controller.pattern.colors){
                                    responseStr += "{r=${c.r.toPositiveInt()};g=${c.g.toPositiveInt()};b=${c.b.toPositiveInt()}}\n"
                                }
                            } else {

                                val newController = LedControllerStatus(params.getValue("id").toInt().toByte(), params.getValue("id").toInt().toString(), 0.toByte(),Pattern(ArrayList()))
                                server.addController(newController)
                                responseStr = "patternlength="+newController.pattern.colors.size.toString()+"\n"
                            }
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseStr.length.toLong())
                        }

                        //If parsing parameters failed send bad request error(400)
                        catch (e: Exception) {
                            responseStr = "Bad Request"
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, responseStr.length.toLong())
                        }
                    }
                    "/controllers" -> {
                        responseStr = GsonBuilder().setPrettyPrinting().create().toJson(server.controllers)
                        exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseStr.length.toLong())
                    }
                    "/poll" -> {
                        val params = parseQueryString(exchange.requestURI.query)

                        val controller = server.controllers.findWithId(params.getValue("id").toInt().toByte())
                        if(controller == null) {
                            val newController = LedControllerStatus(params.getValue("id").toInt().toByte(), params.getValue("id").toInt().toString(), 0.toByte(),Pattern(ArrayList()))
                            server.addController(newController)
                            responseStr = "patternlength="+newController.pattern.colors.size.toString()+"\n"
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseStr.length.toLong())
                        }
                        else{
                            server.responseQueue.addRequest(controller.id, exchange)
                            return
                        }
                    }
                }
            }
            HTTP_POST -> {
                when(exchange.requestURI.path){
                    "/controller" -> {
                        try{
                            val params = parseQueryString(exchange.requestURI.query)

                            val controller = server.controllers.findWithId(params.getValue("id").toInt().toByte())
                            if (controller != null){
                                val body = InputStreamReader(exchange.requestBody, "utf-8")
                                val pattern = Gson().fromJson(body, Pattern::class.java)
                                controller.setPatternAndRespond(pattern, server.responseQueue)
                            }

                            responseStr = GsonBuilder().setPrettyPrinting().create().toJson(controller)
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseStr.length.toLong())

                        } catch (e: Exception){
                            responseStr = "Bad Request"
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, responseStr.length.toLong())
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
        response = responseStr.toByteArray()
        out.write(response)
        out.close()
        exchange.close()
    }

    fun parseQueryString(query: String?): Map<String, String>{
        val result = HashMap<String, String>()
        if (query == null) return result
        var last = 0; var next: Int; val length = query.length
        while (last < 1){
            next = query.indexOf("&", last)
            if (next == -1) next = length

            if (next > last){
                val eqPos = query.indexOf("=", last)
                try {
                    if (eqPos < 0 || eqPos > next) result.put(URLDecoder.decode(query.substring(last, next), "utf-8"), "")
                    else result.put(URLDecoder.decode(query.substring(last, eqPos), "utf-8"), URLDecoder.decode(query.substring(eqPos + 1, next), "utf-8"))
                } catch (e: UnsupportedEncodingException){ throw RuntimeException(e)}
            }
            last = next + 1
        }
        return result
    }
}