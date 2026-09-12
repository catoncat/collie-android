package com.collie.app.ui

import android.app.Activity
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.collie.app.R
import com.collie.app.data.Agent
import com.collie.app.data.AgentKind
import com.collie.app.data.AgentStatus
import com.collie.app.data.Ask
import com.collie.app.data.HerdRepository
import com.collie.app.data.HerdState
import com.collie.app.data.Route
import com.collie.app.data.Sheet
import com.collie.app.data.Tab
import com.collie.app.data.TermLine
import com.collie.app.data.Tone
import com.collie.app.data.lastOutput
import com.collie.app.data.timeAgo
import com.collie.app.data.triage
import com.collie.app.ui.theme.Accent
import com.collie.app.ui.theme.AccentFg
import com.collie.app.ui.theme.Bg
import com.collie.app.ui.theme.Blocked
import com.collie.app.ui.theme.Border
import com.collie.app.ui.theme.Card
import com.collie.app.ui.theme.Chrome
import com.collie.app.ui.theme.ControlOn
import com.collie.app.ui.theme.ControlOnFg
import com.collie.app.ui.theme.Faint
import com.collie.app.ui.theme.Fg
import com.collie.app.ui.theme.Idle
import com.collie.app.ui.theme.Inset
import com.collie.app.ui.theme.Muted
import com.collie.app.ui.theme.Ready
import com.collie.app.ui.theme.Rule
import com.collie.app.ui.theme.Sharp
import com.collie.app.ui.theme.Working
import com.collie.app.ui.theme.hostColor
import kotlinx.coroutines.delay

@Composable
fun CollieRoot() {
    val s by HerdRepository.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    LaunchedEffect(s.haptics, s.toast, s.lastKey) {
        if (s.haptics && (s.toast != null || s.lastKey != null)) buzz(ctx as Activity)
    }
    LaunchedEffect(s.toast, s.lastKey) {
        if (s.toast != null || s.lastKey != null) {
            delay(1400)
            HerdRepository.clearToast()
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        when (s.route) {
            Route.Lock -> LockScreen(s)
            Route.Pane -> PaneScreen(s)
            Route.Main -> MainShell(s)
        }
        s.toast?.takeIf { s.route != Route.Pane }?.let { toast ->
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)) {
                Text(
                    toast,
                    color = AccentFg,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.background(Accent, Sharp).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun MainShell(s: HerdState) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            when (s.tab) {
                Tab.Herd -> HerdScreen(s)
                Tab.Crew -> CrewScreen(s)
                Tab.Me -> MeScreen(s)
            }
        }
        BottomNav(s)
    }
}

@Composable
private fun AppHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .border(width = 0.dp, color = Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CollieMark(40.dp)
        Spacer(Modifier.width(12.dp))
        Text("Collie", color = Fg, fontSize = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
    }
    Hairline()
}

@Composable
private fun AskBanner(s: HerdState) {
    val blocked = s.agents.find { it.status == AgentStatus.Blocked && it.ask != null } ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .border(width = 0.dp, color = Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("${blocked.name} · ${if (s.localeZh) "需要你" else "Needs you"}", color = Fg, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(blocked.ask?.command ?: blocked.ask?.title ?: "", color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            if (s.localeZh) "批准" else "Approve",
            color = AccentFg,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .background(Accent, Sharp)
                .clickable { HerdRepository.answer(blocked.id, "yes") }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (s.localeZh) "打开" else "Open",
            color = Muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .border(1.dp, Border, Sharp)
                .clickable { HerdRepository.openPane(blocked.id) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
    Hairline()
}

@Composable
private fun LockScreen(s: HerdState) {
    val blocked = s.agents.count { it.status == AgentStatus.Blocked }
    val working = s.agents.count { it.status == AgentStatus.Working }
    val activity = LocalContext.current as FragmentActivity
    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        CollieMark(88.dp)
        Spacer(Modifier.height(16.dp))
        Text("Collie", color = Fg, fontSize = 34.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
        Text(if (s.localeZh) "你的 agent 牧群" else "Your agent herd", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "$blocked ${if (s.localeZh) "需要你" else "need you"} · $working ${if (s.localeZh) "工作中" else "working"}",
            color = Faint,
            fontSize = 12.sp,
        )
        Spacer(Modifier.weight(1f))
        PrimaryBtn(if (s.localeZh) "用指纹解锁" else "Unlock with fingerprint", Icons.Filled.Fingerprint) {
            unlockWithBio(activity, s.bio)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (s.localeZh) "直接进入" else "Enter anyway",
            color = Muted,
            fontSize = 14.sp,
            modifier = Modifier.clickable { HerdRepository.unlock() }.padding(12.dp),
        )
    }
}

private fun unlockWithBio(activity: FragmentActivity, bioEnabled: Boolean) {
    if (!bioEnabled) {
        HerdRepository.unlock()
        return
    }
    val mgr = BiometricManager.from(activity)
    val can = mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
    if (can != BiometricManager.BIOMETRIC_SUCCESS) {
        HerdRepository.unlock()
        return
    }
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                HerdRepository.unlock()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) return
                HerdRepository.unlock()
            }
        },
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Collie")
            .setSubtitle(if (bioEnabled) "解锁牧群" else "Unlock")
            .setNegativeButtonText("取消")
            .build(),
    )
}

@Composable
private fun HerdScreen(s: HerdState) {
    val (blocked, working, idle) = s.agents.triage()
    var recentOpen by remember { mutableStateOf(true) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { AppHeader() }
        item { Spacer(Modifier.height(16.dp)) }
        if (blocked.isEmpty()) {
            item {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Ready, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (s.localeZh) "没有人在等你" else "Nobody is waiting", color = Fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            item { SectionLabel(if (s.localeZh) "需要你" else "Needs you", blocked.size, Blocked, accent = true) }
            items(blocked, key = { it.id }) { a ->
                AskCard(a, s)
                Spacer(Modifier.height(8.dp))
            }
        }
        if (working.isNotEmpty()) {
            item { Spacer(Modifier.height(12.dp)); SectionLabel(if (s.localeZh) "工作中" else "Working", working.size, Working) }
            item { ListGroup { working.forEach { AgentRow(it, s, showOutput = true) } } }
        }
        if (idle.isNotEmpty()) {
            item {
                Spacer(Modifier.height(12.dp))
                SectionLabel(if (s.localeZh) "最近" else "Recent", idle.size, Idle, open = recentOpen) { recentOpen = !recentOpen }
            }
            if (recentOpen) item { ListGroup { idle.forEach { AgentRow(it, s, showOutput = false) } } }
        }
    }
}

@Composable
private fun AskCard(a: Agent, s: HerdState) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .border(1.dp, Blocked.copy(alpha = 0.4f), Sharp)
            .background(Blocked.copy(alpha = 0.05f))
            .padding(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { HerdRepository.openPane(a.id) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(a.status)
            Spacer(Modifier.width(8.dp))
            AgentGlyph(a.kind)
            Spacer(Modifier.width(8.dp))
            Text(a.name, color = Fg, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            HostChip(a.host)
        }
        Text("${a.space} · ${a.tab}", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        a.ask?.let { ask ->
            Spacer(Modifier.height(12.dp))
            AskBlock(ask, bare = true) { HerdRepository.answer(a.id, it) }
        }
    }
}

@Composable
private fun AgentRow(a: Agent, s: HerdState, showOutput: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { HerdRepository.openPane(a.id) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(a.status)
                Spacer(Modifier.width(8.dp))
                AgentGlyph(a.kind)
                Spacer(Modifier.width(8.dp))
                Text(a.name, color = Fg, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("${a.space} · ${a.tab}", color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (showOutput) {
                Text(a.lastOutput(), color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            HostChip(a.host)
            Text(timeAgo(a.lastActiveAt, s.clock, s.localeZh), color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CrewScreen(s: HerdState) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { AppHeader() }
        item { AskBanner(s) }
        item { Spacer(Modifier.height(16.dp)); SectionLabel(if (s.localeZh) "机群" else "Crew", s.machines.size, Idle) }
        item {
            ListGroup {
                s.machines.forEach { m ->
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(if (m.online) Ready else Idle, CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Icon(Icons.Filled.Dns, contentDescription = null, tint = hostColor(m.id), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.name, color = Fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            val role = when (m.role) {
                                "lead" -> if (s.localeZh) "主机" else "Lead"
                                "deputy" -> if (s.localeZh) "副机" else "Deputy"
                                else -> if (s.localeZh) "同伴" else "Peer"
                            }
                            Text("$role · ${m.mux} · ${m.agents} agents", color = Muted, fontSize = 12.sp)
                        }
                        if (m.blocked > 0) Text("${m.blocked}", color = Blocked, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            QuietBtn(if (s.localeZh) "切到副机" else "Fail over to deputy") {
                HerdRepository.ping(if (s.localeZh) "演示机群，未切换" else "Demo crew, not switched")
            }
        }
    }
}

@Composable
private fun MeScreen(s: HerdState) {
    val blocked = s.agents.count { it.status == AgentStatus.Blocked }
    val working = s.agents.count { it.status == AgentStatus.Working }
    val waiting = s.agents.find { it.status == AgentStatus.Blocked }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item { AppHeader() }
        item { AskBanner(s) }
        item {
            Row(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .border(1.dp, Rule, Sharp)
                    .background(Card)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CollieMark(40.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(if (s.localeZh) "此设备 · Android" else "This device · Android", color = Fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(if (s.localeZh) "未配对 · 演示数据" else "Unpaired · demo data", color = Muted, fontSize = 12.sp)
                }
            }
        }
        item {
            ListGroup {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (s.localeZh) "语言" else "Language", color = Fg, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Row(Modifier.border(1.dp, Border, Sharp).padding(2.dp)) {
                        Seg("中文", s.localeZh) { HerdRepository.setLocaleZh(true) }
                        Seg("EN", !s.localeZh) { HerdRepository.setLocaleZh(false) }
                    }
                }
                ToggleRow(if (s.localeZh) "触感反馈" else "Haptic feedback", s.haptics) { HerdRepository.setHaptics(it) }
                ToggleRow(if (s.localeZh) "指纹锁" else "Fingerprint lock", s.bio) { HerdRepository.setBio(it) }
                ToggleRow(if (s.localeZh) "系统通知" else "System notifications", !s.quiet) { HerdRepository.setQuiet(!it) }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            SectionLabel(if (s.localeZh) "桌面小部件" else "Home widgets", 2, Idle)
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f).border(1.dp, Rule, Sharp).background(Card).padding(12.dp)) {
                    Text(if (s.localeZh) "牧群概览" else "Herd glance", color = Muted, fontSize = 11.sp)
                    Text("$blocked", color = Fg, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (s.localeZh) "需要你" else "Needs you", color = Blocked, fontSize = 12.sp)
                    Text("$working ${if (s.localeZh) "工作中" else "working"}", color = Muted, fontSize = 12.sp)
                }
                Column(Modifier.weight(1f).border(1.dp, Rule, Sharp).background(Card).padding(12.dp)) {
                    Text(if (s.localeZh) "需要你" else "Needs you", color = Muted, fontSize = 11.sp)
                    Text(waiting?.name ?: "—", color = Fg, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(waiting?.ask?.command ?: if (s.localeZh) "没有人在等你" else "Nobody is waiting", color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        item {
            Spacer(Modifier.height(16.dp))
            Text(if (s.localeZh) "关于" else "About", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Text(
                if (s.localeZh)
                    "Collie 把卡住的 agent 变成手机上的三秒决定。Herd 上直接批；打开 pane 才是打字和按键。系统通知栏批准、桌面小组件是真的。Herd 数据是演示。"
                else
                    "Collie turns a stuck agent into a three-second decision. Approve on the herd; open a pane to type. Shade actions and the widget are real. The herd itself is a demo.",
                color = Muted,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (s.bio) {
                Spacer(Modifier.height(16.dp))
                QuietBtn(if (s.localeZh) "锁定" else "Lock") { HerdRepository.lock() }
            }
        }
    }
}

@Composable
private fun PaneScreen(s: HerdState) {
    val agent = s.agents.find { it.id == s.paneId } ?: return
    val listState = rememberLazyListState()
    LaunchedEffect(agent.lines.size, agent.ask?.id) {
        if (agent.lines.isNotEmpty()) listState.animateScrollToItem(agent.lines.lastIndex)
    }
    val inline = s.sheet == Sheet.Keys || s.sheet == Sheet.Quick || s.sheet == Sheet.Agent
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "back",
                    tint = Fg,
                    modifier = Modifier.size(44.dp).clickable { HerdRepository.closePane() }.padding(10.dp),
                )
                AgentGlyph(agent.kind, 16.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("${agent.name} · ${agent.tab}", color = Fg, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(agent.cwd, color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    statusLabel(agent.status, s.localeZh),
                    color = when (agent.status) {
                        AgentStatus.Blocked -> Blocked
                        AgentStatus.Working -> Working
                        else -> Muted
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            Hairline()
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                items(agent.lines, key = { it.id }) { line -> TermRow(line) }
                if (agent.ask != null) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        AskBlock(agent.ask, bare = false) { HerdRepository.answer(agent.id, it) }
                    }
                }
            }
            if (s.lastKey != null) {
                Text(s.lastKey, color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.fillMaxWidth().padding(4.dp))
            }
            Column(Modifier.fillMaxWidth().background(Chrome)) {
                Hairline()
                if (inline) {
                    when (s.sheet) {
                        Sheet.Keys -> KeysPad { HerdRepository.sendKey(agent.id, it) }
                        Sheet.Quick -> QuickPad { HerdRepository.sendText(agent.id, it) }
                        Sheet.Agent -> AgentPad { HerdRepository.sendText(agent.id, it) }
                        else -> {}
                    }
                }
                Column(Modifier.padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Dock(Modifier.weight(1f), if (s.localeZh) "按键" else "Keys", Icons.Filled.Keyboard, s.sheet == Sheet.Keys) { HerdRepository.openSheet(Sheet.Keys) }
                        Dock(Modifier.weight(1f), if (s.localeZh) "快捷" else "Quick", Icons.Outlined.Bolt, s.sheet == Sheet.Quick) { HerdRepository.openSheet(Sheet.Quick) }
                        Dock(Modifier.weight(1f), if (s.localeZh) "代理" else "Agent", Icons.Outlined.AutoAwesome, s.sheet == Sheet.Agent) { HerdRepository.openSheet(Sheet.Agent) }
                        Dock(Modifier.weight(1f), if (s.localeZh) "附件" else "Attach", Icons.Outlined.AttachFile, s.sheet == Sheet.Share) { HerdRepository.openSheet(Sheet.Share) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Row(
                            Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .border(1.dp, Border, Sharp)
                                .background(Bg)
                                .padding(start = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            BasicTextField(
                                value = s.draft,
                                onValueChange = { HerdRepository.setDraft(it) },
                                textStyle = TextStyle(color = Fg, fontSize = 14.sp),
                                cursorBrush = SolidColor(Accent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { HerdRepository.sendText(agent.id, s.draft) }),
                                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                                decorationBox = { inner ->
                                    if (s.draft.isEmpty()) Text(if (s.localeZh) "回复…" else "Reply…", color = Faint, fontSize = 14.sp)
                                    inner()
                                },
                            )
                            Icon(
                                Icons.Filled.Mic,
                                contentDescription = "voice",
                                tint = Muted,
                                modifier = Modifier.size(44.dp).clickable { HerdRepository.openSheet(Sheet.Voice) }.padding(12.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.size(44.dp).background(Accent, Sharp).clickable { HerdRepository.sendText(agent.id, s.draft) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "send", tint = AccentFg, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        if (s.sheet == Sheet.Voice || s.sheet == Sheet.Share) {
            Sheets(s, agent.id)
        }
    }
}

@Composable
private fun Sheets(s: HerdState, agentId: String) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color(0x73000000)).clickable { HerdRepository.closeSheet() })
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Card)
                .border(1.dp, Rule, Sharp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp).size(width = 40.dp, height = 4.dp).background(Fg.copy(alpha = 0.25f), Sharp))
            when (s.sheet) {
                Sheet.Voice -> VoicePad(s) { HerdRepository.sendText(agentId, if (s.localeZh) "继续" else "continue") }
                Sheet.Share -> SharePad { HerdRepository.receiveShare(it) }
                else -> {}
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun KeysPad(onKey: (String) -> Unit) {
    val row1 = listOf("Esc", "Tab", "⇧", "Ctrl", "Alt", "↑", "Enter")
    Column(Modifier.padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row1.forEach { k -> KeyBtn(k, Modifier.weight(1f), onKey) }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            KeyBtn("^C", Modifier.weight(2f), onKey)
            KeyBtn("Space", Modifier.weight(2f), onKey)
            KeyBtn("←", Modifier.weight(1f), onKey)
            KeyBtn("↓", Modifier.weight(1f), onKey)
            KeyBtn("→", Modifier.weight(1f), onKey)
        }
    }
}

@Composable
private fun KeyBtn(k: String, modifier: Modifier, onKey: (String) -> Unit) {
    Box(
        modifier
            .height(44.dp)
            .border(1.dp, Border, Sharp)
            .background(Bg)
            .clickable { onKey(k) },
        contentAlignment = Alignment.Center,
    ) { Text(k, color = Fg, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace) }
}

@Composable
private fun QuickPad(onPick: (String) -> Unit) {
    Column(Modifier.padding(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("yes", "no").forEach { v ->
                Box(
                    Modifier.weight(1f).height(48.dp).border(1.dp, Border, Sharp).background(Bg).clickable { onPick(v) },
                    contentAlignment = Alignment.Center,
                ) { Text(v, color = if (v == "no") Blocked else Fg, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
            }
        }
        Spacer(Modifier.height(8.dp))
        listOf("continue", "commit and push", "retry", "skip").chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { v ->
                    Box(
                        Modifier.weight(1f).height(48.dp).border(1.dp, Border, Sharp).background(Bg).clickable { onPick(v) },
                        contentAlignment = Alignment.Center,
                    ) { Text(v, color = Fg, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                }
            }
        }
    }
}

@Composable
private fun VoicePad(s: HerdState, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (s.localeZh) "正在听…" else "Listening…", color = Fg, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(if (s.localeZh) "系统语音识别。点一下结束。" else "System speech recognition. Tap to finish.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
        PrimaryBtn(if (s.localeZh) "插入「继续」" else "Insert continue") { onDone() }
    }
}

@Composable
private fun SharePad(onPick: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("相册", "相机", "文件").forEach { label ->
            Box(
                Modifier.weight(1f).height(80.dp).border(1.dp, Border, Sharp).background(Bg).clickable { onPick(label) },
                contentAlignment = Alignment.Center,
            ) { Text(label, color = Fg, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
private fun AgentPad(onPick: (String) -> Unit) {
    Column(Modifier.padding(8.dp)) {
        listOf("/compact", "/status", "/diff", "/undo").chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { cmd ->
                    Box(
                        Modifier.weight(1f).height(48.dp).border(1.dp, Border, Sharp).background(Bg).clickable { onPick(cmd) },
                        contentAlignment = Alignment.Center,
                    ) { Text(cmd, color = Fg, fontSize = 13.sp, fontFamily = FontFamily.Monospace) }
                }
            }
        }
    }
}

@Composable
private fun AskBlock(ask: Ask, bare: Boolean, onPick: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (bare) Modifier else Modifier.border(1.dp, Border, Sharp).background(Card).padding(6.dp)),
    ) {
        Text(ask.title.uppercase(), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        ask.command?.let { Text(it, color = Fg, fontSize = 14.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(top = 4.dp)) }
        ask.detail?.let { Text(it, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
        Spacer(Modifier.height(8.dp))
        ask.options.forEachIndexed { i, opt ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .heightIn(min = 44.dp)
                    .border(1.dp, Border, Sharp)
                    .background(if (opt.destructive) Blocked.copy(alpha = 0.12f) else Color(0xFF3A3A3A))
                    .clickable { onPick(opt.id) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(20.dp).border(1.dp, Border, Sharp).background(Bg),
                    contentAlignment = Alignment.Center,
                ) { Text("${i + 1}", color = Muted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.width(8.dp))
                Text(opt.label, color = if (opt.destructive) Blocked else Fg, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun TermRow(line: TermLine) {
    val color = when (line.tone) {
        Tone.Cmd -> Working
        Tone.Ok -> Ready
        Tone.Err -> Blocked
        Tone.In -> Fg
        Tone.Out, Tone.Dim -> Muted
    }
    Text(line.text, color = color, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 2.dp))
}

@Composable
private fun BottomNav(s: HerdState) {
    val blocked = s.agents.count { it.status == AgentStatus.Blocked }
    val items = listOf(
        Triple(Tab.Herd, if (s.localeZh) "牧群" else "Herd", Icons.Filled.Layers to blocked),
        Triple(Tab.Crew, if (s.localeZh) "机群" else "Crew", Icons.Filled.Dns to 0),
        Triple(Tab.Me, if (s.localeZh) "我的" else "You", Icons.Filled.Person to 0),
    )
    Hairline()
    Row(Modifier.fillMaxWidth().height(56.dp).background(Bg), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (tab, label, iconBadge) ->
            val on = s.tab == tab
            Box(Modifier.weight(1f).clickable { HerdRepository.setTab(tab) }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        Icon(iconBadge.first, contentDescription = label, tint = if (on) Fg else Muted, modifier = Modifier.size(22.dp))
                        if (iconBadge.second > 0) {
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(start = 14.dp).size(16.dp).background(Blocked, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${iconBadge.second}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Text(label, color = if (on) Fg else Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun Dock(modifier: Modifier, label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier
            .height(40.dp)
            .background(if (active) ControlOn else Color.Transparent, Sharp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = if (active) ControlOnFg else Muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = if (active) ControlOnFg else Muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AgentGlyph(kind: AgentKind, size: Dp = 16.dp) {
    Box(
        Modifier.size(size).border(1.dp, Border, Sharp).background(Color(0xFF3A3A3A)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            when (kind) {
                AgentKind.Claude -> Icons.Outlined.AutoAwesome
                AgentKind.Codex -> Icons.Filled.Layers
                AgentKind.OpenCode -> Icons.Filled.Keyboard
            },
            contentDescription = null,
            tint = Fg,
            modifier = Modifier.size(size * 0.6f),
        )
    }
}

@Composable
private fun CollieMark(size: Dp) {
    Icon(
        painter = painterResource(R.drawable.ic_collie),
        contentDescription = "Collie",
        tint = Fg,
        modifier = Modifier.size(size),
    )
}

@Composable
private fun StatusDot(status: AgentStatus) {
    val resting = status == AgentStatus.Idle || status == AgentStatus.Done
    if (resting) {
        Box(Modifier.size(8.dp).border(1.5.dp, Idle.copy(alpha = 0.6f), CircleShape).background(Bg, CircleShape))
    } else {
        Box(Modifier.size(8.dp).background(statusColor(status), CircleShape))
    }
}

@Composable
private fun HostChip(host: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Dns, contentDescription = null, tint = hostColor(host), modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(host, color = Muted, fontSize = 12.sp)
    }
}

private fun statusColor(status: AgentStatus) = when (status) {
    AgentStatus.Blocked -> Blocked
    AgentStatus.Working -> Working
    AgentStatus.Done -> Ready
    AgentStatus.Idle -> Idle
}

private fun statusLabel(status: AgentStatus, zh: Boolean) = when (status) {
    AgentStatus.Blocked -> if (zh) "需要你" else "Needs you"
    AgentStatus.Working -> if (zh) "工作中" else "Working"
    AgentStatus.Done -> if (zh) "完成" else "Done"
    AgentStatus.Idle -> if (zh) "空闲" else "Idle"
}

@Composable
private fun SectionLabel(title: String, count: Int, color: Color, accent: Boolean = false, open: Boolean? = null, onToggle: (() -> Unit)? = null) {
    val row = @Composable {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(title.uppercase(), color = if (accent) Blocked else Muted, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)
            Spacer(Modifier.width(8.dp))
            Text("$count", color = Muted, fontSize = 12.sp)
            if (onToggle != null) {
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp).rotate(if (open == true) 90f else 0f))
            }
        }
    }
    if (onToggle != null) Box(Modifier.clickable(onClick = onToggle)) { row() } else row()
}

@Composable
private fun ListGroup(content: @Composable () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .border(1.dp, Rule, Sharp),
    ) { content() }
}

@Composable
private fun Hairline() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Rule))
}

@Composable
private fun PrimaryBtn(label: String, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).background(Accent, Sharp).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = AccentFg, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = AccentFg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun QuietBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(48.dp)
            .border(1.dp, Border, Sharp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Fg, fontSize = 14.sp, fontWeight = FontWeight.Medium) }
}

@Composable
private fun Seg(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (on) AccentFg else Muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.background(if (on) Accent else Color.Transparent, Sharp).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun ToggleRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!on) }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Fg, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Box(
            Modifier.width(40.dp).height(24.dp).border(1.dp, Border, Sharp).background(if (on) Accent else Inset),
            contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.padding(2.dp).size(20.dp).background(if (on) AccentFg else Muted, CircleShape))
        }
    }
}

@Suppress("DEPRECATION")
private fun buzz(activity: Activity) {
    try {
        val v = if (Build.VERSION.SDK_INT >= 31) {
            (activity.getSystemService(VibratorManager::class.java)).defaultVibrator
        } else {
            activity.getSystemService(Vibrator::class.java)
        }
        if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE))
        else v.vibrate(12)
    } catch (_: Exception) {
    }
}
