package uy.kohesive.injekt.api

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

@Suppress("UNCHECKED_CAST")
fun Type.erasedType(): Class<Any> =
    when (this) {
        is Class<*> -> this as Class<Any>
        is ParameterizedType -> rawType.erasedType()
        is GenericArrayType -> {
            val elementType = genericComponentType.erasedType()
            java.lang.reflect.Array.newInstance(elementType, 0).javaClass
        }
        is TypeVariable<*> -> bounds.firstOrNull()?.erasedType()
            ?: throw IllegalStateException("Type variable has no bounds")
        is WildcardType -> upperBounds.firstOrNull()?.erasedType()
            ?: throw IllegalStateException("Wildcard type has no upper bounds")
        else -> throw IllegalStateException("Unsupported type reference: $this")
    }

interface TypeReference<T> {
    val type: Type
}

abstract class FullTypeReference<T> protected constructor() : TypeReference<T> {
    override val type: Type = javaClass.genericSuperclass.let { superClass ->
        if (superClass is Class<*>) {
            throw IllegalArgumentException("Internal error: TypeReference constructed without actual type information")
        }
        (superClass as ParameterizedType).actualTypeArguments[0]
    }
}

inline fun <reified T : Any> fullType(): FullTypeReference<T> = object : FullTypeReference<T>() {}

inline fun <reified T : Any> typeRef(): FullTypeReference<T> = fullType()
