package com.velocity.auto.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import com.velocity.auto.R

/** The in-car equivalent of [com.velocity.auto.home.HomeActivity] - same three built-ins, car-template UI. */
class HomeCarScreen(carContext: CarContext) : Screen(carContext) {

    override fun onGetTemplate(): Template {
        val itemList = ItemList.Builder()
            .addItem(
                gridItem(
                    title = carContext.getString(R.string.tile_youtube),
                    iconRes = R.drawable.ic_play_circle
                ) {
                    screenManager.push(YouTubeSearchCarScreen(carContext))
                }
            )
            .addItem(
                gridItem(
                    title = carContext.getString(R.string.tile_netflix),
                    iconRes = R.drawable.ic_movie
                ) {
                    screenManager.push(
                        WebMirrorCarScreen(carContext, "https://www.netflix.com/browse")
                    )
                }
            )
            .addItem(
                gridItem(
                    title = carContext.getString(R.string.tile_stremio),
                    iconRes = R.drawable.ic_globe
                ) {
                    screenManager.push(
                        WebMirrorCarScreen(carContext, "https://web.stremio.com/")
                    )
                }
            )
            .addItem(
                gridItem(
                    title = carContext.getString(R.string.tile_disney_plus),
                    iconRes = R.drawable.ic_movie
                ) {
                    screenManager.push(WebMirrorCarScreen(carContext, "https://www.disneyplus.com"))
                }
            )
            .addItem(
                gridItem(
                    title = carContext.getString(R.string.tile_prime_video),
                    iconRes = R.drawable.ic_movie
                ) {
                    screenManager.push(WebMirrorCarScreen(carContext, "https://www.primevideo.com"))
                }
            )
            .addItem(
                gridItem(
                    title = carContext.getString(R.string.tile_max),
                    iconRes = R.drawable.ic_movie
                ) {
                    screenManager.push(WebMirrorCarScreen(carContext, "https://www.max.com"))
                }
            )
            .addItem(
                gridItem(
                    title = carContext.getString(R.string.tile_addons),
                    iconRes = R.drawable.ic_extension
                ) {
                    screenManager.push(AddonHomeCarScreen(carContext))
                }
            )
            .build()

        return GridTemplate.Builder()
            .setTitle(carContext.getString(R.string.app_name))
            .setHeaderAction(Action.APP_ICON)
            .setSingleList(itemList)
            .build()
    }

    private fun gridItem(title: String, iconRes: Int, onClick: () -> Unit): GridItem =
        GridItem.Builder()
            .setTitle(title)
            .setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, iconRes)).build())
            .setOnClickListener(onClick)
            .build()
}
