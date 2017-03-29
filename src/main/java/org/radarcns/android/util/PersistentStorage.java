/*
 * Copyright 2017 Kings College London and The Hyve
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.radarcns.android.util;

import android.os.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

public class PersistentStorage {
    private static Logger logger = LoggerFactory.getLogger(PersistentStorage.class);

    /**
     * Use a class-bounded file-based storage to load or store properties.
     *
     * If a class-bound properties file exists, the default properties are updated with the contents
     * of that file. If any defaults were not stored yet, the combined loaded properties and default
     * properties are stored again. If no values were stored, the given defaults are stored
     * and returned in a copy.
     * @param clazz class to store properties for.
     * @param defaults default properties, null if no defaults are known
     * @throws IOException if the properties cannot be retrieved or stored.
     * @return a new Properties object that combines defaults with any loaded properties.
     */
    public static Properties loadOrStore(Class<?> clazz, Properties defaults)
            throws IOException {
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        File file = new File(path, clazz.getName() + ".properties");

        if (!path.exists() && !path.mkdirs()) {
            logger.error("'{}' could not be created or already exists.", path.getAbsolutePath());
        }

        Properties loadedProps = new Properties();

        if (file.exists()) {
            // Read source id
            try (FileInputStream fin = new FileInputStream(file);
                 Reader fr = new InputStreamReader(fin, "UTF-8");
                 Reader in = new BufferedReader(fr)) {
                loadedProps.load(in);
                logger.debug("Loaded persistent properties from {}", file);
            }
            if (defaults == null || defaults.isEmpty()) {
                return loadedProps;
            }
            // Find out if the defaults had more values than the properties file. If not, do not
            // store the values again.
            Set<Object> originalKeySet = new HashSet<>(defaults.keySet());
            originalKeySet.removeAll(loadedProps.keySet());
            if (originalKeySet.isEmpty()) {
                return loadedProps;
            }
        } else if (defaults == null || defaults.isEmpty()) {
            return loadedProps;
        }

        Properties combinedProperties = new Properties();
        combinedProperties.putAll(defaults);
        combinedProperties.putAll(loadedProps);

        try (FileOutputStream fout = new FileOutputStream(file);
             OutputStreamWriter fwr = new OutputStreamWriter(fout, "UTF-8");
             Writer out = new BufferedWriter(fwr)) {
            combinedProperties.store(out, null);
            logger.debug("Stored persistent properties to {}", file);
        }
        return combinedProperties;
    }

    /**
     * Load or store a single persistent UUID value.
     * @param clazz class name
     * @param key key to store the UUID with.
     * @return The generated UUID value
     */
    public static String loadOrStoreUUID(Class<?> clazz, String key) {
        Properties defaults = new Properties();
        defaults.setProperty(key, UUID.randomUUID().toString());
        try {
            Properties props = PersistentStorage.loadOrStore(clazz, defaults);
            return props.getProperty(key);
        } catch (IOException ex) {
            // Use newly generated UUID
            return defaults.getProperty(key);
        }
    }

    /**
     * Get a String value from persistent storage
     * @param clazz class to store the value for
     * @param key key of the value
     * @return value or null if not present
     */
    public static String get(Class<?> clazz, String key) throws IOException {
        Properties ret = loadOrStore(clazz, null);
        return ret.getProperty(key);
    }

    /**
     * Get a String value from persistent storage
     * @param clazz class to store the value for
     * @param key key of the value
     * @param value value to store
     * @return true if the value was absent or equals the given value, false if a different value is
     *         already stored
     */
    public static boolean putIfAbsent(Class<?> clazz, String key, String value) throws IOException {
        Properties defaults = new Properties();
        defaults.setProperty(key, value);
        Properties ret = loadOrStore(clazz, defaults);
        return ret.getProperty(key).equals(value);
    }
}
