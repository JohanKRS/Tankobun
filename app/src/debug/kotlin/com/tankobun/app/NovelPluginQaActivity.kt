package com.tankobun.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tankobun.app.ui.settings.NovelSourceSettingsDialog
import com.tankobun.core.extensions.novel.LnReaderPlugin
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import org.json.JSONObject

/** Original fixtures in a separate debug application; never shipped in release. */
class NovelPluginQaActivity : ComponentActivity() {
    private var server: ServerSocket? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName.endsWith(".novelqa"))
        val listener = ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))
        server = listener
        Thread {
            while (!listener.isClosed) runCatching {
                listener.accept().use { socket ->
                    val input = socket.getInputStream().bufferedReader()
                    while (!input.readLine().isNullOrEmpty()) { }
                    val body = """<!doctype html><meta name="viewport" content="width=device-width,initial-scale=1"><title>Paper Observatory</title>
                        <style>body{font:18px sans-serif;padding:24px;color:#25313a;background:#faf9f6}button,input{font:inherit;padding:12px;margin:8px 0;display:block}button{border-radius:12px}#status{margin:24px 0}</style>
                        <h1>Paper Observatory</h1><p>A fictional website for local session testing.</p><label>Reader name<input aria-label="Reader name" id="name" value="Test reader"></label>
                        <button onclick="localStorage.setItem('reader',document.getElementById('name').value);sessionStorage.setItem('session','fictional-session');document.getElementById('status').textContent='Session saved';">Sign in</button><p id="status">Ready to sign in</p>
                    """.trimIndent().toByteArray()
                    socket.getOutputStream().write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                    socket.getOutputStream().write(body)
                }
            }
        }.apply { isDaemon = true; start() }
        val site = "http://127.0.0.1:${listener.localPort}"
        val plugin = LnReaderPlugin("settings-fixture", "Paper Observatory", site, "English", "1.0.0", "https://example.invalid/fixture.js", repositoryUrl = "https://example.invalid/plugins.json")
        val code = """
            const {storage}=require('@libs/storage');
            exports.default={id:'settings-fixture',name:'Paper Observatory',version:'1.0.0',site:'$site',webStorageUtilized:true,
              pluginSettings:{
                nickname:{label:'Reader name',type:'Text',value:''},
                password:{label:'Password',type:'Text',value:''},
                hideLocked:{label:'Hide locked chapters',type:'Switch',value:true},
                quality:{label:'Illustration quality',type:'Select',value:'high',options:[{label:'High',value:'high'},{label:'Standard',value:'standard'}]},
                excluded:{label:'Exclude chapter types',type:'CheckboxGroup',value:[],options:[{label:'Announcements',value:'announcements'},{label:'Previews',value:'previews'}]}
              },parseChapter:()=>'<p>Original fictional text.</p>'};
        """.trimIndent()
        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(code.toByteArray()).joinToString("") { "%02x".format(it) }
        File(filesDir, "novel_plugins/${plugin.packageName}.json").apply {
            parentFile!!.mkdirs()
            writeText(JSONObject().put("manifest", kotlinx.serialization.json.Json.encodeToString(LnReaderPlugin.serializer(), plugin)).put("code", code).put("sha256", hash).toString())
        }
        setContent {
            TankobunTheme(SettingsStore(this).themePreference()) {
                NovelSourceSettingsDialog(plugin.packageName, plugin.name, ::finish)
            }
        }
    }
    override fun onDestroy() { server?.close(); super.onDestroy() }
}
