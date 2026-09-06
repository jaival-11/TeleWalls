package me.jaival.telewalls.core.upload

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FolderScanner {
    private const val TAG = "FolderScanner"

    suspend fun scanFolderForImages(context: Context, treeUri: Uri): List<Uri> = withContext(Dispatchers.IO) {
        val imageUris = mutableListOf<Uri>()
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            if (rootDoc != null) {
                scanDocumentFile(rootDoc, imageUris)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning document tree: $treeUri", e)
        }
        imageUris
    }

    private fun scanDocumentFile(doc: DocumentFile, result: MutableList<Uri>) {
        try {
            if (doc.isDirectory) {
                val children = doc.listFiles()
                for (child in children) {
                    scanDocumentFile(child, result)
                }
            } else if (doc.isFile) {
                val mimeType = doc.type
                val name = doc.name?.lowercase() ?: ""
                val isImage = (mimeType != null && mimeType.startsWith("image/", ignoreCase = true)) ||
                        name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                        name.endsWith(".png") || name.endsWith(".webp") ||
                        name.endsWith(".heic") || name.endsWith(".gif") ||
                        name.endsWith(".bmp")
                if (isImage) {
                    result.add(doc.uri)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking document file ${doc.uri}", e)
        }
    }
}
