package euan.bookingservice.resources.service;

import euan.bookingservice.resources.dto.request.ResourceRequest;
import euan.bookingservice.resources.dto.response.ResourceResponse;
import euan.bookingservice.resources.entity.Resource;
import euan.bookingservice.resources.exception.ResourceOwnerNotFoundException;
import euan.bookingservice.resources.repository.ResourceRepository;
import euan.bookingservice.users.port.UserLookup;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ResourceService {
    private final ResourceRepository resourceRepository;
    private final UserLookup userLookup;

    public ResourceService(ResourceRepository resourceRepository, UserLookup userLookup) {
        this.resourceRepository = resourceRepository;
        this.userLookup = userLookup;
    }

    public ResourceResponse createResource(ResourceRequest request) {
        if (!userLookup.existsById(request.getOwnerId())) {
            throw new ResourceOwnerNotFoundException(
                    "Owner with ID " + request.getOwnerId() + " not found."
            );
        }
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
        if (!userLookup.existsById(request.getOwnerId())) {
            throw new ResourceOwnerNotFoundException(
                    "Owner with ID " + request.getOwnerId() + " not found."
            );
        }

        return resourceRepository.findById(id).map(existingResource -> {
            existingResource.setTitle(request.getTitle());
            existingResource.setDescription(request.getDescription());
            existingResource.setResourceType(request.getResourceType());
            existingResource.setOwnerId(request.getOwnerId());

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
        dto.setOwnerId(resource.getOwnerId());
        dto.setResourceType(resource.getResourceType());
        return dto;
    }

    private Resource convertToEntity(ResourceRequest request) {
        Resource resource = new Resource();
        resource.setTitle(request.getTitle());
        resource.setDescription(request.getDescription());
        resource.setResourceType(request.getResourceType());
        resource.setOwnerId(request.getOwnerId());

        return resource;
    }
}
