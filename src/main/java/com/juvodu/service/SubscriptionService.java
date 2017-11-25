package com.juvodu.service;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.juvodu.database.model.Subscription;

/**
 * TODO: description
 *
 * @author Juvodu
 */
public class SubscriptionService<T extends Subscription> extends GenericPersistenceService<T>{

    /**
     * Ctor
     *
     * @param persistenceClass
     *              defines model service works with to vary between dev and prod databases
     */
    public SubscriptionService(Class<T> persistenceClass){

        super(persistenceClass, DynamoDBMapperConfig.SaveBehavior.UPDATE);
    }
}