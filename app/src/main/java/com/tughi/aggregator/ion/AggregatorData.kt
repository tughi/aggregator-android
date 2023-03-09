package com.tughi.aggregator.ion

import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.StructElement
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf

internal class AggregatorData private constructor(structElement: StructElement, validate: Boolean) : CustomElement(structElement, validate) {
    val version: Int
        get() = get(AggregatorData::version.name).longValue.toInt()
    val application: Application
        get() = get(AggregatorData::application.name).asStruct().let { if (it is Application) it else Application(it) }
    val updateSettings: UpdateSettings
        get() = get(AggregatorData::updateSettings.name).asStruct().let { if (it is UpdateSettings) it else UpdateSettings(it) }
    val counters: Counters
        get() = get(AggregatorData::counters.name).asStruct().let { if (it is Counters) it else Counters(it) }

    constructor(structElement: StructElement) : this(structElement, validate = true)

    constructor(
        version: Int,
        application: Application,
        updateSettings: UpdateSettings,
        counters: Counters,
    ) : this(
        ionStructOf(
            AggregatorData::version.name to ionInt(version.toLong()),
            AggregatorData::application.name to application,
            AggregatorData::updateSettings.name to updateSettings,
            AggregatorData::counters.name to counters,
            annotations = listOf(AggregatorData::class.simpleName!!),
        ),
        validate = false
    )

    override fun validate() {
        checkAnnotation(AggregatorData::class.simpleName!!)
        checkField(AggregatorData::version.name, ElementType.INT)
        application.validate()
        updateSettings.validate()
        counters.validate()
    }

    class Application private constructor(structElement: StructElement, validate: Boolean) : CustomElement(structElement, validate) {
        val packageName: String
            get() = get(Application::packageName.name).stringValue
        val versionCode: Int
            get() = get(Application::versionCode.name).longValue.toInt()
        val versionName: String
            get() = get(Application::versionName.name).stringValue

        constructor(structElement: StructElement) : this(structElement, validate = true)

        constructor(
            packageName: String,
            versionCode: Int,
            versionName: String,
        ) : this(
            ionStructOf(
                Application::packageName.name to ionString(packageName),
                Application::versionCode.name to ionInt(versionCode.toLong()),
                Application::versionName.name to ionString(versionName),
            ),
            validate = false,
        )

        override fun validate() {
            checkField(Application::packageName.name, ElementType.STRING)
            checkField(Application::versionCode.name, ElementType.INT)
            checkField(Application::versionName.name, ElementType.STRING)
        }
    }

    class UpdateSettings private constructor(structElement: StructElement, validate: Boolean) : CustomElement(structElement, validate) {
        val backgroundUpdates: Boolean
            get() = get(UpdateSettings::backgroundUpdates.name).booleanValue
        val defaultCleanupMode: String
            get() = get(UpdateSettings::defaultCleanupMode.name).stringValue
        val defaultUpdateMode: String
            get() = get(UpdateSettings::defaultUpdateMode.name).stringValue

        constructor(structElement: StructElement) : this(structElement, validate = true)

        constructor(
            backgroundUpdates: Boolean,
            defaultCleanupMode: String,
            defaultUpdateMode: String,
        ) : this(
            ionStructOf(
                UpdateSettings::backgroundUpdates.name to ionBool(backgroundUpdates),
                UpdateSettings::defaultCleanupMode.name to ionString(defaultCleanupMode),
                UpdateSettings::defaultUpdateMode.name to ionString(defaultUpdateMode),
            ),
            validate = false,
        )

        override fun validate() {
            checkField(UpdateSettings::backgroundUpdates.name, ElementType.BOOL)
            checkField(UpdateSettings::defaultCleanupMode.name, ElementType.STRING)
            checkField(UpdateSettings::defaultUpdateMode.name, ElementType.STRING)
        }
    }

    class Counters private constructor(structElement: StructElement, validate: Boolean) : CustomElement(structElement, validate) {
        val feeds: Int
            get() = get(Counters::feeds.name).longValue.toInt()
        val entries: Int
            get() = get(Counters::entries.name).longValue.toInt()
        val tags: Int
            get() = get(Counters::tags.name).longValue.toInt()
        val entryTagRules: Int
            get() = get(Counters::entryTagRules.name).longValue.toInt()
        val entryTags: Int
            get() = get(Counters::entryTags.name).longValue.toInt()
        val myFeedTags: Int
            get() = get(Counters::myFeedTags.name).longValue.toInt()

        val total: Int
            get() = feeds + entries + tags + entryTagRules + entryTags + myFeedTags

        constructor(structElement: StructElement) : this(structElement, validate = true)

        constructor(
            feeds: Int,
            entries: Int,
            tags: Int,
            entryTagRules: Int,
            entryTags: Int,
            myFeedTags: Int,
        ) : this(
            ionStructOf(
                Counters::feeds.name to ionInt(feeds.toLong()),
                Counters::entries.name to ionInt(entries.toLong()),
                Counters::tags.name to ionInt(tags.toLong()),
                Counters::entryTagRules.name to ionInt(entryTagRules.toLong()),
                Counters::entryTags.name to ionInt(entryTags.toLong()),
                Counters::myFeedTags.name to ionInt(myFeedTags.toLong()),
            ),
            validate = false,
        )

        override fun validate() {
            checkField(Counters::feeds.name, ElementType.INT)
            checkField(Counters::entries.name, ElementType.INT)
            checkField(Counters::tags.name, ElementType.INT)
            checkField(Counters::entryTagRules.name, ElementType.INT)
            checkField(Counters::entryTags.name, ElementType.INT)
            checkField(Counters::myFeedTags.name, ElementType.INT)
        }
    }
}
