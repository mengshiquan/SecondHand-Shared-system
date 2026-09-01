package com.campus.secondhand.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.campus.secondhand.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 文件上传工具类
 */
@Component
public class FileUploadUtil {

    private static final Logger log = LoggerFactory.getLogger(FileUploadUtil.class);

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    @Value("${file.upload-path}")
    private String uploadPath;

    /**
     * 解析上传目录。若配置目录不存在或为空，且工作目录下存在非空的 backend/uploads
     * （说明后端从项目根目录而非 backend/ 目录启动），自动回退到该目录，
     * 避免启动位置不同导致已上传图片无法访问。
     */
    public static File resolveUploadDir(String configuredPath) {
        File dir = new File(configuredPath).getAbsoluteFile();
        boolean empty = !dir.isDirectory() || dir.list() == null || dir.list().length == 0;
        if (empty) {
            File backendDir = new File(dir.getParentFile(), "backend" + File.separator + "uploads");
            if (backendDir.isDirectory() && backendDir.list() != null && backendDir.list().length > 0) {
                log.warn("【文件服务】配置上传目录为空，回退到已有文件的目录: {}", backendDir.getAbsolutePath());
                return backendDir;
            }
        }
        return dir;
    }

    /**
     * 启动时解析上传路径为绝对路径，确保路径稳定
     */
    @PostConstruct
    public void init() {
        File dir = resolveUploadDir(uploadPath);
        uploadPath = dir.getAbsolutePath() + File.separator;
        log.info("【文件上传】上传目录已解析为: {}", uploadPath);
    }

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException("文件名不合法");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 jpg、jpeg、png、gif、webp 格式图片");
        }

        String fileName = IdUtil.simpleUUID() + "." + extension;
        String filePath = uploadPath + fileName;

        FileUtil.mkdir(uploadPath);
        try {
            file.transferTo(FileUtil.file(filePath));
        } catch (IOException e) {
            throw new BusinessException("文件上传失败");
        }

        return "/api/uploads/" + fileName;
    }
}
