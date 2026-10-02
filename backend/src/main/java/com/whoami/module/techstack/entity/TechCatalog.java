package com.whoami.module.techstack.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/** Java 全栈常用技术目录及管理员自建条目。 */
@TableName("tech_catalog")
public class TechCatalog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** Devicon CSS 后缀；自定义图标为空并使用 iconPath。 */
    private String icon;

    private String iconPath;

    private String category;

    @TableField("is_custom")
    private Boolean custom;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getIconPath() { return iconPath; }
    public void setIconPath(String iconPath) { this.iconPath = iconPath; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Boolean getCustom() { return custom; }
    public void setCustom(Boolean custom) { this.custom = custom; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
