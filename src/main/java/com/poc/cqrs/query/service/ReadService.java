package com.poc.cqrs.query.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReadService<T> {

    Page<T> findAll(Pageable pageable);

    List<T> findAll();

    T findById(Object id);
    
    List<T> findListById(Object id);
}
