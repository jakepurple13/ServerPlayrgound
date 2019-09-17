package com.example

import kotlin.reflect.KClass

fun randomName(): String = listOf(
    "Austin", "Bob", "Chuck", "Darren", "Eric", "Frank", "Gary", "Harry", "Jacob", "Kevin", "Logan",
    "Martin", "Norman", "Odin", "Peter", "Ryan", "Steven", "Tom", "Way", "Yosef",
    "Anna", "Bobbette", "Cherry", "Dana", "Erica", "Freja", "Gina", "Hana", "Jenna", "Kari", "Lynda",
    "Maya", "Nora", "Odina", "Patrica", "Rachel", "Sherry", "Tanya", "Whitney", "Yerenny",
    "Chris", "Daniel", "Jordan"
).random()

inline fun <reified T : Enum<T>> randomEnum() = enumValues<T>().random()

inline fun <reified T : Enum<T>> KClass<T>.random() = enumValues<T>().random()
