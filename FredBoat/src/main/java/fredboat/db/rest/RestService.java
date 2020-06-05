/*
 * MIT License
 *
 * Copyright (c) 2017-2018 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fredboat.db.rest;

import fredboat.db.transfer.TransferObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.Serializable;

/**
 * Created by napster on 17.02.18.
 *
 * Counterpart to the EntityController of the Quarterdeck module.
 * The calls to methods of this class are expected to be wrapped by the service implementations
 */
public abstract class RestService<I extends Serializable, E extends TransferObject<I>> {

    protected static final Logger log = LoggerFactory.getLogger(RestService.class);

    public static final int API_VERSION = 0;
    public static final String VERSION_PATH = "v" + API_VERSION + "/";

    protected final String path;
    protected final Class<E> entityClass;
    protected final RestTemplate backendRestTemplate;

    /**
     * @param path base path of this resource, including the version and a trailing slash
     *             Example: http://quarterdeck:4269/v1/blacklist/
     */
    protected RestService(String path, Class<E> entityClass, RestTemplate backendRestTemplate) {
        this.path = path;
        this.entityClass = entityClass;
        this.backendRestTemplate = backendRestTemplate;
    }

    protected Class<E> getEntityClass() {
        return entityClass;
    }

    protected void delete(I id) { //todo success handling?
        try {
            backendRestTemplate.postForObject(path + "delete", id, Void.class);
        } catch (RestClientException e) {
            throw new BackendException(String.format("Could not delete entity with id %s of class %s", id, entityClass), e);
        }
    }

    protected E fetch(I id) {
        try {
            E result = backendRestTemplate.postForObject(path + "fetch", id, entityClass);
            if (result == null) {
                throw new BackendException(String.format("Fetched entity with id %s of class %s is null", id, entityClass));
            }
            return result;
        } catch (RestClientException e) {
            throw new BackendException(String.format("Could not fetch entity with id %s of class %s", id, entityClass), e);
        }
    }

    protected E merge(E entity) {
        try {
            E result = backendRestTemplate.postForObject(path + "merge", entity, entityClass);
            if (result == null) {
                throw new BackendException(String.format("Merged entity with id %s of class %s is null", entity.getId(), entityClass));
            }
            return result;
        } catch (RestClientException e) {
            throw new BackendException(String.format("Could not merge entity with id %s of class %s", entity.getId(), entityClass), e);
        }
    }
}
