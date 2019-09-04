/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.graphdb;

import org.eclipse.collections.api.iterator.LongIterator;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.api.set.primitive.MutableLongSet;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.neo4j.test.rule.ImpermanentDbmsRule;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.neo4j.internal.helpers.collection.MapUtil.map;

public abstract class IndexingStringQueryAcceptanceTestBase
{
    @ClassRule
    public static ImpermanentDbmsRule dbRule = new ImpermanentDbmsRule();
    @Rule
    public final TestName testName = new TestName();

    private final String template;
    private final String[] matching;
    private final String[] nonMatching;
    private final StringSearchMode searchMode;
    private final boolean withIndex;

    private Label LABEL;
    private String KEY = "name";
    private GraphDatabaseService db;

    IndexingStringQueryAcceptanceTestBase( String template, String[] matching,
            String[] nonMatching, StringSearchMode searchMode, boolean withIndex )
    {
        this.template = template;
        this.matching = matching;
        this.nonMatching = nonMatching;
        this.searchMode = searchMode;
        this.withIndex = withIndex;
    }

    @Before
    public void setup()
    {
        LABEL = Label.label( "LABEL1-" + testName.getMethodName() );
        db = dbRule.getGraphDatabaseAPI();
        if ( withIndex )
        {
            try ( org.neo4j.graphdb.Transaction tx = db.beginTx() )
            {
                db.schema().indexFor( LABEL ).on( KEY ).create();
                tx.commit();
            }

            try ( org.neo4j.graphdb.Transaction tx = db.beginTx() )
            {
                db.schema().awaitIndexesOnline( 5, TimeUnit.MINUTES );
                tx.commit();
            }
        }
    }

    @Test
    public void shouldSupportIndexSeek()
    {
        // GIVEN
        createNodes( db, LABEL, nonMatching );
        LongSet expected = createNodes( db, LABEL, matching );

        // WHEN
        MutableLongSet found = new LongHashSet();
        try ( Transaction tx = db.beginTx() )
        {
            collectNodes( found, tx.findNodes( LABEL, KEY, template, searchMode ) );
        }

        // THEN
        assertThat( found, equalTo( expected ) );
    }

    @Test
    public void shouldIncludeNodesCreatedInSameTxInIndexSeek()
    {
        // GIVEN
        createNodes( db, LABEL, nonMatching[0], nonMatching[1] );
        MutableLongSet expected = createNodes( db, LABEL, matching[0], matching[1] );
        // WHEN
        MutableLongSet found = new LongHashSet();
        try ( Transaction tx = db.beginTx() )
        {
            expected.add( createNode( db, map( KEY, matching[2] ), LABEL ).getId() );
            createNode( db, map( KEY, nonMatching[2] ), LABEL );

            collectNodes( found, tx.findNodes( LABEL, KEY, template, searchMode ) );
        }
        // THEN
        assertThat( found, equalTo( expected ) );
    }

    @Test
    public void shouldNotIncludeNodesDeletedInSameTxInIndexSeek()
    {
        // GIVEN
        createNodes( db, LABEL, nonMatching[0] );
        LongSet toDelete = createNodes( db, LABEL, matching[0], nonMatching[1], matching[1], nonMatching[2] );
        MutableLongSet expected = createNodes( db, LABEL, matching[2] );
        // WHEN
        MutableLongSet found = new LongHashSet();
        try ( Transaction tx = db.beginTx() )
        {
            LongIterator deleting = toDelete.longIterator();
            while ( deleting.hasNext() )
            {
                long id = deleting.next();
                tx.getNodeById( id ).delete();
                expected.remove( id );
            }

            collectNodes( found, tx.findNodes( LABEL, KEY, template, searchMode ) );
        }
        // THEN
        assertThat( found, equalTo( expected ) );
    }

    @Test
    public void shouldConsiderNodesChangedInSameTxInIndexSeek()
    {
        // GIVEN
        createNodes( db, LABEL, nonMatching[0] );
        LongSet toChangeToMatch = createNodes( db, LABEL, nonMatching[1] );
        MutableLongSet toChangeToNotMatch = createNodes( db, LABEL, matching[0] );
        MutableLongSet expected = createNodes( db, LABEL, matching[1] );
        // WHEN
        MutableLongSet found = new LongHashSet();
        try ( Transaction tx = db.beginTx() )
        {
            LongIterator toMatching = toChangeToMatch.longIterator();
            while ( toMatching.hasNext() )
            {
                long id = toMatching.next();
                tx.getNodeById( id ).setProperty( KEY, matching[2] );
                expected.add( id );
            }
            LongIterator toNotMatching = toChangeToNotMatch.longIterator();
            while ( toNotMatching.hasNext() )
            {
                long id = toNotMatching.next();
                tx.getNodeById( id ).setProperty( KEY, nonMatching[2] );
                expected.remove( id );
            }

            collectNodes( found, tx.findNodes( LABEL, KEY, template, searchMode ) );
        }
        // THEN
        assertThat( found, equalTo( expected ) );
    }

    public abstract static class EXACT extends IndexingStringQueryAcceptanceTestBase
    {
        static final String[] MATCHING = {"Johan", "Johan", "Johan"};
        static final String[] NON_MATCHING = {"Johanna", "Olivia", "InteJohan"};

        EXACT( boolean withIndex )
        {
            super( "Johan", MATCHING, NON_MATCHING, StringSearchMode.EXACT, withIndex );
        }
    }

    public static class EXACT_WITH_INDEX extends EXACT
    {
        public EXACT_WITH_INDEX()
        {
            super( true );
        }
    }

    public static class EXACT_WITHOUT_INDEX extends EXACT
    {
        public EXACT_WITHOUT_INDEX()
        {
            super( false );
        }
    }

    public abstract static class PREFIX extends IndexingStringQueryAcceptanceTestBase
    {
        static final String[] MATCHING = {"Olivia", "Olivia2", "OliviaYtterbrink"};
        static final String[] NON_MATCHING = {"Johan", "olivia", "InteOlivia"};

        PREFIX( boolean withIndex )
        {
            super( "Olivia", MATCHING, NON_MATCHING, StringSearchMode.PREFIX, withIndex );
        }
    }

    public static class PREFIX_WITH_INDEX extends PREFIX
    {
        public PREFIX_WITH_INDEX()
        {
            super( true );
        }
    }

    public static class PREFIX_WITHOUT_INDEX extends PREFIX
    {
        public PREFIX_WITHOUT_INDEX()
        {
            super( false );
        }
    }

    public abstract static class SUFFIX extends IndexingStringQueryAcceptanceTestBase
    {
        static final String[] MATCHING = {"Jansson", "Hansson", "Svensson"};
        static final String[] NON_MATCHING = {"Taverner", "Svensson-Averbuch", "Taylor"};

        SUFFIX( boolean withIndex )
        {
            super( "sson", MATCHING, NON_MATCHING, StringSearchMode.SUFFIX, withIndex );
        }
    }

    public static class SUFFIX_WITH_INDEX extends SUFFIX
    {
        public SUFFIX_WITH_INDEX()
        {
            super( true );
        }
    }

    public static class SUFFIX_WITHOUT_INDEX extends SUFFIX
    {
        public SUFFIX_WITHOUT_INDEX()
        {
            super( false );
        }
    }

    public abstract static class CONTAINS extends IndexingStringQueryAcceptanceTestBase
    {
        static final String[] MATCHING = {"good", "fool", "fooooood"};
        static final String[] NON_MATCHING = {"evil", "genius", "hungry"};

        CONTAINS( boolean withIndex )
        {
            super( "oo", MATCHING, NON_MATCHING, StringSearchMode.CONTAINS, withIndex );
        }
    }

    public static class CONTAINS_WITH_INDEX extends CONTAINS
    {
        public CONTAINS_WITH_INDEX()
        {
            super( true );
        }
    }

    public static class CONTAINS_WITHOUT_INDEX extends CONTAINS
    {
        public CONTAINS_WITHOUT_INDEX()
        {
            super( false );
        }
    }

    private MutableLongSet createNodes( GraphDatabaseService db, Label label, String... propertyValues )
    {
        MutableLongSet expected = new LongHashSet();
        try ( Transaction tx = db.beginTx() )
        {
            for ( String value : propertyValues )
            {
                expected.add( createNode( db, map( KEY, value ), label ).getId() );
            }
            tx.commit();
        }
        return expected;
    }

    private static void collectNodes( MutableLongSet bucket, ResourceIterator<Node> toCollect )
    {
        while ( toCollect.hasNext() )
        {
            bucket.add( toCollect.next().getId() );
        }
    }

    private static Node createNode( GraphDatabaseService beansAPI, Map<String,Object> properties, Label... labels )
    {
        try ( Transaction tx = beansAPI.beginTx() )
        {
            Node node = tx.createNode( labels );
            for ( Map.Entry<String,Object> property : properties.entrySet() )
            {
                node.setProperty( property.getKey(), property.getValue() );
            }
            tx.commit();
            return node;
        }
    }
}
