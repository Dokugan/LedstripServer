fun ArrayList<LedControllerStatus>.findWithId(id: Byte): LedControllerStatus?{
    for (controller in this){
        if (controller.id == id) return controller
    }
    return null
}

fun Byte.toPositiveInt() = toInt() and 0xFF