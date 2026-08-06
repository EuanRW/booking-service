package euan.bookingservice.resources.controller;

import euan.bookingservice.resources.entity.Resource;
import euan.bookingservice.resources.entity.ResourceType;
import euan.bookingservice.resources.repository.ResourceRepository;
import euan.bookingservice.users.entity.User;
import euan.bookingservice.users.repository.UserRepository;
import euan.bookingservice.bookings.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = Replace.ANY)
class ResourceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
        resource.setResourceType(ResourceType.EVENT);
        resource.setOwner(owner);
        return resourceRepository.save(resource);
    }

    @Test
    void createResource_withoutAuthentication_returnsUnauthorized() throws Exception {

        User owner = createOwner();

        String payload = """
                {
                  "title":"Room A",
                  "description":"Meeting room",
                  "resourceType":"ROOM",
                  "ownerId":%d
                }
                """.formatted(owner.getId());

        mockMvc.perform(post("/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createResource_asUser_returnsForbidden() throws Exception {

        User owner = createOwner();

        String payload = """
                {
                  "title":"Room A",
                  "description":"Meeting room",
                  "resourceType":"ROOM",
                  "ownerId":%d
                }
                """.formatted(owner.getId());

        mockMvc.perform(post("/resources")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void createResource_asOrganizer_createsResource() throws Exception {

        User owner = createOwner();

        String payload = """
                {
                  "title":"Room A",
                  "description":"Meeting room",
                  "resourceType":"ROOM",
                  "ownerId":%d
                }
                """.formatted(owner.getId());

        mockMvc.perform(post("/resources")
                        .with(SecurityMockMvcRequestPostProcessors.user("organizer").roles("ORGANIZER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Room A")))
                .andExpect(jsonPath("$.description", is("Meeting room")))
                .andExpect(jsonPath("$.resourceType", is("ROOM")))
                .andExpect(jsonPath("$.ownerId", is(owner.getId().intValue())));
    }

    @Test
    void createResource_asAdmin_createsResource() throws Exception {

        User owner = createOwner();

        String payload = """
                {
                  "title":"Projector",
                  "description":"HD projector",
                  "resourceType":"EQUIPMENT",
                  "ownerId":%d
                }
                """.formatted(owner.getId());

        mockMvc.perform(post("/resources")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Projector")));
    }

    @Test
    void getAllResources_asUser_returnsResources() throws Exception {

        User owner = createOwner();
        createResource(owner);

        mockMvc.perform(get("/resources")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(1)))
                .andExpect(jsonPath("$[0].title", is("Room A")));
    }

    @Test
    void getAllResources_asUser_whenEmpty_returnsEmptyList() throws Exception {

        mockMvc.perform(get("/resources")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", is(0)));
    }

    @Test
    void getResourceById_asUser_returnsResource() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        mockMvc.perform(get("/resources/" + resource.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Room A")))
                .andExpect(jsonPath("$.ownerId", is(owner.getId().intValue())));
    }

    @Test
    void getResourceById_asUser_whenMissing_returnsNotFound() throws Exception {

        mockMvc.perform(get("/resources/999")
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void updateResource_withoutAuthentication_returnsUnauthorized() throws Exception {

        String payload = """
                {
                  "title":"Updated",
                  "description":"Updated",
                  "resourceType":"ROOM",
                  "ownerId":1
                }
                """;

        mockMvc.perform(put("/resources/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateResource_asUser_returnsForbidden() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        String payload = """
                {
                  "title":"Updated",
                  "description":"Updated",
                  "resourceType":"ROOM",
                  "ownerId":%d
                }
                """.formatted(owner.getId());

        mockMvc.perform(put("/resources/" + resource.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateResource_asOrganizer_updatesResource() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        String payload = """
                {
                  "title":"Updated Room",
                  "description":"Updated description",
                  "resourceType":"ROOM",
                  "ownerId":%d
                }
                """.formatted(owner.getId());

        mockMvc.perform(put("/resources/" + resource.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user("organizer").roles("ORGANIZER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Updated Room")));
    }

    @Test
    void updateResource_whenNotFound_returnsNotFound() throws Exception {

        User owner = createOwner();

        String payload = """
                {
                  "title":"Updated",
                  "description":"Updated",
                  "resourceType":"ROOM",
                  "ownerId":%d
                }
                """.formatted(owner.getId());

        mockMvc.perform(put("/resources/999")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
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
                        .with(SecurityMockMvcRequestPostProcessors.user("user").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteResource_asOrganizer_deletesResource() throws Exception {

        User owner = createOwner();
        Resource resource = createResource(owner);

        mockMvc.perform(delete("/resources/" + resource.getId())
                        .with(SecurityMockMvcRequestPostProcessors.user("organizer").roles("ORGANIZER")))
                .andExpect(status().isNoContent());

        assertThat(resourceRepository.findById(resource.getId())).isEmpty();
    }

    @Test
    void deleteResource_whenMissing_returnsNotFound() throws Exception {

        mockMvc.perform(delete("/resources/999")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
}