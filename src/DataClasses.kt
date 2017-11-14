import java.io.Serializable

data class Color(var r: Byte, var g: Byte, var b: Byte) : Serializable

data class Pattern(var colors: ArrayList<Color>) : Serializable

data class LedControllerStatus(var id: Byte, var name: String ,var pattern : Pattern)
