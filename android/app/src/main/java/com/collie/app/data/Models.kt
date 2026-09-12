package com.collie.app.data

enum class AgentKind { Claude, Codex, OpenCode }

enum class AgentStatus { Blocked, Working, Idle, Done }

enum class Tone { Dim, In, Out, Cmd, Ok, Err }

enum class Tab { Herd, Crew, Me }

enum class Route { Lock, Main, Pane }

enum class Sheet { None, Keys, Quick, Voice, Share, Agent }

data class AskOption(
    val id: String,
    val label: String,
    val destructive: Boolean = false,
)

data class Ask(
    val id: String,
    val kind: String,
    val title: String,
    val command: String? = null,
    val detail: String? = null,
    val options: List<AskOption>,
)

data class TermLine(
    val id: String,
    val tone: Tone,
    val text: String,
)

data class Agent(
    val id: String,
    val name: String,
    val kind: AgentKind,
    val status: AgentStatus,
    val host: String,
    val cwd: String,
    val space: String,
    val tab: String,
    val lastActiveAt: Long,
    val ask: Ask? = null,
    val lines: List<TermLine> = emptyList(),
)

data class Notif(
    val id: String,
    val agentId: String,
    val title: String,
    val body: String,
    val at: Long,
    val unread: Boolean = true,
)

data class Machine(
    val id: String,
    val name: String,
    val role: String,
    val online: Boolean,
    val mux: String,
    val agents: Int,
    val blocked: Int,
)

data class HerdState(
    val localeZh: Boolean = true,
    val haptics: Boolean = true,
    val quiet: Boolean = false,
    val bio: Boolean = false,
    val unlocked: Boolean = true,
    val route: Route = Route.Main,
    val tab: Tab = Tab.Herd,
    val paneId: String? = null,
    val sheet: Sheet = Sheet.None,
    val toast: String? = null,
    val lastKey: String? = null,
    val draft: String = "",
    val agents: List<Agent> = emptyList(),
    val notifs: List<Notif> = emptyList(),
    val machines: List<Machine> = emptyList(),
    val clock: Long = System.currentTimeMillis(),
    val shareIncoming: String? = null,
)

fun Agent.lastOutput(): String {
    ask?.command?.let { return it }
    ask?.title?.let { return it }
    return lines.asReversed().firstOrNull {
        it.tone == Tone.Out || it.tone == Tone.Ok || it.tone == Tone.Cmd
    }?.text ?: cwd
}
