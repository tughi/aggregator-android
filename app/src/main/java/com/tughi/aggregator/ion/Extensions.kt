package com.tughi.aggregator.ion

import com.amazon.ion.IonBlob
import com.amazon.ion.IonBool
import com.amazon.ion.IonInt
import com.amazon.ion.IonString
import com.amazon.ion.IonStruct
import com.amazon.ion.IonValue

inline fun IonValue.booleanValue() = (this as IonBool).booleanValue()

inline fun IonValue.bytesValue(): ByteArray = (this as IonBlob).bytes

inline fun IonValue.intValue() = (this as IonInt).intValue()

inline fun IonValue.longValue() = (this as IonInt).longValue()

inline fun IonValue.stringValue(): String = (this as IonString).stringValue()

inline fun IonValue.structValue() = this as IonStruct
