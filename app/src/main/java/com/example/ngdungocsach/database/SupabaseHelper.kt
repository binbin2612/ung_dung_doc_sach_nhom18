package com.example.ngdungocsach.database

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.ngdungocsach.SupabaseConfig
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SupabaseHelper(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)

    fun uploadFile(uri: Uri, folderName: String, onResult: (String?) -> Unit) {
        if (uri.toString().startsWith("http")) {
            onResult(uri.toString())
            return
        }

        scope.launch {
            try {
                val fileName = "${System.currentTimeMillis()}_${uri.lastPathSegment ?: "file"}"
                val fullPath = "$folderName/$fileName"
                
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }

                if (bytes == null) {
                    onResult(null)
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    SupabaseConfig.client.storage.from(SupabaseConfig.BUCKET_NAME).upload(
                        path = fullPath,
                        data = bytes,
                        upsert = true
                    )
                }

                val publicUrl = SupabaseConfig.getPublicUrl(fullPath)
                Log.d("SupabaseHelper", "Upload success: $publicUrl")
                onResult(publicUrl)

            } catch (e: Exception) {
                Log.e("SupabaseHelper", "Upload failed: ${e.message}", e)
                onResult(null)
            }
        }
    }
}
