package com.gps.locationtracker.data.repository

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.gps.locationtracker.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class GoogleDriveRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun uploadPdfToDrive(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.failure(Exception("No Google account signed in"))

            // Get the auth token using GoogleAuthUtil
            // Scope for Drive: https://www.googleapis.com/auth/drive.file
            val scope = "oauth2:https://www.googleapis.com/auth/drive.file"
            
            val token = try {
                GoogleAuthUtil.getToken(context, account.account!!, scope)
            } catch (e: Exception) {
                Timber.e("Error getting token: ${e.message}")
                return@withContext Result.failure(Exception("Could not get auth token. Please sign in again."))
            }

            // Step 1: Create Metadata
            val metadata = JSONObject().apply {
                put("name", file.name)
                put("mimeType", "application/pdf")
            }

            val metadataPart = metadata.toString()
                .toRequestBody("application/json; charset=UTF-8".toMediaTypeOrNull())

            val filePart = file.asRequestBody("application/pdf".toMediaTypeOrNull())

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addPart(metadataPart)
                .addPart(filePart)
                .build()

            val request = Request.Builder()
                .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                .header("Authorization", "Bearer $token")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                val fileId = json.getString("id")
                Result.success(fileId)
            } else {
                Timber.e("Upload failed: $responseBody")
                Result.failure(Exception("Drive upload failed: ${response.message}"))
            }
        } catch (e: Exception) {
            Timber.e("Drive Upload Error: ${e.message}")
            Result.failure(e)
        }
    }
}
