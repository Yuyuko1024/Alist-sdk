package net.hearnsoft.alist_sdk

import kotlinx.coroutines.runBlocking
import net.hearnsoft.alist_sdk.model.AlistConfig
import net.hearnsoft.alist_sdk.utils.*

class Alist {
    private var config: AlistConfig? = null
    private var token: String? = null
    private var alistService: AlistService? = null

    fun setConfig(config: AlistConfig) {
        this.config = config
        this.alistService = AlistRetrofitUtils.createService(
            config.getBaseUrl(),
            AlistService::class.java
        )
    }

    fun getToken(): String? {
        if (token != null) return token

        return runBlocking {
            config?.let {
                val dataBody = mapOf(
                    "username" to it.username,
                    "password" to it.password
                )

                try {
                    val response = alistService?.login(dataBody)
                    token = response?.data?.token
                    token
                } catch (e: Exception) {
                    throw RuntimeException("获取Token失败: ${e.message}")
                }
            }
        }
    }

    fun getFileList(path: String): FileListData? {
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

    fun getFileInfo(path: String, password: String = ""): FileData? {
        val token = getToken() ?: throw RuntimeException("未获取到Token")
        val bodyData = mapOf(
            "path" to path,
            "password" to password
        )

        return runBlocking {
            try {
                alistService?.getFileInfo(bodyData, token)?.data
            } catch (e: Exception) {
                throw RuntimeException("获取文件信息失败: ${e.message}")
            }
        }
    }

    fun getAllFilesInfo(path: String, password: String = ""): List<Map<String, String>> {
        val filesInfoMaps = mutableListOf<Map<String, String>>()
        val fileListData = getFileList(path) ?: return filesInfoMaps

        return runBlocking {
            processDirectory(filesInfoMaps, path, fileListData.content, password)
            filesInfoMaps
        }
    }

    private suspend fun processDirectory(
        filesInfoMaps: MutableList<Map<String, String>>,
        path: String,
        files: List<FileItem>?,
        password: String
    ) {
        if (files.isNullOrEmpty()) return

        for (file in files) {
            if (!file.is_dir) {
                val filename = safePathJoin(path, file.name)
                val fileInfo = getFileInfo(filename, password)

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
                runBlocking { processDirectory(filesInfoMaps, directoryPath, directoryFiles, password) }
            }
        }
    }

    private fun safePathJoin(base: String, name: String): String {
        return if (base.endsWith("/")) base + name else "$base/$name"
    }
}