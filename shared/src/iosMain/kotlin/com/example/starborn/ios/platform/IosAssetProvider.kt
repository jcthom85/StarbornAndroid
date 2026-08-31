package com.example.starborn.ios.platform

import com.example.starborn.core.platform.AssetProvider
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.stringWithContentsOfFile
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * iOS implementation of AssetProvider that reads resources from NSBundle.mainBundle.
 */
class IosAssetProvider : AssetProvider {

    @OptIn(ExperimentalForeignApi::class)
    override fun open(path: String): InputStream? {
        if (path.isBlank()) return null
        val bundlePath = bundlePathFor(path) ?: return null
        val data = NSData.dataWithContentsOfFile(bundlePath) ?: return null
        val bytes = ByteArray(data.length.toInt())
        return ByteArrayInputStream(bytes)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun readText(path: String): String? {
        if (path.isBlank()) return null
        val bundlePath = bundlePathFor(path) ?: return null
        return NSString.stringWithContentsOfFile(bundlePath, NSUTF8StringEncoding, null) as? String
    }

    override fun exists(path: String): Boolean {
        if (path.isBlank()) return false
        return bundlePathFor(path) != null
    }

    override fun list(dir: String): List<String> {
        val bundlePath = NSBundle.mainBundle.resourcePath ?: return emptyList()
        return emptyList()
    }

    private fun bundlePathFor(path: String): String? {
        val name = path.substringBeforeLast('.')
        val ext = path.substringAfterLast('.', "")
        return NSBundle.mainBundle.pathForResource(name, ofType = if (ext.isEmpty()) null else ext)
    }
}
