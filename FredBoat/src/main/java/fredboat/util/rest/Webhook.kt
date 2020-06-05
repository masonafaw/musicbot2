package fredboat.util.rest

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import reactor.core.publisher.Mono
import java.io.IOException

class Webhook(val url: String) {
    val http = Http(Http.DEFAULT_BUILDER)

    fun send(message: String, username: String? = null, avatarUri: String? = null): Mono<Unit> {
        val json = JSONObject()
        json.put("content", message)
        username?.apply { json.put("username", this) }
        avatarUri?.apply { json.put("avatar_url", this) }
        val request = http.post(url, json.toString(), "application/json")

        return Mono.create<Unit> { request.enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                if (e != null) it.error(e) else it.error(RuntimeException("The webhook call failed"))
            }

            override fun onResponse(call: Call?, response: Response?) {
                it.success()
            }
        })}
    }
}