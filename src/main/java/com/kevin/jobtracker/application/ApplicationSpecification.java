package com.kevin.jobtracker.application;

import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class ApplicationSpecification {

    public static Specification<JobApplication> belongsToUser(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<JobApplication> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    public static Specification<JobApplication> hasStatus(ApplicationStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<JobApplication> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("companyName")), pattern),
                    cb.like(cb.lower(root.get("jobTitle")), pattern)
            );
        };
    }
}
