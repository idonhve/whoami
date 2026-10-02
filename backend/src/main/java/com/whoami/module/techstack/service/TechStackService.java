package com.whoami.module.techstack.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whoami.common.BizException;
import com.whoami.module.techstack.dto.AddCatalogTechRequest;
import com.whoami.module.techstack.dto.TechCatalogItem;
import com.whoami.module.techstack.dto.TechItem;
import com.whoami.module.techstack.dto.TechItemCreate;
import com.whoami.module.techstack.entity.TechCatalog;
import com.whoami.module.techstack.entity.TechStack;
import com.whoami.module.techstack.mapper.TechCatalogMapper;
import com.whoami.module.techstack.mapper.TechStackMapper;
import com.whoami.module.techstack.service.TechIconProcessor.ProcessedIcon;
import com.whoami.module.upload.service.UploadBlobService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 技术栈（Spec 02）：公开与管理两组接口共用同一张表，仅排序口径一致（sortOrder 升序，平级按 id）。
 * 数据由后台维护，前台刷新即见，无需发版。
 */
@Service
public class TechStackService {

    private final TechStackMapper techStackMapper;
    private final TechCatalogMapper techCatalogMapper;
    private final UploadBlobService uploadBlobService;
    private final TechIconProcessor techIconProcessor;

    private static final String UPLOAD_URL_PREFIX = "/uploads/";

    public TechStackService(TechStackMapper techStackMapper, TechCatalogMapper techCatalogMapper,
                            UploadBlobService uploadBlobService, TechIconProcessor techIconProcessor) {
        this.techStackMapper = techStackMapper;
        this.techCatalogMapper = techCatalogMapper;
        this.uploadBlobService = uploadBlobService;
        this.techIconProcessor = techIconProcessor;
    }

    /** 公开列表：按 sortOrder 升序（契约：GET /api/tech-stack） */
    public List<TechItem> listPublic() {
        return selectOrdered().stream().map(this::toItem).toList();
    }

    /** 后台全量列表：与公开同口径，管理页可编辑同一份数据 */
    public List<TechItem> listAll() {
        return listPublic();
    }

    /** 新增，返回新条目 id */
    public TechItem create(TechItemCreate req) {
        TechStack entity = new TechStack();
        apply(entity, req);
        techStackMapper.insert(entity);
        return toItem(entity);
    }

    /** 更新；id 不存在返回 404 */
    public TechItem update(Long id, TechItemCreate req) {
        TechStack existing = techStackMapper.selectById(id);
        if (existing == null) {
            throw new BizException(404, "技术项不存在: " + id);
        }
        apply(existing, req);
        techStackMapper.updateById(existing);
        return toItem(existing);
    }

    /** 删除；id 不存在返回 404 */
    public void delete(Long id) {
        TechStack existing = techStackMapper.selectById(id);
        if (existing == null) {
            throw new BizException(404, "技术项不存在: " + id);
        }
        techStackMapper.deleteById(id);
    }

    /** 管理后台目录：包含预置项、自定义项和当前是否已加入展示列表。 */
    public List<TechCatalogItem> listCatalog() {
        Set<Long> selected = techStackMapper.selectList(new LambdaQueryWrapper<TechStack>()
                        .isNotNull(TechStack::getCatalogId))
                .stream()
                .map(TechStack::getCatalogId)
                .collect(Collectors.toSet());
        return techCatalogMapper.selectList(new LambdaQueryWrapper<TechCatalog>()
                        .orderByAsc(TechCatalog::getSortOrder)
                        .orderByAsc(TechCatalog::getId))
                .stream()
                .map(item -> toCatalogItem(item, selected.contains(item.getId())))
                .toList();
    }

    /** 从目录添加一个项目到公开列表；删除公开项目不会删除目录内容。 */
    @Transactional
    public TechItem addCatalogItem(Long catalogId, AddCatalogTechRequest request) {
        TechCatalog catalog = techCatalogMapper.selectById(catalogId);
        if (catalog == null) {
            throw new BizException(404, "技术目录项不存在: " + catalogId);
        }
        Long existingId = techStackMapper.selectCount(new LambdaQueryWrapper<TechStack>()
                .eq(TechStack::getCatalogId, catalogId));
        if (existingId != null && existingId > 0) {
            throw new BizException(409, "该技术已在展示列表中");
        }

        TechStack entity = new TechStack();
        entity.setCatalogId(catalog.getId());
        entity.setName(catalog.getName());
        entity.setIcon(catalog.getIcon());
        entity.setIconPath(catalog.getIconPath());
        entity.setCategory(catalog.getCategory());
        entity.setProficiency(request.proficiency());
        entity.setWeight(request.weight());
        entity.setSortOrder(request.sortOrder() == null ? nextSortOrder() : request.sortOrder());
        techStackMapper.insert(entity);
        return toItem(entity);
    }

    /** 自定义目录项创建后立即加入公开技术栈；图标内容和目录/展示记录共用事务。 */
    @Transactional
    public TechItem createCustomItem(MultipartFile file, String name, String category,
                                     String proficiency, Integer weight, Integer sortOrder) {
        String validName = requireText(name, 50, "名称");
        String validCategory = requireText(category, 20, "分类");
        requireProficiency(proficiency);
        if (weight == null || weight < 1 || weight > 100) {
            throw new BizException(400, "权重必须是 1~100 的整数");
        }
        if (techCatalogMapper.selectCount(new LambdaQueryWrapper<TechCatalog>()
                .eq(TechCatalog::getName, validName)) > 0) {
            throw new BizException(409, "技术目录中已有同名条目");
        }
        ProcessedIcon icon = techIconProcessor.process(file);

        TechCatalog catalog = new TechCatalog();
        catalog.setName(validName);
        catalog.setIconPath(icon.filePath());
        catalog.setCategory(validCategory);
        catalog.setCustom(true);
        catalog.setSortOrder(sortOrder == null ? nextCatalogSortOrder() : sortOrder);
        techCatalogMapper.insert(catalog);
        uploadBlobService.store(icon.filePath(), icon.contentType(), icon.content());

        TechStack entity = new TechStack();
        entity.setCatalogId(catalog.getId());
        entity.setName(catalog.getName());
        entity.setIconPath(catalog.getIconPath());
        entity.setCategory(catalog.getCategory());
        entity.setProficiency(proficiency);
        entity.setWeight(weight);
        entity.setSortOrder(sortOrder == null ? nextSortOrder() : sortOrder);
        techStackMapper.insert(entity);
        return toItem(entity);
    }

    private List<TechStack> selectOrdered() {
        return techStackMapper.selectList(
                new LambdaQueryWrapper<TechStack>()
                        .orderByAsc(TechStack::getSortOrder)
                        .orderByAsc(TechStack::getId));
    }

    private void apply(TechStack entity, TechItemCreate req) {
        entity.setName(req.name());
        entity.setIcon(blankToNull(req.icon()));
        entity.setCategory(req.category());
        entity.setProficiency(req.proficiency());
        entity.setWeight(req.weight());
        entity.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private TechItem toItem(TechStack entity) {
        return new TechItem(
                entity.getId(), entity.getName(), entity.getIcon(), iconUrl(entity.getIconPath()),
                entity.getCatalogId(), entity.getCategory(),
                entity.getProficiency(), entity.getWeight(), entity.getSortOrder());
    }

    private TechCatalogItem toCatalogItem(TechCatalog entity, boolean inStack) {
        return new TechCatalogItem(entity.getId(), entity.getName(), entity.getIcon(),
                iconUrl(entity.getIconPath()), entity.getCategory(), Boolean.TRUE.equals(entity.getCustom()),
                inStack, entity.getSortOrder());
    }

    private String iconUrl(String iconPath) {
        return iconPath == null || iconPath.isBlank() ? null : UPLOAD_URL_PREFIX + iconPath;
    }

    private int nextSortOrder() {
        return techStackMapper.selectList(new LambdaQueryWrapper<TechStack>()
                        .orderByDesc(TechStack::getSortOrder)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .map(item -> item.getSortOrder() == null ? 1 : item.getSortOrder() + 1)
                .orElse(1);
    }

    private int nextCatalogSortOrder() {
        return techCatalogMapper.selectList(new LambdaQueryWrapper<TechCatalog>()
                        .orderByDesc(TechCatalog::getSortOrder)
                        .last("LIMIT 1"))
                .stream()
                .findFirst()
                .map(item -> item.getSortOrder() == null ? 1 : item.getSortOrder() + 1)
                .orElse(1);
    }

    private String requireText(String value, int maxLength, String label) {
        if (value == null || value.isBlank()) {
            throw new BizException(400, label + "必填");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BizException(400, label + "不能超过 " + maxLength + " 字符");
        }
        return trimmed;
    }

    private void requireProficiency(String proficiency) {
        if (!"master".equals(proficiency) && !"proficient".equals(proficiency)
                && !"familiar".equals(proficiency)) {
            throw new BizException(400, "熟练度必须是 master/proficient/familiar");
        }
    }
}
