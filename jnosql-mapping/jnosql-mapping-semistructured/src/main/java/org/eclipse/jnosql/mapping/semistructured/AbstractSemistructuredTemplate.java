/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.jnosql.mapping.semistructured;


import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.PageRequest;
import jakarta.data.page.impl.CursoredPageRecord;
import jakarta.nosql.QueryMapper;

import org.eclipse.jnosql.communication.semistructured.CommunicationEntity;
import org.eclipse.jnosql.communication.semistructured.CommunicationObserverParser;
import org.eclipse.jnosql.communication.semistructured.DatabaseManager;
import org.eclipse.jnosql.communication.semistructured.DeleteQuery;
import org.eclipse.jnosql.communication.semistructured.QueryParser;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.IdNotFoundException;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.metadata.EntityMetadata;
import org.eclipse.jnosql.mapping.metadata.FieldMetadata;
import org.eclipse.jnosql.mapping.core.util.ConverterUtil;
import org.eclipse.jnosql.mapping.metadata.InheritanceMetadata;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

/**
 * An abstract implementation of the {@link SemistructuredTemplate} interface providing
 * a template method for working with semi-structured NoSQL databases.
 * Concrete subclasses must implement methods to provide necessary dependencies and configuration.
 *
 * @see SemistructuredTemplate
 */
public abstract class AbstractSemistructuredTemplate implements SemistructuredTemplate {

    private static final QueryParser PARSER = new QueryParser();

    /**
     * Retrieves the converter used to convert between entity objects and communication entities.
     *
     * @return the entity converter
     */
    protected abstract EntityConverter converter();

    /**
     * Retrieves the manager responsible for database operations.
     *
     * @return the database manager
     */
    protected abstract DatabaseManager manager();

    /**
     * Retrieves the manager responsible for persisting column events.
     *
     * @return the event manager
     */
    protected abstract EventPersistManager eventManager();


    protected abstract EntitiesMetadata entities();

    /**
     * Retrieves the converters for handling entity conversions.
     *
     * @return the converters
     */
    protected abstract Converters converters();

    private final UnaryOperator<CommunicationEntity> insert = e -> manager().insert(e);

    private final UnaryOperator<CommunicationEntity> update = e -> manager().update(e);

    private CommunicationObserverParser observer;


    private CommunicationObserverParser getObserver() {
        if (Objects.isNull(observer)) {
            observer = new MapperObserver(entities());
        }
        return observer;
    }

    @Override
    public <T> T insert(T entity) {
        requireNonNull(entity, "entity is required");
        return persist(entity, insert);
    }


    @Override
    public <T> T insert(T entity, Duration ttl) {
        requireNonNull(entity, "entity is required");
        requireNonNull(ttl, "ttl is required");
        return persist(entity, e -> manager().insert(e, ttl));
    }


    @Override
    public <T> T update(T entity) {
        requireNonNull(entity, "entity is required");
        return persist(entity, update);
    }

    @Override
    public <T> Iterable<T> update(Iterable<T> entities) {
        requireNonNull(entities, "entity is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::update).collect(Collectors.toList());
    }

    @Override
    public <T> Iterable<T> insert(Iterable<T> entities) {
        requireNonNull(entities, "entities is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::insert).collect(Collectors.toList());
    }

    @Override
    public <T> Iterable<T> insert(Iterable<T> entities, Duration ttl) {
        requireNonNull(entities, "entities is required");
        requireNonNull(ttl, "ttl is required");
        return StreamSupport.stream(entities.spliterator(), false)
                .map(e -> insert(e, ttl))
                .collect(Collectors.toList());
    }

    @Override
    public void delete(DeleteQuery query) {
        requireNonNull(query, "query is required");
        manager().delete(query);
    }


    @Override
    public <T> Stream<T> select(SelectQuery query) {
        requireNonNull(query, "query is required");
        return executeQuery(query);
    }

    @Override
    public long count(SelectQuery query) {
        return manager().count(query);
    }

    @Override
    public boolean exists(SelectQuery query) {
        return manager().exists(query);
    }

    @Override
    public <T> Optional<T> singleResult(SelectQuery query) {
        requireNonNull(query, "query is required");
        final Stream<T> select = select(query);

        final Iterator<T> iterator = select.iterator();

        if (!iterator.hasNext()) {
            return Optional.empty();
        }
        final T entity = iterator.next();

        if (!iterator.hasNext()) {
            return Optional.of(entity);
        }
        throw new NonUniqueResultException("No Unique result found to the query: " + query);
    }

    @Override
    public <T, K> Optional<T> find(Class<T> type, K id) {
        requireNonNull(type, "type is required");
        requireNonNull(id, "id is required");
        EntityMetadata entityMetadata = entities().get(type);
        FieldMetadata idField = entityMetadata.id()
                .orElseThrow(() -> IdNotFoundException.newInstance(type));

        Object value = ConverterUtil.getValue(id, entityMetadata, idField.fieldName(), converters());
        SelectQuery query = SelectQuery.select().from(entityMetadata.name())
                .where(idField.name()).eq(value).build();

        return singleResult(query);
    }

    @Override
    public <T, K> void delete(Class<T> type, K id) {
        requireNonNull(type, "type is required");
        requireNonNull(id, "id is required");

        EntityMetadata entityMetadata = entities().get(type);
        FieldMetadata idField = entityMetadata.id()
                .orElseThrow(() -> IdNotFoundException.newInstance(type));
        Object value = ConverterUtil.getValue(id, entityMetadata, idField.fieldName(), converters());

        DeleteQuery query = DeleteQuery.delete().from(entityMetadata.name())
                .where(idField.name()).eq(value).build();
        manager().delete(query);
    }


    @Override
    public <T> Stream<T> query(String query) {
        requireNonNull(query, "query is required");
        return PARSER.query(query, manager(), getObserver()).map(c -> converter().toEntity(c));
    }

    @Override
    public <T> Optional<T> singleResult(String query) {
        Stream<T> entities = query(query);
        final Iterator<T> iterator = entities.iterator();

        if (!iterator.hasNext()) {
            return Optional.empty();
        }
        final T entity = iterator.next();
        if (!iterator.hasNext()) {
            return Optional.of(entity);
        }
        throw new NonUniqueResultException("No unique result found to the query: " + query);
    }

    @Override
    public org.eclipse.jnosql.mapping.PreparedStatement prepare(String query) {
        return new PreparedStatement(PARSER.prepare(query, manager(), getObserver()), converter());
    }


    @Override
    public long count(String columnFamily) {
        return manager().count(columnFamily);
    }


    @Override
    public <T> long count(Class<T> type) {
        requireNonNull(type, "entity class is required");
        return manager().count(findAllQuery(type));
    }

    private <T> Stream<T> executeQuery(SelectQuery query) {
        requireNonNull(query, "query is required");
        Stream<CommunicationEntity> entities = manager().select(query);
        Function<CommunicationEntity, T> function = e -> converter().toEntity(e);
        return entities.map(function).peek(eventManager()::firePostEntity);
    }

    @Override
    public <T> QueryMapper.MapperFrom select(Class<T> type) {
        Objects.requireNonNull(type, "type is required");
        EntityMetadata metadata = entities().get(type);
        return new MapperSelect(metadata, converters(), this);
    }

    @Override
    public <T> QueryMapper.MapperDeleteFrom delete(Class<T> type) {
        Objects.requireNonNull(type, "type is required");
        EntityMetadata metadata = entities().get(type);
        return new MapperDelete(metadata, converters(), this);
    }

    @Override
    public <T> Stream<T> findAll(Class<T> type) {
        Objects.requireNonNull(type, "type is required");
        return select(findAllQuery(type));
    }

    @Override
    public <T> void deleteAll(Class<T> type) {
        Objects.requireNonNull(type, "type is required");
        EntityMetadata metadata = entities().get(type);
        if(metadata.inheritance().isPresent()){
            InheritanceMetadata inheritanceMetadata = metadata.inheritance().orElseThrow();
            if(!inheritanceMetadata.parent().equals(metadata.type())){
                manager().delete(DeleteQuery.delete().from(metadata.name())
                        .where(inheritanceMetadata.discriminatorColumn())
                        .eq(inheritanceMetadata.discriminatorValue()).build());
                return;
            }
        }
        manager().delete(DeleteQuery.delete().from(metadata.name()).build());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> CursoredPage<T> selectCursor(SelectQuery query, PageRequest<T> pageRequest){
        Objects.requireNonNull(query, "query is required");
        Objects.requireNonNull(pageRequest, "pageRequest is required");
        CursoredPage<CommunicationEntity> cursoredPage = this.manager().selectCursor(query, pageRequest);
        List<T> entities = cursoredPage.stream().<T>map(c -> converter().toEntity(c)).toList();
        PageRequest<T> nextPageRequest = cursoredPage.hasNext()? (PageRequest<T>) cursoredPage.nextPageRequest(): null;
        PageRequest<T> beforePageRequest = cursoredPage.hasPrevious()? (PageRequest<T>) cursoredPage.previousPageRequest(): null;
        List<PageRequest.Cursor> cursors = ((CursoredPageRecord<CommunicationEntity>) cursoredPage).cursors();
        return new CursoredPageRecord<>(entities, cursors, -1, pageRequest, nextPageRequest, beforePageRequest);
    }

    protected <T> T persist(T entity, UnaryOperator<CommunicationEntity> persistAction) {
        return Stream.of(entity)
                .map(toUnary(eventManager()::firePreEntity))
                .map(converter()::toCommunication)
                .map(persistAction)
                .map(t -> converter().toEntity(entity, t))
                .map(toUnary(eventManager()::firePostEntity))
                .findFirst()
                .orElseThrow();
    }

    private <T> UnaryOperator<T> toUnary(Consumer<T> consumer) {
        return t -> {
            consumer.accept(t);
            return t;
        };
    }

    private <T> SelectQuery findAllQuery(Class<T> type){
        EntityMetadata metadata = entities().get(type);

        if(metadata.inheritance().isPresent()){
            InheritanceMetadata inheritanceMetadata = metadata.inheritance().orElseThrow();
            if(!inheritanceMetadata.parent().equals(metadata.type())){
                return SelectQuery.select().from(metadata.name())
                        .where(inheritanceMetadata.discriminatorColumn()).eq(inheritanceMetadata.discriminatorValue()).build();
            }
        }
        return SelectQuery.select().from(metadata.name()).build();
    }
}
