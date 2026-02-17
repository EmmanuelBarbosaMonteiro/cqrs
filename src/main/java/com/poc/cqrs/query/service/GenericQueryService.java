package com.poc.cqrs.query.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public class GenericQueryService<T, ID> {

    private final JpaRepository<T, ID> repository;
    private final JpaSpecificationExecutor<T> specExecutor;

    public GenericQueryService(JpaRepository<T, ID> repository) {
        this.repository = repository;
        this.specExecutor = (JpaSpecificationExecutor<T>) repository;
    }

    public Page<T> findAll(Specification<T> spec, Pageable pageable) {
        if (spec == null) {
            return repository.findAll(pageable);
        }
        return specExecutor.findAll(spec, pageable);
    }
    
    public T findById(ID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro não encontrado: " + id));
    }
}
