package net.hearnsoft.alist_sdk

import android.util.Log
import kotlinx.coroutines.runBlocking
import net.hearnsoft.alist_sdk.model.AlistConfig
import net.hearnsoft.alist_sdk.utils.*

class Alist(config: AlistConfig) {
    private var config: AlistConfig? = config
    private var token: String? = null
    private var alistService: AlistService? = null

    init {
        setConfig(config)
    }

    fun setConfig(config: AlistConfig) {
        this.config = config
        this.alistService = AlistRetrofitUtils.createService(
            config.getBaseUrl(),
            AlistService::class.java
        )
    }

    fun getToken(): String? {
        // 如果已有token，直接返回
        if (token != null) return token

        // 如果是匿名模式，则返回空token
        if (config?.isAnonymous == true) {
            return ""
        }

        return runBlocking {
            config?.let {
                // 确保用户名和密码不为空
                if (it.username.isEmpty() || it.password.isEmpty()) {
                    return@let ""
                }

                val dataBody = mapOf(
                    "username" to it.username,
                    "password" to it.password
                )

                try {
                    val response = alistService?.login(dataBody)
                    token = response?.data?.token
                    token
                } catch (e: Exception) {
                    Log.e("Alist", "获取Token失败: ${e.message}")
                    ""
                }
            }
        }
    }

    fun getFileList(path: String): FileListData? {
        // 如果是匿名访问，直接调用不带token的API
        if (config?.isAnonymous == true) {
            val bodyData = mapOf("path" to path)

            return runBlocking {
                try {
                    alistService?.listFilesAnonymous(bodyData)?.data
                } catch (e: Exception) {
                    Log.e("Alist", "匿名获取文件列表失败: ${e.message}")
                    null
                }
            }
        }

        val token = getToken() ?: throw RuntimeException("未获取到Token")
        val bodyData = mapOf("path" to path)

        return runBlocking {
            try {
                alistService?.listFiles(bodyData, token)?.data
            } catch (e: Exception) {
                throw RuntimeException("获取文件列表失败: ${e.message}")
            }
        }
    }

    fun getFileInfo(path: String): FileData? {
        val token = getToken() ?: throw RuntimeException("未获取到Token")
        val bodyData = mapOf(
            "path" to path
        )

        return runBlocking {
            try {
                alistService?.getFileInfo(bodyData, token)?.data
            } catch (e: Exception) {
                throw RuntimeException("获取文件信息失败: ${e.message}")
            }
        }
    }

    fun getAllFilesInfo(path: String): List<Map<String, String>> {
        val filesInfoMaps = mutableListOf<Map<String, String>>()
        val fileListData = getFileList(path) ?: return filesInfoMaps

        return runBlocking {
            processDirectory(filesInfoMaps, path, fileListData.content)
            filesInfoMaps
        }
    }

    private fun processDirectory(
        filesInfoMaps: MutableList<Map<String, String>>,
        path: String,
        files: List<FileItem>?
    ) {
        if (files.isNullOrEmpty()) return

        for (file in files) {
            if (!file.is_dir) {
                val filename = safePathJoin(path, file.name)
                val fileInfo = getFileInfo(filename)

                if (fileInfo?.content != null) {
                    for (contentItem in fileInfo.content) {
                        contentItem.raw_url?.let {
                            filesInfoMaps.add(mapOf(
                                "filePath" to filename,
                                "rawUrl" to it
                            ))
                        }
                    }
                } else {
                    fileInfo?.raw_url?.let {
                        filesInfoMaps.add(mapOf(
                            "filePath" to filename,
                            "rawUrl" to it
                        ))
                    }
                }
            } else {
                val directoryPath = safePathJoin(path, file.name)
                val directoryFiles = getFileList(directoryPath)?.content
                runBlocking { processDirectory(filesInfoMaps, directoryPath, directoryFiles) }
            }
        }
    }

    private fun safePathJoin(base: String, name: String): String {
        return if (base.endsWith("/")) base + name else "$base/$name"
    }
}