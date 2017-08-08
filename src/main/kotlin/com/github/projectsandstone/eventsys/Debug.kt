/*
 *      EventSys - Event implementation generator written on top of CodeAPI
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
package com.github.projectsandstone.eventsys

import java.nio.file.Paths

object Debug {
    private const val SAVE_PATH = "eventsys.debug.dir"
    private const val DEBUG_PROPERTY = "eventsys.debug"
    private const val FACTORY_GEN_PROPERTY = "eventsys.debug.factorygen"
    private const val EVENT_GEN_PROPERTY = "eventsys.debug.eventgen"
    private const val LISTENER_GEN_PROPERTY = "eventsys.debug.listenergen"

    val SAVE_PATH_DEBUG = getSaveProperty()
    val EVENT_GEN_DEBUG = getDebugProperty(EVENT_GEN_PROPERTY)
    val FACTORY_GEN_DEBUG = getDebugProperty(FACTORY_GEN_PROPERTY)
    val LISTENER_GEN_DEBUG = getDebugProperty(LISTENER_GEN_PROPERTY)

    private fun getDebugProperty(name: String) = System.getProperties()[name]?.equals("true") ?: System.getProperties()[DEBUG_PROPERTY]?.equals("true") ?: false
    private fun getSaveProperty() = Paths.get(System.getProperties()[SAVE_PATH]?.toString() ?: ".")!!

}