package com.oaiss.chain.controller;

import com.oaiss.chain.constant.ErrorCode;
import com.oaiss.chain.dto.ApiResponse;
import com.oaiss.chain.exception.BusinessException;
import com.oaiss.chain.security.JwtUserDetails;
import com.oaiss.chain.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件管理控制器
 * 
 * <p>提供文件上传、下载、删除、预览等接口</p>
 * 
 * @author OAISS Chain Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
@Tag(name = "07. 文件管理", description = "文件上传、下载、删除、预签名等文件操作接口")
@Validated
public class FileController {

    private final MinioService minioService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "上传单个文件", 
        description = "上传文件到MinIO对象存储。支持常见文件格式，单文件最大100MB。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "上传成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = MinioService.UploadResult.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "文件为空或格式不支持"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "413", 
            description = "文件大小超过限制"
        )
    })
    public ApiResponse<MinioService.UploadResult> uploadFile(
            @Parameter(description = "要上传的文件", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "存储文件夹，如 reports/, certificates/", example = "reports/")
            @RequestParam(value = "folder", required = false) String folder) {
        
        MinioService.UploadResult result = minioService.uploadFile(file, folder);
        return ApiResponse.success(result, "文件上传成功");
    }

    @PostMapping(value = "/upload/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "批量上传文件", 
        description = "批量上传多个文件到MinIO。单次最多上传10个文件。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "批量上传成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", 
            description = "文件列表为空"
        )
    })
    public ApiResponse<List<MinioService.UploadResult>> uploadFiles(
            @Parameter(description = "文件列表", required = true)
            @RequestPart("files") List<MultipartFile> files,
            @Parameter(description = "存储文件夹", example = "batch/")
            @RequestParam(value = "folder", required = false) String folder) {
        
        List<MinioService.UploadResult> results = files.stream()
                .map(file -> minioService.uploadFile(file, folder))
                .toList();
        
        return ApiResponse.success(results, "批量上传成功，共 " + results.size() + " 个文件");
    }

    @GetMapping("/download")
    @Operation(
        summary = "下载文件", 
        description = "从MinIO下载指定文件。返回文件流。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "下载成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "文件不存在"
        )
    })
    public void downloadFile(
            @Parameter(description = "对象名称（文件路径）", required = true, example = "reports/2024/report.pdf")
            @RequestParam @NotBlank String objectName,
            HttpServletResponse response) {
        
        try (InputStream inputStream = minioService.downloadFile(objectName)) {
            MinioService.FileInfo fileInfo = minioService.getFileInfo(objectName);
            
            response.setContentType(fileInfo.contentType());
            response.setContentLengthLong(fileInfo.size());
            
            String encodedFilename = URLEncoder.encode(
                    objectName.substring(objectName.lastIndexOf("/") + 1), 
                    StandardCharsets.UTF_8);
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + encodedFilename + "\"");
            
            inputStream.transferTo(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("File download failed", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件下载失败", e);
        }
    }

    @DeleteMapping
    @Operation(
        summary = "删除文件",
        description = "删除MinIO中的指定文件。删除后无法恢复。仅管理员或文件所有者可删除。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "删除成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "文件不存在"
        )
    })
    public ApiResponse<Void> deleteFile(
            @Parameter(description = "对象名称", required = true, example = "reports/2024/report.pdf")
            @RequestParam @NotBlank String objectName,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            HttpServletRequest request) {

        checkDeletePermission(objectName, currentUser, request);
        minioService.deleteFile(objectName);
        return ApiResponse.success(null, "文件删除成功");
    }

    @DeleteMapping("/batch")
    @Operation(
        summary = "批量删除文件",
        description = "批量删除MinIO中的多个文件。仅管理员或文件所有者可删除。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "批量删除成功"
        )
    })
    public ApiResponse<Void> deleteFiles(
            @Parameter(description = "对象名称列表", required = true)
            @RequestBody @NotBlank List<String> objectNames,
            @Parameter(hidden = true) @AuthenticationPrincipal JwtUserDetails currentUser,
            HttpServletRequest request) {

        // Check permission for each file
        for (String objectName : objectNames) {
            checkDeletePermission(objectName, currentUser, request);
        }
        minioService.deleteFiles(objectNames);
        return ApiResponse.success(null, "批量删除成功，共 " + objectNames.size() + " 个文件");
    }

    @GetMapping("/info")
    @Operation(
        summary = "获取文件信息", 
        description = "获取文件的元数据信息，包括大小、类型、修改时间等。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "获取成功",
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = MinioService.FileInfo.class))
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "文件不存在"
        )
    })
    public ApiResponse<MinioService.FileInfo> getFileInfo(
            @Parameter(description = "对象名称", required = true, example = "reports/2024/report.pdf")
            @RequestParam @NotBlank String objectName) {
        
        MinioService.FileInfo fileInfo = minioService.getFileInfo(objectName);
        return ApiResponse.success(fileInfo);
    }

    @GetMapping("/exists")
    @Operation(
        summary = "检查文件是否存在", 
        description = "检查指定文件是否存在于MinIO存储中。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "检查完成"
        )
    })
    public ApiResponse<Boolean> fileExists(
            @Parameter(description = "对象名称", required = true, example = "reports/2024/report.pdf")
            @RequestParam @NotBlank String objectName) {
        
        boolean exists = minioService.fileExists(objectName);
        return ApiResponse.success(exists);
    }

    @GetMapping("/presigned-url")
    @Operation(
        summary = "获取文件预签名URL", 
        description = "获取文件的临时访问URL，有效期1小时。适用于前端直接展示文件。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "获取成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "文件不存在"
        )
    })
    public ApiResponse<String> getPresignedUrl(
            @Parameter(description = "对象名称", required = true, example = "reports/2024/report.pdf")
            @RequestParam @NotBlank String objectName) {
        
        String url = minioService.getPresignedUrl(objectName);
        return ApiResponse.success(url);
    }

    @GetMapping("/presigned-upload-url")
    @Operation(
        summary = "获取上传预签名URL", 
        description = "获取文件直传的预签名URL，用于前端直接上传文件到MinIO。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "获取成功"
        )
    })
    public ApiResponse<String> getPresignedUploadUrl(
            @Parameter(description = "目标对象名称", required = true, example = "uploads/new_file.pdf")
            @RequestParam @NotBlank String objectName) {
        
        String url = minioService.getPresignedUploadUrl(objectName);
        return ApiResponse.success(url);
    }

    @GetMapping("/list")
    @Operation(
        summary = "列出文件", 
        description = "列出指定文件夹下的所有文件。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "查询成功"
        )
    })
    public ApiResponse<MinioService.FileListResult> listFiles(
            @Parameter(description = "文件夹前缀", example = "reports/2024/")
            @RequestParam(value = "prefix", required = false) String prefix,
            @Parameter(description = "页码（从1开始）", example = "1")
            @RequestParam(value = "page", required = false) Integer page,
            @Parameter(description = "每页大小", example = "20")
            @RequestParam(value = "size", required = false) Integer size) {

        return ApiResponse.success(minioService.listFiles(prefix, page, size));
    }

    @PostMapping("/copy")
    @Operation(
        summary = "复制文件", 
        description = "复制文件到新位置。",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", 
            description = "复制成功"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", 
            description = "源文件不存在"
        )
    })
    public ApiResponse<Void> copyFile(
            @Parameter(description = "源对象名称", required = true, example = "reports/original.pdf")
            @RequestParam @NotBlank String sourceObjectName,
            @Parameter(description = "目标对象名称", required = true, example = "reports/copy.pdf")
            @RequestParam @NotBlank String targetObjectName) {
        
        minioService.copyFile(sourceObjectName, targetObjectName);
        return ApiResponse.success(null, "文件复制成功");
    }

    // ==================== 权限检查 ====================

    /**
     * 检查删除权限：管理员可删除任意文件，普通用户只能删除自己上传的文件
     */
    private void checkDeletePermission(String objectName, JwtUserDetails currentUser, HttpServletRequest request) {
        Long userId = resolveUserId(currentUser, request);
        Integer userType = resolveUserType(currentUser, request);

        // 管理员（userType=4）可删除任意文件
        if (userType != null && userType == 4) {
            return;
        }

        // 非管理员：检查是否为文件所有者
        Long fileOwner = minioService.getFileOwner(objectName);
        if (fileOwner == null) {
            // 旧文件无所有者信息，允许删除（向后兼容）
            return;
        }

        if (!fileOwner.equals(userId)) {
            log.warn("Delete denied: user {} (type {}) attempted to delete file {} owned by {}",
                    userId, userType, objectName, fileOwner);
            throw new BusinessException(ErrorCode.PERMISSION_DENIED, "无权删除此文件");
        }
    }

    private Long resolveUserId(JwtUserDetails currentUser, HttpServletRequest request) {
        if (currentUser != null) {
            return currentUser.getUserId();
        }
        String header = request.getHeader("X-User-Id");
        return header != null ? Long.parseLong(header) : null;
    }

    private Integer resolveUserType(JwtUserDetails currentUser, HttpServletRequest request) {
        if (currentUser != null) {
            return currentUser.getUserType();
        }
        String header = request.getHeader("X-User-Type");
        return header != null ? Integer.parseInt(header) : null;
    }
}
