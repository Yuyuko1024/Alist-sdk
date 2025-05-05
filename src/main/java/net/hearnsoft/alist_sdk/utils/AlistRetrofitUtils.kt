package net.hearnsoft.alist_sdk.utils

import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*

class AlistRetrofitUtils {
    companion object {
        private var retrofit: Retrofit? = null

        private fun getRetrofitInstance(baseUrl: String): Retrofit {
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofit!!
        }

        fun <T> createService(baseUrl: String, serviceClass: Class<T>): T {
            return getRetrofitInstance(baseUrl).create(serviceClass)
        }
    }
}

interface AlistService {
    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: Map<String, String>): AlistResponse<TokenData>

    @POST("api/fs/list")
    suspend fun listFiles(
        @Body request: Map<String, String>,
        @Header("Authorization") token: String
    ): AlistResponse<FileListData>

    @POST("api/fs/get")
    suspend fun getFileInfo(
        @Body request: Map<String, String>,
        @Header("Authorization") token: String
    ): AlistResponse<FileData>

    // 添加匿名访问的API方法
    @POST("api/fs/list")
    suspend fun listFilesAnonymous(@Body request: Map<String, String>): AlistResponse<FileListData>

    @POST("api/fs/get")
    suspend fun getFileInfoAnonymous(@Body request: Map<String, String>): AlistResponse<FileData>
}

data class AlistResponse<T>(
    val code: Int,
    val message: String,
    val data: T
)

data class TokenData(
    val token: String
)

data class FileListData(
    val content: List<FileItem>
)

data class FileData(
    val name: String,
    val is_dir: Boolean,
    val raw_url: String? = null,
    val content: List<FileItem>? = null
)

data class FileItem(
    val name: String,
    val is_dir: Boolean,
    val raw_url: String? = null
)