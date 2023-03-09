package com.tughi.aggregator.ion

import com.amazon.ion.IonWriter
import com.amazon.ionelement.api.AnyElement
import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.StructField
import com.amazon.ionelement.api.location
import com.amazon.ionelement.api.locationToString

abstract class CustomElement internal constructor(private val structElement: StructElement, validate: Boolean) : StructElement {
    init {
        if (validate) {
            validate()
        }
    }

    override val annotations: List<String>
        get() = structElement.annotations

    override val fields: Collection<StructField>
        get() = structElement.fields

    override val isNull: Boolean
        get() = structElement.isNull

    override val metas: MetaContainer
        get() = structElement.metas

    override val size: Int
        get() = structElement.size

    override val type: ElementType
        get() = structElement.type

    override val values: Collection<AnyElement>
        get() = structElement.values

    override fun asAnyElement() = structElement.asAnyElement()

    override fun containsField(fieldName: String) = structElement.containsField(fieldName)

    override fun copy(annotations: List<String>, metas: MetaContainer) = structElement.copy(annotations, metas)

    override fun get(fieldName: String) = structElement.get(fieldName)

    override fun getAll(fieldName: String) = structElement.getAll(fieldName)

    override fun getOptional(fieldName: String) = structElement.getOptional(fieldName)

    override fun toString() = structElement.toString()

    override fun withAnnotations(vararg additionalAnnotations: String) = structElement.withAnnotations(*additionalAnnotations)

    override fun withAnnotations(additionalAnnotations: Iterable<String>) = structElement.withAnnotations(additionalAnnotations)

    override fun withMeta(key: String, value: Any) = structElement.withMeta(key, value)

    override fun withMetas(additionalMetas: MetaContainer) = structElement.withMetas(additionalMetas)

    override fun withoutAnnotations() = structElement.withoutAnnotations()

    override fun withoutMetas() = structElement.withoutMetas()

    override fun writeTo(writer: IonWriter) = structElement.writeTo(writer)

    abstract fun validate()

    protected fun checkAnnotation(annotation: String) {
        if (!annotations.contains(annotation)) {
            throw ValidationException(this, "Struct without required annotation: '$annotation'")
        }
    }

    protected fun checkField(fieldName: String, elementType: ElementType) {
        val element = get(fieldName)
        if (element.type != elementType) {
            throw ValidationException(element, "Struct field '$fieldName' has wrong type")
        }
    }

    protected fun checkOptionalField(fieldName: String, elementType: ElementType) {
        if (containsField(fieldName)) {
            checkField(fieldName, elementType)
        }
    }
}

class ValidationException internal constructor(blamedElement: IonElement, description: String) : Error(" ${locationToString(blamedElement.metas.location)}: $description")
