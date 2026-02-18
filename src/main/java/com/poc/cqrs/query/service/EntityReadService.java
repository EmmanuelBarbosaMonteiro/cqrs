package com.poc.cqrs.query.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public class EntityReadService<T, ID> implements ReadService<T> {

    private final JpaRepository<T, ID> repository;
    private final JpaSpecificationExecutor<T> specExecutor;

    public EntityReadService(JpaRepository<T, ID> repository) {
        this.repository = repository;
        this.specExecutor = (JpaSpecificationExecutor<T>) repository;
    }
    
    public Page<T> findAll(Specification<T> spec, Pageable pageable) {
        if (spec == null) {
            return repository.findAll(pageable);
        }
        return specExecutor.findAll(spec, pageable);
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Override
    public List<T> findAll() {
        return repository.findAll();
    }

    @Override
    @SuppressWarnings("unchecked")
    public T findById(Object id) {
        return repository.findById((ID) id)
                .orElseThrow(() -> new IllegalArgumentException("Registro não encontrado: " + id));
    }

    @Override
    public List<T> findListById(Object id) {
        throw new UnsupportedOperationException("Use findById para entidades.");
    }
}
