package com.tughi.aggregator.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.tughi.aggregator.App
import com.tughi.aggregator.utilities.DATABASE_NAME
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

object Storage {

    private val sqlite: SupportSQLiteOpenHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(App.instance)
                    .name(DATABASE_NAME)
                    .callback(object : SupportSQLiteOpenHelper.Callback(16) {
                        override fun onCreate(db: SupportSQLiteDatabase?) {
                            createDatabase(db ?: throw IllegalStateException("Null database"))
                        }

                        override fun onUpgrade(db: SupportSQLiteDatabase?, oldVersion: Int, newVersion: Int) {
                            upgradeDatabase(db ?: throw IllegalStateException("Null database"))
                        }
                    })
                    .build()
    )

    private fun createDatabase(database: SupportSQLiteDatabase) {
        throw UnsupportedOperationException()
    }

    private fun upgradeDatabase(database: SupportSQLiteDatabase) {
        throw UnsupportedOperationException()
    }

    val readableDatabase: SupportSQLiteDatabase
        get() = sqlite.readableDatabase

    val writableDatabase: SupportSQLiteDatabase
        get() = sqlite.writableDatabase

    private val observers = mutableSetOf<Observer>()

    private fun addObserver(observer: Observer) {
        if (observer.table != "entries" && observer.table != "feeds") {
            throw IllegalArgumentException("Unknown table: ${observer.table}")
        }

        synchronized(this) {
            observers.add(WeakObserver(observer))
        }
    }

    fun invalidateLiveData(table: String, id: Any? = null) {
        synchronized(this) {
            if (observers.size > 0) {
                val vanishedObservers = mutableListOf<Observer>()

                for (observer in observers) {
                    try {
                        if (observer.table == table && (observer.id == null || observer.id == id)) {
                            observer.onInvalidated()
                        }
                    } catch (vanished: WeakObserver.VanishedException) {
                        vanishedObservers.add(observer)
                    }
                }

                for (observer in vanishedObservers) {
                    observers.remove(observer)
                }
            }
        }
    }

    fun <T> createLiveData(table: String, id: Any? = null, loadData: () -> T): LiveData<T> = object : MutableLiveData<T>() {
        private val observer = object : Observer(table, id) {
            override fun onInvalidated() {
                update()
            }
        }

        init {
            addObserver(observer)
        }

        override fun onActive() {
            if (value == null) {
                update()
            }
        }

        private fun update() {
            GlobalScope.launch {
                val data = loadData()
                postValue(data)
            }
        }
    }

    private abstract class Observer(val table: String, val id: Any? = null) {

        abstract fun onInvalidated()

    }

    private class WeakObserver(observer: Observer) : Observer(observer.table, observer.id) {

        private val observerReference = WeakReference(observer)

        override fun onInvalidated() {
            val observer = observerReference.get()
            if (observer != null) {
                observer.onInvalidated()
            } else {
                throw VanishedException()
            }
        }

        class VanishedException : Exception()

    }

}
