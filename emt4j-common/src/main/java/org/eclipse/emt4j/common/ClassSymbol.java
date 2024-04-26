/********************************************************************************
 * Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/
package org.eclipse.emt4j.common;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Symbols that used by a class.
 * This class not include all symbol of a classes.Only some we need.
 */
public class ClassSymbol {
    /**
     * typeSet eg:
     *
     * [java.util.Map$Entry,
     * org.apache.commons.collections.MultiHashMap$Values,
     * java.util.Iterator,
     * org.apache.commons.collections.MultiHashMap,
     * java.version,
     * java.lang.System,
     * java.util.Collection,
     * java.io.ObjectInputStream,
     * java.util.Set,
     * org.apache.commons.collections.MultiHashMap$1,
     * java.util.HashMap,
     * java.util.ArrayList,
     * org.apache.commons.collections.MultiMap,
     * 1.2,
     * 1.3,
     * java.util.Map,
     * java.lang.Object,
     * java.lang.SecurityException,
     * java.lang.String,
     * org.apache.commons.collections.iterators.EmptyIterator]
     */
    private Set<String> typeSet;

    /**
     * callMethodSet eg:
     *
     * [org.eclipse.emt4j.common.DependTarget$Method@895fcd1e,
     * org.eclipse.emt4j.common.DependTarget$Method@436e862a,
     * org.eclipse.emt4j.common.DependTarget$Method@391f2259,
     * org.eclipse.emt4j.common.DependTarget$Method@abea17a6,
     * org.eclipse.emt4j.common.DependTarget$Method@45b1274e,
     * …
     *  org.eclipse.emt4j.common.DependTarget$Method@ba292d56]
     */
    private Set<DependTarget.Method> callMethodSet;//被调用的每个方法

    private Map<DependTarget.Method, List<Integer>> callMethodToLines;//被调用每个方法的位置（行数）
    private Set<String> constantPoolSet;//常量池集合，eg: [1.2, java.version, 1.3]
    private Map<String, Set<String>> invokeMap;
    private String className; // Internal class name

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Set<String> getTypeSet() {
        return typeSet;
    }

    public void setTypeSet(Set<String> typeSet) {
        this.typeSet = typeSet;
    }

    public Set<DependTarget.Method> getCallMethodSet() {
        return callMethodSet;
    }

    public void setCallMethodSet(Set<DependTarget.Method> callMethodSet) {
        this.callMethodSet = callMethodSet;
    }

    public Map<DependTarget.Method, List<Integer>> getCallMethodToLines() {
        return callMethodToLines;
    }

    public void setCallMethodToLines(Map<DependTarget.Method, List<Integer>> callMethodToLines) {
        this.callMethodToLines = callMethodToLines;
    }

    public Set<String> getConstantPoolSet() {
        return constantPoolSet;
    }

    public void setConstantPoolSet(Set<String> constantPoolSet) {
        this.constantPoolSet = constantPoolSet;
    }
}
