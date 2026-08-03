package euan.lessonbookingservice.service;

import euan.lessonbookingservice.dto.request.ResourceRequest;
import euan.lessonbookingservice.dto.response.ResourceResponse;
import euan.lessonbookingservice.entity.Resource;
import euan.lessonbookingservice.entity.ResourceType;
import euan.lessonbookingservice.entity.User;
import euan.lessonbookingservice.repository.ResourceRepository;
import euan.lessonbookingservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ResourceService {
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ResourceService(ResourceRepository resourceRepository, UserRepository userRepository) {
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    public ResourceResponse createResource(ResourceRequest request) {
        Resource resource = convertToEntity(request);
        Resource savedResource = resourceRepository.save(resource);
        return convertToDto(savedResource);
    }

    public List<ResourceResponse> getAllResources() {
        return resourceRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<ResourceResponse> getResourceById(Long id) {
        return resourceRepository.findById(id).map(this::convertToDto);
    }

    public Optional<ResourceResponse> updateResource(Long id, ResourceRequest request) {
        return resourceRepository.findById(id).map(existingResource -> {
            existingResource.setTitle(request.getTitle());
            existingResource.setDescription(request.getDescription());
            existingResource.setScheduledTime(request.getScheduledTime());
            existingResource.setResourceType(request.getResourceType());

            Optional<User> organizerOpt = userRepository.findById(request.getOrganizerId());
            organizerOpt.ifPresent(existingResource::setOrganizer);

            Resource updatedResource = resourceRepository.save(existingResource);
            return convertToDto(updatedResource);
        });
    }

    public boolean deleteResource(Long id) {
        if (resourceRepository.existsById(id)) {
            resourceRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private ResourceResponse convertToDto(Resource resource) {
        ResourceResponse dto = new ResourceResponse();
        dto.setId(resource.getId());
        dto.setTitle(resource.getTitle());
        dto.setDescription(resource.getDescription());
        dto.setOrganizerId(resource.getOrganizer() != null ? resource.getOrganizer().getId() : null);
        dto.setScheduledTime(resource.getScheduledTime());
        dto.setResourceType(resource.getResourceType());
        return dto;
    }

    private Resource convertToEntity(ResourceRequest request) {
        Resource resource = new Resource();
        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setScheduledTime(request.getScheduledTime());
        resource.setResourceType(request.getResourceType());

        Optional<User> organizerOpt = userRepository.findById(request.getOrganizerId());
        if (organizerOpt.isPresent()) {
            resource.setOrganizer(organizerOpt.get());
        } else {
            throw new IllegalArgumentException("Organizer with ID " + request.getOrganizerId() + " not found.");
        }

        return resource;
    }
}
