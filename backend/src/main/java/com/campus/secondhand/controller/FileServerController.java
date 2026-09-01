package com.campus.secondhand.controller;

import com.campus.secondhand.util.FileUploadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

/**
 * 文件服务控制器：提供上传文件的访问
 * 替代 WebMvcConfig 中的资源处理器（ResourceHandler），消除相对路径的不确定性
 */
@RestController
public class FileServerController {

    private static final Logger log = LoggerFactory.getLogger(FileServerController.class);

    @Value("${file.upload-path}")
    private String uploadPath;

    /**
     * 启动时解析上传路径为绝对路径，确保路径稳定
     */
    @PostConstruct
    public void init() {
        // 复用统一解析逻辑，兼容从项目根目录启动的场景（自动回退到 backend/uploads）
        File dir = FileUploadUtil.resolveUploadDir(uploadPath);
        uploadPath = dir.getAbsolutePath() + File.separator;
        log.info("【文件服务】上传目录已解析为: {}", uploadPath);
        // 确保目录存在
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            log.info("【文件服务】上传目录不存在，已创建: {}", created);
        }
    }

    /**
     * 提供上传文件访问
     * URL 格式与 FileUploadUtil 返回的一致：/api/uploads/{filename}
     */
    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        File uploadDir = new File(uploadPath);
        File file = new File(uploadDir, filename);

        try {
            // 防止路径穿越攻击
            String canonicalPath = file.getCanonicalPath();
            String canonicalDir = uploadDir.getCanonicalPath();
            if (!canonicalPath.startsWith(canonicalDir)) {
                log.warn("【文件服务】路径穿越攻击被阻止: {}", filename);
                return ResponseEntity.badRequest().build();
            }
        } catch (IOException e) {
            log.error("【文件服务】路径解析失败: {}", filename, e);
            return ResponseEntity.status(500).build();
        }

        if (!file.exists() || !file.isFile()) {
            log.warn("【文件服务】文件不存在: {} (搜索路径: {})", filename, uploadPath);
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }
}
