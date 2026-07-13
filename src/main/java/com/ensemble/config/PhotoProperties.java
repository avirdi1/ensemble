package com.ensemble.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Photo storage settings. Bound from {@code ensemble.photos.*}.
 *
 * @param dir base directory for {@code LocalDiskPhotoStorage}
 */
@ConfigurationProperties(prefix = "ensemble.photos")
public record PhotoProperties(String dir) {
}
