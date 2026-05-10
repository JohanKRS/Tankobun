package uy.kohesive.injekt.api

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

abstract class FullTypeReference<T> {
    val type: Type by lazy {
        (javaClass.genericSuperclass as? ParameterizedType)
            ?.actualTypeArguments
            ?.firstOrNull()
            ?: Any::class.java
    }
}

inline fun <reified T> fullType(): FullTypeReference<T> = object : FullTypeReference<T>() {}

inline fun <reified T> typeRef(): FullTypeReference<T> = fullType()
