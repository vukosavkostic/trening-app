package com.vukos.trening.notion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

private object NotionHttp {
    val JSON = "application/json".toMediaType()
    val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
}

class NotionException(val status: Int, message: String) : IOException(message)

/**
 * Minimalan Notion REST klijent (bez dodatnih biblioteka).
 * Notion dozvoljava ~3 zahteva u sekundi; na 429 se ceka koliko kaze Retry-After.
 */
class NotionClient(private val token: String) {
    private val base = "https://api.notion.com/v1/"

    suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            repeat(4) { attempt ->
                val req = Request.Builder()
                    .url(base + path)
                    .header("Authorization", "Bearer $token")
                    .header("Notion-Version", "2022-06-28")
                    .method(method, body?.toString()?.toRequestBody(NotionHttp.JSON))
                    .build()
                NotionHttp.http.newCall(req).execute().use { resp ->
                    val text = resp.body?.string() ?: "{}"
                    when {
                        resp.isSuccessful -> return@withContext JSONObject(text)
                        resp.code == 429 || resp.code >= 500 -> {
                            val wait = resp.header("Retry-After")?.toLongOrNull() ?: (1L shl attempt)
                            delay(wait * 1000)
                        }
                        else -> {
                            val msg = runCatching { JSONObject(text).optString("message") }.getOrDefault(text)
                            throw NotionException(resp.code, msg)
                        }
                    }
                }
            }
            throw NotionException(429, "Notion je preopterećen, pokušaj kasnije.")
        }

    /** Svi blokovi (decu) stranice, sa paginacijom. */
    suspend fun children(blockId: String): List<JSONObject> {
        val out = mutableListOf<JSONObject>()
        var cursor: String? = null
        do {
            val q = "blocks/$blockId/children?page_size=100" + (cursor?.let { "&start_cursor=$it" } ?: "")
            val r = request("GET", q)
            r.getJSONArray("results").forEachObj { out += it }
            cursor = if (r.optBoolean("has_more")) r.optString("next_cursor") else null
        } while (cursor != null)
        return out
    }

    suspend fun queryDatabase(dbId: String, filter: JSONObject? = null): List<JSONObject> {
        val out = mutableListOf<JSONObject>()
        var cursor: String? = null
        do {
            val body = JSONObject().put("page_size", 100)
            filter?.let { body.put("filter", it) }
            cursor?.let { body.put("start_cursor", it) }
            val r = request("POST", "databases/$dbId/query", body)
            r.getJSONArray("results").forEachObj { out += it }
            cursor = if (r.optBoolean("has_more")) r.optString("next_cursor") else null
        } while (cursor != null)
        return out
    }

    suspend fun updatePage(pageId: String, properties: JSONObject) =
        request("PATCH", "pages/$pageId", JSONObject().put("properties", properties))

    /**
     * Pronalazi stranicu "Trening" i baze ispod nje. Vraca naziv baze -> ID.
     */
    suspend fun discoverDatabases(): Map<String, String> {
        val search = request(
            "POST", "search",
            JSONObject().put("query", "Trening")
                .put("filter", JSONObject().put("property", "object").put("value", "page")),
        )
        val pages = mutableListOf<JSONObject>()
        search.getJSONArray("results").forEachObj { pages += it }
        val page = pages.firstOrNull { titleOf(it) == "Trening" }
            ?: throw NotionException(404, "Stranica \"Trening\" nije podeljena sa integracijom (••• → Connections).")
        val dbs = mutableMapOf<String, String>()
        for (b in children(page.getString("id"))) {
            if (b.optString("type") == "child_database") {
                dbs[b.getJSONObject("child_database").optString("title")] = b.getString("id")
            }
        }
        if ("Routines" !in dbs) throw NotionException(404, "Na stranici Trening nije pronađena baza Routines.")
        return dbs
    }

    companion object {
        fun plain(rich: JSONArray?): String {
            if (rich == null) return ""
            val sb = StringBuilder()
            for (i in 0 until rich.length()) sb.append(rich.getJSONObject(i).optString("plain_text"))
            return sb.toString()
        }

        /** Naslov stranice (title property, bez obzira kako se zove). */
        fun titleOf(page: JSONObject): String {
            val props = page.optJSONObject("properties") ?: return ""
            for (k in props.keys()) {
                val p = props.getJSONObject(k)
                if (p.optString("type") == "title") return plain(p.optJSONArray("title"))
            }
            return ""
        }

        fun text(page: JSONObject, prop: String): String =
            page.optJSONObject("properties")?.optJSONObject(prop)?.let { plain(it.optJSONArray("rich_text")) } ?: ""

        fun multiSelect(page: JSONObject, prop: String): List<String> {
            val arr = page.optJSONObject("properties")?.optJSONObject(prop)?.optJSONArray("multi_select") ?: return emptyList()
            return List(arr.length()) { arr.getJSONObject(it).optString("name") }
        }

        fun richText(value: String) = JSONObject().put(
            "rich_text", JSONArray().put(JSONObject().put("text", JSONObject().put("content", value)))
        )

        /** (tip bloka, tekst) za parser plana. */
        fun blockText(b: JSONObject): Pair<String, String> {
            val type = b.optString("type")
            return type to plain(b.optJSONObject(type)?.optJSONArray("rich_text"))
        }
    }
}

inline fun JSONArray.forEachObj(f: (JSONObject) -> Unit) { for (i in 0 until length()) f(getJSONObject(i)) }
