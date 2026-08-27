package dev.basefile.future.roborazzi

import android.content.Context
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.gif.GifDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.svg.SvgDecoder
import coil3.test.ColorImage
import coil3.test.FakeImageLoaderEngine
import coil3.video.VideoFrameDecoder
import java.io.File
import java.util.Base64
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class CoilLocalIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `local SVG and GIF decode and second SVG load hits memory cache`() = runTest {
        val svg = File(context.cacheDir, "fixture.svg").apply {
            writeText("""<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32"><rect width="32" height="32" fill="#005a9c"/></svg>""")
        }
        val gif = File(context.cacheDir, "fixture.gif").apply {
            writeBytes(Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw=="))
        }
        val loader = ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
                add(GifDecoder.Factory())
                add(VideoFrameDecoder.Factory())
                add(OkHttpNetworkFetcherFactory())
            }
            .build()
        val svgRequest = ImageRequest.Builder(context)
            .data(svg)
            .size(64, 64)
            .memoryCacheKey("local-svg")
            .build()
        assertTrue(loader.execute(svgRequest) is SuccessResult)
        val cached = loader.execute(svgRequest) as SuccessResult
        assertEquals(DataSource.MEMORY_CACHE, cached.dataSource)
        assertTrue(loader.execute(ImageRequest.Builder(context).data(gif).size(16, 16).build()) is SuccessResult)
        loader.shutdown()
    }

    @Test
    fun `network-shaped request stays fake and cancellation disposes work`() = runTest {
        val remote = "https://offline.invalid/fixture.png"
        val deterministicEngine = FakeImageLoaderEngine.Builder()
            .intercept(remote, ColorImage(Color.BLUE))
            .build()
        val deterministicLoader = ImageLoader.Builder(context)
            .components { add(deterministicEngine) }
            .build()
        assertTrue(
            deterministicLoader.execute(ImageRequest.Builder(context).data(remote).build()) is SuccessResult,
        )
        deterministicLoader.shutdown()

        val delayedEngine = FakeImageLoaderEngine.Builder()
            .default { chain ->
                delay(60_000)
                error("Cancellation did not interrupt ${chain.request.data}")
            }
            .build()
        val cancellableLoader = ImageLoader.Builder(context)
            .components { add(delayedEngine) }
            .build()
        val disposable = cancellableLoader.enqueue(
            ImageRequest.Builder(context).data("delayed-local-request").build(),
        )
        disposable.dispose()
        runCatching { disposable.job.await() }
        assertTrue(disposable.isDisposed)
        assertTrue(disposable.job.isCancelled)
        cancellableLoader.shutdown()
    }
}
