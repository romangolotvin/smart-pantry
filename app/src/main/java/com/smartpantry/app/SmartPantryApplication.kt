package com.smartpantry.app

import android.app.Application
import com.smartpantry.app.data.PantryRepository
import com.smartpantry.app.data.ProductLookupService
import com.smartpantry.app.data.db.AppDatabase
import com.smartpantry.app.data.remote.ChestnyZnakClient
import com.smartpantry.app.update.UpdateService

class SmartPantryApplication : Application() {
    lateinit var pantryRepository: PantryRepository
        private set

    lateinit var updateService: UpdateService
        private set

    lateinit var productLookupService: ProductLookupService
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.get(this)
        pantryRepository = PantryRepository(db.pantryDao())
        updateService = UpdateService(this)
        productLookupService = ProductLookupService(
            chestnyZnakClient = ChestnyZnakClient()
        )
    }
}
