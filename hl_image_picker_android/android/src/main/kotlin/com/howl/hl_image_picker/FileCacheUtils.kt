// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
/*
 * Copyright (C) 2007-2008 OpenIntents.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file was modified by the Flutter authors from the following original file:
 * https://raw.githubusercontent.com/iPaulPro/aFileChooser/master/aFileChooser/src/com/ipaulpro/afilechooser/utils/FileUtils.java
 */
package com.howl.hl_image_picker

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import io.flutter.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

internal class FileCacheUtils {
    /**
     * Copies the file from the given content URI to a temporary directory, retaining the original
     * file name if possible.
     *
     *
     * Each file is placed in its own directory to avoid conflicts according to the following
     * scheme: {cacheDir}/{randomUuid}/{fileName}
     *
     *
     * File extension is changed to match MIME type of the file, if known. Otherwise, the extension
     * is left unchanged.
     *
     *
     * If the original file name is unknown, a predefined "image_picker" filename is used and the
     * file extension is deduced from the mime type (with fallback to ".jpg" in case of failure).
     */
    fun getPathFromUri(context: Context, uri: Uri): String? {
            context.getContentResolver().openInputStream(uri).use { inputStream ->
                val uuid: String = UUID.randomUUID().toString()
                val targetDirectory = File(context.getCacheDir(), uuid)
                targetDirectory.mkdir()
                // TODO(SynSzakala) according to the docs, `deleteOnExit` does not work reliably on Android; we should preferably
                //  just clear the picked files after the app startup.
                targetDirectory.deleteOnExit()
                var fileName =
                    getImageName(context, uri)
                var extension =
                    getImageExtension(
                        context,
                        uri
                    )
                if (fileName == null) {
                    Log.w("FileUtils", "Cannot get file name for $uri")
                    if (extension == null) extension = ".jpg"
                    fileName = "hl_image_picker$extension"
                } else if (extension != null) {
                    fileName =
                        getBaseName(fileName) + extension
                }
                val file = File(targetDirectory, fileName)
                    FileOutputStream(file).use { outputStream ->
                        copy(
                            inputStream!!,
                            outputStream
                        )
                        return file.getPath()
                    }
            }


        return null
    }

    companion object {
        /** @return extension of image with dot, or null if it's empty.
         */
        private fun getImageExtension(context: Context, uriImage: Uri): String? {
            val extension: String?
            extension = try {
                if (uriImage.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
                    val mime: MimeTypeMap = MimeTypeMap.getSingleton()
                    mime.getExtensionFromMimeType(context.getContentResolver().getType(uriImage))
                } else {
                    MimeTypeMap.getFileExtensionFromUrl(
                        Uri.fromFile(File(uriImage.getPath())).toString()
                    )
                }
            } catch (e: Exception) {
                return null
            }
            return if (extension == null || extension.isEmpty()) {
                null
            } else ".$extension"
        }

        /** @return name of the image provided by ContentResolver; this may be null.
         */
        private fun getImageName(context: Context, uriImage: Uri): String? {
            queryImageName(context, uriImage).use { cursor ->
                return if (cursor == null || !cursor.moveToFirst() || cursor.getColumnCount() < 1) null else cursor.getString(
                    0
                )
            }
        }

        private fun queryImageName(context: Context, uriImage: Uri): Cursor? {
            return context
                .getContentResolver()
                .query(
                    uriImage,
                    arrayOf<String>(MediaStore.MediaColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )
        }

        @kotlin.Throws(IOException::class)
        private fun copy(`in`: InputStream, out: OutputStream) {
            val buffer = ByteArray(4 * 1024)
            var bytesRead: Int
            while (`in`.read(buffer).also { bytesRead = it } != -1) {
                out.write(buffer, 0, bytesRead)
            }
            out.flush()
        }

        private fun getBaseName(fileName: String): String {
            val lastDotIndex: Int = fileName.lastIndexOf('.')
            return if (lastDotIndex < 0) {
                fileName
            } else fileName.substring(0, lastDotIndex)
            // Basename is everything before the last '.'.
        }
    }
}