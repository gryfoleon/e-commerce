package com.gila.api.ecommerce.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gila.api.ecommerce.configuration.IntegrationTestContainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("IT")
@Import(IntegrationTestContainersConfiguration.class)
class CategoryControllerIT {

    private static final String BASE = "/v1/categories";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void findAll_returnsSortedByNameByDefault() throws Exception {
        mockMvc.perform(get(BASE).param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].name").value("Accessories"))
                .andExpect(jsonPath("$.content[1].name").value("Beauty"))
                .andExpect(jsonPath("$.content[2].name").value("Books"))
                .andExpect(jsonPath("$.pageDetails.page").value(0))
                .andExpect(jsonPath("$.pageDetails.size").value(3))
                .andExpect(jsonPath("$.pageDetails.totalElements", greaterThanOrEqualTo(18)))
                .andExpect(jsonPath("$.pageDetails.isLast").value(false));
    }

    @Test
    void findAll_supportsDescendingSort() throws Exception {
        String last = "Zzzz-" + UUID.randomUUID().toString().substring(0, 8);
        createCategory(last);

        mockMvc.perform(get(BASE).param("size", "1").param("sortBy", "name").param("sortDirection", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value(last));
    }

    @Test
    void findAll_rejectsUnknownSortField() throws Exception {
        mockMvc.perform(get(BASE).param("sortBy", "unknown"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_rejectsUnknownQueryParameter() throws Exception {
        mockMvc.perform(get(BASE).param("foo", "bar"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findAll_rejectsInvalidPageSize() throws Exception {
        mockMvc.perform(get(BASE).param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findById_returnsCategory() throws Exception {
        int id = createCategory(uniqueName("ById")).get("id").asInt();

        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void findById_returnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", 30000))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    void findById_returnsBadRequestForNonNumericId() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findByName_isCaseInsensitive() throws Exception {
        mockMvc.perform(get(BASE + "/name/{name}", "eLeCtRoNiCs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    void findByName_returnsNotFoundForUnknownName() throws Exception {
        mockMvc.perform(get(BASE + "/name/{name}", "Does Not Exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    void create_returnsCreatedWithLocation() throws Exception {
        String name = uniqueName("Create");

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body(name)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/" + findIdByName(name))))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void create_stripsNameWhitespace() throws Exception {
        String name = uniqueName("Strip");

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body("  " + name + "  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name));
    }

    @Test
    void create_returnsConflictForDuplicateNameIgnoringCase() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body("ELECTRONICS")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409));
    }

    @Test
    void create_returnsBadRequestForBlankName() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body("   ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returnsBadRequestForTooLongName() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body("x".repeat(101))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_renamesCategory() throws Exception {
        int id = createCategory(uniqueName("Before")).get("id").asInt();
        String newName = uniqueName("After");

        mockMvc.perform(put(BASE + "/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body(newName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(newName));

        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(newName));
    }

    @Test
    void update_allowsChangingOnlyTheCaseOfOwnName() throws Exception {
        String name = uniqueName("Case");
        short id = createCategory(name).get("id").asShort();

        mockMvc.perform(put(BASE + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(body(name.toUpperCase())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name.toUpperCase()));
    }

    @Test
    void update_returnsConflictWhenNameBelongsToAnotherCategory() throws Exception {
        short id = createCategory(uniqueName("Conflict")).get("id").asShort();

        mockMvc.perform(put(BASE + "/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body("Books")))
                .andExpect(status().isConflict());
    }

    @Test
    void update_returnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(put(BASE + "/{id}", 30000).contentType(MediaType.APPLICATION_JSON)
                        .content(body(uniqueName("Missing"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void update_returnsBadRequestForBlankName() throws Exception {
        short id = createCategory(uniqueName("Blank")).get("id").asShort();

        mockMvc.perform(put(BASE + "/{id}", id).contentType(MediaType.APPLICATION_JSON).content(body("")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_removesCategory() throws Exception {
        short id = createCategory(uniqueName("Delete")).get("id").asShort();

        mockMvc.perform(delete(BASE + "/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void delete_returnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(delete(BASE + "/{id}", 30000)).andExpect(status().isNotFound());
    }

    @Test
    void delete_returnsConflictWhenCategoryHasProducts() throws Exception {
        short electronicsId = findIdByName("Electronics");

        mockMvc.perform(delete(BASE + "/{id}", electronicsId)).andExpect(status().isConflict());

        mockMvc.perform(get(BASE + "/{id}", electronicsId)).andExpect(status().isOk());
    }

    private JsonNode createCategory(String name) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString());
    }

    private short findIdByName(String name) throws Exception {
        MvcResult result = mockMvc.perform(get(BASE + "/name/{name}", name)).andExpect(status().isOk()).andReturn();
        return jsonMapper.readTree(result.getResponse().getContentAsString()).get("id").asShort();
    }

    private String body(String name) {
        return jsonMapper.createObjectNode().put("name", name).toString();
    }

    private static String uniqueName(String prefix) {
        return "Zz-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
