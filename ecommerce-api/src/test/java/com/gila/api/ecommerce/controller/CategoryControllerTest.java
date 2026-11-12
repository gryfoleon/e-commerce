package com.gila.api.ecommerce.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.gila.api.ecommerce.error.exceptions.InvalidSortException;
import com.gila.api.ecommerce.model.dto.CustomPageRequest;
import com.gila.api.ecommerce.model.dto.PageableResponse;
import com.gila.api.ecommerce.model.dto.category.CategoryRequest;
import com.gila.api.ecommerce.model.dto.category.CategoryResponse;
import com.gila.api.ecommerce.service.category.CategoryService;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class CategoryControllerTest {

    private final CategoryService service = mock(CategoryService.class);
    private final CategoryController controller = new CategoryController(service);
    private final CategoryResponse category = new CategoryResponse((short) 5, "Books", Instant.EPOCH);

    @BeforeEach
    void setUp() {
        RequestContextHolder.setRequestAttributes(
                new ServletRequestAttributes(new MockHttpServletRequest("POST", "/v1/categories")));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private Pageable listedWith(CustomPageRequest pageRequest, MockHttpServletRequest request) {
        when(service.findAll(any())).thenReturn(null);
        controller.findAll(pageRequest, request);
        ArgumentCaptor<Pageable> captor = forClass(Pageable.class);
        verify(service).findAll(captor.capture());
        return captor.getValue();
    }

    @Test
    void findAll_noSort_ordersByNameAscendingWithIdTieBreaker() {
        Pageable pageable = listedWith(new CustomPageRequest(0, 10, null, null), new MockHttpServletRequest());

        assertThat(pageable.getSort()).containsExactly(Sort.Order.asc("name"), Sort.Order.asc("id"));
    }

    @Test
    void findAll_sortByCreatedAtInAnyCase_mapsToEntityProperty() {
        Pageable pageable = listedWith(new CustomPageRequest(0, 10, "CREATEDAT", Sort.Direction.DESC),
                new MockHttpServletRequest());

        assertThat(pageable.getSort()).containsExactly(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    }

    @Test
    void findAll_unknownSort_throwsInvalidSortWithoutQueryingService() {
        assertThatThrownBy(() -> controller.findAll(new CustomPageRequest(0, 10, "price", null),
                new MockHttpServletRequest())).isInstanceOf(InvalidSortException.class);

        verifyNoInteractions(service);
    }

    @Test
    void findAll_unknownQueryParameter_isRejected() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("foo", "bar");

        assertThatThrownBy(() -> controller.findAll(new CustomPageRequest(0, 10, null, null), request))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(service);
    }

    @Test
    void findAll_returnsServicePage() {
        PageableResponse<CategoryResponse> page = PageableResponse.from(new org.springframework.data.domain.PageImpl<>(java.util.List.of(category)));
        when(service.findAll(any())).thenReturn(page);

        ResponseEntity<PageableResponse<CategoryResponse>> response =
                controller.findAll(new CustomPageRequest(0, 10, null, null), new MockHttpServletRequest());

        assertThat(response.getBody()).isSameAs(page);
    }

    @Test
    void findById_and_findByName_returnOk() {
        when(service.findById((short) 5)).thenReturn(category);
        when(service.findByName("Books")).thenReturn(category);

        assertThat(controller.findById((short) 5).getBody()).isEqualTo(category);
        assertThat(controller.findByName("Books").getBody()).isEqualTo(category);
    }

    @Test
    void create_valid_returns201WithLocation() {
        when(service.create(new CategoryRequest("Books"))).thenReturn(category);

        ResponseEntity<CategoryResponse> response = controller.create(new CategoryRequest("Books"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).hasToString("http://localhost/v1/categories/5");
        assertThat(response.getBody()).isEqualTo(category);
    }

    @Test
    void update_valid_returns200() {
        when(service.update((short) 5, new CategoryRequest("Books"))).thenReturn(category);

        assertThat(controller.update((short) 5, new CategoryRequest("Books")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    void delete_existing_returns204() {
        ResponseEntity<Void> response = controller.delete((short) 5);

        verify(service).delete((short) 5);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
