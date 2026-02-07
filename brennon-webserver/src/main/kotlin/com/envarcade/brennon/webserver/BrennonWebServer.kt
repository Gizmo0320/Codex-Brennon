package com.envarcade.brennon.webserver

import com.envarcade.brennon.api.Platform
import com.envarcade.brennon.common.config.BrennonConfig
import com.envarcade.brennon.common.config.DiscordConfig
import com.envarcade.brennon.common.config.LuckPermsConfig
import com.envarcade.brennon.core.Brennon
import com.envarcade.brennon.webserver.audit.AuditCategory
import com.envarcade.brennon.webserver.audit.AuditLogEntry
import com.envarcade.brennon.webserver.audit.AuditLogger
import com.envarcade.brennon.webserver.auth.JwtAuth
import com.envarcade.brennon.webserver.data.AppealRepository
import com.envarcade.brennon.webserver.luckperms.LuckPermsEditorBridge
import com.envarcade.brennon.webserver.pterodactyl.PterodactylClient
import com.envarcade.brennon.webserver.routes.*
import com.envarcade.brennon.webserver.ws.ConsoleWebSocketHandler
import com.envarcade.brennon.webserver.ws.RedisEventBridge
import com.envarcade.brennon.webserver.ws.WebSocketHandler
import com.google.gson.Gson
import com.google.gson.JsonParser
import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.staticfiles.Location
import java.io.File
import java.util.UUID

fun main(args: Array<String>) {
    println()
    println("  ____                                    ")
    println(" | __ ) _ __ ___ _ __  _ __   ___  _ __   ")
    println(" |  _ \\| '__/ _ \\ '_ \\| '_ \\ / _ \\| '_ \\ ")
    println(" | |_) | | |  __/ | | | | | | (_) | | | | ")
    println(" |____/|_|  \\___|_| |_|_| |_|\\___/|_| |_| ")
    println()
    println("  Network Core — Web Server")
    println()

    val dataFolder = File(args.firstOrNull() ?: "data")
    dataFolder.mkdirs()

    // Load web server config (single config file)
    val webConfig = loadWebServerConfig(dataFolder)

    // Build BrennonConfig from webserver config (only the fields Brennon needs)
    val brennonConfig = BrennonConfig(
        serverName = "webserver",
        serverGroup = "webserver",
        network = webConfig.network,
        database = webConfig.database,
        redis = webConfig.redis,
        modules = webConfig.modules,
        luckperms = LuckPermsConfig(enabled = false),
        discord = DiscordConfig(enabled = false)
    )

    // Initialize Brennon core with preloaded config (no config.json needed)
    val brennon = Brennon(Platform.STANDALONE, dataFolder)

    Runtime.getRuntime().addShutdownHook(Thread {
        brennon.disable()
    })

    try {
        brennon.enable(brennonConfig)
    } catch (e: Exception) {
        println("[Brennon] FATAL: Failed to start: ${e.message}")
        e.printStackTrace()
        return
    }

    // Initialize auth
    val jwtAuth = JwtAuth(webConfig)

    // Initialize audit logger
    val auditLogger = AuditLogger(webConfig.audit, brennon.databaseManager, dataFolder)
    auditLogger.initialize()

    // Initialize appeal repository
    val appealRepository = AppealRepository(brennon.databaseManager)
    appealRepository.initialize()

    // Initialize Pterodactyl client
    val pteroClient = if (webConfig.pterodactyl.enabled) {
        PterodactylClient(webConfig.pterodactyl)
    } else null

    // Initialize LuckPerms editor bridge
    val lpEditorBridge = LuckPermsEditorBridge(brennon.redisMessaging)
    lpEditorBridge.initialize()

    // Initialize WebSocket handler
    val wsHandler = WebSocketHandler(jwtAuth)
    val redisBridge = if (webConfig.wsEnabled) {
        RedisEventBridge(brennon.redisMessaging, wsHandler)
    } else null

    // Start Javalin
    val app = Javalin.create { config ->
        config.staticFiles.add("/public", Location.CLASSPATH)

        config.bundledPlugins.enableCors { cors ->
            cors.addRule { rule ->
                rule.anyHost()
            }
        }
    }

    // Auth filter for API routes
    app.before("/api/*") { ctx ->
        // Skip auth for login, player auth, and public endpoints
        if (ctx.path() == "/api/auth/login") return@before
        if (ctx.path().startsWith("/api/player-auth/")) return@before
        if (ctx.path().startsWith("/api/public/")) return@before

        val authHeader = ctx.header("Authorization")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ctx.status(401).json(mapOf("error" to "Missing or invalid Authorization header"))
            ctx.skipRemainingHandlers()
            return@before
        }

        val token = authHeader.removePrefix("Bearer ").trim()

        // Check API key first, then JWT
        if (token != webConfig.apiKey && !jwtAuth.validateToken(token)) {
            ctx.status(401).json(mapOf("error" to "Invalid credentials"))
            ctx.skipRemainingHandlers()
            return@before
        }
    }

    // Audit logging after handler
    if (webConfig.audit.enabled) {
        app.after("/api/*") { ctx ->
            val method = ctx.method().name
            if (!webConfig.audit.logReads && method == "GET") return@after

            val path = ctx.path()
            val status = ctx.status().code
            val username = extractUsername(ctx, jwtAuth, webConfig)
            val category = categorizeRoute(path)
            val body = if (method != "GET") sanitizeBody(ctx.body(), path) else null

            auditLogger.log(AuditLogEntry(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                username = username,
                action = method,
                path = path,
                statusCode = status,
                targetInfo = null,
                requestBody = body,
                ipAddress = ctx.ip(),
                category = category
            ))
        }
    }

    // Initialize console WS handler
    val consoleHandler = if (pteroClient != null) {
        ConsoleWebSocketHandler(jwtAuth, pteroClient, webConfig.pterodactyl)
    } else null

    // Register WebSocket
    if (webConfig.wsEnabled) {
        app.ws("/ws") { ws ->
            wsHandler.configure(ws)
        }
    }

    // Register console WebSocket (Pterodactyl)
    if (consoleHandler != null) {
        app.ws("/ws/console/{serverName}") { ws ->
            consoleHandler.configure(ws)
        }
    }

    // Register routes
    AuthRoutes(jwtAuth, webConfig).register(app)
    if (webConfig.playerAuth.enabled) {
        PlayerAuthRoutes(brennon, jwtAuth, webConfig).register(app)
    }
    ModuleRoutes(brennon, mapOf(
        "pterodactyl" to webConfig.pterodactyl.enabled,
        "luckperms" to (brennon.luckPermsHook?.isActive == true)
    )).register(app)
    PlayerRoutes(brennon).register(app)
    RankRoutes(brennon).register(app)
    PunishmentRoutes(brennon).register(app)
    EconomyRoutes(brennon).register(app)
    ServerRoutes(brennon).register(app)
    AuditRoutes(auditLogger).register(app)

    if (brennon.config.modules.tickets) {
        TicketRoutes(brennon).register(app)
    }
    if (brennon.config.modules.stats) {
        StatsRoutes(brennon).register(app)
    }
    if (brennon.config.modules.chat) {
        ChatRoutes(brennon).register(app)
    }
    if (brennon.config.modules.staffTools) {
        StaffRoutes(brennon).register(app)
        ReportRoutes(brennon).register(app)
    }

    // Player-facing routes
    AppealRoutes(brennon, appealRepository, jwtAuth, webConfig).register(app)
    PlayerProfileRoutes(brennon, jwtAuth, webConfig).register(app)
    AdminPlayerRoutes(brennon, jwtAuth, webConfig).register(app)
    PublicRoutes(brennon).register(app)

    if (brennon.config.modules.tickets) {
        PlayerTicketRoutes(brennon, jwtAuth, webConfig).register(app)
    }

    // LuckPerms routes
    LuckPermsRoutes(brennon, lpEditorBridge).register(app)

    // Pterodactyl routes
    if (pteroClient != null) {
        PterodactylRoutes(pteroClient, jwtAuth, webConfig).register(app)
    }

    // SPA fallback — serve index.html for non-API routes
    app.error(404) { ctx ->
        if (!ctx.path().startsWith("/api") && !ctx.path().startsWith("/ws")) {
            val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("public/index.html")
            if (stream != null) {
                ctx.contentType("text/html")
                ctx.result(stream)
                ctx.status(200)
            }
        }
    }

    // Start Redis event bridge
    redisBridge?.start()

    app.start(webConfig.port)
    println("[Brennon] Web server started on port ${webConfig.port}")
    println("[Brennon] Dashboard: http://localhost:${webConfig.port}")
    if (webConfig.wsEnabled) {
        println("[Brennon] WebSocket: ws://localhost:${webConfig.port}/ws")
    }

    // Keep main thread alive
    Thread.currentThread().join()
}

private fun loadWebServerConfig(dataFolder: File): WebServerConfig {
    val configFile = File(dataFolder, "webserver.json")
    val gson = Gson()

    if (!configFile.exists()) {
        configFile.parentFile.mkdirs()
        configFile.writeText(gson.newBuilder().setPrettyPrinting().create().toJson(WebServerConfig()))
        println("[Brennon] Created default webserver.json — please configure it.")
    }

    return gson.fromJson(configFile.readText(), WebServerConfig::class.java)
}

private fun extractUsername(ctx: Context, jwtAuth: JwtAuth, config: WebServerConfig): String {
    val authHeader = ctx.header("Authorization") ?: return "anonymous"
    val token = authHeader.removePrefix("Bearer ").trim()

    if (token == config.apiKey) return "api-key"
    return jwtAuth.getUsername(token) ?: "unknown"
}

private fun categorizeRoute(path: String): AuditCategory {
    return when {
        path.startsWith("/api/auth") -> AuditCategory.AUTH
        path.startsWith("/api/player-auth") -> AuditCategory.AUTH
        path.startsWith("/api/admin/players") -> AuditCategory.PLAYER
        path.startsWith("/api/players") -> AuditCategory.PLAYER
        path.startsWith("/api/player/") -> AuditCategory.PLAYER
        path.startsWith("/api/ranks") -> AuditCategory.RANK
        path.startsWith("/api/punishments") -> AuditCategory.PUNISHMENT
        path.startsWith("/api/appeals") -> AuditCategory.PUNISHMENT
        path.startsWith("/api/economy") -> AuditCategory.ECONOMY
        path.startsWith("/api/tickets") -> AuditCategory.TICKET
        path.startsWith("/api/stats") -> AuditCategory.STATS
        path.startsWith("/api/leaderboard") -> AuditCategory.STATS
        path.startsWith("/api/servers") -> AuditCategory.SERVER
        path.startsWith("/api/pterodactyl") -> AuditCategory.SERVER
        path.startsWith("/api/staff") -> AuditCategory.STAFF
        path.startsWith("/api/reports") -> AuditCategory.REPORT
        path.startsWith("/api/chat") -> AuditCategory.CHAT
        else -> AuditCategory.OTHER
    }
}

private fun sanitizeBody(body: String, path: String): String? {
    if (body.isBlank()) return null
    // Redact passwords from auth requests
    if (path.startsWith("/api/auth")) {
        try {
            val json = JsonParser.parseString(body).asJsonObject
            if (json.has("password")) {
                json.addProperty("password", "***REDACTED***")
            }
            return json.toString()
        } catch (_: Exception) { }
    }
    return body
}
