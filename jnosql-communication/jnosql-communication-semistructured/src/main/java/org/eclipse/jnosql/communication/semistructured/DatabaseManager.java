/*
 *  Copyright (c) 2024 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 * You may elect to redistribute this code under either of these licenses.
 *
 */
package org.eclipse.jnosql.communication.semistructured;


import jakarta.data.exceptions.NonUniqueResultException;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.PageRequest;

import java.time.Duration;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The manager instance bridges between Jakarta NoSQL and the NoSQL vendor, providing operations
 * to interact with a database.
 *
 * @see CommunicationEntity
 */
public interface DatabaseManager extends AutoCloseable {

    /**
     * Returns the name of the managed database.
     *
     * @return the name of the database
     */
    String name();

    /**
     * Inserts an entity into the database.
     *
     * <p>If an entity with the same unique identifier already exists in the database and the database
     * supports ACID transactions, this method throws an exception. In databases following the BASE model
     * or using an append model to write data, no exception is thrown.</p>
     *
     * <p>The returned entity includes all values written to the database, including automatically generated
     * or incremented values. After calling this method, avoid using the supplied entity instance, as this
     * method makes no guarantees about its state.</p>
     *
     * @param entity the entity to be saved
     * @return the saved entity
     * @throws NullPointerException when the provided entity is null
     */
    CommunicationEntity insert(CommunicationEntity entity);

    /**
     * Inserts an entity into the database with a specified time-to-live (TTL).
     *
     * <p>If an entity with the same unique identifier already exists in the database and the database
     * supports ACID transactions, this method raises an error. In databases following the BASE model
     * or using an append model to write data, no exception is thrown.</p>
     *
     * <p>The returned entity includes all values written to the database, including automatically generated
     * or incremented values. After calling this method, avoid using the supplied entity instance, as this
     * method makes no guarantees about its state.</p>
     *
     * <p>Time-to-live (TTL) is a feature provided by some NoSQL databases where data is automatically removed
     * from the database after a specified duration. If the database does not support TTL or if the TTL feature
     * is not enabled, this operation will not have any effect on the entity's expiration.</p>
     *
     * @param entity the entity to insert
     * @param ttl    time to live
     * @return the inserted entity
     * @throws NullPointerException          if the entity is null
     * @throws UnsupportedOperationException when the database does not support TTL
     */
    CommunicationEntity insert(CommunicationEntity entity, Duration ttl);

    /**
     * Inserts multiple entities into the database.
     *
     * <p>If any entity with the same unique identifier as any of the given entities already exists in the database
     * and the database supports ACID transactions, this method raises an error. In databases following the BASE model
     * or using an append model to write data, no exception is thrown.</p>
     *
     * <p>The returned iterable contains all inserted entities, including all automatically generated or incremented
     * values. After calling this method, avoid using the supplied entity instances, as this method makes no guarantees
     * about their state.</p>
     *
     * @param entities entities to insert
     * @return an iterable containing the inserted entities
     * @throws NullPointerException if the iterable is null or any element is null
     */
    Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities);

    /**
     * Inserts multiple entities into the database with a specified time-to-live (TTL).
     *
     * <p>If any entity with the same unique identifier as any of the given entities already exists in the database
     * and the database supports ACID transactions, this method raises an error. In databases following the BASE model
     * or using an append model to write data, no exception is thrown.</p>
     *
     * <p>The returned iterable contains all inserted entities, including all automatically generated or incremented
     * values. After calling this method, avoid using the supplied entity instances, as this method makes no guarantees
     * about their state.</p>
     *
     * <p>Time-to-live (TTL) is a feature provided by some NoSQL databases where data is automatically removed
     * from the database after a specified duration. If the database does not support TTL or if the TTL feature
     * is not enabled, this operation will not have any effect on the expiration of the entities.</p>
     *
     * @param entities entities to insert
     * @param ttl      time to live
     * @return an iterable containing the inserted entities
     * @throws NullPointerException          if the iterable is null or any element is null
     * @throws UnsupportedOperationException when the database does not support TTL
     */
    Iterable<CommunicationEntity> insert(Iterable<CommunicationEntity> entities, Duration ttl);

    /**
     * Modifies an existing entity in the database.
     *
     * <p>To perform an update, a matching entity with the same unique identifier must exist in the database.
     * In databases using an append model to write data or following the BASE model, this method behaves
     * the same as the {@link #insert} method.</p>
     *
     * <p>If the entity is versioned (e.g., with an annotation or by convention from the entity model),
     * the version must also match. The version is automatically incremented during the update.</p>
     *
     * <p>Non-matching entities are ignored and do not cause an error.</p>
     *
     * @param entity the entity to update
     * @return the updated entity
     * @throws NullPointerException if the entity is null
     */
    CommunicationEntity update(CommunicationEntity entity);

    /**
     * Modifies multiple entities that already exist in the database.
     *
     * <p>To perform an update to an entity, a matching entity with the same unique identifier must exist
     * in the database. In databases using an append model to write data or following the BASE model,
     * this method behaves the same as the {@link #insert(Iterable)} method.</p>
     *
     * <p>If the entity is versioned (e.g., with an annotation or by convention from the entity model),
     * the version must also match. The version is automatically incremented during the update.</p>
     *
     * <p>Non-matching entities are ignored and do not cause an error.</p>
     *
     * @param entities entities to update
     * @return the number of matching entities that were found in the database and updated
     * @throws NullPointerException if the iterable is null or any element is null
     */
    Iterable<CommunicationEntity> update(Iterable<CommunicationEntity> entities);

    /**
     * Deletes entities from the database based on the specified query.
     *
     * @param query the query used to select entities to be deleted
     * @throws NullPointerException          when either the query or the collection is null
     * @throws UnsupportedOperationException if the database does not support any operation specified in the query
     */
    void delete(DeleteQuery query);

    /**
     * Finds entities in the database based on the specified query.
     *
     * @param query the query used to select entities
     * @return a stream of entities found by the query
     * @throws NullPointerException          when the query is null
     * @throws UnsupportedOperationException if the database does not support any operation specified in the query
     */
    Stream<CommunicationEntity> select(SelectQuery query);

    /**
     * Returns the number of entities in the database that match the specified query.
     *
     * @param query the query
     * @return the number of entities returned by the query
     * @throws NullPointerException when the query is null
     */
    default long count(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        return this.select(DefaultSelectQuery.countBy(query)).count();
    }

    /**
     * Checks if an entity matching the specified query exists in the database.
     *
     * @param query the query
     * @return true if an entity with the given query exists, false otherwise
     * @throws NullPointerException when the query is null
     */
    default boolean exists(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        return this.select(DefaultSelectQuery.existsBy(query)).findAny().isPresent();
    }

    /**
     * Executes a query and returns the result. If the query is an insert, update, or select command,
     * it returns the result of the operation. If the query is a delete command, it returns an empty collection.
     *
     * @param query the query as a string
     * @return the result of the operation; for delete operations, an empty list is returned
     * @throws NullPointerException     when the query is null
     * @throws IllegalArgumentException when the query contains value parameters
     * @throws IllegalStateException    when there is no {@link QueryParser} available
     */
    default Stream<CommunicationEntity> query(String query) {
        Objects.requireNonNull(query, "query is required");
        QueryParser parser = new QueryParser();
        return parser.query(query, this, CommunicationObserverParser.EMPTY);
    }

    /**
     * Prepares a query for execution.
     *
     * @param query the query as a string
     * @return a {@link CommunicationPreparedStatement} instance
     * @throws NullPointerException  when the query is null
     * @throws IllegalStateException when there is no {@link QueryParser} available
     */
    default CommunicationPreparedStatement prepare(String query) {
        Objects.requireNonNull(query, "query is required");
        QueryParser parser = new QueryParser();
        return parser.prepare(query, this, CommunicationObserverParser.EMPTY);
    }

    /**
     * Returns a single entity from the database based on the specified query.
     *
     * @param query the query used to select the entity
     * @return an entity wrapped in an {@link Optional}, or {@link Optional#empty()} if no entity is found
     * @throws NonUniqueResultException when more than one entity is returned by the query
     * @throws NullPointerException     when the query is null
     */
    default Optional<CommunicationEntity> singleResult(SelectQuery query) {
        Objects.requireNonNull(query, "query is required");
        Stream<CommunicationEntity> entities = select(query);
        final Iterator<CommunicationEntity> iterator = entities.iterator();
        if (!iterator.hasNext()) {
            return Optional.empty();
        }
        final CommunicationEntity entity = iterator.next();
        if (!iterator.hasNext()) {
            return Optional.of(entity);
        }
        throw new NonUniqueResultException("More than one entity was returned by the query: " + query);
    }

    /**
     * Select entities using pagination with cursor-based paging.
     *
     * <p>This method retrieves entities based on cursor-based paging, where the cursor acts as a bookmark for the next page of results.
     * If the provided {@link PageRequest} has a mode of {@link jakarta.data.page.PageRequest.Mode#OFFSET}, the method will consider
     * the initial request as an offset-based pagination and extract the order key to create a new {@link PageRequest} with
     * {@link jakarta.data.page.PageRequest.Mode#CURSOR_NEXT}. If the initial request is already cursor-based, the method will proceed as instructed.
     * </p>
     * <p>
     * If the cursor-based pagination is used, at least one order key is required to be specified in the {@link SelectQuery} order
     * clause; otherwise, an {@link IllegalStateException} will be thrown.
     * </p>
     *
     * @param query         the query to retrieve entities
     * @param pageRequest   the page request defining the cursor-based paging
     * @return a {@link CursoredPage} instance containing the entities within the specified page
     * @throws NullPointerException     if the query or pageRequest is null
     * @throws IllegalStateException    if the cursor-based pagination is used without any order key specified
     */
    default CursoredPage<CommunicationEntity> selectCursor(SelectQuery query, PageRequest<?> pageRequest){
        Objects.requireNonNull(query, "query is required");
        Objects.requireNonNull(pageRequest, "pageRequest is required");
        if(query.sorts().isEmpty()){
            throw new IllegalArgumentException("To execute a cursor pagination, it is necessary to define at least one sort field." +
                    query);
        }
        CursorExecutor executor = CursorExecutor.of(pageRequest.mode());
        return executor.cursor(query, pageRequest, this);
    }

    /**
     * Returns the number of entities in the database.
     *
     * @param entity the entity name
     * @return the number of entities
     * @throws NullPointerException when the entity name is null
     * @throws UnsupportedOperationException if the database does not support this operation
     */
    long count(String entity);

    /**
     * Closes the database manager and releases any associated resources.
     */
    void close();
}