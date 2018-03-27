import java.io.Serializable

data class Color(var r: Byte, var g: Byte, var b: Byte) : Serializable

data class Pattern(var colors: ArrayList<Color>) : Serializable

data class LedControllerStatus(var id: Byte, var name: String, var numLeds: Byte,var pattern : Pattern, var on: Boolean) : Serializable{
    fun setPatternAndRespond(pattern: Pattern, responseQueue: ResponseQueue){
        this.pattern = pattern
        if (on) {
            responseQueue.sendResponse(this, this.pattern)
        } else {
            val noColors: ArrayList<Color> = arrayListOf(Color(0.toByte(), 0.toByte(), 0.toByte()))
            responseQueue.sendResponse(this, Pattern(noColors))
        }
    }

    fun turnOffAndRespond(responseQueue: ResponseQueue){
        this.on = false
        val noColors: ArrayList<Color> = arrayListOf(Color(0.toByte(), 0.toByte(), 0.toByte()))
        responseQueue.sendResponse(this, Pattern(noColors))
    }

    fun turnOnAndRepond(responseQueue: ResponseQueue){
        this.on = true
        responseQueue.sendResponse(this, this.pattern)
    }
}
