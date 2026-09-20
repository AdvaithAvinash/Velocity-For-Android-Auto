package com.velocity.auto

import android.app.Application
import com.velocity.auto.youtube.extractor.VelocityDownloader
import org.schabi.newpipe.extractor.NewPipe

class VelocityApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // One-time init for the NewPipeExtractor engine that powers the
        // native YouTube search + playback screens.
        NewPipe.init(VelocityDownloader.instance)
    }
}
