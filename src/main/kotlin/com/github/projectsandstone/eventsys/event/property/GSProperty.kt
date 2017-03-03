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
package com.github.projectsandstone.eventsys.event.property

import com.github.projectsandstone.eventsys.event.property.Property
import com.github.projectsandstone.eventsys.event.property.SetterProperty
import com.github.projectsandstone.eventsys.event.property.GetterProperty
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * A Property that have getter and setter methods.
 */
interface GSProperty<R> : GetterProperty<R>, SetterProperty<R> {

    /**
     * Default implementation.
     */
    class Impl<R>(override val type: Class<R>, val getter: Supplier<R>, val setter: Consumer<R>) : Property<R>, GSProperty<R> {
        override fun getValue(): R = this.getter.get()
        override fun setValue(value: R) = this.setter.accept(value)
    }

}