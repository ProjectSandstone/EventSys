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

import com.github.jonathanxd.codeapi.CodeAPI
import com.github.jonathanxd.codeapi.CodePart
import com.github.jonathanxd.codeapi.MutableCodeSource
import com.github.jonathanxd.codeapi.Types
import com.github.jonathanxd.codeapi.annotation.Default
import com.github.jonathanxd.codeapi.builder.ClassDeclarationBuilder
import com.github.jonathanxd.codeapi.bytecode.VISIT_LINES
import com.github.jonathanxd.codeapi.bytecode.VisitLineType
import com.github.jonathanxd.codeapi.bytecode.gen.BytecodeGenerator
import com.github.jonathanxd.codeapi.common.CodeModifier
import com.github.jonathanxd.codeapi.common.CodeParameter
import com.github.jonathanxd.codeapi.common.InvokeType
import com.github.jonathanxd.codeapi.common.TypeSpec
import com.github.jonathanxd.codeapi.conversions.parameterNames
import com.github.jonathanxd.codeapi.conversions.toCodeArgument
import com.github.jonathanxd.codeapi.conversions.toMethodDeclaration
import com.github.jonathanxd.codeapi.literal.Literals
import com.github.jonathanxd.codeapi.util.codeType
import com.github.jonathanxd.iutils.exception.RethrowException
import com.github.jonathanxd.iutils.type.TypeInfo
import com.github.jonathanxd.iutils.type.TypeUtil
import com.github.projectsandstone.eventsys.Debug
import com.github.projectsandstone.eventsys.event.Cancellable
import com.github.projectsandstone.eventsys.event.Event
import com.github.projectsandstone.eventsys.event.annotation.Extension
import com.github.projectsandstone.eventsys.event.annotation.Mutable
import com.github.projectsandstone.eventsys.event.annotation.Name
import com.github.projectsandstone.eventsys.gen.GeneratedEventClass
import com.github.projectsandstone.eventsys.gen.save.ClassSaver
import com.github.projectsandstone.eventsys.reflect.getImplementation
import com.github.projectsandstone.eventsys.reflect.parameterNames
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.kotlinFunction

/**
 * This class generates an implementation of an event factory, this method will create the event class
 * and direct-call the constructor.
 *
 * Additional properties that are mutable must be annotated with [Mutable] annotation.
 *
 * Extensions are provided via [Extension] annotation in the factory method.
 *
 */
internal object EventFactoryClassGenerator {

    private val cached = mutableMapOf<Class<*>, Any>()

    /**
     * Create [factoryClass] instance invoking generated event classes constructor.
     */
    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> create(eventGenerator: EventGenerator, factoryClass: Class<T>): T {

        if (this.cached.containsKey(factoryClass))
            return this.cached[factoryClass]!! as T

        val superClass = factoryClass.superclass

        if (!factoryClass.isInterface)
            throw IllegalArgumentException("Factory class must be an interface.")

        if (superClass != null && factoryClass != Any::class.java || factoryClass.interfaces.isNotEmpty())
            throw IllegalArgumentException("Factory class must not extend any class.")

        val body = MutableCodeSource()

        val declaration = ClassDeclarationBuilder.builder()
                .withModifiers(CodeModifier.PUBLIC)
                .withQualifiedName("${factoryClass.canonicalName}\$Impl")
                .withImplementations(factoryClass.codeType)
                .withSuperClass(Types.OBJECT)
                .withBody(body)
                .build()

        factoryClass.declaredMethods.forEach { factoryMethod ->
            if (!factoryMethod.isDefault) {

                val kFunc = factoryMethod.kotlinFunction
                val cl = factoryMethod.declaringClass.kotlin

                val impl = kFunc?.let { getImplementation(cl, it) }

                if (kFunc != null && impl != null) {
                    val base = kFunc
                    val delegateClass = impl.first
                    val delegate = impl.second

                    val parameters = base.parameterNames.mapIndexed { i, it ->
                        CodeParameter(delegate.parameters[i + 1].type.codeType, it)
                    }

                    val arguments = mutableListOf<CodePart>(CodeAPI.accessThis()) + parameters.map { it.toCodeArgument() }

                    val invoke = CodeAPI.invoke(
                            InvokeType.INVOKE_STATIC,
                            delegateClass.codeType,
                            CodeAPI.accessStatic(),
                            delegate.name,
                            TypeSpec(delegate.returnType.codeType, delegate.parameters.map { it.type.codeType }),
                            arguments
                    ).let {
                        if (kFunc.returnType.jvmErasure.java == Void.TYPE)
                            it
                        else
                            CodeAPI.returnValue(kFunc.returnType.jvmErasure.codeType, it)
                    }

                    val methodDeclaration = factoryMethod.toMethodDeclaration()
                    val methodBody = methodDeclaration.body as MutableCodeSource
                    methodBody.add(invoke)

                    body.add(methodDeclaration)
                } else {

                    val eventType = factoryMethod.returnType
                    val ktNames = factoryMethod.parameterNames

                    if (!Event::class.java.isAssignableFrom(eventType))
                        throw IllegalArgumentException("Failed to generate implementation of method '$factoryMethod': event factory methods must return a type assignable to 'Event'.")

                    val parameterNames = factoryMethod.parameters.mapIndexed { i, it ->
                        if (it.isAnnotationPresent(Name::class.java))
                            it.getDeclaredAnnotation(Name::class.java).value
                        else
                            ktNames[i]
                    }

                    val properties = EventClassGenerator.getProperties(eventType)
                    val additionalProperties = mutableListOf<PropertyInfo>()

                    factoryMethod.parameters.forEachIndexed { i, parameter ->
                        val find = properties.any { it.propertyName == parameterNames[i] && it.type == parameter.type }

                        if (!find) {
                            val name = parameterNames[i]

                            val getterName = "get${name.capitalize()}"
                            val setterName = if (parameter.isAnnotationPresent(Mutable::class.java)) "set${name.capitalize()}" else null


                            additionalProperties += PropertyInfo(
                                    propertyName = name,
                                    type = parameter.type,
                                    getterName = getterName,
                                    setterName = setterName
                            )
                        }
                    }

                    val eventTypeInfo = TypeUtil.toReference(factoryMethod.genericReturnType) as TypeInfo<Event>

                    if (!Event::class.java.isAssignableFrom(eventTypeInfo.aClass))
                        throw IllegalStateException("Factory method '$factoryMethod' present in factory class '${factoryClass.canonicalName}' must returns a class that extends 'Event' class (currentClass: ${eventTypeInfo.aClass.canonicalName}).")

                    val extensions = factoryMethod.getDeclaredAnnotationsByType(Extension::class.java).map {
                        val implement = it.implement.java.let { if (it == Default::class.java) null else it }
                        val extension = it.extensionMethodsClass.java.let { if (it == Default::class.java) null else it }
                        ExtensionSpecification(implement, extension)
                    }

                    val implClass = eventGenerator.createEventClass(eventTypeInfo, additionalProperties, extensions)

                    val methodDeclaration = factoryMethod.toMethodDeclaration { index, parameter ->
                        parameterNames[index]
                    }

                    val methodBody = methodDeclaration.body as MutableCodeSource

                    val ctr = implClass.declaredConstructors[0]
                    val names = ctr.parameterNames

                    val arguments = ctr.parameters.mapIndexed map@{ index, it ->
                        val name = it.getDeclaredAnnotation(Name::class.java)?.value
                                ?: names[index] // Should we remove it?

                        if(!methodDeclaration.parameters.any { codeParameter -> codeParameter.name == it.name && codeParameter.type.canonicalName == it.type.canonicalName })
                            throw IllegalStateException("Cannot find property '[name: $name, type: ${it.type.canonicalName}]' in factory method '$factoryMethod'. Please provide a parameter with this name, use '-parameters' javac option or annotate parameters with '@${Name::class.java.canonicalName}' annotation.",
                                    IllegalStateException("Found properties: ${methodDeclaration.parameters.map { "${it.type.canonicalName} ${it.name}" }}. Required: ${ctr.parameters.contentToString()}."))

                        if (name == "cancelled"
                                && it.type == java.lang.Boolean.TYPE
                                && Cancellable::class.java.isAssignableFrom(eventType))
                            return@map Literals.FALSE

                        return@map CodeAPI.accessLocalVariable(it.type.codeType, name)
                    }

                    methodBody.add(CodeAPI.returnValue(eventType, CodeAPI.invokeConstructor(implClass.codeType, CodeAPI.constructorTypeSpec(*ctr.parameterTypes), arguments)))

                    body.add(methodDeclaration)
                }
            }

        }

        val generator = BytecodeGenerator()

        generator.options.set(VISIT_LINES, VisitLineType.FOLLOW_CODE_SOURCE)

        val bytecodeClass = generator.gen(declaration)[0]

        val bytes = bytecodeClass.bytecode
        val disassembled = lazy { bytecodeClass.disassembledCode }

        @Suppress("UNCHECKED_CAST")
        val generatedEventClass = EventGenClassLoader.defineClass(declaration, bytes, disassembled) as GeneratedEventClass<T>

        if (Debug.FACTORY_GEN_DEBUG) {
            ClassSaver.save("factorygen", generatedEventClass)
        }

        return generatedEventClass.javaClass.let {
            this.cached.put(factoryClass, it)
            it.getConstructor().newInstance()
        }
    }

}