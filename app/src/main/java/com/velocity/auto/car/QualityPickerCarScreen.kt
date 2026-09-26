package com.velocity.auto.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import com.velocity.auto.R
import com.velocity.auto.youtube.extractor.QualityOption

/** A small picker pushed on top of the player - pick a quality, pop back, the player screen restarts at it. */
class QualityPickerCarScreen(
    carContext: CarContext,
    private val qualities: List<QualityOption>,
    private val onSelected: (Int?) -> Unit
) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val itemList = ItemList.Builder()
            .addItem(
                Row.Builder()
                    .setTitle(carContext.getString(R.string.quality_auto))
                    .setOnClickListener {
                        onSelected(null)
                        screenManager.pop()
                    }
                    .build()
            )
        qualities.forEach { quality ->
            itemList.addItem(
                Row.Builder()
                    .setTitle(quality.label)
                    .setOnClickListener {
                        onSelected(quality.heightPx)
                        screenManager.pop()
                    }
                    .build()
            )
        }

        return ListTemplate.Builder()
            .setTitle(carContext.getString(R.string.quality_picker_title))
            .setHeaderAction(Action.BACK)
            .setSingleList(itemList.build())
            .build()
    }
}
