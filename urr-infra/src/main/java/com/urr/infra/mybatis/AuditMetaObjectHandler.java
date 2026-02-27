package com.urr.infra.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审计字段自动填充：create update delete_flag/version
 * 后续接入登录态时，把 system 改成当前账号。
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    private static final String SYSTEM = "system";

    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        strictInsertFill(metaObject, "createUser", String.class, SYSTEM);
        strictInsertFill(metaObject, "updateUser", String.class, SYSTEM);
        strictInsertFill(metaObject, "deleteFlag", Integer.class, 0);
        strictInsertFill(metaObject, "version", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updateUser", String.class, SYSTEM);
    }
}
