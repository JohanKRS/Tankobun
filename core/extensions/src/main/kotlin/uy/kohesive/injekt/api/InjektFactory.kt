package uy.kohesive.injekt.api

import java.lang.reflect.Type

interface InjektFactory {
    fun <R : Any> getInstance(type: Type): R

    fun <R : Any> getInstanceOrElse(type: Type, default: R): R =
        runCatching { getInstance<R>(type) }.getOrDefault(default)

    fun <R : Any> getInstanceOrElse(type: Type, default: () -> R): R =
        runCatching { getInstance<R>(type) }.getOrElse { default() }

    fun <R : Any> getInstanceOrNull(type: Type): R? =
        runCatching { getInstance<R>(type) }.getOrNull()

    fun <R : Any, K : Any> getKeyedInstance(type: Type, key: K): R = getInstance(type)

    fun <R : Any, K : Any> getKeyedInstanceOrElse(type: Type, key: K, default: R): R =
        runCatching { getKeyedInstance<R, K>(type, key) }.getOrDefault(default)

    fun <R : Any, K : Any> getKeyedInstanceOrElse(type: Type, key: K, default: () -> R): R =
        runCatching { getKeyedInstance<R, K>(type, key) }.getOrElse { default() }

    fun <R : Any, K : Any> getKeyedInstanceOrNull(type: Type, key: K): R? =
        runCatching { getKeyedInstance<R, K>(type, key) }.getOrNull()

    fun <R : Any> getLogger(expectedLoggerType: Type, byName: String): R = getInstance(expectedLoggerType)

    fun <R : Any, T : Any> getLogger(expectedLoggerType: Type, forClass: Class<T>): R = getInstance(expectedLoggerType)
}
