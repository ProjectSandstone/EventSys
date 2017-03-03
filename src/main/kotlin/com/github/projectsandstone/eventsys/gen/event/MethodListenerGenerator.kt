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

import com.github.jonathanxd.codeapi.*
import com.github.jonathanxd.codeapi.base.Typed
import com.github.jonathanxd.codeapi.bytecode.VISIT_LINES
import com.github.jonathanxd.codeapi.bytecode.VisitLineType
import com.github.jonathanxd.codeapi.bytecode.extra.Dup
import com.github.jonathanxd.codeapi.bytecode.extra.Pop
import com.github.jonathanxd.codeapi.bytecode.gen.BytecodeGenerator
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.common.CodeParameter
import com.github.jonathanxd.codeapi.conversions.createInvocation
import com.github.jonathanxd.codeapi.conversions.createStaticInvocation
import com.github.jonathanxd.codeapi.factory.field
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.type.Generic
import com.github.jonathanxd.codeapi.util.Stack
import com.github.jonathanxd.codeapi.util.codeType
import com.github.projectsandstone.eventsys.Debug
import com.github.projectsandstone.eventsys.event.Event
import com.github.projectsandstone.eventsys.event.EventListener
import com.github.projectsandstone.eventsys.event.EventPriority
import com.github.projectsandstone.eventsys.event.ListenerSpec
import com.github.projectsandstone.eventsys.event.annotation.Listener
import com.github.projectsandstone.eventsys.event.property.GetterProperty
import com.github.projectsandstone.eventsys.event.property.Property
import com.github.projectsandstone.eventsys.event.property.PropertyHolder
import com.github.projectsandstone.eventsys.gen.GeneratedEventClass
import com.github.projectsandstone.eventsys.gen.save.ClassSaver
import com.github.projectsandstone.eventsys.util.toGeneric
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*

/**
 * Creates [EventListener] class that invokes a method (that are annotated with [Listener]) directly (without reflection).
 */
internal object MethodListenerGenerator {

    private val cache = mutableMapOf<Method, EventListener<Event>>()

    fun create(owner: Any, method: Method, instance: Any?, listenerSpec: ListenerSpec): EventListener<Event> {

        if(this.cache.containsKey(method)) {
            return this.cache[method]!!
        }

        val klass = this.createClass(owner, instance, method, listenerSpec)

        val isStatic = Modifier.isStatic(method.modifiers)

        return if (!isStatic) {
            try {
                klass.classLoader.loadClass(instance!!::class.java.canonicalName)
            } catch (e: ClassNotFoundException) {
                throw IllegalStateException("Cannot lookup for Plugin class: '${instance!!::class.java}' from class loader: '${klass.classLoader}'")
            }

            klass.getConstructor(instance::class.java).newInstance(instance)
        } else {
            klass.newInstance()
        }.let {
            this.cache[method] = it
            it
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun createClass(owner: Any, instance: Any?, method: Method, listenerSpec: ListenerSpec): Class<EventListener<Event>> {
        val baseCanonicalName = "${EventListener::class.java.`package`.name}.generated."
        val declaringName = method.declaringClass.canonicalName.replace('.', '_')

        val name = "${baseCanonicalName}_${declaringName}_${method.name}"

        val eventType = listenerSpec.eventType

        val codeClass = CodeAPI.aClassBuilder()
                .withModifiers(CodeModifier.PUBLIC)
                .withQualifiedName(name)
                .withImplementations(Generic.type(EventListener::class.java.codeType).of(eventType.toGeneric()))
                .withSuperClass(Types.OBJECT)
                .withBody(genBody(method, instance, listenerSpec))
                .build()

        val source = CodeAPI.sourceOfParts(codeClass)

        val generator = BytecodeGenerator()

        generator.options.set(VISIT_LINES, VisitLineType.FOLLOW_CODE_SOURCE)

        val bytecodeClass = generator.gen(source)[0]

        val bytes = bytecodeClass.bytecode

        val definedClass = EventGenClassLoader.defineClass(codeClass, bytes, lazy { bytecodeClass.disassembledCode }, (owner::class.java.classLoader as ClassLoader)) as GeneratedEventClass<EventListener<Event>>

        if (Debug.LISTENER_GEN_DEBUG) {
            ClassSaver.save("listenergen", definedClass)
        }

        return definedClass.javaClass
    }

    private const val eventVariableName: String = "event"
    private const val ownerVariableName: String = "pluginContainer"
    private const val instanceFieldName: String = "\$instance"

    private fun genBody(method: Method, instance: Any?, listenerSpec: ListenerSpec): CodeSource {
        val source = MutableCodeSource()

        val isStatic = Modifier.isStatic(method.modifiers)

        if (!isStatic) {
            val instanceType = instance!!::class.java.codeType

            source.add(field(EnumSet.of(CodeModifier.PRIVATE, CodeModifier.FINAL), instanceType, instanceFieldName))
            source.add(CodeAPI.constructorBuilder()
                    .withModifiers(CodeModifier.PUBLIC)
                    .withParameters(CodeParameter(instanceType, instanceFieldName))
                    .withBody(CodeAPI.sourceOfParts(
                            CodeAPI.setThisField(instanceType, instanceFieldName, CodeAPI.accessLocalVariable(instanceType, instanceFieldName))
                    ))
                    .build()
            )
        }

        source.addAll(genMethods(method, instance, listenerSpec))

        return source
    }

    private fun genMethods(method: Method, instance: Any?, listenerSpec: ListenerSpec): CodeSource {
        val source = MutableCodeSource()

        val isStatic = Modifier.isStatic(method.modifiers)
        val eventType = Event::class.java.codeType

        // This is hard to maintain, but, is funny :D
        fun genOnEventBody(): CodeSource {
            val body = MutableCodeSource()

            val parameters = listenerSpec.parameters

            val arguments = mutableListOf<CodePart>()

            val accessEventVar = CodeAPI.accessLocalVariable(eventType, eventVariableName)

            arguments.add(CodeAPI.cast(eventType, parameters[0].type.aClass.codeType, accessEventVar))

            parameters.forEachIndexed { i, param ->
                if (i > 0) {
                    val name = param.name
                    val typeInfo = param.type

                    val toAdd: CodePart

                    if (typeInfo.aClass == Property::class.java
                            && typeInfo.related.isNotEmpty()) {
                        toAdd = this.callGetPropertyDirectOn(accessEventVar, name, typeInfo.related[0].aClass, true, param.isNullable)
                    } else {
                        toAdd = this.callGetPropertyDirectOn(accessEventVar, name, typeInfo.aClass, false, param.isNullable)
                    }

                    arguments.add(toAdd)
                }
            }


            if (isStatic) {
                body.add(method.createStaticInvocation(arguments))
            } else {
                body.add(method.createInvocation(CodeAPI.accessThisField(instance!!::class.java.codeType, instanceFieldName), arguments))
            }

            return body
        }

        val onEvent = CodeAPI.methodBuilder()
                .withModifiers(CodeModifier.PUBLIC)
                .withBody(genOnEventBody())
                .withReturnType(Types.VOID)
                .withName("onEvent")
                .withParameters(
                        CodeAPI.parameter(eventType, eventVariableName), CodeAPI.parameter(Any::class.java, ownerVariableName)
                )
                .build()

        source.add(onEvent)

        val getPriorityMethod = CodeAPI.methodBuilder()
                .withModifiers(CodeModifier.PUBLIC)
                .withBody(CodeAPI.sourceOfParts(
                        CodeAPI.returnValue(EventPriority::class.java,
                                CodeAPI.accessStaticField(EventPriority::class.java, EventPriority::class.java, listenerSpec.priority.name)
                        )
                ))
                .withName("getPriority")
                .withReturnType(EventPriority::class.java.codeType)
                .build()

        source.add(getPriorityMethod)

        val getPhaseMethod = CodeAPI.methodBuilder()
                .withModifiers(CodeModifier.PUBLIC)
                .withBody(CodeAPI.sourceOfParts(
                        CodeAPI.returnValue(Types.INT, Literals.INT(listenerSpec.phase))
                ))
                .withName("getPhase")
                .withReturnType(Types.INT)
                .build()

        source.add(getPhaseMethod)

        val ignoreCancelledMethod = CodeAPI.methodBuilder()
                .withModifiers(CodeModifier.PUBLIC)
                .withBody(CodeAPI.sourceOfParts(
                        CodeAPI.returnValue(Types.BOOLEAN, Literals.BOOLEAN(listenerSpec.ignoreCancelled))
                ))
                .withName("getIgnoreCancelled")
                .withReturnType(Types.BOOLEAN)
                .build()

        source.add(ignoreCancelledMethod)

        return source
    }

    private fun callGetPropertyDirectOn(target: CodePart, name: String, type: Class<*>, propertyOnly: Boolean, isNullable: Boolean): CodePart {
        val getPropertyMethod = CodeAPI.invokeInterface(PropertyHolder::class.java, target,
                if (propertyOnly) "getProperty" else "getGetterProperty",
                CodeAPI.typeSpec(if (propertyOnly) Property::class.java else GetterProperty::class.java, Class::class.java, String::class.java),
                listOf(Literals.CLASS(type), Literals.STRING(name))
        )

        val elsePart = if (isNullable) Literals.NULL else CodeAPI.returnVoid()

        val getPropMethod = checkNull(getPropertyMethod, elsePart)

        if (propertyOnly)
            return getPropMethod

        return CodeAPI.ifStatement(CodeAPI.checkNotNull(Dup(getPropMethod, GetterProperty::class.codeType)),
                // Body
                CodeAPI.source(CodeAPI.invokeInterface(GetterProperty::class.java, Stack,
                        "getValue",
                        CodeAPI.typeSpec(Any::class.java),
                        emptyList())),
                // Else
                CodeAPI.source(
                        Pop,
                        elsePart
                ))

    }

    private fun checkNull(part: Typed, else_: CodePart) = CodeAPI.ifStatement(CodeAPI.checkNotNull(Dup(part)), CodeAPI.source(Stack), CodeAPI.source(Pop, else_))
}
