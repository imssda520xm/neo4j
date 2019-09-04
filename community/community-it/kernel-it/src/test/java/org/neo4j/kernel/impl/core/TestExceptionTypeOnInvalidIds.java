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
package org.neo4j.kernel.impl.core;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;
import org.neo4j.test.rule.TestDirectory;

import static org.junit.Assert.fail;
import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class TestExceptionTypeOnInvalidIds
{
    private static final long SMALL_POSITIVE_INTEGER = 5;
    private static final long SMALL_NEGATIVE_INTEGER = -5;
    private static final long BIG_POSITIVE_INTEGER = Integer.MAX_VALUE;
    private static final long BIG_NEGATIVE_INTEGER = Integer.MIN_VALUE;
    private static final long SMALL_POSITIVE_LONG = ((long) Integer.MAX_VALUE) + 1;
    private static final long SMALL_NEGATIVE_LONG = -((long) Integer.MIN_VALUE) - 1;
    private static final long BIG_POSITIVE_LONG = Long.MAX_VALUE;
    private static final long BIG_NEGATIVE_LONG = Long.MIN_VALUE;
    private static GraphDatabaseService graphdb;
    private static GraphDatabaseService graphDbReadOnly;
    private Transaction tx;

    @ClassRule
    public static final TestDirectory testDirectory = TestDirectory.testDirectory();
    private static DatabaseManagementService readOnlyService;
    private static DatabaseManagementService managementService;

    @BeforeClass
    public static void createDatabase()
    {
        var writableLayout = testDirectory.databaseLayout( testDirectory.storeDir( "writable" ) );
        var readOnlyLayout = testDirectory.databaseLayout( testDirectory.storeDir( "readOnly" ) );
        managementService = new TestDatabaseManagementServiceBuilder( writableLayout.databaseDirectory() ).build();
        graphdb = managementService.database( DEFAULT_DATABASE_NAME );
        DatabaseManagementService managementService1 =
                new TestDatabaseManagementServiceBuilder( readOnlyLayout.databaseDirectory() ).build();
        managementService1.shutdown();
        readOnlyService = new TestDatabaseManagementServiceBuilder( readOnlyLayout.databaseDirectory() ).
                setConfig( GraphDatabaseSettings.read_only, true ).build();
        graphDbReadOnly = readOnlyService.database( DEFAULT_DATABASE_NAME );
    }

    @AfterClass
    public static void destroyDatabase()
    {
        readOnlyService.shutdown();
        graphDbReadOnly = null;
        managementService.shutdown();
        graphdb = null;
    }

    @Before
    public void startTransaction()
    {
        tx = graphdb.beginTx();
    }

    @After
    public void endTransaction()
    {
        tx.close();
        tx = null;
    }

    /* behaves as expected */
    @Test( expected = NotFoundException.class )
    public void getNodeBySmallPositiveInteger()
    {
        getNodeById( SMALL_POSITIVE_INTEGER );
        getNodeByIdReadOnly( SMALL_POSITIVE_INTEGER );
    }

    /* throws IllegalArgumentException instead of NotFoundException */
    @Test( expected = NotFoundException.class )
    public void getNodeBySmallNegativeInteger()
    {
        getNodeById( SMALL_NEGATIVE_INTEGER );
        getNodeByIdReadOnly( SMALL_NEGATIVE_INTEGER );
    }

    /* behaves as expected */
    @Test( expected = NotFoundException.class )
    public void getNodeByBigPositiveInteger()
    {
        getNodeById( BIG_POSITIVE_INTEGER );
        getNodeByIdReadOnly( BIG_POSITIVE_INTEGER );
    }

    /* throws IllegalArgumentException instead of NotFoundException */
    @Test( expected = NotFoundException.class )
    public void getNodeByBigNegativeInteger()
    {
        getNodeById( BIG_NEGATIVE_INTEGER );
        getNodeByIdReadOnly( BIG_NEGATIVE_INTEGER );
    }

    /* throws IllegalArgumentException instead of NotFoundException */
    @Test( expected = NotFoundException.class )
    public void getNodeBySmallPositiveLong()
    {
        getNodeById( SMALL_POSITIVE_LONG );
        getNodeByIdReadOnly( SMALL_POSITIVE_LONG );
    }

    /* behaves as expected */
    @Test( expected = NotFoundException.class )
    public void getNodeBySmallNegativeLong()
    {
        getNodeById( SMALL_NEGATIVE_LONG );
        getNodeByIdReadOnly( SMALL_NEGATIVE_LONG );
    }

    /* throws IllegalArgumentException instead of NotFoundException */
    @Test( expected = NotFoundException.class )
    public void getNodeByBigPositiveLong()
    {
        getNodeById( BIG_POSITIVE_LONG );
        getNodeByIdReadOnly( BIG_POSITIVE_LONG );
    }

    /* finds the node with id=0, since that what the id truncates to */
    @Test( expected = NotFoundException.class )
    public void getNodeByBigNegativeLong()
    {
        getNodeById( BIG_NEGATIVE_LONG );
        getNodeByIdReadOnly( BIG_NEGATIVE_LONG );
    }

    /* behaves as expected */
    @Test( expected = NotFoundException.class )
    public void getRelationshipBySmallPositiveInteger()
    {
        getRelationshipById( SMALL_POSITIVE_INTEGER );
        getRelationshipByIdReadOnly( SMALL_POSITIVE_INTEGER );
    }

    /* throws IllegalArgumentException instead of NotFoundException */
    @Test( expected = NotFoundException.class )
    public void getRelationshipBySmallNegativeInteger()
    {
        getRelationshipById( SMALL_NEGATIVE_INTEGER );
        getRelationshipByIdReadOnly( SMALL_POSITIVE_INTEGER );
    }

    /* behaves as expected */
    @Test( expected = NotFoundException.class )
    public void getRelationshipByBigPositiveInteger()
    {
        getRelationshipById( BIG_POSITIVE_INTEGER );
        getRelationshipByIdReadOnly( BIG_POSITIVE_INTEGER );
    }

    /* throws IllegalArgumentException instead of NotFoundException */
    @Test( expected = NotFoundException.class )
    public void getRelationshipByBigNegativeInteger()
    {
        getRelationshipById( BIG_NEGATIVE_INTEGER );
        getRelationshipByIdReadOnly( BIG_NEGATIVE_INTEGER );
    }

    /* throws IllegalArgumentException instead of NotFoundException */
    @Test( expected = NotFoundException.class )
    public void getRelationshipBySmallPositiveLong()
    {
        getRelationshipById( SMALL_POSITIVE_LONG );
        getRelationshipByIdReadOnly( SMALL_POSITIVE_LONG );
    }

    /* behaves as expected */
    @Test( expected = NotFoundException.class )
    public void getRelationshipBySmallNegativeLong()
    {
        getRelationshipById( SMALL_NEGATIVE_LONG );
        getRelationshipByIdReadOnly( SMALL_NEGATIVE_LONG );
    }

    /* throws IllegalArgumentException instead of NotFoundException */
    @Test( expected = NotFoundException.class )
    public void getRelationshipByBigPositiveLong()
    {
        getRelationshipById( BIG_POSITIVE_LONG );
        getRelationshipByIdReadOnly( BIG_POSITIVE_LONG );
    }

    /* behaves as expected */
    @Test( expected = NotFoundException.class )
    public void getRelationshipByBigNegativeLong()
    {
        getRelationshipById( BIG_NEGATIVE_LONG );
        getRelationshipByIdReadOnly( BIG_NEGATIVE_LONG );
    }

    private void getNodeById( long index )
    {
        Node value = tx.getNodeById( index );
        fail( String.format( "Returned Node [0x%x] for index 0x%x (int value: 0x%x)",
                value.getId(), index, (int) index ) );
    }

    private void getNodeByIdReadOnly( long index )
    {
        Node value = tx.getNodeById( index );
        fail( String.format( "Returned Node [0x%x] for index 0x%x (int value: 0x%x)",
                value.getId(), index, (int) index ) );
    }

    private void getRelationshipById( long index )
    {
        Relationship value = tx.getRelationshipById( index );
        fail( String.format( "Returned Relationship [0x%x] for index 0x%x (int value: 0x%x)",
                value.getId(), index, (int) index ) );
    }

    private void getRelationshipByIdReadOnly( long index )
    {
        Relationship value = tx.getRelationshipById( index );
        fail( String.format( "Returned Relationship [0x%x] for index 0x%x (int value: 0x%x)",
                value.getId(), index, (int) index ) );
    }
}
