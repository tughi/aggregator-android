package com.tughi.aggregator.activities.optionpicker

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.StringRes

data class Option(val value: String, @StringRes val name: Int, @StringRes val description: Int?) : Parcelable {

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(value)
        parcel.writeInt(name)
        parcel.writeInt(description ?: 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Option> {
        override fun createFromParcel(parcel: Parcel): Option {
            return Option(
                    parcel.readString()!!,
                    parcel.readInt(),
                    with(parcel.readInt()) {
                        if (this == 0) null else this
                    }
            )
        }

        override fun newArray(size: Int): Array<Option?> {
            return arrayOfNulls(size)
        }
    }

}
