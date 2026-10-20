package com.pingpong.integration;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.pingpong.PingpongRecommendationApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = PingpongRecommendationApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RuntimeSchemaCompatibilityIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void mappedEntityColumnsShouldExistInDatabaseTables() throws Exception {
        List<Class<?>> entityClasses = findEntityClasses();
        assertFalse(entityClasses.isEmpty(), "No entity classes annotated with @TableName were discovered.");

        List<String> mismatches = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();
            String catalog = connection.getCatalog();

            for (Class<?> entityClass : entityClasses) {
                TableName tableName = entityClass.getAnnotation(TableName.class);
                String table = tableName.value();
                Set<String> expectedColumns = resolveExpectedColumns(entityClass);
                Set<String> actualColumns = loadTableColumns(metadata, catalog, table);

                if (actualColumns.isEmpty()) {
                    mismatches.add(table + " -> table not found");
                    continue;
                }

                List<String> missingColumns = new ArrayList<>();
                for (String expectedColumn : expectedColumns) {
                    if (!actualColumns.contains(expectedColumn)) {
                        missingColumns.add(expectedColumn);
                    }
                }

                if (!missingColumns.isEmpty()) {
                    mismatches.add(table + " -> missing columns " + missingColumns);
                }
            }
        }

        assertTrue(mismatches.isEmpty(), "Runtime schema drift detected: " + mismatches);
    }

    @Test
    void activeTablesShouldNotReferenceLegacySysUserTable() {
        List<String> legacyReferences = jdbcTemplate.query(
            "SELECT TABLE_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME = 'sys_user'",
            (rs, rowNum) -> rs.getString("TABLE_NAME")
        );

        assertTrue(
            legacyReferences.isEmpty(),
            "Legacy sys_user foreign keys still exist on active tables: " + legacyReferences
        );
    }

    private List<Class<?>> findEntityClasses() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(TableName.class));

        List<Class<?>> classes = new ArrayList<>();
        for (BeanDefinition candidate : scanner.findCandidateComponents("com.pingpong.entity")) {
            classes.add(ClassUtils.forName(candidate.getBeanClassName(), getClass().getClassLoader()));
        }
        return classes;
    }

    private Set<String> resolveExpectedColumns(Class<?> entityClass) {
        Set<String> columns = new LinkedHashSet<>();
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField != null && !tableField.exist()) {
                continue;
            }

            TableId tableId = field.getAnnotation(TableId.class);
            if (tableId != null && StringUtils.hasText(tableId.value())) {
                columns.add(tableId.value().toLowerCase(Locale.ROOT));
                continue;
            }

            if (tableField != null && StringUtils.hasText(tableField.value())) {
                columns.add(tableField.value().toLowerCase(Locale.ROOT));
                continue;
            }

            columns.add(camelToSnake(field.getName()));
        }
        return columns;
    }

    private Set<String> loadTableColumns(DatabaseMetaData metadata, String catalog, String table) throws SQLException {
        Set<String> columns = new LinkedHashSet<>();
        try (ResultSet resultSet = metadata.getColumns(catalog, null, table, null)) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("COLUMN_NAME").toLowerCase(Locale.ROOT));
            }
        }
        return columns;
    }

    private String camelToSnake(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isUpperCase(current)) {
                if (builder.length() > 0) {
                    builder.append('_');
                }
                builder.append(Character.toLowerCase(current));
            } else {
                builder.append(current);
            }
        }
        return builder.toString().toLowerCase(Locale.ROOT);
    }
}
