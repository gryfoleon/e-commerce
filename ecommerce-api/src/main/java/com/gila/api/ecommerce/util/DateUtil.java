package com.gila.api.ecommerce.util;

import com.gila.api.ecommerce.configuration.constants.GlobalConstants;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class DateUtil {

    private static final ZoneId AMERICA_CDMX_ZONE_ID = ZoneId.of(GlobalConstants.AMERICA_MEXICO_CITY_ZONE_ID);
    private static final DateTimeFormatter ERROR_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(GlobalConstants.GENERAL_DATE_FORMAT);

    private DateUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String parseErrorDate() {
        return ZonedDateTime.ofInstant(Instant.now(), AMERICA_CDMX_ZONE_ID).format(ERROR_DATE_FORMATTER);
    }
}
