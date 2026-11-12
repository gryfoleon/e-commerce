package com.gila.api.ecommerce.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Sort;
import org.springframework.format.support.DefaultFormattingConversionService;

class WebConfigurationSortDirectionTest {

    private final WebConfiguration.SortDirectionConverter converter = new WebConfiguration.SortDirectionConverter();

    @ParameterizedTest
    @CsvSource({"asc,ASC", "ASC,ASC", "Asc,ASC", "desc,DESC", "DESC,DESC", "' desc ',DESC"})
    void convert_validDirectionInAnyCase_returnsDirection(String source, Sort.Direction expected) {
        assertThat(converter.convert(source)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"up", "ascending", "''"})
    void convert_invalidDirection_throwsIllegalArgument(String source) {
        assertThatThrownBy(() -> converter.convert(source)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addFormatters_registersCaseInsensitiveConverterOnTheConversionService() {
        DefaultFormattingConversionService service = new DefaultFormattingConversionService();
        new WebConfiguration(null).addFormatters(service);

        assertThat(service.convert("desc", Sort.Direction.class)).isEqualTo(Sort.Direction.DESC);
        assertThat(service.convert("aSc", Sort.Direction.class)).isEqualTo(Sort.Direction.ASC);
        assertThat(DefaultConversionService.getSharedInstance()).isNotNull();
    }
}
