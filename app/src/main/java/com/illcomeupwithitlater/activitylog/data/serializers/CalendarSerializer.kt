import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Calendar

object CalendarSerializer : KSerializer<Calendar> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Calendar", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Calendar) {
        // Convert Calendar to milliseconds (long)
        encoder.encodeLong(value.timeInMillis)
    }

    override fun deserialize(decoder: Decoder): Calendar {
        // Convert milliseconds back to Calendar
        val millis = decoder.decodeLong()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = millis
        return calendar
    }
}
