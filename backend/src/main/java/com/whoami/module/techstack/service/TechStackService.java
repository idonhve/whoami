package com.whoami.module.techstack.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whoami.common.BizException;
import com.whoami.module.techstack.dto.TechItem;
import com.whoami.module.techstack.dto.TechItemCreate;
import com.whoami.module.techstack.entity.TechStack;
import com.whoami.module.techstack.mapper.TechStackMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 技术栈（Spec 02）：公开与管理两组接口共用同一张表，仅排序口径一致（sortOrder 升序，平级按 id）。
 * 数据由后台维护，前台刷新即见，无需发版。
 */
@Service
public class TechStackService {

    private final TechStackMapper techStackMapper;

    public TechStackService(TechStackMapper techStackMapper) {
        this.techStackMapper = techStackMapper;
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
        return new TechItem(
                entity.getId(), entity.getName(), entity.getIcon(), entity.getCategory(),
                entity.getProficiency(), entity.getWeight(), entity.getSortOrder());
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
                entity.getId(), entity.getName(), entity.getIcon(), entity.getCategory(),
                entity.getProficiency(), entity.getWeight(), entity.getSortOrder());
    }
}