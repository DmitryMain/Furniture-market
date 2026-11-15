package com.example.furniturefactory

import android.app.Application
import android.content.Context
import com.example.furniturefactory.data.local.AppPreferences
import com.example.furniturefactory.data.repository.CartRepository
import com.example.furniturefactory.data.repository.FurnitureRepository
import com.example.furniturefactory.data.repository.LocalCartRepository
import com.example.furniturefactory.data.repository.LocalFurnitureRepository
import com.example.furniturefactory.data.repository.LocalOrderRepository
import com.example.furniturefactory.data.repository.LocalUserRepository
import com.example.furniturefactory.data.repository.OrderRepository
import com.example.furniturefactory.data.repository.UserRepository

/** Глобальные настройки приложения — название меняется здесь и в res/values/strings.xml. */
object AppConfig {
    const val APP_NAME = "Furniture Factory"
    const val SUPPORT_PHONE = "+7 800 555-35-35"
    const val SUPPORT_EMAIL = "support@furniture-factory.example"
}

class FurnitureApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Простой контейнер зависимостей. Чтобы перейти на сервер завода, замените
 * Local*-реализации на сетевые — экраны и ViewModel менять не придётся.
 */
class AppContainer(context: Context) {
    private val preferences = AppPreferences(context)

    val furnitureRepository: FurnitureRepository = LocalFurnitureRepository()
    val cartRepository: CartRepository = LocalCartRepository(preferences, furnitureRepository)
    val orderRepository: OrderRepository = LocalOrderRepository(preferences)
    val userRepository: UserRepository = LocalUserRepository(preferences)
}
