package com.tughi.aggregator.activities.optionpicker

import android.os.Parcel
import android.os.Parcelable

data class Option(val value: String, val name: String, val description: String?) : Parcelable {

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(value)
        parcel.writeString(name)
        parcel.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Option> {
        override fun createFromParcel(parcel: Parcel): Option {
            return Option(
                    parcel.readString()!!,
                    parcel.readString()!!,
                    parcel.readString()
            )
        }

        override fun newArray(size: Int): Array<Option?> {
            return arrayOfNulls(size)
        }
    }

}
