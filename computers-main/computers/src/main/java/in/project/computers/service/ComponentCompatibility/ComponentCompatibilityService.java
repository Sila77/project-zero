package in.project.computers.service.ComponentCompatibility;


import in.project.computers.dto.builds.CompatibilityResult;

public interface ComponentCompatibilityService {

    CompatibilityResult checkCompatibility(String buildId);
}