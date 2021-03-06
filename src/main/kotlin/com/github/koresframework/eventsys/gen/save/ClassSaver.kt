/*
 *      EventSys - Event implementation generator written on top of Kores
 *
 *         The MIT License (MIT)
 *
 *      Copyright (c) 2021 ProjectSandstone <https://github.com/ProjectSandstone/EventSys>
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
package com.github.koresframework.eventsys.gen.save

import com.github.koresframework.eventsys.Debug
import com.github.koresframework.eventsys.gen.GeneratedEventClass
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Class saving utility
 */
internal object ClassSaver {

    /**
     * Save a class
     */
    fun save(directory: Path, generatedEventClass: GeneratedEventClass<*>) {

        val className: String = generatedEventClass.javaClass.canonicalName
        val classBytes: ByteArray = generatedEventClass.bytes
        val source: String = generatedEventClass.disassembled.value

        val sourceBytes = source.toByteArray(Charset.forName("UTF-8"))

        val saveClass = "${className.replace('.', '/')}.class"
        val saveJava = "${className.replace('.', '/')}.disassembled"

        val resolvedClass = directory.resolve(saveClass)
        val resolvedJava = directory.resolve(saveJava)

        try {
            Files.deleteIfExists(resolvedClass)
            Files.deleteIfExists(resolvedJava)
        } catch (ignored: Exception) {
        }

        Files.createDirectories(resolvedClass.parent)
        Files.createDirectories(resolvedJava.parent)

        Files.write(resolvedClass, classBytes, StandardOpenOption.CREATE)
        Files.write(resolvedJava, sourceBytes, StandardOpenOption.CREATE)
    }
}