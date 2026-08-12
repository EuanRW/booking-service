package euan.bookingservice.resources.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import euan.bookingservice.bookings.repository.BookingRepository;
import euan.bookingservice.resources.entity.Resource;
import euan.bookingservice.resources.entity.ResourceType;
import euan.bookingservice.resources.repository.ResourceRepository;
import euan.bookingservice.users.entity.User;
import euan.bookingservice.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.ANY)
class ResourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        resourceRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User createOwner() {
        User user = new User();
        user.setUsername("organizer");
        user.setPassword("pw");
        user.setRole("ORGANIZER");
        return userRepository.save(user);
    }

    private Resource createResource(User owner) {
        Resource resource = new Resource();
        resource.setTitle("Room A");
        resource.setDescription("Meeting room");
        resource.setResourceType(ResourceType.ROOM);
        resource.setOwnerId(owner.getId());
        resource.setCapacity(1);
        return resourceRepository.save(resource);
    }

    private String resourcePayload(
            String title,
            String description,
            ResourceType resourceType,
            Long ownerId
    ) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "title", title,
                "description", description,
                "resourceType", resourceType,
                "ownerId", ownerId
        ));
    }

    private RequestPostProcessor regularUser() {
        return user("user").roles("USER");
    }

    private RequestPostProcessor organizer() {
        return user("organizer").roles("ORGANIZER");
    }

    private RequestPostProcessor admin() {
        return user("admin").roles("ADMIN");
    }

    @Test
    void createResource_withoutAuthentication_returnsUnauthorized() throws Exception {
        User owner = createOwner();

        mockMvc.perform(post("/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourcePayload(
                                "Room A",
                                "Meeting room",
                                ResourceType.ROOM,
                                owner.getId()
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createResource_asUser_returnsForbidden() throws Exception {
        User owner = createOwner();

        mockMvc.perform(post("/resources")
                        .with(regularUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourcePayload(
                                "Room A",
                                "Meeting room",
                                ResourceType.ROOM,
                                owner.getId()
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void createResource_asOrganizer_createsResource() throws Exception {
        User owner = createOwner();

        mockMvc.perform(post("/resources")
                        .with(organizer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourcePayload(
                                "Room A",
                                "Meeting room",
                                ResourceType.ROOM,
                                owner.getId()
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Room A")))
                .andExpect(jsonPath("$.description", is("Meeting room")))
                .andExpect(jsonPath("$.resourceType", is("ROOM")))
                .andExpect(jsonPath("$.ownerId", is(owner.getId().intValue())));

        assertThat(resourceRepository.findAll())
                .singleElement()
                .satisfies(resource -> {
                    assertThat(resource.getTitle()).isEqualTo("Room A");
                    assertThat(resource.getDescription()).isEqualTo("Meeting room");
                    assertThat(resource.getResourceType()).isEqualTo(ResourceType.ROOM);
                    assertThat(resource.getOwnerId()).isEqualTo(owner.getId());
                });
    }

    @Test
    void createResource_asAdmin_createsResource() throws Exception {
        User owner = createOwner();

        mockMvc.perform(post("/resources")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourcePayload(
                                "Projector",
                                "HD projector",
                                ResourceType.EQUIPMENT,
                                owner.getId()
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Projector")));

        assertThat(resourceRepository.findAll())
                .singleElement()
                .satisfies(resource -> {
                    assertThat(resource.getTitle()).isEqualTo("Projector");
                    assertThat(resource.getDescription()).isEqualTo("HD projector");
                    assertThat(resource.getResourceType()).isEqualTo(ResourceType.EQUIPMENT);
                    assertThat(resource.getOwnerId()).isEqualTo(owner.getId());
                });
    }

    @Test
    void createResource_whenOwnerDoesNotExist_returnsNotFound() throws Exception {
        Long missingOwnerId = 999L;

        mockMvc.perform(post("/resources")
                        .with(organizer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourcePayload(
                                "Room A",
                                "Meeting room",
                                ResourceType.ROOM,
                                missingOwnerId
                        )))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(not(emptyOrNullString())));
    }

    @Test
    void getAllResources_asUser_returnsResources() throws Exception {
        User owner = createOwner();
        createResource(owner);

        mockMvc.perform(get("/resources")
                        .with(regularUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].title", is("Room A")));
    }

    @Test
    void getAllResources_asUser_whenEmpty_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/resources")
                        .with(regularUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void getResourceById_asUser_returnsResource() throws Exception {
        User owner = createOwner();
        Resource resource = createResource(owner);

        mockMvc.perform(get("/resources/" + resource.getId())
                        .with(regularUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Room A")))
                .andExpect(jsonPath("$.ownerId", is(owner.getId().intValue())));
    }

    @Test
    void getResourceById_asUser_whenMissing_returnsNotFound() throws Exception {
        mockMvc.perform(get("/resources/999")
                        .with(regularUser()))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateResource_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(put("/resources/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourcePayload(
                                "Updated",
                                "Updated",
                                ResourceType.ROOM,
                                1L
                        )))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateResource_asUser_returnsForbidden() throws Exception {
        User owner = createOwner();
        Resource resource = createResource(owner);

        mockMvc.perform(put("/resources/" + resource.getId())
                        .with(regularUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourcePayload(
                                "Updated",
                                "Updated",
                                ResourceType.ROOM,
                                owner.getId()
                        )))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateResource_asOrganizer_updatesResource() throws Exception {
        User owner = createOwner();
        Resource resource = createResource(owner);

        mockMvc.perform(put("/resources/" + resource.getId())
                        .with(organizer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourcePayload(
                                "Updated Room",
                                "Updated description",
                                ResourceType.ROOM,
                                owner.getId()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Room")));

        Resource updatedResource = resourceRepository.findById(resource.getId()).orElseThrow();

        assertThat(updatedResource.getTitle()).isEqualTo("Updated Room");
        assertThat(updatedResource.getDescription()).isEqualTo("Updated description");
        assertThat(updatedResource.getResourceType()).isEqualTo(ResourceType.ROOM);
        assertThat(updatedResource.getOwnerId()).isEqualTo(owner.getId());
    }

    @Test
    void updateResource_whenNotFound_returnsNotFound() throws Exception {
        User owner = createOwner();

        mockMvc.perform(put("/resources/999")
                        .with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourcePayload(
                                "Updated",
                                "Updated",
                                ResourceType.ROOM,
                                owner.getId()
                        )))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateResource_whenOwnerDoesNotExist_returnsNotFound() throws Exception {
        User owner = createOwner();
        Resource resource = createResource(owner);
        Long missingOwnerId = 999L;

        mockMvc.perform(put("/resources/" + resource.getId())
                        .with(organizer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resourcePayload(
                                "Updated Room",
                                "Updated description",
                                ResourceType.ROOM,
                                missingOwnerId
                        )))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(not(emptyOrNullString())));
    }

    @Test
    void deleteResource_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/resources/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteResource_asUser_returnsForbidden() throws Exception {
        User owner = createOwner();
        Resource resource = createResource(owner);

        mockMvc.perform(delete("/resources/" + resource.getId())
                        .with(regularUser()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteResource_asOrganizer_deletesResource() throws Exception {
        User owner = createOwner();
        Resource resource = createResource(owner);

        mockMvc.perform(delete("/resources/" + resource.getId())
                        .with(organizer()))
                .andExpect(status().isNoContent());

        assertThat(resourceRepository.findById(resource.getId())).isEmpty();
    }

    @Test
    void deleteResource_whenMissing_returnsNotFound() throws Exception {
        mockMvc.perform(delete("/resources/999")
                        .with(admin()))
                .andExpect(status().isNotFound());
    }
}