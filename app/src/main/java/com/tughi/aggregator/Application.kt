package com.tughi.aggregator

class Application : android.app.Application() {

    override fun onCreate() {
        super.onCreate()

        instance = this
    }

    companion object {
        lateinit var instance: Application
            private set
    }

}
