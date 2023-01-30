import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import io.kommunicate.models.KmApiResponse

private const val SUCCESS = "success"

@Keep
data class KmNetworkResponse<T>(@SerializedName("message") val message: String) :
    KmApiResponse<T>() {
    fun isSuccess(): Boolean {
        return message == SUCCESS
    }
}