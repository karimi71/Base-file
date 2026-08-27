package dev.basefile.future.android

import android.content.Context
import android.os.Build
import coil3.ImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import coil3.video.VideoFrameDecoder

object CoilFixtureLoader {
    fun create(context: Context): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(SvgDecoder.Factory())
            if (Build.VERSION.SDK_INT >= 28) {
                add(AnimatedImageDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
            add(VideoFrameDecoder.Factory())
            // Resolution and construction are tested; no offline test performs I/O.
            add(OkHttpNetworkFetcherFactory())
        }
        .build()
}
