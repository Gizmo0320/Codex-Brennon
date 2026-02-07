package com.envarcade.brennon.webserver.pterodactyl

import com.envarcade.brennon.webserver.PterodactylConfig
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * HTTP client for the Pterodactyl/Pelican panel API.
 * Uses java.net.http.HttpClient (Java 11+) — no extra dependencies.
 */
class PterodactylClient(private val config: PterodactylConfig) {

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    private val gson = Gson()
    private val baseUrl = config.apiUrl.trimEnd('/')

    /** Resolves a Brennon server name to a Pterodactyl server ID. */
    fun resolveServerId(serverName: String): String? {
        return config.serverMappings[serverName]
    }

    /** Lists all servers accessible with the configured API key. */
    fun listServers(): List<PteroServer> {
        val response = get("/api/client")
        val json = JsonParser.parseString(response).asJsonObject
        val data = json.getAsJsonArray("data") ?: return emptyList()
        return data.map { elem ->
            val attrs = elem.asJsonObject.getAsJsonObject("attributes")
            PteroServer(
                identifier = attrs.get("identifier").asString,
                name = attrs.get("name").asString,
                description = attrs.get("description")?.asString ?: "",
                isInstalling = attrs.get("is_installing")?.asBoolean ?: false
            )
        }
    }

    /** Gets the current resource usage for a server. */
    fun getServerStatus(serverId: String): ServerStatus {
        val response = get("/api/client/servers/$serverId/resources")
        val json = JsonParser.parseString(response).asJsonObject
        val attrs = json.getAsJsonObject("attributes")
        val resources = attrs.getAsJsonObject("resources")
        return ServerStatus(
            currentState = attrs.get("current_state").asString,
            cpuPercent = resources.get("cpu_absolute")?.asDouble ?: 0.0,
            memoryBytes = resources.get("memory_bytes")?.asLong ?: 0,
            memoryLimitBytes = resources.get("memory_limit_bytes")?.asLong ?: 0,
            diskBytes = resources.get("disk_bytes")?.asLong ?: 0,
            networkRxBytes = resources.get("network_rx_bytes")?.asLong ?: 0,
            networkTxBytes = resources.get("network_tx_bytes")?.asLong ?: 0,
            uptime = resources.get("uptime")?.asLong ?: 0
        )
    }

    /** Sends a power action (start, stop, restart, kill). */
    fun sendPowerAction(serverId: String, action: PowerAction) {
        post("/api/client/servers/$serverId/power", mapOf("signal" to action.signal))
    }

    /** Sends a command to the server console. */
    fun sendCommand(serverId: String, command: String) {
        post("/api/client/servers/$serverId/command", mapOf("command" to command))
    }

    /** Gets the WebSocket credentials for console streaming. */
    fun getConsoleWebSocket(serverId: String): ConsoleWsInfo {
        val response = get("/api/client/servers/$serverId/websocket")
        val json = JsonParser.parseString(response).asJsonObject
        val data = json.getAsJsonObject("data")
        return ConsoleWsInfo(
            token = data.get("token").asString,
            socket = data.get("socket").asString
        )
    }

    /** Lists files in a directory. */
    fun listFiles(serverId: String, directory: String): List<FileEntry> {
        val encodedDir = java.net.URLEncoder.encode(directory, "UTF-8")
        val response = get("/api/client/servers/$serverId/files/list?directory=$encodedDir")
        val json = JsonParser.parseString(response).asJsonObject
        val data = json.getAsJsonArray("data") ?: return emptyList()
        return data.map { elem ->
            val attrs = elem.asJsonObject.getAsJsonObject("attributes")
            FileEntry(
                name = attrs.get("name").asString,
                mode = attrs.get("mode")?.asString ?: "",
                size = attrs.get("size")?.asLong ?: 0,
                isFile = attrs.get("is_file")?.asBoolean ?: true,
                isEditable = attrs.get("is_editable")?.asBoolean ?: false,
                modifiedAt = attrs.get("modified_at")?.asString ?: ""
            )
        }
    }

    /** Gets file content. */
    fun getFileContent(serverId: String, filePath: String): String {
        val encodedFile = java.net.URLEncoder.encode(filePath, "UTF-8")
        return get("/api/client/servers/$serverId/files/contents?file=$encodedFile")
    }

    /** Writes file content. */
    fun writeFileContent(serverId: String, filePath: String, content: String) {
        val encodedFile = java.net.URLEncoder.encode(filePath, "UTF-8")
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/api/client/servers/$serverId/files/write?file=$encodedFile"))
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Accept", "application/json")
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.ofString(content))
            .timeout(Duration.ofSeconds(30))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw PterodactylException("Write file failed: HTTP ${response.statusCode()} - ${response.body()}")
        }
    }

    // HTTP helpers

    private fun get(path: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Accept", "application/json")
            .GET()
            .timeout(Duration.ofSeconds(30))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw PterodactylException("GET $path failed: HTTP ${response.statusCode()} - ${response.body()}")
        }
        return response.body()
    }

    private fun post(path: String, body: Map<String, Any>) {
        val jsonBody = gson.toJson(body)
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .timeout(Duration.ofSeconds(30))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw PterodactylException("POST $path failed: HTTP ${response.statusCode()} - ${response.body()}")
        }
    }

    // Data classes

    data class PteroServer(
        val identifier: String,
        val name: String,
        val description: String,
        val isInstalling: Boolean
    )

    data class ServerStatus(
        val currentState: String,
        val cpuPercent: Double,
        val memoryBytes: Long,
        val memoryLimitBytes: Long,
        val diskBytes: Long,
        val networkRxBytes: Long,
        val networkTxBytes: Long,
        val uptime: Long
    )

    data class ConsoleWsInfo(
        val token: String,
        val socket: String
    )

    data class FileEntry(
        val name: String,
        val mode: String,
        val size: Long,
        val isFile: Boolean,
        val isEditable: Boolean,
        val modifiedAt: String
    )

    enum class PowerAction(val signal: String) {
        START("start"),
        STOP("stop"),
        RESTART("restart"),
        KILL("kill")
    }

    class PterodactylException(message: String) : RuntimeException(message)
}
