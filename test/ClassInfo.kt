package com.example

import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.full.*

class ClassInfo(clazz: Class<*>) {
    sealed class ClassInfoItems(val name: String, val type: String, val visibility: String?, val mutability: String) {
        override fun toString(): String = visibility?.toLowerCase()?.let { "$it " } ?: ""
        class PropertyInfo(name: String, type: String, visibility: String? = null, mutability: String) :
                ClassInfoItems(name, type, visibility, mutability) {
            override fun toString(): String = "${super.toString()}$mutability $name: $type"
        }

        class MethodInfo(name: String, returnType: String, visibility: String? = null, vararg val parameters: PropertyInfo) :
                ClassInfoItems(name, returnType, visibility, "fun") {
            override fun toString(): String = "${super.toString()}$mutability $name(${parameters.joinToString { "${it.name}: ${it.type}" }}): $type"
        }

        class ConstructorInfo(name: String, visibility: String? = null, vararg val parameters: PropertyInfo) :
                ClassInfoItems(name, name, visibility, "") {
            override fun toString(): String = "${super.toString()}$name(${parameters.joinToString { "${it.name}: ${it.type}" }})"
        }
    }

    private val toProps: (KCallable<*>) -> Array<ClassInfoItems.PropertyInfo> = { it.parameters.mapNotNullTryCatch(paramToProp).toTypedArray() }
    private val nameCon: (KCallable<*>) -> String = { "${it.extensionReceiverParameter?.type?.let { type -> "$type." } ?: ""}${it.name}" }
    private val propConvert: (KProperty<*>) -> ClassInfoItems.PropertyInfo = {
        ClassInfoItems.PropertyInfo(nameCon(it), it.returnType.toString(), it.visibility?.toString(), if (it is KMutableProperty<*>) "var" else "val")
    }
    private val paramToProp: (KParameter) -> ClassInfoItems.PropertyInfo? =
        { p -> ClassInfoItems.PropertyInfo(p.name!!, p.type.toString(), null, "") }
    val className: String = clazz.simpleName
    val superClasses: List<ClassInfo> = clazz.kotlin.superclasses.filter { it.simpleName != "Any" }.mapNotNullTryCatch { ClassInfo(it.java) }
    val constructors: List<ClassInfoItems.ConstructorInfo> = clazz.kotlin.constructors.map {
        ClassInfoItems.ConstructorInfo(it.name, it.visibility?.toString(), *toProps(it))
    }
    val subClasses: List<ClassInfo> = clazz.classes.map { ClassInfo(it) }
    val propertyList: List<ClassInfoItems.PropertyInfo> = clazz.kotlin.memberProperties.map(propConvert) +
            clazz.kotlin.memberExtensionProperties.map(propConvert) + clazz.kotlin.staticProperties.map(propConvert)
    val methodList: List<ClassInfoItems.MethodInfo> = clazz.kotlin.functions.map {
        ClassInfoItems.MethodInfo(nameCon(it), it.returnType.toString(), it.visibility?.toString(), *toProps(it))
    }
    val enumSet = clazz.enumConstants?.map { it } ?: emptyList()
    override fun toString(): String = className
    private fun <T, R : Any> List<T>.mapNotNullTryCatch(transform: (T) -> R?) = mapNotNull {
        try {
            transform(it)
        } catch (e: Exception) {
            null
        }
    }
}

@Suppress("TestFunctionName")
inline fun <reified T> ClassInfo() = ClassInfo(T::class.java)

fun <T : Comparable<T>> ClassInfo.getClassBox(sort: ((ClassInfo.ClassInfoItems) -> T?)? = null): String = run {
    val tag = "$className${if (superClasses.isNotEmpty()) superClasses.joinToString(prefix = ": ") { it.className } else ""}"
    val prop = "Properties"
    val method = "Methods"
    val inn = "Inner Classes"
    val con = "Constructors"
    val enum = "Enums"
    fun List<*>.length(): Int = maxBy { it.toString().length }?.toString()?.length ?: 0
    val size = listOf(
            tag.length,
            con.length, constructors.length(),
            enum.length, enumSet.length(),
            prop.length, propertyList.length(),
            method.length, methodList.length(),
            inn.length, subClasses.length()
    ).max()!!
    val section: (List<*>, String) -> String? = { list, header -> if (list.isNotEmpty()) "$header${"-".repeat(size - header.length)}" else null }
    listOfNotNull(
            section(constructors, con), *constructors.let { list -> sort?.let { list.sortedBy(it) } ?: list }.toTypedArray(),
            section(enumSet, enum), *enumSet.toTypedArray(),
            section(propertyList, prop), *propertyList.let { list -> sort?.let { list.sortedBy(it) } ?: list }.toTypedArray(),
            section(methodList, method), *methodList.let { list -> sort?.let { list.sortedBy(it) } ?: list }.toTypedArray(),
            section(subClasses, inn), *subClasses.map { it.className }.toTypedArray()
    ).frame(FrameType.BOX.copy(top = tag))
}

fun <T : Comparable<T>> ClassInfo.printClassInfoInBox(sort: ((ClassInfo.ClassInfoItems) -> T?)? = null) = Loged.f(
        listOf(
                getClassBox(sort),
                *subClasses.map { it.getClassBox(sort) }.union(getSubClasses(subClasses).map { it.getClassBox(sort) }).toTypedArray()
        ).joinToString("\n")
)

fun getSubClasses(sub: List<ClassInfo>): List<ClassInfo> = when {
    sub.isEmpty() -> emptyList()
    sub.size == 1 -> sub
    else -> sub.map { getSubClasses(it.subClasses) }.flatten()
}
