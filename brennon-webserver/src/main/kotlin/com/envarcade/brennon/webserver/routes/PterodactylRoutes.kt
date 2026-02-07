package com.envarcade.brennon.webserver.routes

import com.envarcade.brennon.webserver.WebServerConfig
import com.envarcade.brennon.webserver.auth.JwtAuth
import com.envarcade.brennon.webserver.auth.RoleMiddleware
import com.envarcade.brennon.webserver.pterodactyl.PterodactylClient
import io.javalin.Javalin

class PterodactylRoutes(
    private val pteroClient: PterodactylClient,
    private val jwtAuth: JwtAuth,
    private val config: WebServerConfig
) {

    private val allowedFileExtensions = setOf(
        ".yml", ".yaml", ".json", ".properties", ".toml",
        ".txt", ".conf", ".cfg", ".ini", ".log"
    )

    fun register(app: Javalin) {

        // List all mapped servers with their power state
        app.get("/api/pterodactyl/servers") { ctx ->
            if (!RoleMiddleware.requireAdmin(ctx, jwtAuth, config.apiKey)) return@get

            val results = config.pterodactyl.serverMappings.map { (brennonName, pteroId) ->
                try {
                    val status = pteroClient.getServerStatus(pteroId)
                    mapOf(
                        "name" to brennonName,
                        "pterodactylId" to pteroId,
                        "state" to status.currentState,
                        "cpu" to status.cpuPercent,
                        "memoryMb" to (status.memoryBytes / 1024 / 1024),
                        "memoryLimitMb" to (status.memoryLimitBytes / 1024 / 1024),
                        "diskMb" to (status.diskBytes / 1024 / 1024),
                        "uptime" to status.uptime
                    )
                } catch (e: Exception) {
                    mapOf(
                        "name" to brennonName,
                        "pterodactylId" to pteroId,
                        "state" to "unknown",
                        "error" to (e.message ?: "Failed to fetch status")
                    )
                }
            }
            ctx.json(results)
        }

        // Get server status
        app.get("/api/pterodactyl/servers/{name}/status") { ctx ->
            if (!RoleMiddleware.requireAdmin(ctx, jwtAuth, config.apiKey)) return@get
            val name = ctx.pathParam("name")
            val pteroId = pteroClient.resolveServerId(name)
            if (pteroId == null) {
                ctx.status(404).json(mapOf("error" to "Server '$name' not mapped in pterodactyl config"))
                return@get
            }

            try {
                val status = pteroClient.getServerStatus(pteroId)
                ctx.json(mapOf(
                    "name" to name,
                    "state" to status.currentState,
                    "cpu" to status.cpuPercent,
                    "memoryMb" to (status.memoryBytes / 1024 / 1024),
                    "memoryLimitMb" to (status.memoryLimitBytes / 1024 / 1024),
                    "diskMb" to (status.diskBytes / 1024 / 1024),
                    "networkRxMb" to (status.networkRxBytes / 1024 / 1024),
                    "networkTxMb" to (status.networkTxBytes / 1024 / 1024),
                    "uptime" to status.uptime
                ))
            } catch (e: Exception) {
                ctx.status(500).json(mapOf("error" to "Failed to get status: ${e.message}"))
            }
        }

        // Send power action
        app.post("/api/pterodactyl/servers/{name}/power") { ctx ->
            if (!RoleMiddleware.requireAdmin(ctx, jwtAuth, config.apiKey)) return@post
            val name = ctx.pathParam("name")
            val pteroId = pteroClient.resolveServerId(name)
            if (pteroId == null) {
                ctx.status(404).json(mapOf("error" to "Server '$name' not mapped in pterodactyl config"))
                return@post
            }

            val body = ctx.bodyAsClass(PowerRequest::class.java)
            val action = try {
                PterodactylClient.PowerAction.valueOf(body.action.uppercase())
            } catch (_: Exception) {
                ctx.status(400).json(mapOf("error" to "Invalid action. Must be: start, stop, restart, kill"))
                return@post
            }

            try {
                pteroClient.sendPowerAction(pteroId, action)
                ctx.json(mapOf("success" to true, "action" to action.signal, "server" to name))
            } catch (e: Exception) {
                ctx.status(500).json(mapOf("error" to "Power action failed: ${e.message}"))
            }
        }

        // Send console command
        app.post("/api/pterodactyl/servers/{name}/command") { ctx ->
            if (!RoleMiddleware.requireAdmin(ctx, jwtAuth, config.apiKey)) return@post
            val name = ctx.pathParam("name")
            val pteroId = pteroClient.resolveServerId(name)
            if (pteroId == null) {
                ctx.status(404).json(mapOf("error" to "Server '$name' not mapped in pterodactyl config"))
                return@post
            }

            val body = ctx.bodyAsClass(CommandRequest::class.java)
            if (body.command.isBlank()) {
                ctx.status(400).json(mapOf("error" to "command is required"))
                return@post
            }

            try {
                pteroClient.sendCommand(pteroId, body.command)
                ctx.json(mapOf("success" to true, "server" to name))
            } catch (e: Exception) {
                ctx.status(500).json(mapOf("error" to "Command failed: ${e.message}"))
            }
        }

        // List files in directory
        app.get("/api/pterodactyl/servers/{name}/files") { ctx ->
            if (!RoleMiddleware.requireAdmin(ctx, jwtAuth, config.apiKey)) return@get
            val name = ctx.pathParam("name")
            val pteroId = pteroClient.resolveServerId(name)
            if (pteroId == null) {
                ctx.status(404).json(mapOf("error" to "Server '$name' not mapped in pterodactyl config"))
                return@get
            }

            val dir = ctx.queryParam("dir") ?: "/"
            try {
                val files = pteroClient.listFiles(pteroId, dir)
                ctx.json(files.map { f ->
                    mapOf(
                        "name" to f.name,
                        "size" to f.size,
                        "isFile" to f.isFile,
                        "isEditable" to f.isEditable,
                        "modifiedAt" to f.modifiedAt
                    )
                })
            } catch (e: Exception) {
                ctx.status(500).json(mapOf("error" to "Failed to list files: ${e.message}"))
            }
        }

        // Read file content
        app.get("/api/pterodactyl/servers/{name}/files/content") { ctx ->
            if (!RoleMiddleware.requireAdmin(ctx, jwtAuth, config.apiKey)) return@get
            val name = ctx.pathParam("name")
            val pteroId = pteroClient.resolveServerId(name)
            if (pteroId == null) {
                ctx.status(404).json(mapOf("error" to "Server '$name' not mapped in pterodactyl config"))
                return@get
            }

            val file = ctx.queryParam("file")
            if (file.isNullOrBlank()) {
                ctx.status(400).json(mapOf("error" to "file query parameter is required"))
                return@get
            }

            try {
                val content = pteroClient.getFileContent(pteroId, file)
                ctx.json(mapOf("file" to file, "content" to content))
            } catch (e: Exception) {
                ctx.status(500).json(mapOf("error" to "Failed to read file: ${e.message}"))
            }
        }

        // Write file content (text files only)
        app.put("/api/pterodactyl/servers/{name}/files/content") { ctx ->
            if (!RoleMiddleware.requireAdmin(ctx, jwtAuth, config.apiKey)) return@put
            val name = ctx.pathParam("name")
            val pteroId = pteroClient.resolveServerId(name)
            if (pteroId == null) {
                ctx.status(404).json(mapOf("error" to "Server '$name' not mapped in pterodactyl config"))
                return@put
            }

            val body = ctx.bodyAsClass(WriteFileRequest::class.java)
            if (body.file.isBlank() || body.content.isBlank()) {
                ctx.status(400).json(mapOf("error" to "file and content are required"))
                return@put
            }

            // Restrict to text files
            val ext = body.file.substringAfterLast('.', "").let { ".$it" }
            if (ext !in allowedFileExtensions) {
                ctx.status(400).json(mapOf(
                    "error" to "File type not allowed. Allowed: ${allowedFileExtensions.joinToString()}"
                ))
                return@put
            }

            try {
                pteroClient.writeFileContent(pteroId, body.file, body.content)
                ctx.json(mapOf("success" to true, "file" to body.file))
            } catch (e: Exception) {
                ctx.status(500).json(mapOf("error" to "Failed to write file: ${e.message}"))
            }
        }
    }

    data class PowerRequest(val action: String = "")
    data class CommandRequest(val command: String = "")
    data class WriteFileRequest(val file: String = "", val content: String = "")
}
