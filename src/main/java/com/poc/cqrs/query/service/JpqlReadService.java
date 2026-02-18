package com.poc.cqrs.query.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class JpqlReadService<T> implements ReadService<T> {

    private final Supplier<List<T>> findAllFn;
    private final Function<Pageable, Page<T>> findAllPagedFn;
    private final Function<Object, Optional<T>> findByIdFn;
    private final Function<Object, List<T>> findListByIdFn;

    private JpqlReadService(Builder<T> builder) {
        this.findAllFn = builder.findAllFn;
        this.findAllPagedFn = builder.findAllPagedFn;
        this.findByIdFn = builder.findByIdFn;
        this.findListByIdFn = builder.findListByIdFn;
    }

    @Override
    public Page<T> findAll(Pageable pageable) {
        return findAllPagedFn.apply(pageable);
    }

    @Override
    public List<T> findAll() {
        return findAllFn.get();
    }

    @Override
    public T findById(Object id) {
        return findByIdFn.apply(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro não encontrado: " + id));
    }

    @Override
    public List<T> findListById(Object id) {
        return findListByIdFn.apply(id);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private Supplier<List<T>> findAllFn = unsupportedSupplier("findAll");
        private Function<Pageable, Page<T>> findAllPagedFn;
        private Function<Object, Optional<T>> findByIdFn = unsupportedFn("findById");
        private Function<Object, List<T>> findListByIdFn = unsupportedFn("findListById");
        private boolean pagedExplicitlySet = false;

        public Builder<T> findAll(Supplier<List<T>> fn) {
            this.findAllFn = fn;
            // Se paginação não foi definida explicitamente, cria uma baseada no findAll
            if (!pagedExplicitlySet) {
                this.findAllPagedFn = pageable -> {
                    var all = fn.get();
                    int start = (int) pageable.getOffset();
                    int end = Math.min(start + pageable.getPageSize(), all.size());
                    var content = start >= all.size() ? List.<T>of() : all.subList(start, end);
                    return new PageImpl<>(content, pageable, all.size());
                };
            }
            return this;
        }

        public Builder<T> findAllPaged(Function<Pageable, Page<T>> fn) {
            this.findAllPagedFn = fn;
            this.pagedExplicitlySet = true;
            return this;
        }

        public Builder<T> findById(Function<Object, Optional<T>> fn) {
            this.findByIdFn = fn;
            return this;
        }

        public Builder<T> findListById(Function<Object, List<T>> fn) {
            this.findListByIdFn = fn;
            return this;
        }

        public JpqlReadService<T> build() {
            if (this.findAllPagedFn == null) {
                this.findAllPagedFn = unsupportedFn("findAll(Pageable)");
            }
            return new JpqlReadService<>(this);
        }

        private static <R> Supplier<R> unsupportedSupplier(String method) {
            return () -> {
                throw new UnsupportedOperationException(method + " não configurado para esta view.");
            };
        }

        @SuppressWarnings("unchecked")
        private static <I, R> Function<I, R> unsupportedFn(String method) {
            Function<?, ?> fn = id -> {
                throw new UnsupportedOperationException(method + " não configurado para esta view.");
            };
            return (Function<I, R>) fn;
        }
    }
}
