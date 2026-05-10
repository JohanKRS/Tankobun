package uy.kohesive.injekt

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.AppInfo
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import uy.kohesive.injekt.api.FullTypeReference
import uy.kohesive.injekt.api.InjektScope
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

object Injekt : InjektScope {
    override fun getInstance(type: Type): Any = TankobunInjektRegistry.getInstance(type)

    fun <T : Any> get(type: Class<T>): T = TankobunInjektRegistry.getInstance(type) as T

    fun <T : Any> get(forType: FullTypeReference<T>): T = TankobunInjektRegistry.getInstance(forType.type) as T
}

fun getInjekt(): InjektScope = Injekt

inline fun <reified T : Any> InjektScope.get(): T = getInstance(T::class.java) as T

inline fun <reified T : Any> Injekt.get(): T = get(T::class.java)

fun <T : Any> InjektScope.get(forType: FullTypeReference<T>): T = getInstance(forType.type) as T

inline fun <reified T : Any> injectLazy(): Lazy<T> = lazy { Injekt.get<T>() }

inline fun <reified T : Any> InjektScope.injectLazy(): Lazy<T> = lazy { get<T>() }

inline fun <reified T : Any> injectValue(): T = Injekt.get()

object TankobunInjektRegistry {
    @Volatile
    private var application: Application? = null
    @Volatile
    private var sharedNetworkHelper: NetworkHelper? = null
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    fun registerApplication(application: Application) {
        this.application = application
        NetworkHelper.configure(application.applicationContext)
        sharedNetworkHelper = NetworkHelper()
        ensureUserAgent()
    }

    fun getInstance(type: Type): Any {
        val app = application
            ?: throw IllegalStateException("Tankobun application has not been registered for extension injection")
        return when (rawClassName(type)) {
            Application::class.java.name -> app
            Context::class.java.name -> app
            Json::class.java.name -> json
            SharedPreferences::class.java.name -> sharedPreferences(app)
            NetworkHelper::class.java.name -> networkHelper(app)
            OkHttpClient::class.java.name -> networkHelper(app).client
            AppInfo::class.java.name -> AppInfo
            else -> app
        }
    }

    fun applicationOrNull(): Application? = application

    fun json(): Json = json

    fun sharedPreferences(): SharedPreferences? = application?.let(::sharedPreferences)

    fun networkHelper(): NetworkHelper = application?.let(::networkHelper) ?: NetworkHelper()

    private fun networkHelper(app: Application): NetworkHelper {
        NetworkHelper.configure(app.applicationContext)
        return sharedNetworkHelper ?: NetworkHelper().also { sharedNetworkHelper = it }
    }

    private fun sharedPreferences(app: Application): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(app)

    private fun rawClassName(type: Type): String =
        when (type) {
            is Class<*> -> type.name
            is ParameterizedType -> (type.rawType as? Class<*>)?.name ?: type.typeName
            else -> type.typeName
        }

    private fun ensureUserAgent() {
        val current = System.getProperty("http.agent")
        if (current.isNullOrBlank()) {
            System.setProperty("http.agent", "Tankobun Android")
        }
    }
}
