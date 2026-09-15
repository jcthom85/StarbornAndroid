package com.example.starborn

import android.content.Context
import android.content.ContextWrapper
import java.io.File

/** Keep independently-lived AppServices persistence jobs out of each other's save files. */
internal class IsolatedProgressionContext(base: Context) : ContextWrapper(base) {
    private val root = File(base.cacheDir, "progression-test-${System.nanoTime()}").apply { mkdirs() }
    override fun getApplicationContext(): Context = this
    override fun getFilesDir(): File = File(root, "files").apply { mkdirs() }
    override fun getNoBackupFilesDir(): File = File(root, "no-backup").apply { mkdirs() }
    override fun getCacheDir(): File = File(root, "cache").apply { mkdirs() }
}
