package com.collie.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

object HerdRepository {
    private var seq = 1
    private fun nid(p: String = "n") = "$p${seq++}"

    private val _state = MutableStateFlow(seed())
    val state: StateFlow<HerdState> = _state.asStateFlow()

    fun snapshot(): HerdState = _state.value

    fun setLocaleZh(v: Boolean) = _state.update { it.copy(localeZh = v) }
    fun setHaptics(v: Boolean) = _state.update { it.copy(haptics = v) }
    fun setQuiet(v: Boolean) = _state.update { it.copy(quiet = v) }
    fun setBio(v: Boolean) = _state.update { it.copy(bio = v) }
    fun setDraft(v: String) = _state.update { it.copy(draft = v) }
    fun setTab(tab: Tab) = _state.update { it.copy(tab = tab, route = Route.Main, paneId = null, sheet = Sheet.None) }
    fun openSheet(sheet: Sheet) = _state.update { it.copy(sheet = if (it.sheet == sheet) Sheet.None else sheet) }
    fun closeSheet() = _state.update { it.copy(sheet = Sheet.None) }
    fun ping(msg: String) = _state.update { it.copy(toast = msg) }
    fun clearToast() = _state.update { it.copy(toast = null, lastKey = null) }
    fun consumeShare() = _state.update { it.copy(shareIncoming = null) }

    fun unlock() = _state.update { it.copy(unlocked = true, route = Route.Main) }

    fun lock() = _state.update {
        it.copy(unlocked = false, route = Route.Lock, paneId = null, sheet = Sheet.None)
    }

    fun openPane(id: String) = _state.update { s ->
        s.copy(
            route = Route.Pane,
            paneId = id,
            sheet = Sheet.None,
            agents = s.agents.map { if (it.id == id) it.copy(lastActiveAt = now()) else it },
            notifs = s.notifs.map { if (it.agentId == id) it.copy(unread = false) else it },
        )
    }

    fun closePane() = _state.update { it.copy(route = Route.Main, paneId = null, sheet = Sheet.None) }

    fun back() = _state.update { s ->
        when {
            s.sheet != Sheet.None -> s.copy(sheet = Sheet.None)
            s.route == Route.Pane -> s.copy(route = Route.Main, paneId = null)
            else -> s
        }
    }

    fun receiveShare(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _state.update { s ->
            val target = s.paneId ?: s.agents.firstOrNull()?.id
            if (target == null) s.copy(shareIncoming = trimmed)
            else appendLine(s, target, Tone.In, "attached $trimmed").copy(
                toast = if (s.localeZh) "已送达" else "Sent",
                shareIncoming = null,
            )
        }
    }

    fun answer(agentId: String, optionId: String) {
        _state.update { s ->
            val agent = s.agents.find { it.id == agentId } ?: return@update s
            val ask = agent.ask ?: return@update s
            val label = ask.options.find { it.id == optionId }?.label ?: optionId
            val denied = optionId == "no"
            val agents = s.agents.map { a ->
                if (a.id != agentId) a
                else a.copy(
                    ask = null,
                    status = if (denied) AgentStatus.Idle else AgentStatus.Working,
                    lastActiveAt = now(),
                    lines = a.lines + TermLine(nid("l"), Tone.In, "→ $label") +
                        TermLine(nid("l"), if (denied) Tone.Err else Tone.Ok, if (denied) "rejected" else "approved · running"),
                )
            }
            s.copy(
                agents = agents,
                machines = bump(s.machines, agents),
                notifs = s.notifs.filter { it.agentId != agentId },
                toast = if (s.localeZh) "已送达" else "Sent",
                sheet = Sheet.None,
            )
        }
    }

    fun sendText(agentId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _state.update { s ->
            appendLine(s, agentId, Tone.In, "❯ $trimmed").copy(
                draft = "",
                toast = if (s.localeZh) "已送达" else "Sent",
                sheet = Sheet.None,
            )
        }
    }

    fun sendKey(agentId: String, key: String) {
        _state.update { s ->
            appendLine(s, agentId, Tone.Dim, "[$key]").copy(lastKey = key)
        }
    }

    fun tick() {
        _state.update { s ->
            val tnow = now()
            val agents = s.agents.map { it.copy() }.toMutableList()
            var notifs = s.notifs
            for (i in agents.indices) {
                val a = agents[i]
                if (a.status == AgentStatus.Working && Random.nextFloat() < 0.42f) {
                    val (tone, text) = WORK.random()
                    agents[i] = a.copy(
                        lines = (a.lines + TermLine(nid("l"), tone, text)).takeLast(40),
                        lastActiveAt = tnow,
                    )
                }
            }
            if (agents.none { it.status == AgentStatus.Blocked } && Random.nextFloat() < 0.22f) {
                val idx = agents.indices.filter { agents[it].status == AgentStatus.Working }.randomOrNull()
                if (idx != null) {
                    val pick = agents[idx]
                    val ask = nextAsk(pick.name)
                    agents[idx] = pick.copy(
                        status = AgentStatus.Blocked,
                        ask = ask,
                        lastActiveAt = tnow,
                        lines = pick.lines + TermLine(nid("l"), Tone.Dim, "This command requires approval"),
                    )
                    if (!s.quiet) {
                        notifs = listOf(
                            Notif(nid("n"), pick.id, "${pick.name} needs you", ask.command ?: ask.title, tnow),
                        ) + notifs
                        notifs = notifs.take(8)
                    }
                }
            }
            s.copy(
                agents = agents,
                notifs = notifs,
                clock = tnow,
                machines = bump(s.machines, agents),
            )
        }
    }

    private fun appendLine(s: HerdState, agentId: String, tone: Tone, text: String): HerdState {
        val agents = s.agents.map { a ->
            if (a.id != agentId) a
            else a.copy(
                lastActiveAt = now(),
                status = if (a.status == AgentStatus.Idle) AgentStatus.Working else a.status,
                lines = a.lines + TermLine(nid("l"), tone, text),
            )
        }
        return s.copy(agents = agents, machines = bump(s.machines, agents))
    }

    private fun now() = System.currentTimeMillis()

    private fun bump(machines: List<Machine>, agents: List<Agent>) = machines.map { m ->
        val mine = agents.filter { it.host == m.id }
        m.copy(agents = mine.size, blocked = mine.count { it.status == AgentStatus.Blocked }, online = true)
    }

    private val MKFIFO = Ask(
        id = "ask-mkfifo",
        kind = "permission",
        title = "Bash 命令",
        command = "mkfifo fixture-fifo",
        detail = "创建命名管道（FIFO）。这条命令需要你批准。",
        options = listOf(
            AskOption("yes", "批准并继续"),
            AskOption("always", "批准，并且 mkfifo * 不再问"),
            AskOption("no", "拒绝", destructive = true),
        ),
    )

    private val PUSH = Ask(
        id = "ask-push",
        kind = "permission",
        title = "Bash 命令",
        command = "git push origin feat/billing --force-with-lease",
        detail = "会改写远端分支。这条命令需要你批准。",
        options = listOf(
            AskOption("yes", "批准并继续"),
            AskOption("always", "批准，并且 git push * 不再问"),
            AskOption("no", "拒绝", destructive = true),
        ),
    )

    private val THEME = Ask(
        id = "ask-theme",
        kind = "question",
        title = "仪表盘用哪种颜色？",
        detail = "只给三个选项。",
        options = listOf(
            AskOption("red", "红"),
            AskOption("green", "绿"),
            AskOption("blue", "蓝"),
        ),
    )

    private val WORK = listOf(
        Tone.Out to "Reading src/server.ts",
        Tone.Out to "Checking types…",
        Tone.Ok to "● Update(src/routes.ts)",
        Tone.Cmd to "$ bun test",
        Tone.Ok to "✓ 14 passed",
        Tone.Out to "Drafting the retry path",
        Tone.Dim to "thinking",
        Tone.Out to "Searching for callers of sendKeys",
    )

    private fun nextAsk(name: String): Ask = when (name) {
        "api" -> PUSH.copy(id = nid("ask"))
        "mobile" -> THEME.copy(id = nid("ask"))
        else -> MKFIFO.copy(id = nid("ask"), command = "rm -rf dist/", detail = "Deletes the build output. This command requires approval.")
    }

    private fun lines(items: List<Pair<Tone, String>>) = items.map { TermLine(nid("l"), it.first, it.second) }

    private fun seed(): HerdState {
        val t0 = now()
        val agents = listOf(
            Agent(
                "webapp", "webapp", AgentKind.Claude, AgentStatus.Blocked, "lodge",
                "~/src/webapp", "webapp", "1", t0 - 20_000, MKFIFO,
                lines(
                    listOf(
                        Tone.Dim to "Claude Code  v2.1.201",
                        Tone.In to "Now run exactly this bash command: mkfifo fixture-fifo",
                        Tone.Out to "Running 1 shell command…",
                        Tone.Cmd to "$ mkfifo fixture-fifo",
                        Tone.Dim to "This command requires approval",
                    ),
                ),
            ),
            Agent(
                "api", "api", AgentKind.Codex, AgentStatus.Working, "lodge",
                "~/src/sprqvntrs-api", "api", "fix-deploy", t0 - 80_000, null,
                lines(listOf(Tone.Dim to "codex · gpt-5", Tone.Out to "Implementing /v2/webhooks retry ledger…", Tone.Ok to "✓ wrote src/webhooks/retry.ts")),
            ),
            Agent(
                "mobile", "mobile", AgentKind.Claude, AgentStatus.Working, "den",
                "~/src/mobile", "mobile", "layout", t0 - 2 * 60_000, null,
                lines(listOf(Tone.Out to "Refactoring bottom composer to sit above IME.", Tone.Cmd to "$ npm test -- composer")),
            ),
            Agent(
                "collie", "collie", AgentKind.Claude, AgentStatus.Working, "lodge",
                "~/src/collie", "collie", "code", t0 - 3 * 60_000, null,
                lines(listOf(Tone.Dim to "[Fable 5] ctx:15%  ~/collie", Tone.Out to "feature/block-renderer*  151.5k tokens")),
            ),
            Agent(
                "docs", "docs", AgentKind.Claude, AgentStatus.Idle, "lodge",
                "~/src/webapp/docs", "docs", "readme", t0 - 12 * 60_000, null,
                lines(listOf(Tone.Ok to "Wrote docs/android.md", Tone.Dim to "idle")),
            ),
            Agent(
                "infra", "infra", AgentKind.OpenCode, AgentStatus.Idle, "kennel",
                "~/infra", "infra", "hosts", t0 - 40 * 60_000, null,
                lines(listOf(Tone.Out to "flake lock bumped", Tone.Dim to "idle")),
            ),
        )
        return HerdState(
            unlocked = true,
            route = Route.Main,
            bio = false,
            agents = agents,
            notifs = listOf(Notif("n0", "webapp", "webapp needs you", "mkfifo fixture-fifo", t0 - 18_000)),
            machines = listOf(
                Machine("lodge", "lodge", "lead", true, "herdr", 4, 1),
                Machine("den", "den", "deputy", true, "herdr", 1, 0),
                Machine("kennel", "kennel", "peer", true, "herdr", 1, 0),
            ),
        )
    }
}

fun List<Agent>.triage(): Triple<List<Agent>, List<Agent>, List<Agent>> {
    val blocked = filter { it.status == AgentStatus.Blocked }.sortedByDescending { it.lastActiveAt }
    val working = filter { it.status == AgentStatus.Working }.sortedByDescending { it.lastActiveAt }
    val idle = filter { it.status == AgentStatus.Idle || it.status == AgentStatus.Done }.sortedByDescending { it.lastActiveAt }
    return Triple(blocked, working, idle)
}

fun timeAgo(at: Long, now: Long, zh: Boolean): String {
    val d = ((now - at) / 1000).coerceAtLeast(0)
    return when {
        d < 20 -> if (zh) "刚刚" else "now"
        d < 60 -> if (zh) "${d} 秒前" else "${d}s"
        d < 3600 -> if (zh) "${d / 60} 分钟前" else "${d / 60}m"
        d < 86400 -> if (zh) "${d / 3600} 小时前" else "${d / 3600}h"
        else -> if (zh) "${d / 86400} 天前" else "${d / 86400}d"
    }
}
