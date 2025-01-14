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
package org.eclipse.jnosql.mapping.semistructured.query;

import jakarta.data.Limit;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.BasicRepository;
import jakarta.data.Sort;
import jakarta.data.repository.By;
import jakarta.data.repository.Find;
import jakarta.data.repository.Param;
import jakarta.inject.Inject;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.jnosql.communication.Condition;
import org.eclipse.jnosql.communication.TypeReference;
import org.eclipse.jnosql.communication.Value;
import org.eclipse.jnosql.communication.semistructured.CriteriaCondition;
import org.eclipse.jnosql.communication.semistructured.Element;
import org.eclipse.jnosql.communication.semistructured.SelectQuery;
import org.eclipse.jnosql.mapping.core.Converters;
import org.eclipse.jnosql.mapping.core.NoSQLPage;
import org.eclipse.jnosql.mapping.semistructured.EntityConverter;
import org.eclipse.jnosql.mapping.semistructured.SemistructuredTemplate;
import org.eclipse.jnosql.mapping.semistructured.MockProducer;
import org.eclipse.jnosql.mapping.semistructured.entities.Person;
import org.eclipse.jnosql.mapping.semistructured.entities.Vendor;
import org.eclipse.jnosql.mapping.metadata.EntitiesMetadata;
import org.eclipse.jnosql.mapping.reflection.Reflections;
import org.eclipse.jnosql.mapping.core.spi.EntityMetadataExtension;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.jboss.weld.junit5.auto.AddPackages;
import org.jboss.weld.junit5.auto.EnableAutoWeld;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jnosql.communication.Condition.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableAutoWeld
@AddPackages(value = {Converters.class, EntityConverter.class})
@AddPackages(MockProducer.class)
@AddPackages(Reflections.class)
@AddExtensions({EntityMetadataExtension.class})
public class RepositoryProxyPageRequestTest {

    private SemistructuredTemplate template;

    @Inject
    private EntitiesMetadata entities;

    @Inject
    private Converters converters;

    private PersonRepository personRepository;

    private VendorRepository vendorRepository;


    @BeforeEach
    public void setUp() {
        this.template = Mockito.mock(SemistructuredTemplate.class);

        SemistructuredRepositoryProxy personHandler = new SemistructuredRepositoryProxy(template,
                entities, PersonRepository.class, converters);

        SemistructuredRepositoryProxy vendorHandler = new SemistructuredRepositoryProxy(template,
                entities, VendorRepository.class, converters);

        when(template.insert(any(Person.class))).thenReturn(Person.builder().build());
        when(template.insert(any(Person.class), any(Duration.class))).thenReturn(Person.builder().build());
        when(template.update(any(Person.class))).thenReturn(Person.builder().build());

        personRepository = (PersonRepository) Proxy.newProxyInstance(PersonRepository.class.getClassLoader(),
                new Class[]{PersonRepository.class},
                personHandler);
        vendorRepository = (VendorRepository) Proxy.newProxyInstance(VendorRepository.class.getClassLoader(),
                new Class[]{VendorRepository.class}, vendorHandler);
    }


    @Test
    public void shouldFindByNameInstance() {

        when(template.singleResult(any(SelectQuery.class))).thenReturn(Optional
                .of(Person.builder().build()));

        PageRequest pageRequest = getPageRequest();
        personRepository.findByName("name", pageRequest);

        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).singleResult(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(Condition.EQUALS, condition.condition());
        assertEquals(pageRequest.size(), query.skip());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());

        assertEquals(Element.of("name", "name"), condition.element());

        assertNotNull(personRepository.findByName("name", pageRequest));
        when(template.singleResult(any(SelectQuery.class))).thenReturn(Optional
                .empty());

        assertNull(personRepository.findByName("name", pageRequest));


    }

    @Test
    public void shouldFindByNameANDAge() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(SelectQuery.class)))
                .thenReturn(Stream.of(ada));

        PageRequest pageRequest = getPageRequest();
        List<Person> persons = personRepository.findByNameAndAge("name", 20, pageRequest);
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        assertThat(persons).contains(ada);

        SelectQuery query = captor.getValue();
        assertEquals("Person", query.name());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());

    }

    @Test
    public void shouldFindByAgeANDName() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(SelectQuery.class)))
                .thenReturn(Stream.of(ada));

        PageRequest pageRequest = getPageRequest();
        Set<Person> persons = personRepository.findByAgeAndName(20, "name", pageRequest);
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        assertThat(persons).contains(ada);
        SelectQuery query = captor.getValue();
        assertEquals("Person", query.name());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());

    }

    @Test
    public void shouldFindByNameANDAgeOrderByName() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(SelectQuery.class)))
                .thenReturn(Stream.of(ada));

        PageRequest pageRequest = getPageRequest();

        Stream<Person> persons = personRepository.findByNameAndAgeOrderByName("name", 20, pageRequest);
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        assertThat(persons.collect(Collectors.toList())).contains(ada);
        SelectQuery query = captor.getValue();
        assertEquals("Person", query.name());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());

    }

    @Test
    public void shouldFindByNameANDAgeOrderByAge() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(SelectQuery.class)))
                .thenReturn(Stream.of(ada));

        PageRequest pageRequest = getPageRequest();
        Queue<Person> persons = personRepository.findByNameAndAgeOrderByAge("name", 20, pageRequest);
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        assertThat(persons).contains(ada);
        SelectQuery query = captor.getValue();
        assertEquals("Person", query.name());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());


    }


    @Test
    public void shouldFindAll() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.findAll(Person.class))
                .thenReturn(Stream.of(ada));

        PageRequest pageRequest = getPageRequest();

        List<Person> persons = personRepository.findAll(pageRequest).content();
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        SelectQuery query = captor.getValue();
        assertFalse(query.condition().isPresent());
        assertEquals("Person", query.name());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());
    }


    @Test
    public void shouldFindByNameAndAgeGreaterEqualThan() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(SelectQuery.class)))
                .thenReturn(Stream.of(ada));

        PageRequest pageRequest = getPageRequest();
        personRepository.findByNameAndAgeGreaterThanEqual("Ada", 33, pageRequest);
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(AND, condition.condition());
        List<CriteriaCondition> conditions = condition.element().get(new TypeReference<>() {
        });
        CriteriaCondition columnCondition = conditions.get(0);
        CriteriaCondition columnCondition2 = conditions.get(1);

        assertEquals(Condition.EQUALS, columnCondition.condition());
        assertEquals("Ada", columnCondition.element().get());
        assertEquals("name", columnCondition.element().name());

        assertEquals(Condition.GREATER_EQUALS_THAN, columnCondition2.condition());
        assertEquals(33, columnCondition2.element().get());
        assertEquals("age", columnCondition2.element().name());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());
    }

    @Test
    public void shouldFindByGreaterThan() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(SelectQuery.class)))
                .thenReturn(Stream.of(ada));

        PageRequest pageRequest = getPageRequest();
        personRepository.findByAgeGreaterThan(33, pageRequest);
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(GREATER_THAN, condition.condition());
        assertEquals(Element.of("age", 33), condition.element());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());

    }

    @Test
    public void shouldFindByAgeLessThanEqual() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(SelectQuery.class)))
                .thenReturn(Stream.of(ada));

        PageRequest pageRequest = getPageRequest();
        personRepository.findByAgeLessThanEqual(33, pageRequest);
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(LESSER_EQUALS_THAN, condition.condition());
        assertEquals(Element.of("age", 33), condition.element());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());

    }

    @Test
    public void shouldFindByAgeLessEqual() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(SelectQuery.class)))
                .thenReturn(Stream.of(ada));

        PageRequest pageRequest = getPageRequest();
        personRepository.findByAgeLessThan(33, pageRequest);
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(LESSER_THAN, condition.condition());
        assertEquals(Element.of("age", 33), condition.element());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());

    }

    @Test
    public void shouldFindByAgeBetween() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(SelectQuery.class)))
                .thenReturn(Stream.of(ada));

        PageRequest pageRequest = getPageRequest();
        personRepository.findByAgeBetween(10, 15, pageRequest);
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(BETWEEN, condition.condition());
        List<Value> values = condition.element().get(new TypeReference<>() {
        });
        assertEquals(Arrays.asList(10, 15), values.stream().map(Value::get).collect(Collectors.toList()));
        assertTrue(condition.element().name().contains("age"));
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());
    }


    @Test
    public void shouldFindByNameLike() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(SelectQuery.class)))
                .thenReturn(Stream.of(ada));

        PageRequest pageRequest = getPageRequest();
        personRepository.findByNameLike("Ada", pageRequest);
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(LIKE, condition.condition());
        assertEquals(Element.of("name", "Ada"), condition.element());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());

    }


    @Test
    public void shouldFindByStringWhenFieldIsSet() {
        Vendor vendor = new Vendor("vendor");
        vendor.setPrefixes(Collections.singleton("prefix"));

        when(template.select(any(SelectQuery.class)))
                .thenReturn(Stream.of(vendor));

        PageRequest pageRequest = getPageRequest();
        vendorRepository.findByPrefixes("prefix", pageRequest);

        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).singleResult(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("vendors", query.name());
        assertEquals(EQUALS, condition.condition());
        assertEquals(Element.of("prefixes", "prefix"), condition.element());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());

    }

    @Test
    public void shouldFindByIn() {
        Vendor vendor = new Vendor("vendor");
        vendor.setPrefixes(Collections.singleton("prefix"));

        when(template.select(any(SelectQuery.class)))
                .thenReturn(Stream.of(vendor));

        PageRequest pageRequest = getPageRequest();
        vendorRepository.findByPrefixesIn(singletonList("prefix"), pageRequest);

        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).singleResult(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("vendors", query.name());
        assertEquals(IN, condition.condition());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());

    }

    @Test
    public void shouldConvertFieldToTheType() {
        Person ada = Person.builder()
                .withAge(20).withName("Ada").build();

        when(template.select(any(SelectQuery.class)))
                .thenReturn(Stream.of(ada));

        PageRequest pageRequest = getPageRequest();
        Page<Person> slice = personRepository.findByAge("120", pageRequest);
        Assertions.assertNotNull(slice);
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(EQUALS, condition.condition());
        assertEquals(Element.of("age", 120), condition.element());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());
    }

    @Test
    public void shouldFindByNameOrderName() {

        when(template.singleResult(any(SelectQuery.class))).thenReturn(Optional
                .of(Person.builder().build()));

        PageRequest pageRequest = getPageRequest().sortBy(Sort.asc("name"));
        personRepository.findByName("name", pageRequest);

        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).singleResult(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(EQUALS, condition.condition());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());
        assertThat(query.sorts()).hasSize(1)
                .contains(Sort.asc("name"));

        assertEquals(Element.of("name", "name"), condition.element());

        assertNotNull(personRepository.findByName("name", pageRequest));
        when(template.singleResult(any(SelectQuery.class))).thenReturn(Optional
                .empty());
        assertNull(personRepository.findByName("name", pageRequest));
    }

    @Test
    public void shouldFindByNameOrderName2() {

        when(template.singleResult(any(SelectQuery.class))).thenReturn(Optional
                .of(Person.builder().build()));

        PageRequest pageRequest = getPageRequest().sortBy(Sort.asc("name"));
        Page<Person> page = personRepository.findByNameOrderByAge("name", pageRequest);

        Assertions.assertNotNull(page);

        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(EQUALS, condition.condition());
        assertEquals(NoSQLPage.skip(pageRequest), query.limit());
        assertEquals(pageRequest.size(), query.limit());
        assertThat(query.sorts()).hasSize(2)
                .containsExactly(Sort.asc("age"), Sort.asc("name"));

        assertEquals(Element.of("name", "name"), condition.element());

        assertNotNull(personRepository.findByName("name", pageRequest));
        when(template.singleResult(any(SelectQuery.class))).thenReturn(Optional
                .empty());
        assertNull(personRepository.findByName("name", pageRequest));
    }

    @Test
    public void shouldFindByNameSort() {
        when(template.singleResult(any(SelectQuery.class))).thenReturn(Optional
                .of(Person.builder().build()));

        PageRequest pageRequest = getPageRequest().sortBy(Sort.desc("age"));
        personRepository.findByName("name", Sort.asc("name"), pageRequest);

        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(EQUALS, condition.condition());
        assertThat(query.sorts()).hasSize(2)
                .containsExactly(Sort.asc("name"), Sort.desc("age"));
        assertEquals(Element.of("name", "name"), condition.element());
    }

    @Test
    public void shouldFindByNameSortPagination() {
        when(template.singleResult(any(SelectQuery.class))).thenReturn(Optional
                .of(Person.builder().build()));

        personRepository.findByName("name", Sort.asc("name"));

        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(EQUALS, condition.condition());
        assertThat(query.sorts()).hasSize(1)
                .containsExactly(Sort.asc("name"));
        assertEquals(Element.of("name", "name"), condition.element());
    }

    @Test
    public void shouldFindByNameLimit() {
        when(template.singleResult(any(SelectQuery.class))).thenReturn(Optional
                .of(Person.builder().build()));

        personRepository.findByName("name", Limit.of(3), Sort.asc("name"));
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(0, query.skip());
        assertEquals(3, query.limit());
        assertEquals(EQUALS, condition.condition());
        assertThat(query.sorts()).hasSize(1)
                .containsExactly(Sort.asc("name"));
        assertEquals(Element.of("name", "name"), condition.element());
    }

    @Test
    public void shouldFindByNameLimit2() {
        when(template.singleResult(any(SelectQuery.class))).thenReturn(Optional
                .of(Person.builder().build()));

        personRepository.findByName("name", Limit.range(1, 3), Sort.asc("name"));
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(0, query.skip());
        assertEquals(3, query.limit());
        assertEquals(EQUALS, condition.condition());
        assertThat(query.sorts()).hasSize(1)
                .containsExactly(Sort.asc("name"));
        assertEquals(Element.of("name", "name"), condition.element());
    }

    @Test
    public void shouldFindByNameLimit3() {
        when(template.singleResult(any(SelectQuery.class))).thenReturn(Optional
                .of(Person.builder().build()));

        personRepository.findByName("name", Limit.range(2, 3), Sort.asc("name"));
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        SelectQuery query = captor.getValue();
        CriteriaCondition condition = query.condition().get();
        assertEquals("Person", query.name());
        assertEquals(1, query.skip());
        assertEquals(2, query.limit());
        assertEquals(EQUALS, condition.condition());
        assertThat(query.sorts()).hasSize(1)
                .containsExactly(Sort.asc("name"));
        assertEquals(Element.of("name", "name"), condition.element());
    }

    @Test
    public void shouldFindByNameOrderByName() {
        CursoredPage<Person> mock = Mockito.mock(CursoredPage.class);
        when(template.selectCursor(any(SelectQuery.class),
                any(PageRequest.class))).thenReturn(mock);

        CursoredPage<Person> page = personRepository.findByNameOrderByName("name",
                PageRequest.<Person>ofSize(10).afterKey("Ada"));

        SoftAssertions.assertSoftly(s -> {
            s.assertThat(page).isEqualTo(mock);
        });
    }

    @Test
    public void shouldMachParameter() {
        when(template.singleResult(any(SelectQuery.class))).thenReturn(Optional
                .of(Person.builder().build()));

        personRepository.parameter("name", 10);
        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).select(captor.capture());
        SelectQuery query = captor.getValue();

        SoftAssertions.assertSoftly(soft ->{
            soft.assertThat(query.name()).isEqualTo("Person");
            soft.assertThat(query.skip()).isEqualTo(0);
            soft.assertThat(query.limit()).isEqualTo(0);
            soft.assertThat(query.condition().isPresent()).isTrue();
            soft.assertThat(query.sorts()).hasSize(0);
            CriteriaCondition condition = query.condition().orElseThrow();
            soft.assertThat(condition.condition()).isEqualTo(AND);
            List<CriteriaCondition> conditions = condition.element().get(new TypeReference<>() {
            });
            soft.assertThat(conditions).hasSize(2);
            soft.assertThat(conditions.get(0)).isEqualTo(CriteriaCondition.eq(Element.of("name", "name")));
            soft.assertThat(conditions.get(1)).isEqualTo(CriteriaCondition.eq(Element.of("age", 10)));

        });
    }

    @Test
    public void shouldParameterMatch() {
        CursoredPage<Person> mock = Mockito.mock(CursoredPage.class);
        when(template.selectCursor(any(SelectQuery.class),
                any(PageRequest.class))).thenReturn(mock);

        CursoredPage<Person> page = personRepository.findPageParameter("name",
                PageRequest.<Person>ofSize(10).afterKey("Ada"));

        SoftAssertions.assertSoftly(s -> s.assertThat(page).isEqualTo(mock));

        ArgumentCaptor<SelectQuery> captor = ArgumentCaptor.forClass(SelectQuery.class);
        verify(template).selectCursor(captor.capture(), Mockito.any());
        var query = captor.getValue();

        SoftAssertions.assertSoftly(soft ->{
            soft.assertThat(query.name()).isEqualTo("Person");
            soft.assertThat(query.skip()).isEqualTo(0);
            soft.assertThat(query.limit()).isEqualTo(0);
            soft.assertThat(query.condition().isPresent()).isTrue();
            soft.assertThat(query.sorts()).hasSize(0);
            CriteriaCondition condition = query.condition().orElseThrow();
            soft.assertThat(condition.condition()).isEqualTo(EQUALS);
            soft.assertThat(condition.element()).isEqualTo(Element.of("name", "name"));

        });
    }



    private PageRequest getPageRequest() {
        return PageRequest.ofPage(2).size(6);
    }

    interface PersonRepository extends BasicRepository<Person, Long> {

        Person findByName(String name, PageRequest<Person> pageRequest);

        @Find
        List<Person> parameter(@By("name") String name, @By("age") Integer age);

        CursoredPage<Person> findByNameOrderByName(String name, PageRequest<Person> pageRequest);

        @Find
        CursoredPage<Person> findPageParameter(@By("name") String name, PageRequest<Person> pageRequest);

        List<Person> findByName(String name, Sort<Person> sort);

        List<Person> findByName(String name, Limit limit, Sort<Person> sort);

        List<Person> findByName(String name, Sort<Person> sort, PageRequest<Person> pageRequest);

        Page<Person> findByNameOrderByAge(String name, PageRequest<Person> pageRequest);

        Page<Person> findByAge(String age, PageRequest<Person> pageRequest);

        List<Person> findByNameAndAge(String name, Integer age, PageRequest<Person> pageRequest);

        Set<Person> findByAgeAndName(Integer age, String name, PageRequest<Person> pageRequest);

        Stream<Person> findByNameAndAgeOrderByName(String name, Integer age, PageRequest<Person> pageRequest);

        Queue<Person> findByNameAndAgeOrderByAge(String name, Integer age, PageRequest<Person> pageRequest);

        Set<Person> findByNameAndAgeGreaterThanEqual(String name, Integer age, PageRequest<Person> pageRequest);

        Set<Person> findByAgeGreaterThan(Integer age, PageRequest<Person> pageRequest);

        Set<Person> findByAgeLessThanEqual(Integer age, PageRequest<Person> pageRequest);

        Set<Person> findByAgeLessThan(Integer age, PageRequest<Person> pageRequest);

        Set<Person> findByAgeBetween(Integer ageA, Integer ageB, PageRequest<Person> pageRequest);

        Set<Person> findByNameLike(String name, PageRequest<Person> pageRequest);

    }

    public interface VendorRepository extends BasicRepository<Vendor, String> {

        Vendor findByPrefixes(String prefix, PageRequest<Vendor> pageRequest);

        Vendor findByPrefixesIn(List<String> prefix, PageRequest<Vendor> pageRequest);

    }
}
