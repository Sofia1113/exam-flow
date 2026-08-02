package com.examflow.user.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 组织树节点。
 */
public record OrgNode(Long id, Long parentId, String name, String path, String orgType,
                      String status, List<OrgNode> children) {

    public OrgNode withChildren(List<OrgNode> children) {
        return new OrgNode(id, parentId, name, path, orgType, status, children);
    }

    public static OrgNode emptyChildren(Long id, Long parentId, String name, String path,
                                        String orgType, String status) {
        return new OrgNode(id, parentId, name, path, orgType, status, new ArrayList<>());
    }
}
