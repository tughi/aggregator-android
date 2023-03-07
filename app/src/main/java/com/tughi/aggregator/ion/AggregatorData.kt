package com.tughi.aggregator.ion

import com.amazon.ion.IonStruct
import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import com.amazon.ion.IonWriter
import com.amazon.ion.util.AbstractValueVisitor

class AggregatorData(
    val version: Int,
    val application: Application,
    val updateSettings: UpdateSettings,
    val counters: Counters,
) {
    class Application(
        val packageName: String,
        val versionCode: Int,
        val versionName: String,
    )

    class UpdateSettings(
        val backgroundUpdates: Boolean,
        val defaultCleanupMode: String,
        val defaultUpdateMode: String,
    )

    class Counters(
        val feeds: Int,
        val entries: Int,
        val tags: Int,
        val entryTagRules: Int,
        val entryTags: Int,
        val myFeedTags: Int,
    ) {
        val total: Int
            get() = feeds + entries + tags + entryTagRules + entryTags + myFeedTags
    }

    fun writeTo(ionWriter: IonWriter, ionSystem: IonSystem) {
        ionSystem.newEmptyStruct().apply {
            setTypeAnnotations(AggregatorData::class.simpleName)
            add(AggregatorData::version.name, ionSystem.newInt(version))
            add(AggregatorData::application.name, ionSystem.newEmptyStruct().apply {
                add(Application::packageName.name, ionSystem.newString(application.packageName))
                add(Application::versionCode.name, ionSystem.newInt(application.versionCode))
                add(Application::versionName.name, ionSystem.newString(application.versionName))
            })
            add(AggregatorData::updateSettings.name, ionSystem.newEmptyStruct().apply {
                add(UpdateSettings::backgroundUpdates.name, ionSystem.newBool(updateSettings.backgroundUpdates))
                add(UpdateSettings::defaultCleanupMode.name, ionSystem.newString(updateSettings.defaultCleanupMode))
                add(UpdateSettings::defaultUpdateMode.name, ionSystem.newString(updateSettings.defaultUpdateMode))
            })
            add(AggregatorData::counters.name, ionSystem.newEmptyStruct().apply {
                add(Counters::feeds.name, ionSystem.newInt(counters.feeds))
                add(Counters::entries.name, ionSystem.newInt(counters.entries))
                add(Counters::tags.name, ionSystem.newInt(counters.tags))
                add(Counters::entryTagRules.name, ionSystem.newInt(counters.entryTagRules))
                add(Counters::entryTags.name, ionSystem.newInt(counters.entryTags))
                add(Counters::myFeedTags.name, ionSystem.newInt(counters.myFeedTags))
            })
        }.writeTo(ionWriter)
    }
}

private class AggregatorDataVisitor : AbstractValueVisitor() {
    lateinit var aggregatorData: AggregatorData

    override fun defaultVisit(ionValue: IonValue) {
        throw IllegalStateException("Not an AggregatorData value: $ionValue")
    }

    override fun visit(ionValue: IonStruct) {
        if (!ionValue.hasTypeAnnotation(AggregatorData::class.simpleName)) {
            super.visit(ionValue)
        }

        aggregatorData = AggregatorData(
            version = ionValue.get(AggregatorData::version.name).intValue(),
            application = ionValue.get(AggregatorData::application.name).structValue().let {
                AggregatorData.Application(
                    packageName = it.get(AggregatorData.Application::packageName.name).stringValue(),
                    versionCode = it.get(AggregatorData.Application::versionCode.name).intValue(),
                    versionName = it.get(AggregatorData.Application::versionName.name).stringValue(),
                )
            },
            updateSettings = ionValue.get(AggregatorData::updateSettings.name).structValue().let {
                AggregatorData.UpdateSettings(
                    backgroundUpdates = it.get(AggregatorData.UpdateSettings::backgroundUpdates.name).booleanValue(),
                    defaultCleanupMode = it.get(AggregatorData.UpdateSettings::defaultCleanupMode.name).stringValue(),
                    defaultUpdateMode = it.get(AggregatorData.UpdateSettings::defaultUpdateMode.name).stringValue(),
                )
            },
            counters = ionValue.get(AggregatorData::counters.name).structValue().let {
                AggregatorData.Counters(
                    feeds = it.get(AggregatorData.Counters::feeds.name).intValue(),
                    entries = it.get(AggregatorData.Counters::entries.name).intValue(),
                    tags = it.get(AggregatorData.Counters::tags.name).intValue(),
                    entryTagRules = it.get(AggregatorData.Counters::entryTagRules.name).intValue(),
                    entryTags = it.get(AggregatorData.Counters::entryTags.name).intValue(),
                    myFeedTags = it.get(AggregatorData.Counters::myFeedTags.name).intValue(),
                )
            },
        )
    }
}

fun Iterator<IonValue>.expectAggregatorData(): AggregatorData {
    val valueVisitor = AggregatorDataVisitor()

    next().accept(valueVisitor)

    return valueVisitor.aggregatorData
}
