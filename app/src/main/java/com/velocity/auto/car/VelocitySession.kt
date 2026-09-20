package com.velocity.auto.car

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class VelocitySession : Session() {
    override fun onCreateScreen(intent: Intent): Screen = HomeCarScreen(carContext)
}
