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
     * 启动时解析上传路径为绝对路径，确保路径稳定
     */
    @PostConstruct
    public void init() {
        File dir = new File(uploadPath);
        if (!dir.isAbsolute()) {
            uploadPath = dir.getAbsolutePath() + File.separator;
        }
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
