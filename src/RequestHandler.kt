import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.URLDecoder
import java.util.LinkedHashMap






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
                    //returns simplified pattern data meanth for controllers
                    "/controller" -> {

                        //Try to parse querry
                        try {
                            val params = parseQueryString(exchange.requestURI.query)
                            println(params)
                            //Find controller with id from querry, if it does not exists make new controller with given id
                            var controller = server.controllers.findWithId(params.getValue("id").toInt().toByte())
                            if(controller != null){

                                //If controller exists, return length of the pattern followed by colors in the pattern
                                if (controller.on) {
                                    responseStr = controller.pattern.colors.size.toString()+"\n"
                                    for (c in controller.pattern.colors) {
                                        responseStr += "${c.g.toPositiveInt()};${c.r.toPositiveInt()};${c.b.toPositiveInt()}\n"
                                    }
                                } else {
                                    //1 because no color is only 1 0,0,0 color
                                    responseStr = "1\n0;0;0\n"
                                }
                            } else {

                                val newController = LedControllerStatus(params.getValue("id").toInt().toByte(), params.getValue("id").toInt().toString(), 0.toByte(),Pattern(ArrayList()), true)
                                server.addController(newController)
                                responseStr = "patternlength="+newController.pattern.colors.size.toString()+"\n"
                                controller = newController
                            }

                            //check for on/off param
                            try {
                                val onParam = params.getValue("on")
                                if (onParam == "true") {
                                    controller.turnOnAndRepond(server.responseQueue)
                                } else if (onParam == "false") {
                                    controller.turnOffAndRespond(server.responseQueue)
                                }
                            } catch (e: Exception){}
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseStr.length.toLong())
                        }

                        //If parsing parameters failed send bad request error(400)
                        catch (e: Exception) {
                            responseStr = "Bad Request"
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, responseStr.length.toLong())
                            e.printStackTrace()
                        }
                    }
                    //returns data of all controllers in JSON form
                    "/controllers" -> {
                        //check for on/off param
                        try {
                            val params = parseQueryString(exchange.requestURI.query)
                            val onParam = params.getValue("on")
                            if (onParam == "true") {
                                for (s in server.controllers){
                                    s.turnOnAndRepond(server.responseQueue)
                                }
                                //controller.turnOnAndRepond(server.responseQueue)
                            } else if (onParam == "false") {
                                for (s in server.controllers){
                                    s.turnOffAndRespond(server.responseQueue)
                                }
                            }
                        } catch (e: Exception){}
                        finally {
                            responseStr = GsonBuilder().setPrettyPrinting().create().toJson(server.controllers)
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseStr.length.toLong())
                        }
                    }
                    //returns simplified pattern data once data is changed
                    "/poll" -> {
                        val params = parseQueryString(exchange.requestURI.query)

                        val controller = server.controllers.findWithId(params.getValue("id").toInt().toByte())
                        if(controller == null) {
                            val newController = LedControllerStatus(params.getValue("id").toInt().toByte(), params.getValue("id").toInt().toString(), 0.toByte(),Pattern(ArrayList()), true)
                            server.addController(newController)
                            responseStr = "patternlength="+newController.pattern.colors.size.toString()+"\n"
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseStr.length.toLong())
                        }
                        else{
                            server.responseQueue.addRequest(controller.id, exchange)
                            return
                        }
                    }
                    //returns whether one or more controllers are turned on or not or if one specific controller is turned on with specified id
                    "/state" -> {

                        try {
                            val params = parseQueryString(exchange.requestURI.query)
                            val controller = server.controllers.findWithId(params.getValue("id").toInt().toByte())
                            var state = false
                            if (controller != null){
                                if (controller.on) state = true
                            }
                            responseStr = state.toString()
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseStr.length.toLong())
                        } catch (e: Exception){
                            var state = false
                            for (c: LedControllerStatus in server.controllers)
                            {
                                if (c.on){
                                    state = true
                                    break
                                }
                            }
                            responseStr = state.toString()
                            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, responseStr.length.toLong())
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
                                println(pattern.toString())
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

    @Throws(UnsupportedEncodingException::class)
    fun parseQueryString(query: String): Map<String, String> {
        val query_pairs = LinkedHashMap<String, String>()
        val pairs = query.split("&".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            query_pairs[URLDecoder.decode(pair.substring(0, idx), "UTF-8")] = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
        }
        return query_pairs
    }
}