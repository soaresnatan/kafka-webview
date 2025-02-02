/**
 * MIT License
 * <p>
 * Copyright (c) 2017, 2018, 2019 SourceLab.org (https://github.com/SourceLabOrg/kafka-webview/)
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.sourcelab.kafka.webview.ui.repository;

import org.sourcelab.kafka.webview.ui.model.Connector;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * For interacting w/ the Cluster database table.
 */
@Repository
public interface ConnectorRepository extends CrudRepository<Connector, Long> {
    /**
     * Find cluster by name.
     * @param name Name of cluster to retrieve.
     * @return the Cluster instance, or null if none found.
     */
    Connector findByName(final String name);

    /**
     * Retrieve all clusters ordered by name.
     * @return all clusters ordered by name.
     */
    Iterable<Connector> findAllByOrderByNameAsc();

    Long countByClusterId(final long clusterId);

    Iterable<Connector> findAllByClusterIdOrderByNameAsc(final long clusterId);
}
