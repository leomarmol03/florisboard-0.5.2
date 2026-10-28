/*
 * Copyright (C) 2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.patrickgold.florisboard.ime.theme

import android.content.Context
import dev.patrickgold.florisboard.lib.devtools.flogError
import org.florisboard.lib.kotlin.io.subFile
import org.florisboard.lib.snygg.value.SnyggAssetResolver
import java.io.File
import java.net.URI

class FlorisAssetResolver(val context: Context, val themeInfo: ThemeManager.ThemeInfo) : SnyggAssetResolver {
    override fun resolveAbsolutePath(uri: String) = runCatching {
        val uri = URI.create(uri)
        when (uri.scheme) {
            "flex" -> resolveThemeAsset(uri)
            "res" -> resolveResourceAsset(uri)
            else -> error("Unsupported URI scheme '${uri.scheme}'")
        }
    }.onFailure { exception ->
        flogError { "FlorisAssetResolver failed to resolve URI '$uri'\n  error: ${exception.message}\n  with:  $themeInfo" }
    }

    /**
     * Resolves a URI which points to a file within the currently loaded theme directory.
     */
    private fun resolveThemeAsset(uri: URI): String {
        require(uri.authority.isNullOrEmpty())
        val baseDir = checkNotNull(themeInfo.loadedDir) { "Loaded directory was null" }
        val basePath = baseDir.canonicalPath
        val canonicalFile = baseDir.subFile(uri.path).canonicalFile
        val canonicalPath = canonicalFile.path
        check(canonicalPath.startsWith(basePath)) {
            "Calculated path '$canonicalPath' does not start with base path '$basePath'"
        }
        check(canonicalFile.exists()) {
            "Calculated path '$canonicalPath' does not exist"
        }
        check(canonicalFile.isFile()) {
            "Calculated path '$canonicalPath' is not a file"
        }
        return canonicalPath
    }

    /**
     * Resolves a URI which points to a resource of the app, e.g. `res:/font/google_sans`.
     *
     * As the snygg theme compiler expects an absolute path of a file, the resource is copied into the cache
     * directory of the app. The copy is only created once and is reused afterwards.
     */
    private fun resolveResourceAsset(uri: URI): String {
        require(uri.authority.isNullOrEmpty())
        val resourcePath = uri.path.removePrefix("/")
        check(resourcePath.isNotBlank()) { "The resource path must not be empty" }
        val (resourceType, resourceName) = resourcePath.split("/", limit = 2).let { parts ->
            if (parts.size == 2) parts[0] to parts[1] else "raw" to parts[0]
        }
        val resourceId = context.resources.getIdentifier(resourceName, resourceType, context.packageName)
        check(resourceId != 0) { "Unable to find the resource '$resourcePath'" }

        val cacheDir = File(context.cacheDir, "snygg-assets").apply { mkdirs() }
        val cacheFile = File(cacheDir, resourcePath.replace('/', '_'))
        if (!cacheFile.exists()) {
            val temporaryFile = File(cacheDir, "${cacheFile.name}.tmp")
            context.resources.openRawResource(resourceId).use { input ->
                temporaryFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (!temporaryFile.renameTo(cacheFile)) {
                temporaryFile.copyTo(cacheFile, overwrite = true)
                temporaryFile.delete()
            }
        }
        return cacheFile.absolutePath
    }
}
