package com.gila.api.ecommerce.controller;

import com.gila.api.ecommerce.configuration.constants.GlobalConstants;
import com.gila.api.ecommerce.error.exceptions.InvalidSortException;
import com.gila.api.ecommerce.model.dto.CustomPageRequest;
import com.gila.api.ecommerce.model.dto.PageableResponse;
import com.gila.api.ecommerce.model.dto.category.CategoryRequest;
import com.gila.api.ecommerce.model.dto.category.CategoryResponse;
import com.gila.api.ecommerce.model.error.CustomErrorResponse;
import com.gila.api.ecommerce.service.category.CategoryService;
import com.gila.api.ecommerce.util.CommonUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import static com.gila.api.ecommerce.util.QueryParameters.requireOnly;

@RestController
@RequestMapping(GlobalConstants.API_V1_CATEGORIES)
@Tag(name = "Categories", description = "Product category catalog")
public class CategoryController {

    private static final String DEFAULT_SORT = "name";
    private static final Map<String, String> SORT_PROPERTIES = Map.of("name", "name", "createdat", "createdAt");

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List categories",
            description = "Paginated listing. sortBy: name (default, ASC) or createdAt. Unknown query parameters "
                    + "are rejected with 400.")
    @ApiResponse(responseCode = "200", description = "Category page, possibly empty")
    @ApiResponse(responseCode = "400", description = "Invalid pagination, sort or unknown parameter",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    public ResponseEntity<PageableResponse<CategoryResponse>> findAll(
            @ParameterObject @Valid @ModelAttribute CustomPageRequest pageRequest,
            @Parameter(hidden = true) HttpServletRequest request) {
        requireOnly(request, Set.of());
        return ResponseEntity.ok(categoryService.findAll(toPageable(pageRequest)));
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a category by id")
    @ApiResponse(responseCode = "200", description = "Category found")
    @ApiResponse(responseCode = "400", description = "The identifier is not a valid number",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    public ResponseEntity<CategoryResponse> findById(
            @Parameter(description = "Category id", example = "5") @PathVariable Short id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @GetMapping(value = "/name/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a category by name", description = "Case-insensitive exact match.")
    @ApiResponse(responseCode = "200", description = "Category found")
    @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    public ResponseEntity<CategoryResponse> findByName(
            @Parameter(description = "Category name", example = "Electronics") @PathVariable String name) {
        return ResponseEntity.ok(categoryService.findByName(name));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a category")
    @ApiResponse(responseCode = "201", description = "Category created")
    @ApiResponse(responseCode = "400", description = "Invalid payload",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "A category with that name already exists",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a category",
            description = "Renames the category; products referencing it follow the new name.")
    @ApiResponse(responseCode = "200", description = "Category updated")
    @ApiResponse(responseCode = "400", description = "Invalid payload or identifier",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "A category with that name already exists",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    public ResponseEntity<CategoryResponse> update(
            @Parameter(description = "Category id", example = "5") @PathVariable Short id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category", description = "Rejected with 409 while products reference it.")
    @ApiResponse(responseCode = "204", description = "Category deleted")
    @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Category is still referenced by products",
            content = @Content(schema = @Schema(implementation = CustomErrorResponse.class)))
    public ResponseEntity<Void> delete(
            @Parameter(description = "Category id", example = "5") @PathVariable Short id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private static Pageable toPageable(CustomPageRequest request) {
        String property = request.sortBy() == null
                ? DEFAULT_SORT
                : SORT_PROPERTIES.get(request.sortBy().toLowerCase(Locale.ROOT));
        if (property == null) {
            throw new InvalidSortException(request.sortBy(), List.of("name", "createdAt"));
        }
        return CommonUtils.buildPageable(request, property, Sort.Direction.ASC, "id");
    }
}
