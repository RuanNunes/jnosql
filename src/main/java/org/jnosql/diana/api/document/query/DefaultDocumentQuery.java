/*
 *
 *  Copyright (c) 2017 Otávio Santana and others
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
 *
 */
package org.jnosql.diana.api.document.query;


import org.jnosql.diana.api.Sort;
import org.jnosql.diana.api.document.DocumentCondition;
import org.jnosql.diana.api.document.DocumentQuery;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.unmodifiableList;
import static java.util.Optional.ofNullable;

class DefaultDocumentQuery implements DocumentQuery {

    private final long maxResult;

    private final long firstResult;

    private final String documentCollection;

    private final DocumentCondition condition;

    private final List<Sort> sorts;

    private final List<String> documents;

    DefaultDocumentQuery(long maxResult, long firstResult, String documentCollection, DocumentCondition condition,
                         List<Sort> sorts, List<String> documents) {

        this.maxResult = maxResult;
        this.firstResult = firstResult;
        this.documentCollection = documentCollection;
        this.condition = condition;
        this.sorts = sorts;
        this.documents = documents;
    }

    @Override
    public long getMaxResults() {
        return maxResult;
    }

    @Override
    public long getFirstResult() {
        return firstResult;
    }

    @Override
    public String getCollection() {
        return documentCollection;
    }

    @Override
    public Optional<DocumentCondition> getCondition() {
        return ofNullable(condition);
    }

    @Override
    public List<Sort> getSorts() {
        return unmodifiableList(sorts);
    }

    @Override
    public List<String> getDocuments() {
        return unmodifiableList(documents);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentQuery)) {
            return false;
        }
        DocumentQuery that = (DocumentQuery) o;
        return maxResult == that.getMaxResults() &&
                firstResult == that.getFirstResult() &&
                Objects.equals(documentCollection, that.getCollection()) &&
                Objects.equals(condition, that.getCondition()) &&
                Objects.equals(sorts, that.getSorts()) &&
                Objects.equals(documents, that.getDocuments());
    }

    @Override
    public int hashCode() {
        return Objects.hash(maxResult, firstResult, documentCollection, condition, sorts, documents);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DefaultDocumentQuery{");
        sb.append("maxResult=").append(maxResult);
        sb.append(", firstResult=").append(firstResult);
        sb.append(", documentCollection='").append(documentCollection).append('\'');
        sb.append(", condition=").append(condition);
        sb.append(", sorts=").append(sorts);
        sb.append(", documents=").append(documents);
        sb.append('}');
        return sb.toString();
    }
}
