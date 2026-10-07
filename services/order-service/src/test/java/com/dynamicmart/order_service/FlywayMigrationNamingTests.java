package com.dynamicmart.order_service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class FlywayMigrationNamingTests {
    private static final Path MIGRATION_DIRECTORY = Path.of("src", "main", "resources", "db", "migration");
    private static final Pattern VERSIONED_MIGRATION = Pattern.compile("^V([0-9]+(?:[._][0-9]+)*)__.+\\.sql$");

    @Test
    void versionedMigrationsHaveUniqueVersions() throws IOException {
        List<String> migrationNames;
        try (var files = Files.list(MIGRATION_DIRECTORY)) {
            migrationNames = files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.startsWith("V"))
                    .sorted()
                    .toList();
        }

        assertFalse(migrationNames.isEmpty(), "Order Service phải có Flyway migration.");
        assertTrue(migrationNames.stream().allMatch(name -> VERSIONED_MIGRATION.matcher(name).matches()),
                () -> "Tên migration không đúng chuẩn Flyway: " + migrationNames);

        Map<String, List<String>> migrationsByVersion = migrationNames.stream()
                .collect(Collectors.groupingBy(this::versionOf));
        Map<String, List<String>> duplicateVersions = migrationsByVersion.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        assertTrue(duplicateVersions.isEmpty(),
                () -> "Mỗi Flyway version chỉ được xuất hiện một lần: " + duplicateVersions);
    }

    private String versionOf(String migrationName) {
        var matcher = VERSIONED_MIGRATION.matcher(migrationName);
        if (!matcher.matches()) {
            return migrationName;
        }
        return matcher.group(1).replace('_', '.');
    }
}
