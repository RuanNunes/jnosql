/*
 *  Copyright (c) 2023 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.mapping.core;


import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * A JNoSQL implementation of {@link  Page}
 *
 * @param <T> the entity type
 */
public class NoSQLPage<T> implements Page<T> {

    private final List<T> entities;

    private final PageRequest<T> pageRequest;

    private NoSQLPage(List<T> entities, PageRequest<T> pageRequest) {
        this.entities = entities;
        this.pageRequest = pageRequest;
    }

    @Override
    public long totalElements() {
        throw new UnsupportedOperationException("JNoSQL has no support for this feature yet");
    }

    @Override
    public long totalPages() {
        throw new UnsupportedOperationException("JNoSQL has no support for this feature yet");
    }

    @Override
    public List<T> content() {
        return Collections.unmodifiableList(entities);
    }

    @Override
    public boolean hasContent() {
        return !this.entities.isEmpty();
    }

    @Override
    public int numberOfElements() {
        return this.entities.size();
    }

    @Override
    public boolean hasNext() {
        throw new UnsupportedOperationException("Eclipse JNoSQL has no support for this feature hasNext");
    }

    @Override
    public boolean hasPrevious() {
       throw new UnsupportedOperationException("Eclipse JNoSQL has no support for this feature hasPrevious");
    }

    @Override
    public PageRequest<T> pageRequest() {
        return this.pageRequest;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> PageRequest<E> pageRequest(Class<E> type) {
        Objects.requireNonNull(type, "type is required");
        return (PageRequest<E>) this.pageRequest;
    }

    @Override
    public PageRequest<T> nextPageRequest() {
        return this.pageRequest.next();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E> PageRequest<E> nextPageRequest(Class<E> type) {
        Objects.requireNonNull(type, "type is required");
        return (PageRequest<E>) this.pageRequest.next();
    }

    @Override
    public PageRequest<T> previousPageRequest() {
        return this.pageRequest.previous();
    }

    @Override
    public <E> PageRequest<E> previousPageRequest(Class<E> type) {
        Objects.requireNonNull(type, "type is required");
        return (PageRequest<E>) this.pageRequest.previous();
    }

    @Override
    public boolean hasTotals() {
        throw new UnsupportedOperationException("Eclipse JNoSQL has no support for this feature hasTotals");
    }

    @Override
    public Iterator<T> iterator() {
        return this.entities.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NoSQLPage<?> noSQLPage = (NoSQLPage<?>) o;
        return Objects.equals(entities, noSQLPage.entities) && Objects.equals(pageRequest, noSQLPage.pageRequest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entities, pageRequest);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("NoSQLPage{");
        sb.append("entities=").append(entities);
        sb.append(", pageRequest=").append(pageRequest);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Creates a {@link  Page} implementation from entities and a PageRequest
     * @param entities the entities
     * @param pageRequest the PageRequest
     * @return a {@link Page} instance
     * @param <T> the entity type
     */
    public static <T> Page<T> of(List<T> entities, PageRequest<T> pageRequest) {
        Objects.requireNonNull(entities, "entities is required");
        Objects.requireNonNull(pageRequest, "pageRequest is required");
        return new NoSQLPage<>(entities, pageRequest);
    }

    /**
     * Create skip formula from pageRequest instance
     * @param pageRequest the pageRequest
     * @param <T> the entity type
     * @return the skip
     * @throws NullPointerException when parameter is null
     */
    public static <T>  long skip(PageRequest<T> pageRequest) {
        Objects.requireNonNull(pageRequest, "pageRequest is required");
        return pageRequest.size() * (pageRequest.page() - 1);
    }
}
