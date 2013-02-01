/*
 * Copyright (C) 2005-2012 NAUMEN. All rights reserved.
 *
 * This file may be distributed and/or modified under the terms of the
 * GNU General Public License version 2 as published by the Free Software
 * Foundation and appearing in the file LICENSE.GPL included in the
 * packaging of this file.
 */

package ru.naumen.servacc.test.settings.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Properties;

import org.junit.Test;
import ru.naumen.servacc.settings.impl.FileSourceListProvider;

/**
 * @author Andrey Hitrin
 * @since 30.09.12
 */
public class FileSourceListProviderTest
{
    private FileSourceListProvider sourceListProvider;

    @Test
    public void canListAllSourcesAtOnce() throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty("source", "sourceFile");
        properties.setProperty("source1", "anotherFile");
        properties.setProperty("hello", "world");
        sourceListProvider = new FileSourceListProvider(properties);
        Collection<String> configSources = sourceListProvider.list();
        assertThat(configSources.size(), is(2));
        assertThat(configSources.contains("sourceFile"), is(true));
        assertThat(configSources.contains("anotherFile"), is(true));
    }

    @Test
    public void useOnlyLatestSourceOnCollision() throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty("source", "missingSource");
        properties.setProperty("source", "collision");
        sourceListProvider = new FileSourceListProvider(properties);
        Collection<String> configSources = sourceListProvider.list();
        assertThat(configSources.size(), is(1));
        assertThat(configSources.contains("missingSource"), is(false));
        assertThat(configSources.contains("collision"), is(true));
    }
}
