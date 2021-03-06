package com.juvodu.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.juvodu.database.model.Favorite;
import com.juvodu.database.model.User;
import com.juvodu.service.testmodel.FavoriteTestModel;
import com.juvodu.service.testmodel.UserTestModel;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 *  Test suite for the GenericPersistenceService
 *
 * @author Juvodu
 */
public class GenericPersistenceServiceTest {

    /** user table as an example with a simple hashkey */
    private static GenericPersistenceService<UserTestModel> persistenceServiceSimpleKey;

    /** favorite table as an example with a composite key (hashkey + rangekey) */
    private static GenericPersistenceService<FavoriteTestModel> persistenceServiceCompositeKey;

    @BeforeClass
    public static void beforeClass(){

        persistenceServiceSimpleKey = new GenericPersistenceService(UserTestModel.class, DynamoDBMapperConfig.SaveBehavior.UPDATE);
        persistenceServiceCompositeKey = new GenericPersistenceService(FavoriteTestModel.class, DynamoDBMapperConfig.SaveBehavior.UPDATE);
    }

    @Before
    public void before(){

        persistenceServiceSimpleKey.deleteAll();
        persistenceServiceCompositeKey.deleteAll();
    }


    @Test
    public void givenRecordWhenSaveThenSuccess(){

        //execute
        persistenceServiceSimpleKey.save(createRecordWithSimpleKey());
    }

    @Test
    public void givenSavedRecordWhenDeleteThenSuccess(){

        //setup
        UserTestModel record = createRecordWithSimpleKey();
        persistenceServiceSimpleKey.save(record);

        //execute
        persistenceServiceSimpleKey.delete(record);
    }

    @Test
    public void givenSavedRecordWhenGetHashKeyThenReturnRecord(){

        //setup
        UserTestModel record = createRecordWithSimpleKey();
        persistenceServiceSimpleKey.save(record);
        String id = record.getUsername();

        //execute
        User recordResult = persistenceServiceSimpleKey.getByHashKey(id);

        //verify
        assertNotNull(recordResult);
        assertEquals(id, recordResult.getUsername());
    }

    @Test
    public void givenSavedRecordWhenGetCompositeKeyThenReturnRecord(){

        //setup
        FavoriteTestModel record = new FavoriteTestModel();
        record.setUsername("user");
        record.setSpotId("spot");
        persistenceServiceCompositeKey.save(record);

        //execute
        Favorite favoriteResult = persistenceServiceCompositeKey.getByCompositeKey(record.getUsername(), record.getSpotId());

        //verify
        assertNotNull(favoriteResult);
        assertEquals("user", favoriteResult.getUsername());
        assertEquals("spot", favoriteResult.getSpotId());
    }

    @Test
    public void givenNonExistingRecordWhenGetByCompositeKeyThenReturnNull(){

        //setup
        FavoriteTestModel record  = new FavoriteTestModel();
        record.setUsername("user");
        record.setSpotId("spot");

        // execute
        FavoriteTestModel favoriteResult = persistenceServiceCompositeKey.getByCompositeKey(record.getUsername(), record.getSpotId());

        // verify
        assertNull(favoriteResult);
    }

    @Test
    public void given2SavedRecordsWhenGetAllRecordsThenReturnBoth(){

        //setup
        UserTestModel record1 = createRecordWithSimpleKey();
        record1.setUsername("user1");
        UserTestModel record2 = createRecordWithSimpleKey();
        record2.setUsername("user2");
        persistenceServiceSimpleKey.save(record1);
        persistenceServiceSimpleKey.save(record2);

        //execute
        List<UserTestModel> allRecords = persistenceServiceSimpleKey.findAll();

        //verify
        assertNotNull(allRecords);
        assertEquals(2, allRecords.size());
    }

    @Test
    public void givenExistingRecordWhenUpdateThenSuccess(){

        //setup
        UserTestModel record = createRecordWithSimpleKey();
        persistenceServiceSimpleKey.save(record);
        String id = record.getUsername();

        //execute
        persistenceServiceSimpleKey.save(record);

        //verify
        User recordResult = persistenceServiceSimpleKey.getByHashKey(record.getUsername());
        assertEquals(id, recordResult.getUsername());
    }

    /**
     * Helper function to create a record
     *
     * @return the created instance of the record
     */
    private UserTestModel createRecordWithSimpleKey(){

        UserTestModel record = new UserTestModel();
        record.setUsername("user");
        return record;
    }

}
