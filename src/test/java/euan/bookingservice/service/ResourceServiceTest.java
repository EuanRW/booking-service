package euan.bookingservice.service;

import euan.bookingservice.dto.request.ResourceRequest;
import euan.bookingservice.dto.response.ResourceResponse;
import euan.bookingservice.entity.Resource;
import euan.bookingservice.entity.ResourceType;
import euan.bookingservice.entity.User;
import euan.bookingservice.repository.ResourceRepository;
import euan.bookingservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {
    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ResourceService resourceService;

    @Test
    void createResourcePersistsGenericResource() {
        User organizer = new User();
        organizer.setId(2L);
        organizer.setUsername("teacher");
        organizer.setRole("ORGANIZER");

        ResourceRequest request = new ResourceRequest();
        request.setTitle("Room A");
        request.setDescription("Boardroom");
        request.setOrganizerId(2L);
        request.setScheduledTime(LocalDateTime.of(2026, 8, 7, 10, 0));
        request.setResourceType(ResourceType.ROOM);

        Resource savedResource = new Resource();
        savedResource.setId(7L);
        savedResource.setTitle(request.getTitle());
        savedResource.setDescription(request.getDescription());
        savedResource.setOrganizer(organizer);
        savedResource.setScheduledTime(request.getScheduledTime());
        savedResource.setResourceType(request.getResourceType());

        when(userRepository.findById(2L)).thenReturn(Optional.of(organizer));
        when(resourceRepository.save(any(Resource.class))).thenReturn(savedResource);

        ResourceResponse response = resourceService.createResource(request);

        assertEquals(7L, response.getId());
        assertEquals("Room A", response.getTitle());
        assertEquals(ResourceType.ROOM, response.getResourceType());
        assertEquals(2L, response.getOrganizerId());
        verify(resourceRepository).save(any(Resource.class));
    }
}
