package dev.basefile.future.android

import android.content.Context
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteCoroutineWorker

class RemoteFixtureWorker(
    appContext: Context,
    parameters: WorkerParameters,
) : RemoteCoroutineWorker(appContext, parameters) {
    override suspend fun doRemoteWork(): Result = Result.success(
        Data.Builder().putString("process-proof", "remote-coroutine-worker").build(),
    )
}
