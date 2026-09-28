package com.whoami.module.certificate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.whoami.common.BizException;
import com.whoami.module.certificate.dto.CertificateDTO;
import com.whoami.module.certificate.dto.UpdateCertificateRequest;
import com.whoami.module.certificate.entity.Certificate;
import com.whoami.module.certificate.mapper.CertificateMapper;
import com.whoami.module.certificate.service.CertificateImageProcessor.ProcessedImage;
import com.whoami.module.upload.service.UploadBlobService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    private static final ProcessedImage PROCESSED = new ProcessedImage(
            "certificate/abc.webp", "certificate/abc_thumb.webp", "image/webp", new byte[] {1}, new byte[] {2});

    @Mock
    private CertificateMapper certificateMapper;

    @Mock
    private CertificateImageProcessor imageProcessor;

    @Mock
    private UploadBlobService uploadBlobService;

    @InjectMocks
    private CertificateService certificateService;

    private static MultipartFile file() {
        return new MockMultipartFile("file", "cert.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    private static Certificate certificate(long id, String name, LocalDate obtainedAt, int sortOrder) {
        Certificate entity = new Certificate();
        entity.setId(id);
        entity.setName(name);
        entity.setObtainedAt(obtainedAt);
        entity.setOriginalFile("certificate/" + id + ".webp");
        entity.setThumbnailFile("certificate/" + id + "_thumb.webp");
        entity.setSortOrder(sortOrder);
        return entity;
    }

    @Test
    void listPublicExposesUploadUrlsAndSortOrder() {
        when(certificateMapper.selectList(any())).thenReturn(List.of(
                certificate(1, "软考中级", LocalDate.of(2025, 6, 1), 2),
                certificate(2, "蓝桥杯一等奖", LocalDate.of(2026, 4, 20), 0)));

        List<CertificateDTO> result = certificateService.listPublic();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(1L);
        assertThat(result.get(0).name()).isEqualTo("软考中级");
        assertThat(result.get(0).obtainedAt()).isEqualTo(LocalDate.of(2025, 6, 1));
        assertThat(result.get(0).thumbUrl()).isEqualTo("/uploads/certificate/1_thumb.webp");
        assertThat(result.get(0).imageUrl()).isEqualTo("/uploads/certificate/1.webp");
        assertThat(result.get(0).sortOrder()).isEqualTo(2);
    }

    @Test
    void createInsertsProcessedFilesAndReturnsGeneratedId() {
        when(imageProcessor.process(any())).thenReturn(PROCESSED);
        when(certificateMapper.insert(any(Certificate.class))).thenAnswer(invocation -> {
            Certificate entity = invocation.getArgument(0);
            entity.setId(42L);
            return 1;
        });

        long id = certificateService.create(file(), "  软考中级  ", "2025-06-01");

        assertThat(id).isEqualTo(42L);
        ArgumentCaptor<Certificate> captor = ArgumentCaptor.forClass(Certificate.class);
        verify(certificateMapper).insert(captor.capture());
        Certificate inserted = captor.getValue();
        assertThat(inserted.getName()).isEqualTo("软考中级");
        assertThat(inserted.getObtainedAt()).isEqualTo(LocalDate.of(2025, 6, 1));
        assertThat(inserted.getOriginalFile()).isEqualTo(PROCESSED.originalFile());
        assertThat(inserted.getThumbnailFile()).isEqualTo(PROCESSED.thumbnailFile());
        // sortOrder 交由 DB 默认值 0，新建项默认排在末尾（按获取时间倒序）
        assertThat(inserted.getSortOrder()).isNull();
        // 产物字节先落 upload_blob，再写记录
        verify(uploadBlobService).store(eq("certificate/abc.webp"), eq("image/webp"), eq(new byte[] {1}));
        verify(uploadBlobService).store(eq("certificate/abc_thumb.webp"), eq("image/webp"), eq(new byte[] {2}));
    }

    @Test
    void createRejectsBlankOrTooLongNameWithoutTouchingStorage() {
        assertThatThrownBy(() -> certificateService.create(file(), null, "2025-06-01"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    assertThat(((BizException) e).getStatus()).isEqualTo(400);
                    assertThat(e.getMessage()).contains("name 必填");
                });
        assertThatThrownBy(() -> certificateService.create(file(), "   ", "2025-06-01"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getStatus()).isEqualTo(400));
        assertThatThrownBy(() -> certificateService.create(file(), "证".repeat(101), "2025-06-01"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    assertThat(((BizException) e).getStatus()).isEqualTo(400);
                    assertThat(e.getMessage()).contains("100");
                });

        verify(imageProcessor, never()).process(any());
        verify(certificateMapper, never()).insert(any(Certificate.class));
        verify(uploadBlobService, never()).store(anyString(), anyString(), any());
    }

    @Test
    void createRejectsMissingOrMalformedObtainedAt() {
        assertThatThrownBy(() -> certificateService.create(file(), "软考", null))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    assertThat(((BizException) e).getStatus()).isEqualTo(400);
                    assertThat(e.getMessage()).contains("obtainedAt 必填");
                });
        assertThatThrownBy(() -> certificateService.create(file(), "软考", "2025/06/01"))
                .isInstanceOf(BizException.class)
                .satisfies(e -> {
                    assertThat(((BizException) e).getStatus()).isEqualTo(400);
                    assertThat(e.getMessage()).contains("格式错误");
                });

        verify(imageProcessor, never()).process(any());
    }

    @Test
    void createCleansUpStoredBlobsWhenInsertFails() {
        when(imageProcessor.process(any())).thenReturn(PROCESSED);
        when(certificateMapper.insert(any(Certificate.class))).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> certificateService.create(file(), "软考", "2025-06-01"))
                .isInstanceOf(IllegalStateException.class);

        verify(uploadBlobService).delete("certificate/abc.webp");
        verify(uploadBlobService).delete("certificate/abc_thumb.webp");
    }

    @Test
    void updateWritesOnlyProvidedFields() {
        when(certificateMapper.selectById(1L)).thenReturn(certificate(1, "旧名", LocalDate.of(2024, 1, 1), 0));

        certificateService.update(1L, new UpdateCertificateRequest("新名", null, 3));

        ArgumentCaptor<Certificate> captor = ArgumentCaptor.forClass(Certificate.class);
        verify(certificateMapper).updateById(captor.capture());
        Certificate update = captor.getValue();
        assertThat(update.getId()).isEqualTo(1L);
        assertThat(update.getName()).isEqualTo("新名");
        assertThat(update.getSortOrder()).isEqualTo(3);
        // 未提供的字段不动
        assertThat(update.getObtainedAt()).isNull();
        assertThat(update.getOriginalFile()).isNull();
    }

    @Test
    void updateRejectsInvalidNameAndUnknownId() {
        when(certificateMapper.selectById(1L)).thenReturn(certificate(1, "旧名", LocalDate.of(2024, 1, 1), 0));
        assertThatThrownBy(() -> certificateService.update(1L, new UpdateCertificateRequest("证".repeat(101), null, null)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getStatus()).isEqualTo(400));

        when(certificateMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> certificateService.update(99L, new UpdateCertificateRequest("新名", null, null)))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getStatus()).isEqualTo(404));
    }

    @Test
    void deleteRemovesRecordAndStoredBlobs() {
        Certificate existing = certificate(5, "软考", LocalDate.of(2025, 6, 1), 0);
        when(certificateMapper.selectById(5L)).thenReturn(existing);

        certificateService.delete(5L);

        verify(certificateMapper).deleteById(5L);
        verify(uploadBlobService).delete("certificate/5.webp");
        verify(uploadBlobService).delete("certificate/5_thumb.webp");
    }

    @Test
    void deleteUnknownIdThrows404WithoutDeletingBlobs() {
        when(certificateMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> certificateService.delete(99L))
                .isInstanceOf(BizException.class)
                .satisfies(e -> assertThat(((BizException) e).getStatus()).isEqualTo(404));

        verify(certificateMapper, never()).deleteById(99L);
        verify(uploadBlobService, never()).delete(anyString());
    }
}
