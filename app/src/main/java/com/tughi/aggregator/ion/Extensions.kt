package com.tughi.aggregator.ion

import com.amazon.ion.IonReader
import com.amazon.ion.IonType
import com.amazon.ion.IonWriter
import com.tughi.aggregator.data.Entries
import com.tughi.aggregator.data.EntryTagRules
import com.tughi.aggregator.data.EntryTags
import com.tughi.aggregator.data.Feeds
import com.tughi.aggregator.data.MyFeedTags
import com.tughi.aggregator.data.Tags

fun IonReader.readAggregatorData(): AggregatorData {
    next()
    if (!typeAnnotations.contains("AggregatorData")) {
        throw IllegalStateException("Not an AggregatorData file")
    }

    val data = AggregatorData()

    stepIn()
    while (next() != null) {
        when (fieldName) {
            AggregatorData::entriesCount.name -> data.entriesCount = intValue()
            AggregatorData::entryTagRulesCount.name -> data.entryTagRulesCount = intValue()
            AggregatorData::entryTagsCount.name -> data.entryTagsCount = intValue()
            AggregatorData::feedsCount.name -> data.feedsCount = intValue()
            AggregatorData::myFeedTagsCount.name -> data.myFeedTagsCount = intValue()
            AggregatorData::tagsCount.name -> data.tagsCount = intValue()
            AggregatorData::version.name -> data.version = intValue()
        }
    }
    stepOut()

    return data
}

fun IonWriter.writeAggregatorData(data: AggregatorData) {
    setTypeAnnotations(AggregatorData::class.simpleName)
    stepIn(IonType.STRUCT)

    setFieldName(AggregatorData::version.name)
    writeInt(data.version.toLong())

    setFieldName(AggregatorData::feedsCount.name)
    writeInt(data.feedsCount.toLong())

    setFieldName(AggregatorData::entriesCount.name)
    writeInt(data.entriesCount.toLong())

    setFieldName(AggregatorData::tagsCount.name)
    writeInt(data.tagsCount.toLong())

    setFieldName(AggregatorData::entryTagRulesCount.name)
    writeInt(data.entryTagRulesCount.toLong())

    setFieldName(AggregatorData::entryTagsCount.name)
    writeInt(data.entryTagsCount.toLong())

    setFieldName(AggregatorData::myFeedTagsCount.name)
    writeInt(data.myFeedTagsCount.toLong())

    stepOut()
}

fun IonWriter.writeTypedList(listType: String, listSize: Int, writeListItems: () -> Unit) {
    if (listSize > 0) {
        setTypeAnnotations(listType)
        stepIn(IonType.LIST)

        writeListItems()

        stepOut()
    }
}

fun IonReader.readEntry(): Array<Pair<Entries.TableColumn, Any?>> {
    val columns = mutableListOf<Pair<Entries.TableColumn, Any?>>()

    stepIn()
    while (next() != null) {
        columns.add(
            when (fieldName) {
                Entry::author.name -> Entries.AUTHOR to stringValue()
                Entry::content.name -> Entries.CONTENT to stringValue()
                Entry::feedId.name -> Entries.FEED_ID to longValue()
                Entry::id.name -> Entries.ID to longValue()
                Entry::insertTime.name -> Entries.INSERT_TIME to longValue()
                Entry::link.name -> Entries.LINK to stringValue()
                Entry::pinnedTime.name -> Entries.PINNED_TIME to longValue()
                Entry::publishTime.name -> Entries.PUBLISH_TIME to longValue()
                Entry::readTime.name -> Entries.READ_TIME to longValue()
                Entry::starredTime.name -> Entries.STARRED_TIME to longValue()
                Entry::title.name -> Entries.TITLE to stringValue()
                Entry::uid.name -> Entries.UID to stringValue()
                Entry::updateTime.name -> Entries.UPDATE_TIME to longValue()
                else -> throw IllegalStateException("Unsupported field: %s".format(fieldName))
            }
        )
    }
    stepOut()

    return columns.toTypedArray()
}

fun IonWriter.writeEntry(entry: Entry) {
    stepIn(IonType.STRUCT)

    setFieldName(entry::id.name)
    writeInt(entry.id)

    setFieldName(entry::uid.name)
    writeString(entry.uid)

    setFieldName(entry::feedId.name)
    writeInt(entry.feedId)

    if (entry.title != null) {
        setFieldName(entry::title.name)
        writeString(entry.title)
    }

    if (entry.link != null) {
        setFieldName(entry::link.name)
        writeString(entry.link)
    }

    if (entry.content != null) {
        setFieldName(entry::content.name)
        writeString(entry.content)
    }

    if (entry.author != null) {
        setFieldName(entry::author.name)
        writeString(entry.author)
    }

    if (entry.publishTime != null) {
        setFieldName(entry::publishTime.name)
        writeInt(entry.publishTime)
    }

    setFieldName(entry::insertTime.name)
    writeInt(entry.insertTime)

    setFieldName(entry::updateTime.name)
    writeInt(entry.updateTime)

    setFieldName(entry::readTime.name)
    writeInt(entry.readTime)

    setFieldName(entry::pinnedTime.name)
    writeInt(entry.pinnedTime)

    setFieldName(entry::starredTime.name)
    writeInt(entry.starredTime)

    stepOut()
}

fun IonReader.readEntryTag(): Array<Pair<EntryTags.TableColumn, Any?>> {
    val columns = mutableListOf<Pair<EntryTags.TableColumn, Any?>>()

    stepIn()
    while (next() != null) {
        columns.add(
            when (fieldName) {
                EntryTag::entryId.name -> EntryTags.ENTRY_ID to longValue()
                EntryTag::entryTagRuleId.name -> EntryTags.ENTRY_TAG_RULE_ID to longValue()
                EntryTag::tagId.name -> EntryTags.TAG_ID to longValue()
                EntryTag::tagTime.name -> EntryTags.TAG_TIME to longValue()
                else -> throw IllegalStateException("Unsupported field: %s".format(fieldName))
            }
        )
    }
    stepOut()

    return columns.toTypedArray()
}

fun IonWriter.writeEntryTag(entryTag: EntryTag) {
    stepIn(IonType.STRUCT)

    setFieldName(entryTag::entryId.name)
    writeInt(entryTag.entryId)

    setFieldName(entryTag::tagId.name)
    writeInt(entryTag.tagId)

    setFieldName(entryTag::tagTime.name)
    writeInt(entryTag.tagTime)

    if (entryTag.entryTagRuleId != null) {
        setFieldName(entryTag::entryTagRuleId.name)
        writeInt(entryTag.entryTagRuleId)
    }

    stepOut()
}

fun IonReader.readEntryTagRule(): Array<Pair<EntryTagRules.TableColumn, Any?>> {
    val columns = mutableListOf<Pair<EntryTagRules.TableColumn, Any?>>()

    stepIn()
    while (next() != null) {
        columns.add(
            when (fieldName) {
                EntryTagRule::condition.name -> EntryTagRules.CONDITION to stringValue()
                EntryTagRule::feedId.name -> EntryTagRules.FEED_ID to longValue()
                EntryTagRule::id.name -> EntryTagRules.ID to longValue()
                EntryTagRule::tagId.name -> EntryTagRules.TAG_ID to longValue()
                else -> throw IllegalStateException("Unsupported field: %s".format(fieldName))
            }
        )
    }
    stepOut()

    return columns.toTypedArray()
}

fun IonWriter.writeEntryTagRule(entryTagRule: EntryTagRule) {
    stepIn(IonType.STRUCT)

    setFieldName(entryTagRule::id.name)
    writeInt(entryTagRule.id)

    if (entryTagRule.feedId != null) {
        setFieldName(entryTagRule::feedId.name)
        writeInt(entryTagRule.feedId)
    }

    setFieldName(entryTagRule::tagId.name)
    writeInt(entryTagRule.tagId)

    setFieldName(entryTagRule::condition.name)
    writeString(entryTagRule.condition)

    stepOut()
}

fun IonReader.readFeed(): Array<Pair<Feeds.TableColumn, Any?>> {
    val columns = mutableListOf<Pair<Feeds.TableColumn, Any?>>()

    stepIn()
    while (next() != null) {
        columns.add(
            when (fieldName) {
                Feed::cleanupMode.name -> Feeds.CLEANUP_MODE to stringValue()
                Feed::customTitle.name -> Feeds.CUSTOM_TITLE to stringValue()
                Feed::httpEtag.name -> Feeds.HTTP_ETAG to stringValue()
                Feed::httpLastModified.name -> Feeds.HTTP_LAST_MODIFIED to stringValue()
                Feed::id.name -> Feeds.ID to longValue()
                Feed::language.name -> Feeds.LANGUAGE to stringValue()
                Feed::faviconUrl.name -> Feeds.FAVICON_URL to stringValue()
                Feed::faviconContent.name -> Feeds.FAVICON_CONTENT to newBytes()
                Feed::lastUpdateError.name -> Feeds.LAST_UPDATE_ERROR to stringValue()
                Feed::lastUpdateTime.name -> Feeds.LAST_UPDATE_TIME to longValue()
                Feed::link.name -> Feeds.LINK to stringValue()
                Feed::nextUpdateRetry.name -> Feeds.NEXT_UPDATE_RETRY to intValue()
                Feed::nextUpdateTime.name -> Feeds.NEXT_UPDATE_TIME to longValue()
                Feed::title.name -> Feeds.TITLE to stringValue()
                Feed::updateMode.name -> Feeds.UPDATE_MODE to stringValue()
                Feed::url.name -> Feeds.URL to stringValue()
                else -> throw IllegalStateException("Unsupported field: %s".format(fieldName))
            }
        )
    }
    stepOut()

    return columns.toTypedArray()
}

fun IonWriter.writeFeed(feed: Feed) {
    stepIn(IonType.STRUCT)

    setFieldName(feed::id.name)
    writeInt(feed.id)

    setFieldName(feed::url.name)
    writeString(feed.url)

    setFieldName(feed::title.name)
    writeString(feed.title)

    if (feed.customTitle != null) {
        setFieldName(feed::customTitle.name)
        writeString(feed.customTitle)
    }

    if (feed.link != null) {
        setFieldName(feed::link.name)
        writeString(feed.link)
    }

    if (feed.language != null) {
        setFieldName(feed::language.name)
        writeString(feed.language)
    }

    if (feed.faviconUrl != null) {
        setFieldName(feed::faviconUrl.name)
        writeString(feed.faviconUrl)
    }

    if (feed.faviconContent != null) {
        setFieldName(feed::faviconContent.name)
        writeBlob(feed.faviconContent)
    }

    setFieldName(feed::cleanupMode.name)
    writeString(feed.cleanupMode)

    setFieldName(feed::updateMode.name)
    writeString(feed.updateMode)

    setFieldName(feed::lastUpdateTime.name)
    writeInt(feed.lastUpdateTime)

    if (feed.lastUpdateError != null) {
        setFieldName(feed::lastUpdateError.name)
        writeString(feed.lastUpdateError)
    }

    setFieldName(feed::nextUpdateTime.name)
    writeInt(feed.nextUpdateTime)

    setFieldName(feed::nextUpdateRetry.name)
    writeInt(feed.nextUpdateRetry.toLong())

    if (feed.httpEtag != null) {
        setFieldName(feed::httpEtag.name)
        writeString(feed.httpEtag)
    }

    if (feed.httpLastModified != null) {
        setFieldName(feed::httpLastModified.name)
        writeString(feed.httpLastModified)
    }

    stepOut()
}

fun IonReader.readMyFeedTag(): Array<Pair<MyFeedTags.TableColumn, Any?>> {
    val columns = mutableListOf<Pair<MyFeedTags.TableColumn, Any?>>()

    stepIn()
    while (next() != null) {
        columns.add(
            when (fieldName) {
                MyFeedTag::tagId.name -> MyFeedTags.TAG_ID to longValue()
                MyFeedTag::type.name -> MyFeedTags.TYPE to intValue()
                else -> throw IllegalStateException("Unsupported field: %s".format(fieldName))
            }
        )
    }
    stepOut()

    return columns.toTypedArray()
}

fun IonWriter.writeMyFeedTag(myFeedTag: MyFeedTag) {
    stepIn(IonType.STRUCT)

    setFieldName(myFeedTag::tagId.name)
    writeInt(myFeedTag.tagId)

    setFieldName(myFeedTag::type.name)
    writeInt(myFeedTag.type.toLong())

    stepOut()
}

fun IonReader.readTag(): Array<Pair<Tags.TableColumn, Any?>> {
    val columns = mutableListOf<Pair<Tags.TableColumn, Any?>>()

    stepIn()
    while (next() != null) {
        columns.add(
            when (fieldName) {
                Tag::editable.name -> Tags.EDITABLE to if (booleanValue()) 1 else 0
                Tag::id.name -> Tags.ID to longValue()
                Tag::name.name -> Tags.NAME to stringValue()
                else -> throw IllegalStateException("Unsupported field: %s".format(fieldName))
            }
        )
    }
    stepOut()

    return columns.toTypedArray()
}

fun IonWriter.writeTag(tag: Tag) {
    stepIn(IonType.STRUCT)

    setFieldName(tag::id.name)
    writeInt(tag.id)

    setFieldName(tag::name.name)
    writeString(tag.name)

    setFieldName(tag::editable.name)
    writeBool(tag.editable)

    stepOut()
}
