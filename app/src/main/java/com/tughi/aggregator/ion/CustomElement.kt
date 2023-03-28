package com.tughi.aggregator.ion

import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.location
import com.amazon.ionelement.api.locationToString

abstract class CustomElement internal constructor(structElement: StructElement, validate: Boolean) : StructElement by structElement {
    init {
        if (validate) {
            validate()
        }
    }

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
