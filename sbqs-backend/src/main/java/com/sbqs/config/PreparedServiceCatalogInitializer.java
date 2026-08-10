package com.sbqs.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import com.sbqs.service.ServiceCatalogService;

/**
 * Chỉ giữ các bước tương thích dữ liệu cũ. Danh mục dịch vụ không được seed từ Java;
 * Super Admin là nguồn tạo dữ liệu trong bảng service_catalog.
 */
@Component
public class PreparedServiceCatalogInitializer {
    private static final String CATALOG_VERSION = "prepared-service-catalog-v2";
    private static final String MANUAL_MAPPING_VERSION = "prepared-service-manual-mapping-v1";
    private static final String DEFAULT_PROFILE_VERSION = "prepared-service-default-profile-v1";
    private static final String DEFAULT_PROFILE_FIELDS =
            "FULL_NAME,MOBILE_PHONE,PERMANENT_ADDRESS,CONTACT_ADDRESS";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactionTemplate;
    private final CacheManager cacheManager;
    private final ServiceCatalogService serviceCatalogService;

    public PreparedServiceCatalogInitializer(
            JdbcTemplate jdbc,
            TransactionTemplate transactionTemplate,
            CacheManager cacheManager,
            ServiceCatalogService serviceCatalogService) {
        this.jdbc = jdbc;
        this.transactionTemplate = transactionTemplate;
        this.cacheManager = cacheManager;
        this.serviceCatalogService = serviceCatalogService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeCatalog() {
        transactionTemplate.executeWithoutResult(status -> {
            jdbc.execute("create table if not exists system_settings (setting_key varchar(100) primary key, setting_value varchar(500) not null)");
            applyLegacyCatalogMigration();
            applyLegacyManualMappingMigration();
            jdbc.update("update tickets set status = 'CANCELLED' where service_id is null and status in ('WAITING', 'SERVING')");
            synchronizeDefaultProfileFields();
        });

        serviceCatalogService.synchronizeAllBranches();

        Cache servicesCache = cacheManager.getCache("services");
        if (servicesCache != null) servicesCache.clear();
    }

    private void applyLegacyCatalogMigration() {
        Integer migrated = jdbc.queryForObject(
                "select count(*) from system_settings where setting_key = ?",
                Integer.class,
                CATALOG_VERSION);
        if (migrated != null && migrated > 0) return;

        jdbc.update("delete from queue_machine_services");
        jdbc.update("update tickets set status = 'CANCELLED' where status in ('WAITING', 'SERVING')");
        jdbc.update("update appointments set status = 'CANCELLED' where status = 'PENDING'");
        jdbc.update("update tickets set service_id = null");
        jdbc.update("update appointments set service_id = null");
        jdbc.update("delete from services");
        jdbc.update(
                "insert into system_settings(setting_key, setting_value) values (?, 'completed')",
                CATALOG_VERSION);
    }

    private void applyLegacyManualMappingMigration() {
        Integer migrated = jdbc.queryForObject(
                "select count(*) from system_settings where setting_key = ?",
                Integer.class,
                MANUAL_MAPPING_VERSION);
        if (migrated != null && migrated > 0) return;

        jdbc.update("delete from queue_machine_services");
        jdbc.update(
                "insert into system_settings(setting_key, setting_value) values (?, 'completed')",
                MANUAL_MAPPING_VERSION);
    }

    private void synchronizeDefaultProfileFields() {
        Integer synchronizedCount = jdbc.queryForObject(
                "select count(*) from system_settings where setting_key = ?",
                Integer.class,
                DEFAULT_PROFILE_VERSION);
        if (synchronizedCount != null && synchronizedCount > 0) return;

        jdbc.update("""
                update services
                set required_customer_fields = case
                    when required_customer_fields is null or trim(required_customer_fields) = '' then ?
                    else concat(?, ',', required_customer_fields)
                end
                """, DEFAULT_PROFILE_FIELDS, DEFAULT_PROFILE_FIELDS);
        jdbc.update(
                "insert into system_settings(setting_key, setting_value) values (?, 'completed')",
                DEFAULT_PROFILE_VERSION);
    }
}
