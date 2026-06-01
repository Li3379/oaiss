package com.oaiss.chain.service;

import com.oaiss.chain.config.MinioConfig;
import com.oaiss.chain.exception.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MinioServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private MinioConfig minioConfig;

    @InjectMocks
    private MinioService minioService;

    private MultipartFile testFile;

    @BeforeEach
    void setUp() {
        when(minioConfig.getBucketName()).thenReturn("test-bucket");
        when(minioConfig.getEndpoint()).thenReturn("http://localhost:9000");
        when(minioConfig.getMaxFileSize()).thenReturn(10L * 1024 * 1024);
        when(minioConfig.getPresignedUrlExpiry()).thenReturn(3600);

        testFile = mock(MultipartFile.class);
        when(testFile.getOriginalFilename()).thenReturn("test.pdf");
        when(testFile.getSize()).thenReturn(1024L);
        when(testFile.getContentType()).thenReturn("application/pdf");
        when(testFile.isEmpty()).thenReturn(false);
    }

    @Test
    @DisplayName("initBucket skips creation when bucket already exists")
    void initBucket_skipsCreateWhenBucketExists() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        minioService.initBucket();

        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("initBucket creates bucket when missing")
    void initBucket_createsBucketWhenMissing() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        minioService.initBucket();

        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("initBucket wraps initialization failure")
    void initBucket_wrapsFailure() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenThrow(new RuntimeException("bucket failed"));

        assertThrows(BusinessException.class, () -> minioService.initBucket());
    }

    @Test
    @DisplayName("uploadFile stores object and returns metadata")
    void uploadFile_returnsUploadResult() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        when(testFile.getInputStream()).thenReturn(inputStream);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));

        MinioService.UploadResult result = minioService.uploadFile(testFile, "reports");

        assertNotNull(result);
        assertNotNull(result.objectName());
        assertEquals(1024L, result.size());
        assertEquals("application/pdf", result.contentType());
        assertTrue(result.url().contains("test-bucket"));
    }

    @Test
    @DisplayName("uploadFile rejects empty file")
    void uploadFile_rejectsEmptyFile() {
        when(testFile.isEmpty()).thenReturn(true);

        assertThrows(BusinessException.class, () -> minioService.uploadFile(testFile, "reports"));
    }

    @Test
    @DisplayName("uploadFile rejects oversized file")
    void uploadFile_rejectsOversizedFile() {
        when(testFile.getSize()).thenReturn(20L * 1024 * 1024);

        assertThrows(BusinessException.class, () -> minioService.uploadFile(testFile, "reports"));
    }

    @Test
    @DisplayName("uploadFile rejects dangerous jsp extension")
    void uploadFile_rejectsDangerousJsp() throws Exception {
        when(testFile.getOriginalFilename()).thenReturn("malicious.jsp");
        when(testFile.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));

        assertThrows(BusinessException.class, () -> minioService.uploadFile(testFile, "reports"));
    }

    @Test
    @DisplayName("uploadFile rejects dangerous exe extension")
    void uploadFile_rejectsDangerousExe() throws Exception {
        when(testFile.getOriginalFilename()).thenReturn("program.exe");
        when(testFile.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));

        assertThrows(BusinessException.class, () -> minioService.uploadFile(testFile, "reports"));
    }

    @Test
    @DisplayName("uploadFile rejects dangerous bat extension")
    void uploadFile_rejectsDangerousBat() throws Exception {
        when(testFile.getOriginalFilename()).thenReturn("script.bat");
        when(testFile.getInputStream()).thenReturn(new ByteArrayInputStream("content".getBytes()));

        assertThrows(BusinessException.class, () -> minioService.uploadFile(testFile, "reports"));
    }

    @Test
    @DisplayName("uploadFile stores uploader metadata when owner id is provided")
    void uploadFile_storesUploaderMetadata() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        when(testFile.getInputStream()).thenReturn(inputStream);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));

        MinioService.UploadResult result = minioService.uploadFile(testFile, "reports", 42L);

        assertNotNull(result);
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("uploadStream uploads stream successfully")
    void uploadStream_returnsUploadResult() throws Exception {
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(mock(ObjectWriteResponse.class));

        MinioService.UploadResult result = minioService.uploadStream(inputStream, "test/object.pdf", "application/pdf", 1024L);

        assertEquals("test/object.pdf", result.objectName());
        assertEquals(1024L, result.size());
    }

    @Test
    @DisplayName("downloadFile returns object stream")
    void downloadFile_returnsStream() throws Exception {
        GetObjectResponse response = mock(GetObjectResponse.class);
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

        InputStream result = minioService.downloadFile("test.pdf");

        assertNotNull(result);
    }

    @Test
    @DisplayName("downloadFile wraps missing object failure")
    void downloadFile_wrapsFailure() throws Exception {
        when(minioClient.getObject(any(GetObjectArgs.class))).thenThrow(new RuntimeException("Not found"));

        assertThrows(BusinessException.class, () -> minioService.downloadFile("test.pdf"));
    }

    @Test
    @DisplayName("deleteFile removes object")
    void deleteFile_removesObject() throws Exception {
        minioService.deleteFile("test.pdf");

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    @DisplayName("deleteFile wraps remove failure")
    void deleteFile_wrapsFailure() throws Exception {
        doThrow(new RuntimeException("Error")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertThrows(BusinessException.class, () -> minioService.deleteFile("test.pdf"));
    }

    @Test
    @DisplayName("deleteFiles removes multiple objects")
    void deleteFiles_removesObjects() throws Exception {
        minioService.deleteFiles(List.of("file1.pdf", "file2.pdf"));

        verify(minioClient).removeObjects(any(RemoveObjectsArgs.class));
    }

    @Test
    @DisplayName("fileExists returns true when stat succeeds")
    void fileExists_returnsTrueOnSuccess() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(mock(StatObjectResponse.class));

        assertTrue(minioService.fileExists("test.pdf"));
    }

    @Test
    @DisplayName("fileExists returns false on stat failure")
    void fileExists_returnsFalseOnFailure() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(new RuntimeException("Not found"));

        assertFalse(minioService.fileExists("test.pdf"));
    }

    @Test
    @DisplayName("getFileInfo returns stat details")
    void getFileInfo_returnsMetadata() throws Exception {
        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.size()).thenReturn(1024L);
        when(stat.contentType()).thenReturn("application/pdf");
        when(stat.etag()).thenReturn("etag123");
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(stat);

        MinioService.FileInfo info = minioService.getFileInfo("test.pdf");

        assertEquals(1024L, info.size());
        assertEquals("application/pdf", info.contentType());
        assertEquals("etag123", info.etag());
    }

    @Test
    @DisplayName("getFileInfo wraps stat failure")
    void getFileInfo_wrapsFailure() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(new RuntimeException("Not found"));

        assertThrows(BusinessException.class, () -> minioService.getFileInfo("test.pdf"));
    }

    @Test
    @DisplayName("getPresignedUrl returns read url")
    void getPresignedUrl_returnsUrl() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn("http://presigned-url");

        String url = minioService.getPresignedUrl("test.pdf");

        assertEquals("http://presigned-url", url);
    }

    @Test
    @DisplayName("getPresignedUrl wraps failure")
    void getPresignedUrl_wrapsFailure() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("presign failed"));

        assertThrows(BusinessException.class, () -> minioService.getPresignedUrl("test.pdf"));
    }

    @Test
    @DisplayName("getPresignedUploadUrl returns upload url")
    void getPresignedUploadUrl_returnsUrl() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn("http://upload-url");

        String url = minioService.getPresignedUploadUrl("test.pdf");

        assertEquals("http://upload-url", url);
    }

    @Test
    @DisplayName("getPresignedUploadUrl wraps failure")
    void getPresignedUploadUrl_wrapsFailure() throws Exception {
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("upload presign failed"));

        assertThrows(BusinessException.class, () -> minioService.getPresignedUploadUrl("test.pdf"));
    }

    @Test
    @DisplayName("listFiles returns all admin-visible files")
    void listFiles_returnsAdminVisibleFiles() throws Exception {
        Result<Item> first = mock(Result.class);
        Item firstItem = mock(Item.class);
        when(first.get()).thenReturn(firstItem);
        when(firstItem.objectName()).thenReturn("reports/a.pdf");
        when(firstItem.size()).thenReturn(12L);
        when(firstItem.etag()).thenReturn("etag-a");

        Result<Item> second = mock(Result.class);
        Item secondItem = mock(Item.class);
        when(second.get()).thenReturn(secondItem);
        when(secondItem.objectName()).thenReturn("reports/b.pdf");
        when(secondItem.size()).thenReturn(18L);
        when(secondItem.etag()).thenReturn("etag-b");

        when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(List.of(first, second));

        MinioService.FileListResult result = minioService.listFiles("reports", 1, 10, null, true);

        assertEquals(2, result.total());
        assertEquals(2, result.files().size());
        assertEquals("reports/a.pdf", result.files().get(0).objectName());
    }

    @Test
    @DisplayName("listFiles filters non-admin view by owner")
    void listFiles_filtersFilesByOwner() throws Exception {
        Result<Item> allowed = mock(Result.class);
        Item allowedItem = mock(Item.class);
        when(allowed.get()).thenReturn(allowedItem);
        when(allowedItem.objectName()).thenReturn("reports/allowed.pdf");
        when(allowedItem.size()).thenReturn(20L);
        when(allowedItem.etag()).thenReturn("etag-allowed");

        Result<Item> denied = mock(Result.class);
        Item deniedItem = mock(Item.class);
        when(denied.get()).thenReturn(deniedItem);
        when(deniedItem.objectName()).thenReturn("reports/denied.pdf");
        when(deniedItem.size()).thenReturn(21L);
        when(deniedItem.etag()).thenReturn("etag-denied");

        StatObjectResponse allowedStat = mock(StatObjectResponse.class);
        when(allowedStat.userMetadata()).thenReturn(Map.of("x-amz-meta-uploader-id", "7"));
        StatObjectResponse deniedStat = mock(StatObjectResponse.class);
        when(deniedStat.userMetadata()).thenReturn(Map.of("x-amz-meta-uploader-id", "9"));

        when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(List.of(allowed, denied));
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(allowedStat, deniedStat);

        MinioService.FileListResult result = minioService.listFiles("reports", 1, 10, 7L, false);

        assertEquals(1, result.total());
        assertEquals(1, result.files().size());
        assertEquals("reports/allowed.pdf", result.files().get(0).objectName());
    }

    @Test
    @DisplayName("listFiles paginates and normalizes invalid page size")
    void listFiles_paginatesWithSafeDefaults() throws Exception {
        Result<Item> itemResult = mock(Result.class);
        Item item = mock(Item.class);
        when(itemResult.get()).thenReturn(item);
        when(item.objectName()).thenReturn("reports/a.pdf");
        when(item.size()).thenReturn(12L);
        when(item.etag()).thenReturn("etag-a");

        when(minioClient.listObjects(any(ListObjectsArgs.class))).thenReturn(List.of(itemResult));

        MinioService.FileListResult result = minioService.listFiles("reports", 0, 0, null, true);

        assertEquals(1, result.total());
        assertEquals(1, result.page());
        assertEquals(20, result.size());
    }

    @Test
    @DisplayName("listFiles wraps list failure")
    void listFiles_wrapsFailure() throws Exception {
        when(minioClient.listObjects(any(ListObjectsArgs.class))).thenThrow(new RuntimeException("list failed"));

        assertThrows(BusinessException.class, () -> minioService.listFiles("reports", 1, 10));
    }

    @Test
    @DisplayName("getFileOwner returns uploader id when metadata exists")
    void getFileOwner_returnsUploaderId() throws Exception {
        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.userMetadata()).thenReturn(Map.of("x-amz-meta-uploader-id", "42"));
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(stat);

        Long ownerId = minioService.getFileOwner("reports/test.pdf");

        assertEquals(42L, ownerId);
    }

    @Test
    @DisplayName("getFileOwner returns null without metadata")
    void getFileOwner_returnsNullWithoutMetadata() throws Exception {
        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.userMetadata()).thenReturn(Collections.emptyMap());
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(stat);

        Long ownerId = minioService.getFileOwner("reports/test.pdf");

        assertNull(ownerId);
    }

    @Test
    @DisplayName("getFileOwner returns null for invalid metadata")
    void getFileOwner_returnsNullForInvalidMetadata() throws Exception {
        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.userMetadata()).thenReturn(Collections.singletonMap("x-amz-meta-uploader-id", "oops"));
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(stat);

        Long ownerId = minioService.getFileOwner("reports/test.pdf");

        assertNull(ownerId);
    }

    @Test
    @DisplayName("copyFile delegates to MinIO")
    void copyFile_delegatesToMinio() throws Exception {
        minioService.copyFile("source.pdf", "target.pdf");

        verify(minioClient).copyObject(any(CopyObjectArgs.class));
    }

    @Test
    @DisplayName("copyFile wraps copy failure")
    void copyFile_wrapsFailure() throws Exception {
        when(minioClient.copyObject(any(CopyObjectArgs.class))).thenThrow(new RuntimeException("copy failed"));

        assertThrows(BusinessException.class, () -> minioService.copyFile("source.pdf", "target.pdf"));
    }
}
