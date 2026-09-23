package com.viktor.edgeguard

import android.app.Activity
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors
import java.util.regex.Pattern

class MainActivity : Activity() {

    private val executor = Executors.newSingleThreadExecutor()
    private var privileged: IEdgeGuardPrivilegedService? = null
    private var physicalWidth = 0
    private var physicalHeight = 0

    private lateinit var status: TextView
    private lateinit var viewport: TextView
    private lateinit var applyButton: Button
    private lateinit var sideValue: TextView
    private lateinit var verticalValue: TextView

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == REQUEST_SHIZUKU && grantResult == PackageManager.PERMISSION_GRANTED) {
            bindPrivilegedService()
        } else {
            status.text = "Доступ Shizuku не надано"
        }
    }

    private val binderListener = Shizuku.OnBinderReceivedListener {
        updateShizukuState()
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            privileged = IEdgeGuardPrivilegedService.Stub.asInterface(service)
            status.text = "Системний доступ активний ✓"
            readDisplayInfo()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            privileged = null
            status.text = "Системний доступ від'єднано"
        }
    }

    private val userServiceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(this, EdgeGuardPrivilegedService::class.java))
            .processNameSuffix("edgeguard")
            .daemon(false)
            .tag("edgeguard-wm-v2")
            .version(2)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Display Borders"
        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        setContentView(buildUi())
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        Shizuku.removeBinderReceivedListener(binderListener)
        runCatching { Shizuku.unbindUserService(userServiceArgs, serviceConnection, false) }
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi(): View {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(32))
        }
        scroll.addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        root.addView(TextView(this).apply {
            text = "Display Borders"
            textSize = 30f
            setTypeface(typeface, Typeface.BOLD)
        })

        root.addView(TextView(this).apply {
            text = "Чорна рамка = справжня межа робочого екрана. Android перебудовує інтерфейс під меншу логічну роздільність; за рамкою не залишається активного контенту."
            textSize = 16f
            setPadding(0, dp(8), 0, dp(18))
        })

        status = TextView(this).apply {
            text = "Перевірка системного доступу…"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 0, 0, dp(12))
        }
        root.addView(status)

        root.addView(Button(this).apply {
            text = "Підключити / дозволити Shizuku"
            setOnClickListener { ensureShizukuPermission() }
        })

        root.addView(sectionTitle("Рамка"))

        sideValue = TextView(this).apply { textSize = 16f }
        root.addView(sideValue)
        root.addView(SeekBar(this).apply {
            max = 300
            progress = Prefs.side(this@MainActivity)
            setOnSeekBarChangeListener(simpleSeek { value ->
                Prefs.setSide(this@MainActivity, value)
                refreshPreview()
            })
        })

        verticalValue = TextView(this).apply { textSize = 16f }
        root.addView(verticalValue)
        root.addView(SeekBar(this).apply {
            max = 300
            progress = Prefs.vertical(this@MainActivity)
            setOnSeekBarChangeListener(simpleSeek { value ->
                Prefs.setVertical(this@MainActivity, value)
                refreshPreview()
            })
        })

        viewport = TextView(this).apply {
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(14), 0, dp(12))
        }
        root.addView(viewport)

        applyButton = Button(this).apply {
            text = "Застосувати рамку"
            isEnabled = false
            setOnClickListener { applyViewport() }
        }
        root.addView(applyButton)

        root.addView(Button(this).apply {
            text = "Повернути весь екран"
            setOnClickListener { resetViewport() }
        })

        root.addView(TextView(this).apply {
            text = "Рамка симетрична: однакова ширина зліва/справа та однакова висота зверху/знизу. Це дозволяє системі реально центрувати зменшений дисплей без прихованого контенту за краями. Після застосування Shizuku може бути закритий — системний override зберігається до скидання."
            textSize = 14f
            setPadding(0, dp(18), 0, 0)
        })

        refreshPreview()
        return scroll
    }

    private fun simpleSeek(onChange: (Int) -> Unit) = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (fromUser) onChange(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
    }

    private fun sectionTitle(text: String) = TextView(this).apply {
        this.text = text
        textSize = 20f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(18), 0, dp(8))
    }

    private fun updateShizukuState() {
        if (!Shizuku.pingBinder()) {
            status.text = "Shizuku не запущений"
            return
        }
        when (Shizuku.checkSelfPermission()) {
            PackageManager.PERMISSION_GRANTED -> bindPrivilegedService()
            else -> status.text = "Shizuku працює — потрібен дозвіл Display Borders"
        }
    }

    private fun ensureShizukuPermission() {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(this, "Спочатку запусти Shizuku", Toast.LENGTH_LONG).show()
            return
        }
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            bindPrivilegedService()
        } else if (!Shizuku.shouldShowRequestPermissionRationale()) {
            Shizuku.requestPermission(REQUEST_SHIZUKU)
        } else {
            status.text = "Дозвіл Shizuku заблоковано — дозволь Display Borders у Shizuku"
        }
    }

    private fun bindPrivilegedService() {
        if (privileged != null) return
        status.text = "Підключення системного сервісу…"
        runCatching { Shizuku.bindUserService(userServiceArgs, serviceConnection) }
            .onFailure { status.text = "Помилка Shizuku: ${it.message}" }
    }

    private fun readDisplayInfo() {
        val service = privileged ?: return
        executor.execute {
            runCatching { service.displaySizeInfo }
                .onSuccess { info ->
                    parsePhysicalSize(info)
                    runOnUiThread {
                        status.text = "Системний доступ активний ✓\n$info"
                        applyButton.isEnabled = physicalWidth > 0 && physicalHeight > 0
                        refreshPreview()
                    }
                }
                .onFailure { e -> runOnUiThread { status.text = "Не вдалося прочитати дисплей: ${e.message}" } }
        }
    }

    private fun parsePhysicalSize(info: String) {
        val m = Pattern.compile("Physical size:\\s*(\\d+)x(\\d+)").matcher(info)
        if (m.find()) {
            physicalWidth = m.group(1)?.toIntOrNull() ?: 0
            physicalHeight = m.group(2)?.toIntOrNull() ?: 0
        }
    }

    private fun refreshPreview() {
        val side = Prefs.side(this)
        val vertical = Prefs.vertical(this)
        sideValue.text = "Ліва + права рамка: $side px з кожного боку"
        verticalValue.text = "Верх + низ: $vertical px з кожного боку"

        if (physicalWidth > 0 && physicalHeight > 0) {
            val w = physicalWidth - side * 2
            val h = physicalHeight - vertical * 2
            viewport.text = if (w >= 320 && h >= 320) {
                "Робочий екран: ${w} × ${h} px"
            } else {
                "Рамка завелика"
            }
            applyButton.isEnabled = privileged != null && w >= 320 && h >= 320
        } else {
            viewport.text = "Робочий розмір з'явиться після підключення Shizuku"
        }
    }

    private fun applyViewport() {
        val service = privileged ?: return
        val w = physicalWidth - Prefs.side(this) * 2
        val h = physicalHeight - Prefs.vertical(this) * 2
        if (w < 320 || h < 320) return
        status.text = "Застосування…"
        executor.execute {
            runCatching { service.applyViewport(w, h) }
                .onSuccess { out -> runOnUiThread { status.text = "Рамку застосовано ✓\n$out" } }
                .onFailure { e -> runOnUiThread { status.text = "Помилка: ${e.message}" } }
        }
    }

    private fun resetViewport() {
        val service = privileged
        if (service == null) {
            ensureShizukuPermission()
            return
        }
        status.text = "Відновлення…"
        executor.execute {
            runCatching { service.resetViewport() }
                .onSuccess { out -> runOnUiThread { status.text = "Повний екран відновлено ✓\n$out" } }
                .onFailure { e -> runOnUiThread { status.text = "Помилка: ${e.message}" } }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_SHIZUKU = 1001
    }
}
