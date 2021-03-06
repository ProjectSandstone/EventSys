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
package com.github.koresframework.eventsys.ap

import com.github.jonathanxd.iutils.io.DelegatePrintStream
import com.github.koresframework.eventsys.context.EnvironmentContext
import com.github.koresframework.eventsys.logging.Level
import com.github.koresframework.eventsys.logging.LoggerInterface
import com.github.koresframework.eventsys.logging.MessageType
import javax.annotation.processing.Messager
import javax.tools.Diagnostic

class CTLogger(val messager: Messager) : LoggerInterface {

    override fun log(message: String, messageType: MessageType, ctx: EnvironmentContext) {
        messager.printMessage(messageType.level.toKind(), message)
    }

    override fun log(message: String, messageType: MessageType, throwable: Throwable, ctx: EnvironmentContext) {
        messager.printMessage(messageType.level.toKind(), message)

        throwable.printStackTrace(DelegatePrintStream("UTF-8") {
            messager.printMessage(messageType.level.toKind(), it)
        })
    }

    override fun log(messages: List<String>, messageType: MessageType, ctx: EnvironmentContext) {
        messages.forEach {
            messager.printMessage(messageType.level.toKind(), it)
        }
    }

    override fun log(messages: List<String>, messageType: MessageType, throwable: Throwable, ctx: EnvironmentContext) {
        messages.forEach {
            messager.printMessage(messageType.level.toKind(), it)
        }

        throwable.printStackTrace(DelegatePrintStream("UTF-8") {
            messager.printMessage(messageType.level.toKind(), it)
        })
    }

    private fun Level.toKind() =
        when (this) {
            Level.TRACE -> Diagnostic.Kind.OTHER
            Level.DEBUG -> Diagnostic.Kind.OTHER
            Level.INFO -> Diagnostic.Kind.NOTE
            Level.WARN -> Diagnostic.Kind.WARNING
            Level.ERROR -> Diagnostic.Kind.ERROR
            Level.FATAL -> Diagnostic.Kind.ERROR
        }


}