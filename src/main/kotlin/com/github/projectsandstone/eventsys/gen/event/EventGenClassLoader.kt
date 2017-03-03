/**
 *      EventImpl - Event implementation generator written on top of CodeAPI
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2017 ProjectSandstone <https://github.com/ProjectSandstone/EventImpl>
 *      Copyright (c) contributors
 *
 *
 *      Permission is hereby granted, free of charge, to any person obtaining a copy
 *      of this software and associated documentation files (the "Software"), to deal
 *      in the Software without restriction, including without limitation the rights
 *      to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *      copies of the Software, and to permit persons to whom the Software is
 *      furnished to do so, subject to the following conditions:
 *
 *      The above copyright notice and this permission notice shall be included in
 *      all copies or substantial portions of the Software.
 *
 *      THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *      IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *      FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *      AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *      LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *      OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *      THE SOFTWARE.
 */
package com.github.projectsandstone.eventsys.gen.event

import com.github.jonathanxd.codeapi.base.TypeDeclaration
import com.github.projectsandstone.eventsys.gen.GeneratedEventClass
import com.github.projectsandstone.eventsys.event.Event
import java.util.*

/**
 * [ClassLoader] of all event generated classes
 */
internal object EventGenClassLoader {

    private val loadedClasses_ = mutableListOf<GeneratedEventClass<*>>()
    val loadedClasses = Collections.unmodifiableList(loadedClasses_)

    fun defineClass(name: String, byteArray: ByteArray, disassembled: Lazy<String>): GeneratedEventClass<*> {
        val cl = Event::class.java.classLoader
        return this.defineClass(name, byteArray, disassembled, cl)
    }

    fun defineClass(name: String, byteArray: ByteArray, disassembled: Lazy<String>, classLoader: ClassLoader): GeneratedEventClass<*> {
        val definedClass = this.inject(classLoader, name, byteArray)

        val sandstoneClass = GeneratedEventClass(definedClass, byteArray, disassembled)

        this.loadedClasses_ += sandstoneClass

        return sandstoneClass
    }

    private fun inject(classLoader: ClassLoader, name: String, bytes: ByteArray): Class<*> {
        val method = ClassLoader::class.java.getDeclaredMethod("defineClass", String::class.java, ByteArray::class.java, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        method.isAccessible = true
        try {
            return method.invoke(classLoader, name, bytes, 0, bytes.size) as Class<*>
        } catch (t: Throwable) {
            throw IllegalArgumentException("Cannot inject provided class: $name.", t)
        }
    }

    fun defineClass(typeDeclaration: TypeDeclaration, byteArray: ByteArray, disassembled: Lazy<String>) =
            this.defineClass(typeDeclaration.canonicalName, byteArray, disassembled)

    fun defineClass(typeDeclaration: TypeDeclaration, byteArray: ByteArray, disassembled: Lazy<String>, classLoader: ClassLoader) =
            this.defineClass(typeDeclaration.canonicalName, byteArray, disassembled, classLoader)

}